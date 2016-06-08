/*
 
    Copyright IBM Corp. 2012, 2016
    This file is part of Anomaly Detection Engine for Linux Logs (ADE).

    ADE is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ADE is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ADE.  If not, see <http://www.gnu.org/licenses/>.
 
*/
package org.openmainframe.ade.ext.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.dbUtils.DriverType;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.ext.utils.AtomicTransaction;
import org.openmainframe.ade.ext.utils.ExtDataStoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds the additional system-informational parameters which were provided
 * (as required) on a given action (e.g., Upload, Analyze).
 *
 * It is also charged with keeping their representations up-to-date in the analytics
 * database.  The logic for "when and what to update"  is specialized.
 *
 * The 2 Constructors are called for data from 2 very different origins:
 * - The "parameters" one where the values originate from the given action (e.g., Upload)
 * - The "database" one where the values originate from pre-existing db tables' rows,
 *   produced by "this" instance as its "alter ego"
 *
 * Notice that "the missing piece" is the Source, which provides the <plex-sys> name,
 * as well as a potential db lookup key.  For example, UploadSysInfo processing could be
 * working against a log file which contains messages from multiple systems.  In that scenario,
 * UploadSysInfo would instantiate one ManagedSystemInfo object, then use it repeatedly with
 * each of the systems (Sources) whose messages it encounters.  In other words, though not
 * instantiated with a Source, an instance of this class is useless unless used in conjunction
 * with an instance of Source (since only then is a full Managed System context realized).
 *
 *
 */
public class ManagedSystemInfo {

    private static final Logger logger = LoggerFactory.getLogger(ManagedSystemInfo.class);

    private int m_gmtOffset;
    private String m_osName;
    private ManagedSystemInfo m_dbqueryManagedSystem = null;

    public ManagedSystemInfo(long gmtOffset, String osName) {
        m_osName = osName;
        m_gmtOffset = (int) gmtOffset;

        logger.trace(String.format("ManagedSystemInfo CTOR( gmt=%d, osName=%s )...",
                m_gmtOffset, m_osName));
    }

    /**
     * Internal CTOR.
     *
     * Values come from database row in MANAGED_SYSTEMS table.
     * Vacant values are moot, as they will be set unconditionally
     * from external values.
     *
     * @param gmtOffset
     * @param osName
     */
    private ManagedSystemInfo(int gmtOffset,String osName) {

        if ((osName == null) || (osName.length() == 0)) {
            m_osName = "";
        } else {
            m_osName = osName.trim();
        }
        
        m_gmtOffset = gmtOffset;


        logger.trace(String.format("ManagedSystemInfo CTOR( gmt=%d, osName=%s )...",
                m_gmtOffset, m_osName));
    } 

    public final long getGmtOffset() {
        return (long) m_gmtOffset;
    } 

    public final String getOsName() {
        return m_osName;
    } 

    /**
     * The gmtOffset (in hours) can come two places: 
     * 1) the -g option
     * 2) the date and time from the message logs
     * @param gmtOffset
     */
    public final void setGmtOffset(long gmtOffset) {
        this.m_gmtOffset = (int) gmtOffset;
    } 

    /**
     * This method is the "front door", the primary interface that a caller has
     * to making updates to the analytic database to ensure that it is up to date
     * in its information about managed systems.
     *
     * @param ISource - datastore object which is associated 1:1 with a SOURCES Table row
     * @throws AdeException
     */
    public final void updateDataStore(ISource S) throws AdeException {

        logger.info(String.format("updateDataStore(%s) -->entry",
                S.getSourceId())); 
        try {
            final UpdateManagedSystemAtomicTransaction atomicTrans = new UpdateManagedSystemAtomicTransaction(S);
            atomicTrans.execute();
        } catch (AdeException e) {
            throw e;
        } catch (Throwable t) {
            final String msg = String.format("updateDataStore(%s) caught unexpected throwable: %s",
                    S.getSourceId(), t.getMessage()); 
            logger.info(msg); 
            throw new AdeInternalException(msg, t); 
        }
        logger.info("updateDataStore() <--exit");

    } 

