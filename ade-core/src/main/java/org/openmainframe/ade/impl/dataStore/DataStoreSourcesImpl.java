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
package org.openmainframe.ade.impl.dataStore;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.data.SourceMetaData;
import org.openmainframe.ade.dataStore.IDataStoreGroups;
import org.openmainframe.ade.dataStore.IDataStoreSources;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.dbUtils.PreparedStatementWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.SourceImpl;
import org.openmainframe.ade.impl.dbUtils.DbDictionary;
import org.openmainframe.ade.impl.dbUtils.QueryStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.TableGeneralUtils;

/**
 * This is the implementation class for the "SOURCES" data store.
 * It stores and retrieves data associated with a source.
 * 
 */
public class DataStoreSourcesImpl implements IDataStoreSources {

    protected DbDictionary m_dictionary;
    protected Map<String, String> m_src2AnalysisGrpMap;

    private static final int MINUTE_IN_MILLIS = 60000;
    
    /**
     * The default unassigned analysis group id. Note, this is NOT the internal id in the GROUPS table.
     */
    private static final int UNASSIGNED_ANALYSIS_GROUP_ID = -1;

    /**
     * The default unassigned analysis group name.
     */
    private static final String UNASSIGNED_GROUP = "UNASSIGNED";

    private long m_minRefreshTime = MINUTE_IN_MILLIS;

    DataStoreSourcesImpl(AdeDictionaries dictionaries) throws AdeException {
        m_dictionary = dictionaries.getSourceIdDictionary();
        m_src2AnalysisGrpMap = dictionaries.getSrc2AnalysisGrpMap();
        m_minRefreshTime = Ade.getAde().getConfigProperties().getSourcesMinRefreshTime();

    }

    /**
     * This method creates a new Source or retrieves an old one from the data store.
     * 
     * @param  sourceID         The string representation of a source, the entity that produces 
     *                          log messages.
     * @return Source           The entity that produces log messages
     * @throws AdeException  Throws a AdeException
     *                             
     */
    @Override
    public final ISource getOrAddSource(String sourceId) throws AdeException {
        refreshDictionaryIfNeeded();
        final int sourceInternalId = m_dictionary.addWord(sourceId);
        return new SourceImpl(sourceInternalId);
    }

    /**
     * This method refreshes the data dictionary for a source if the minimum refresh
     * time has been exceeded.
     * @throws AdeException Throws a AdeException
     */
    private void refreshDictionaryIfNeeded() throws AdeException {
        if (System.currentTimeMillis() - m_dictionary.getLastRefreshTime() > m_minRefreshTime) {
            m_dictionary.refresh();
            m_src2AnalysisGrpMap.clear();
        }
    }

    /**
     * This method gets a source for the given sourceId from the data store.
     * 
     * @param  sourceID        The string representation in the data store of a source
     * @return Source          The entity that produces log messages.
     * @throws AdeException Throws a AdeException
     */
    @Override
    public final ISource getSource(String sourceId) throws AdeException {
        m_dictionary.refresh();
        m_src2AnalysisGrpMap.clear();
        final int sourceInternalId = m_dictionary.getWordId(sourceId);
        if (sourceInternalId == DbDictionary.InvalidID) {
            return null;
        }
        return new SourceImpl(sourceInternalId);
    }

    /**
     * This method deletes the requested source from the data store.
     * @param source           The entity that produces log messages
     * @throws AdeException Throws a AdeException
     */
    @Override
    public final void deleteSource(ISource source) throws AdeException {
        m_dictionary.delete(source.getSourceInternalId());
    }

    /**
     * This method retrieves ALL of the sources from the data store.
     * @return                 A collection of source, which are the entities that
     *                         produce log messages.
     * @throws AdeException Throws a AdeException
     */
    @Override
    public final Collection<ISource> getAllSources() throws AdeException {
        m_dictionary.refresh();
        m_src2AnalysisGrpMap.clear();
        final List<ISource> result = new ArrayList<ISource>();
        for (int id : m_dictionary.getIds()) {
            result.add(new SourceImpl(id));
        }
        return result;
    }

    /**
     * This method retrieves the source for the input sourceInternalId.
     * @param  sourceInternalId The integer representation of a source
     * @return source           The entity that produces log messages
     */
    public ISource getSourceByInternalId(int sourceInternalId) {

        return new SourceImpl(sourceInternalId);

    }

