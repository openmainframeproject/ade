/*
 
    Copyright IBM Corp. 2010, 2016
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
package org.openmainframe.ade.impl.patches;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.impl.dataStore.TableManager;
import org.openmainframe.ade.impl.dbUtils.MyJDBCConnection;
import org.openmainframe.ade.impl.dbUtils.QueryStatementExecuter;
import org.openmainframe.ade.impl.utils.patches.SingleChainPatchManager;
import org.openmainframe.ade.utils.patches.IPatch;
import org.openmainframe.ade.utils.patches.IPatchManager;
import org.openmainframe.ade.utils.patches.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class containing members used for managing Ade version updates.
 */
public class AdePatches {

    /** default {@link IPatchManager} */
    private IPatchManager m_defaultPatchManager = null;

    /**
     * @return the default Ade {@link IPatchManager} 
     */
    public IPatchManager getDefaultPatchManager() {

        m_defaultPatchManager = new SingleChainPatchManager();

        return m_defaultPatchManager;
    }

    /**
     * This {@link IPatch} is used to add the {@link SQL#ADE_VERSIONS} table.
     * This is cruical in situations where the Ade DB has existed prior to
     * version 3.0.2 
     */
    public static class AdeMetaPatch implements IPatch {
        private Logger logger = LoggerFactory.getLogger(getClass());
        private final Version m_currentVersion;

        public AdeMetaPatch(Version currentVersion) {
            m_currentVersion = currentVersion;
        }

        @Override
        public void run() {
            logger.info("Executing meta patch");
            ConnectionWrapper cw = null;
            try {
                try {
                    cw = new ConnectionWrapper(MyJDBCConnection.getConnection());
                    cw.startTransaction();
                    TableManager.createTables(SQL.ADE_VERSIONS);
                    cw.executeDml("insert into " + SQL.ADE_VERSIONS + " (ADE_VERSION, PATCHED_TIME) values ('" + m_currentVersion + "', current timestamp)");
                } catch (Exception e) {
                    if (cw != null) {
                        cw.rollback();
                    }
                    throw e;
                } finally {
                    if (cw != null) {
                        cw.close();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed applying meta patch", e);
            }
            logger.info("Successfuly applied meta patch");
        }

        @Override
        public Version toVersion() {
            return null;
        }

        @Override
        public Version fromVersion() {
            return null;
        }
    }

    /**
     * the Ade {@link Version} from the DB with the latest timestamp
     */
    private Version m_currentVersion = null;

    /**
     * Retrieves the latest Ade version from the DB
     * @throws AdeException 
     */
    public Version getCurrentVersion() throws AdeException {
        if (m_currentVersion == null) {
            (new QueryStatementExecuter("select ADE_VERSION from " + SQL.ADE_VERSIONS
                    + " where PATCHED_TIME = (select max(PATCHED_TIME) from " + SQL.ADE_VERSIONS + ")") {
                @Override
                protected void handleResultSet(ResultSet rs) throws SQLException,
                        AdeException {
                    final String versionStr = rs.getString(1);
                    try {
                        m_currentVersion = Version.parse(versionStr);
                    } catch (IllegalArgumentException e) {
                        throw new AdeInternalException("Failed parsing Ade " 
                                + "version from DB", e);
                    }
                }
            }).executeQuery();
        }
        if (m_currentVersion == null) {
            throw new AdeInternalException("Failed loading current DB version from DB");
        }
        return m_currentVersion;
    }

}