    private class UpdateManagedSystemAtomicTransaction extends AtomicTransaction {
        private ISource source;
        private boolean mySQL;

        public UpdateManagedSystemAtomicTransaction(ISource source) throws AdeException {
            super();
            this.source = source;
            final String driver = Ade.getAde().getConfigProperties().database().getDatabaseDriver();

            if ((DriverType.parseDriverType(driver) == DriverType.MY_SQL) ||
                (DriverType.parseDriverType(driver) == DriverType.MARIADB))
                mySQL = true;
            else
                mySQL = false;
        }

        public boolean execute() throws AdeException {
            return ExtDataStoreUtils.executeAtomicTransaction(this);
        }

        @Override
        public boolean performAtomicTransaction() throws AdeException {
            try {
		if (mySQL) 
                    execute("LOCK TABLES " + GroupsQueryImpl.MANAGED_SYSTEMS_TABLE + " WRITE");
		else
                    execute("LOCK TABLE " + GroupsQueryImpl.MANAGED_SYSTEMS_TABLE + " IN EXCLUSIVE MODE");

                m_dbqueryManagedSystem = lookupManagedSystemInfo(source);
                if (m_dbqueryManagedSystem == null) {
                    addManagedSystem(source);
                } else {
                    updateManagedSystem(source);
                }
            } catch (SQLException e) {
                logger.error("Error encountered executing the transaction.", e);
            }
            if (mySQL) {
                try {
                    execute("UNLOCK TABLES");
                } catch (SQLException e) {
                    logger.error("Error encountered unlocking the table.", e);
                }
            }

            return true;
        }

        /**
         * Query the analytics database to obtain the current managed system-related information
         * for the managed system designated by the input Source.  If there is such information
         * in the database, return a second instance of the current class informed by those values,
         * so that the caller knows that it should pursue a "read and perhaps modify database"
         * logic path, based on comparing "this" instance with "that" (i.e., to determine what, if
         * any, changes are to be made in the database).
         *
         * Of course, there may well be no such information, as this may be driven on a "first
         * encounter", in which case we return a null "that", and the caller knows that it should
         * pursue a "create various database rows" logic path.
         *
         * SPECIAL NOTE:  Practice shows that none of these column values can really be
         *                trusted to have content.  These are set/reset by various
         *                untrusted parties (e.g., bulk load).  It is imperative that
         *                a null value be handled and that a non-null object M be
         *                returned so that the caller is not mislead into thinking that
         *                they need to do a (erroneous, redundant) database ADD operation
         *                when in fact the designated ManagedSystem exists in the database
         *                and it is really an UPDATE operation that should be performed.
         *                Hence the super-paranoid try catch logic.
         *
         * @param ISource - datastore Source object which is associated with a SOURCES Table row,
         *                 and *may* already be associated with a MANAGED_SYSTEMS Table row
         * @return instance of ManagedSystemInfo, or <null>
         * @throws AdeException                                                         
         */
        private ManagedSystemInfo lookupManagedSystemInfo(ISource S) throws AdeException {

            ManagedSystemInfo M = null;
            final String MANAGED_QUERY = String.format("SELECT * FROM MANAGED_SYSTEMS WHERE SOURCE_INTERNAL_ID=%s",
                    S.getSourceInternalId());
            ResultSet R = null;
            String osName;
            int gmtOffset;

            logger.trace(String.format("lookupManagedSystemInfo(%s) -->entry",
                    S.getSourceId())); 
            try {
                R = executeScrollInsensitiveQuery(MANAGED_QUERY);

                /********************************************************************************
                 * Here we make a vital distinction between a null result (query malfunction),
                 * and an empty result (query successful, managed system not found).  In the
                 * former case, we throw to prevent the caller from wrongly concluding that it
                 * should add a new (spurious) MANAGED_SYSTEMS row to the database.
                 *******************************************************************************/
                if (R == null) {
                    final String msg = String.format("lookupManagedSystemInfo(%s) - unexpected null (ResultSet)",
                            S.getSourceId()); 
                    logger.error(msg); 
                    throw new AdeInternalException(msg); 
                }

                if (ExtDataStoreUtils.nonemptyQueryResult(R)) {
                    try {
                        R.first(); 
                        gmtOffset = R.getInt("GMT_OFFSET"); 
                        osName = R.getString("OPERATING_SYSTEM"); 
                        M = new ManagedSystemInfo(gmtOffset, osName); 
                    } catch (Throwable t) {
                        final String msg = String.format("lookupManagedSystemInfo(%s) - (ResultSet) extract error",
                                S.getSourceId()); 
                        logger.error(msg); 
                        throw new AdeInternalException(msg, t);
                    //* end catch inner try
                    }
                //* end if nonempty ResultSet
                } 
            } catch (AdeException e) {
                throw e;
            } catch (Throwable t) {
                final String msg = String.format("lookupManagedSystemInfo(%s) caught unexpected throwable: %s",
                        S.getSourceId(), t.getMessage()); 
                logger.error(msg); 
                throw new AdeInternalException(msg, t); 
            } finally {
                if (R != null) {
                    // Nothing to be done here if the result set can't close
                    try {
                        R.close();
                    } catch (SQLException ex) {
                        logger.error("Error encountered closing the ResultSet.", ex);
                    }
                }
            }

            logger.trace(String.format("lookupManagedSystemInfo(%s) <--exit",
                    S.getSourceId())); 
            return M;
        } 