    /**
     * This method sets the analysisGroup for the given source to that which is input.
     * 
     * @param  source          The entity that produces log messages                        
     * @param  analysisGroup   This is the analysisGroup that is being requested to be set 
     *                         for the input source.
     *                         An analysis group contains sources that will be
     *                         trained together and will use the same model for analysis.
     * @throws AdeException Throws a AdeException
     */
    @Override
    public final void resetAnalysisGroup(String source, int analysisGroup) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        try {
            cw.startTransaction();
            TableGeneralUtils.executeDml("update " + SQL.SOURCES + " set ANALYSIS_GROUP=" + analysisGroup
                    + " where SOURCE_ID='" + source + "'");
            final String groupName = GroupRead.getAnalysisGroupName(analysisGroup);
            m_src2AnalysisGrpMap.put(source, groupName);
            cw.endTransaction();
            cw.close();
            return;

        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
    }

    /** 
     * This method sets the given meta data for the given "source".
     * @param source           The unique representation of a Anomaly Detection Engine client in the data store
     * @param souceMetaData    The data that describes the input source
     * @throws AdeException Throws a AdeException 
     */
    @Override
    public final void setSourceMetaData(ISource source, SourceMetaData sourceMetaData)
            throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        try {
            final PreparedStatementWrapper psw = cw.preparedStatement("update " + SQL.SOURCES 
                    + " set file_name=?,analysis_group=?,log_type=? where source_internal_id=?");
            final PreparedStatement ps = psw.getPreparedStatement();
            int pos = 1;
            ps.setString(pos++, sourceMetaData.m_fileName);
            ps.setInt(pos++, sourceMetaData.m_analysisGroupId);
            ps.setString(pos++, sourceMetaData.m_logType);
            ps.setInt(pos++, source.getSourceInternalId());
            ps.execute();
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
    }