        /**
         * Add a new row to the Analytics Database' "MANAGED_SYSTEMS" table which reflects the
         * input managed system info.
         *
         * NOTE:
         * In the most subtle of cases, these rows may already exist *and* be marked as "obsolete"
         * by the earlier setting of their END_TIME column, and in this case our new reference should
         * "reinstate" them.  Since managing this setting is somewhat difficult to do with perfect
         * precision, we opt instead for simply unconditionally clearing their END_TIME each time we
         * reference them.
         *
         * @param ISource - datastore object from is associated with a SOURCES Table row and
         *                 not yet associated with a MANAGED_SYSTEMS Table row
         * @throws AdeException                                                           
         */
        private void addManagedSystem(ISource S) throws AdeException {

            logger.trace(String.format("addManagedSystem(%s) -->entry",
                    S.getSourceId())); 
            boolean addOk;

            try {
                logger.info(String.format("addManagedSystem(%s) -->executeBatch",
                        S.getSourceId())); 
                addOk = executeBatch(getSqlForAdd(S)); 
            } catch (AdeException e) {
                throw e;
            } catch (Throwable t) {
                final String msg = String.format("addManagedSystem(%s) caught unexpected throwable: %s",
                        S.getSourceId(), t.getMessage()); 
                logger.error(msg); 
                throw new AdeInternalException(msg, t); 
            }
            if (!addOk) {
                final String msg = String.format("addManagedSystem(%s) unexpected (sql) failure",
                        S.getSourceId()); 
                logger.error(msg); 
                throw new AdeInternalException(msg); 
            }
            logger.trace("addManagedSystem() <-- exit");

        } 
        
        
        /**
         * Drive any database update(s) determined by a comparison of "this" (the parameter
         * values which inform this ManagedSystemInfo instance) with the database (the
         * ManagedSystemInfo instance informed by the values found in the database query).
         *
         * @param ISource - datastore object which is associated with a SOURCES Table row
         *                 and also with a MANAGED_SYSTEMS Table row
         * @throws AdeException                                                     
         */
        private void updateManagedSystem(ISource S) throws AdeException {

            logger.trace(String.format("updateManagedSystem(%s) -->entry",
                    S.getSourceId())); 
            boolean updateOk;
            try {
                logger.info(String.format("updateManagedSystem(%s) -->executeBatch",
                        S.getSourceId()));
                updateOk = executeBatch(getSqlForUpdate(S)); 
                if (updateOk) {
                    checkForObsoleteReferences();
                } else {
                    final String msg = String.format("updateManagedSystem(%s) - unexpected (sql) failure",
                            S.getSourceId()); 
                    logger.error(msg); 
                    throw new AdeInternalException(msg); 
                } 
            } catch (AdeException e) {
                throw e;
            } catch (Throwable t) {
                final String msg = String.format("updateManagedSystem(%s) caught unexpected throwable: %s",
                        S.getSourceId(), t.getMessage()); 
                logger.error(msg); 
                throw new AdeInternalException(msg, t); 
            }
            logger.trace("updateManagedSystem() <-- exit");

        }