    /** 
     * This method gets the meta data for the requested "source" or client.
     * @param  source          The entity that produces log messages
     * @return SouceMetaData   The data that describes the input source or client
     * @throws AdeException Throws a AdeException 
     * @throws AdeInternalException if an unexpected result was received from 
     *                                 the attempt to get metadata for a source.
     */
    @Override
    public final SourceMetaData getSourceMetaData(ISource source) throws AdeException {
        SourceMetaData smd = null;
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        try {
            final PreparedStatementWrapper psw = cw.preparedStatement("select log_type,analysis_group,file_name from " 
                    + SQL.SOURCES + " where source_internal_id=?");
            psw.getPreparedStatement().setInt(1, source.getSourceInternalId());
            final ResultSet rs = psw.executeQuery();
            if (rs.next()){
                String logType = rs.getString(1);
                int analysisGroup = rs.getInt(2);
                String fileName = rs.getString(3);
                smd = new SourceMetaData(source, logType, analysisGroup, fileName);
            }
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        return smd;
    }

    /**
     * This method determines whether or not the input sourceID is associated with
     * a source 
     * @param  sourceId  the string representation of a source 
     * @return true if the input sourceID is associated with a source in the data store, 
     *         false if an associated source is not found.
     * @throws AdeException Throws a AdeException
     */
    @Override
    public final boolean hasSource(String sourceId) throws AdeException {
        refreshDictionaryIfNeeded();
        return m_dictionary.getWordId(sourceId) != DbDictionary.InvalidID;
    }

    /**
     * This method returns the analysis group associated with the input source.
     * @param  source          The entity that produces log messages
     * @return analysisGroup   The group internal id that contains sources that will be trained together 
     * and will use the same model for analysis.
     * @throws AdeException Throws a AdeException
     * @throws AdeInternalException  Thrown if, when trying to get the source from
     *                                  the datastore, the source does not exist.
     */
    @Override
    public final String getAnalysisGroup(String source) throws AdeException {
        refreshDictionaryIfNeeded();
        String analysisGroup;
        analysisGroup = m_src2AnalysisGrpMap.get(source);
        if (analysisGroup != null) {
            return analysisGroup;
        }

        final ISource src = getSource(source);
        if (src == null) {
            throw new AdeInternalException("The source: '" + source + "' does not exist in the Data Store!");
        }
        final SourceMetaData metaData = getSourceMetaData(src);
        int analysisGroupId = metaData.m_analysisGroupId;
        if (analysisGroupId == 0) {
            return null;
        }
        analysisGroup = metaData.getAnalysisGroupName();
        m_src2AnalysisGrpMap.put(source, analysisGroup);
        return analysisGroup;
    }

    /**
     * This method adds a source to an analysis group.
     * @param source This is the sourceId associated with the source
     * @param analysisGroup Analysis group internal ids of those sources that will be trained together and 
     * will use the same model for analysis. Note, in the unassigned case, this will be the unassigned group
     * id which is NOT the group internal id.
     * @throws AdeException Throws a AdeException
     * @throws AdeInternalException Thrown if an attempt was made to set an analysis group 
     * when a different one was already set for this source.
     */
    @Override
    public final void setAnalysisGroup(String source, int analysisGroup) throws AdeException {
        String curAnalysisGroup = null;
        String groupName;        
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        try {
            cw.startTransaction();
            cw.lockTableShare(SQL.SOURCES);
            // check to see if an analysis group is already set.
            curAnalysisGroup = getAnalysisGroup(source);
            cw.endTransaction();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        if (analysisGroup == UNASSIGNED_ANALYSIS_GROUP_ID){
            groupName = UNASSIGNED_GROUP;
        } else{
            groupName = GroupRead.getAnalysisGroupName(analysisGroup);
        }        
        if (curAnalysisGroup != null) {
            // if the existing analysis group differs from the current one,
            // throw an exception.
            if (!curAnalysisGroup.equals(groupName)) {
                throw new AdeInternalException("For source: " + source 
                        + " - Trying to set analysis group: " + analysisGroup + " while a different one is already set: "
                        + curAnalysisGroup);
            }
            // otherwise, we do not need to do anything, as the existing
            // analysis group matches the input analysis group (The cache
            // is updated in the getAnalysisGroup method).
            return;
        }

        try {
            cw.startTransaction();
            cw.lockTableExclusive(SQL.SOURCES);
            curAnalysisGroup = getAnalysisGroup(source);
            if (curAnalysisGroup != null) {
                // if the existing analysis group differs from the current one,
                // throw an exception.
                if (!curAnalysisGroup.equals(groupName)) {
                    throw new AdeInternalException("For source: " + source + " - Trying to set analysis group: " 
                                                      + analysisGroup + " while a different one is already set: " 
                                                      + curAnalysisGroup);
                }
                // otherwise, we do not need to do anything, as the existing
                // analysis group matches the input analysis group (The cache
                // is updated in the getAnalysisGroup method).
                return;
            }
            // else 
            int analysisGroupInternalId = -1;
            if (analysisGroup == UNASSIGNED_ANALYSIS_GROUP_ID){
                final IDataStoreGroups dataStoreGroups = Ade.getAde().getDataStore().groups();
                analysisGroupInternalId = dataStoreGroups.insertUnassignedGroup(analysisGroup);
            }
            TableGeneralUtils.executeDml("update " + SQL.SOURCES + " set ANALYSIS_GROUP=" + analysisGroupInternalId + " where SOURCE_ID='" + source + "'");
            groupName = GroupRead.getAnalysisGroupName(analysisGroupInternalId);
            m_src2AnalysisGrpMap.put(source, groupName);
            cw.endTransaction();
            cw.close();
            return;
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }

    }

    /**
     * This method gets all the analysis groups for all sources in the data store.
     * @return set of analysis group internal ids.
     * @throws AdeException Throws a AdeException 
     */
    @Override
    public final Set<Integer> getAllAnalysisGroups() throws AdeException {
        final Set<Integer> analysisGroups = new TreeSet<Integer>();

        new QueryStatementExecuter("select distinct ANALYSIS_GROUP from " + SQL.SOURCES) {

            @Override
            protected void handleResultSet(ResultSet rs) throws SQLException,
                    AdeException {
                final int analysisGroup = rs.getInt("ANALYSIS_GROUP");
                analysisGroups.add(analysisGroup);
            }
        }.executeQuery();

        return analysisGroups;
    }

    /**
     * This method gets all the sources that are part of the input analysis group.
     * @param analysisGroup the group internal id of those sources that will be trained together.
     * @return set of sourceIDs (a string representation of a source or entity that produces log message).
     * @throws AdeException
     */
    @Override
    public final Set<ISource> getSourcesForAnalysisGroup(int analysisGroup) throws AdeException {
        final Set<ISource> sources = new TreeSet<ISource>();

        new QueryStatementExecuter("select distinct SOURCE_INTERNAL_ID from " + SQL.SOURCES
                + " where ANALYSIS_GROUP=" + analysisGroup) {

            @Override
            protected void handleResultSet(ResultSet rs) throws SQLException, AdeException {
                final int sourceInternalId = rs.getInt("SOURCE_INTERNAL_ID");
                sources.add(new SourceImpl(sourceInternalId));
            }
        }.executeQuery();
        return sources;
    }

    /**
     * This method creates a source if one doesn't exist, then adds the analysis group to it.
     * @param sourceID The string the string representation of a source
     * @param analysisGroup Contains group ids of the sources that will be trained together and uses 
     * the same model for analysis.
     * @throws AdeException Throws a AdeException
     */
    @Override
    public final void addSourceAndAnalysisGroup(String sourceId, int analysisGroup)
            throws AdeException {

        if (!hasSource(sourceId)) {
            getOrAddSource(sourceId);
            setAnalysisGroup(sourceId, analysisGroup);
        }

    }

}