        /**
         * Return an array of SQL dml statements which will perform the insertion
         * of a new MANAGED_SYSTEMS Table row
         *
         * @param ISource - datastore object which is associated with a SOURCES Table row
         * @return ArrayList<String> as described above
         */
        private ArrayList<String> getSqlForAdd(ISource S) {

            logger.trace("getSqlForAdd() -->entry");

            final ArrayList<String> addList = new ArrayList<String>();

            final String MANAGED_COLUMNS = String.format("%s, %s, %s",
                    "SOURCE_INTERNAL_ID", 
                    "GMT_OFFSET", "OPERATING_SYSTEM");
            final String MANAGED_VALUES = String.format("%s, %s, '%s'",
                    S.getSourceInternalId(),
                    m_gmtOffset,
                    m_osName);
            final String MANAGED_INSERT = String.format("INSERT INTO MANAGED_SYSTEMS ( %s ) VALUES ( %s )",
                    MANAGED_COLUMNS, MANAGED_VALUES);

            addList.add(MANAGED_INSERT);

            logger.trace("getSqlForAdd() <-- exit");
            return addList;

        } 

        /**
         * Look for the differences between the input managed system info fields
         * and the equivalent column values extracted from the analytics database
         * via earlier SQL query, available to us now thru getter() methods of the
         * ManagedSystemInfo instance constructed from them.
         *
         * Build the appropriate sql UPDATE statement(s) to set the new value(s)
         * in the appropriate column(s) of the appropriate table row(s).
         *
         * @param ISource - datastore object which is associated with a SOURCES Table row
         *                 and also wth a MANAGED_SYSTEMS Table row
         * @return ArrayList<String> as described above
         */
        private ArrayList<String> getSqlForUpdate(ISource S) {

            final List<String> msSets = new ArrayList<String>();
            String SET_STRING, WHERE_STRING, UPDATE_STRING;
            final ArrayList<String> updates = new ArrayList<String>();

            logger.trace("getSqlForUpdate() --> entry");
            /************************************************************************
             * Might as well do conditionally, since we have the current db values.
             ***********************************************************************/
            if (m_gmtOffset != m_dbqueryManagedSystem.getGmtOffset()) {
                msSets.add(String.format("GMT_OFFSET=%d", m_gmtOffset));
            }
            if (!m_osName.equals(m_dbqueryManagedSystem.getOsName())) {
                msSets.add(String.format("OPERATING_SYSTEM='%s'", m_osName));
            }

            // only perform the update if there is something to update
            if (!msSets.isEmpty()) {
                /****************************************
                 * Form the full sql update statements
                 ****************************************/
                SET_STRING = StringUtils.join(msSets,',');
            //* loop thru list
                WHERE_STRING = "SOURCE_INTERNAL_ID=" + S.getSourceInternalId();
                UPDATE_STRING = String.format("UPDATE MANAGED_SYSTEMS SET %s WHERE %s", SET_STRING, WHERE_STRING);
                updates.add(UPDATE_STRING);
            }

            logger.trace("getSqlForUpdate() <-- exit");
            return updates;
        } 

        /**
         * Following an update to a MANAGED_SYSTEMS row, other rows in other database tables
         * may now "dangle" (i.e., no longer be referenced by anyone).
         *
         * In those cases, we would like to preserve such rows, but mark them as "obsolete"
         * by setting their END_TIME column (to the approximate current time).
         *
         *
         */
        private void checkForObsoleteReferences() throws AdeException {

            logger.trace("checkForObsoleteReferences() -->entry");

            logger.trace("checkForObsoleteReferences() <-- exit");
        } 
    }
    
} 
