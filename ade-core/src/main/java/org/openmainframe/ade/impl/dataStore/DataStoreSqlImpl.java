/*
 
    Copyright IBM Corp. 2009, 2016
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

import java.sql.Connection;
import java.sql.SQLException;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.dataStore.IDataStore;
import org.openmainframe.ade.dataStore.IDataStoreDictionaryApi;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.AdeConfigPropertiesImpl;
import org.openmainframe.ade.impl.data.PeriodSummary;
import org.openmainframe.ade.impl.dbUtils.DbDictionary;
import org.openmainframe.ade.impl.dbUtils.MyJDBCConnection;
import org.openmainframe.ade.impl.flow.IntervalByPeriodSummaryDbIterator;
import org.openmainframe.ade.impl.patches.AdePatches;
import org.openmainframe.ade.impl.scoringApi.MainScorerImpl;
import org.openmainframe.ade.scoringApi.IMainScorer;
import org.openmainframe.ade.utils.patches.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic implementation of the {@link IDataStore} interface
 * It stores a list of logs handles in the data store identified by {@link LogID} 
 *
 */
public class DataStoreSqlImpl implements IDataStore {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private final AdeDictionaries a_adeDictionary;
    private final DataStoreSourcesImpl m_dataStoreSources;
    private final DataStorePeriodsImpl m_dataStorePeriods;
    private final DataStoreGroupsImpl m_dataStoreGroups;
    private final DataStoreRulesImpl m_dataStoreRules;
    private final DataStorePeriodSummaries m_dataStorePeriodSummaries;
    private final DataStoreModelsImpl<IMainScorer> m_dataStoreModels;
    private final DataStoreUserImpl m_dataStoreUser;
    private final DataStoreTextClusteringModelsImpl m_dataStoreTextClustering;
    private final Ade ade;

    public DataStoreSqlImpl(Ade ade, AdeDictionaries adeDictionaries, DataStoreSourcesImpl dataStoreSources,
            DataStorePeriodsImpl dataStorePeriods, DataStoreGroupsImpl dataStoreGroups, DataStoreRulesImpl dataStoreRules,
            DataStorePeriodSummaries dataStorePeriodSummaries, DataStoreModelsImpl<IMainScorer> dataStoreModels, 
            DataStoreUserImpl dataStoreUser, DataStoreTextClusteringModelsImpl dataStoreTextClustering, 
            boolean create) throws AdeException {
        MyJDBCConnection.initConnection(create);
        if (create) {
            createTables();
        }
        assertCompatibleDb();

        this.ade = ade;
        this.a_adeDictionary = adeDictionaries;
        this.m_dataStoreSources = dataStoreSources;
        this.m_dataStorePeriods = dataStorePeriods;
        this.m_dataStoreGroups = dataStoreGroups;
        this.m_dataStoreRules = dataStoreRules;
        this.m_dataStorePeriodSummaries = dataStorePeriodSummaries;
        this.m_dataStoreModels = dataStoreModels;
        this.m_dataStoreUser = dataStoreUser;
        this.m_dataStoreTextClustering = dataStoreTextClustering;
    }

    public DataStoreSqlImpl(boolean create) throws AdeException {
        MyJDBCConnection.initConnection(create);
        if (create) {
            createTables();
        }
        assertCompatibleDb();

        this.ade = Ade.getAde();
        final DbDictionary messageIds = new DbDictionary(SQL.MESSAGE_IDS, "MESSAGE_INTERNAL_ID", "MESSAGE_ID");
        final DbDictionary componentIds = new DbDictionary(SQL.COMPONENT_IDS, "COMPONENT_INTERNAL_ID", "COMPONENT_ID");
        final DbDictionary systemIds = new DbDictionary(SQL.SOURCES, "SOURCE_INTERNAL_ID", "SOURCE_ID");
        a_adeDictionary = new AdeDictionaries(messageIds, componentIds, systemIds);
        m_dataStorePeriods = new DataStorePeriodsImpl();
        m_dataStoreGroups = new DataStoreGroupsImpl();
        m_dataStoreRules = new DataStoreRulesImpl();
        m_dataStorePeriodSummaries = new DataStorePeriodSummaries(m_dataStorePeriods);
        m_dataStoreModels = new DataStoreModelsImpl<IMainScorer>(SQL.MODELS, new MainScorerImpl.FileHandler());
        m_dataStoreUser = new DataStoreUserImpl();
        m_dataStoreSources = new DataStoreSourcesImpl(getAdeDictionaries());
        m_dataStoreTextClustering = new DataStoreTextClusteringModelsImpl();
    }

    public DataStoreSqlImpl(Connection conn) throws AdeException {
        MyJDBCConnection.setConnection(conn);
        assertCompatibleDb();

        this.ade = Ade.getAde();
        final DbDictionary messageIds = new DbDictionary(SQL.MESSAGE_IDS, "MESSAGE_INTERNAL_ID", "MESSAGE_ID");
        final DbDictionary componentIds = new DbDictionary(SQL.COMPONENT_IDS, "COMPONENT_INTERNAL_ID", "COMPONENT_ID");
        final DbDictionary systemIds = new DbDictionary(SQL.SOURCES, "SOURCE_INTERNAL_ID", "SOURCE_ID");
        a_adeDictionary = new AdeDictionaries(messageIds, componentIds, systemIds);
        m_dataStorePeriods = new DataStorePeriodsImpl();
        m_dataStoreGroups = new DataStoreGroupsImpl();
        m_dataStoreRules = new DataStoreRulesImpl();
        m_dataStorePeriodSummaries = new DataStorePeriodSummaries(m_dataStorePeriods);
        m_dataStoreModels = new DataStoreModelsImpl<IMainScorer>(SQL.MODELS, new MainScorerImpl.FileHandler());
        m_dataStoreUser = new DataStoreUserImpl();
        m_dataStoreSources = new DataStoreSourcesImpl(getAdeDictionaries());
        m_dataStoreTextClustering = new DataStoreTextClusteringModelsImpl();
    }

    private void assertCompatibleDb() throws AdeException {
        // make sure data store code is compatible with db
        final Version curDbVersion = new AdePatches().getCurrentVersion();
        final Version deploymentDbVersion = Ade.getAde().getDbVersion();
        if (!deploymentDbVersion.equals(curDbVersion)) {
            final String msg = "Current Ade DB version " + curDbVersion + " is incompatible "
                    + "with Deployment Ade version " + deploymentDbVersion;
            if (ade.getConfigProperties().getOverrideVersionCheck()) {
                logger.warn(msg + ". Overriding version check (due to "
                        + AdeConfigPropertiesImpl.ADE_OVERRIDE_VERSION_CHECK
                        + " setup property)");
            } else {
                throw new AdeUsageException(msg + ". Please apply required patches, or turn on the "
                        + AdeConfigPropertiesImpl.ADE_OVERRIDE_VERSION_CHECK + " setup property");
            }
        }
    }

    public void close() throws AdeException {
        try {
            final Connection conn = MyJDBCConnection.getConnection();
            if (conn != null && !conn.getAutoCommit()) {
                conn.commit();
            }
        } catch (SQLException e) {
            throw new AdeInternalException("Failed committing", e);
        }
        MyJDBCConnection.close();
        if (a_adeDictionary != null) {
            a_adeDictionary.clearAll();
        }
    }

    public void quietCleanup() {
        try {
            MyJDBCConnection.getConnection().rollback();
            MyJDBCConnection.close();
            if (a_adeDictionary != null) {
                a_adeDictionary.clearAll();
            }
        } catch (Exception e) {
            logger.error("Internal error encountered while cleaning up the connection.", e);
        }
    }

    public AdeDictionaries getAdeDictionaries() throws AdeException {
        return a_adeDictionary;
    }

    @Override
    public DataStoreSourcesImpl sources() {
        return m_dataStoreSources;
    }

    @Override
    public DataStorePeriodsImpl periods() {
        return m_dataStorePeriods;
    }
    
    @Override 
    public DataStoreGroupsImpl groups() {
        return m_dataStoreGroups;
    }
    
    @Override 
    public DataStoreRulesImpl rules() {
        return m_dataStoreRules;
    }

    public DataStorePeriodSummaries periodSummaries() {
        return m_dataStorePeriodSummaries;
    }

    @Override
    public DataStoreModelsImpl<IMainScorer> models() {
        return m_dataStoreModels;
    }

    @Override
    public DataStoreUserImpl user() {
        return m_dataStoreUser;
    }

    public IntervalByPeriodSummaryDbIterator getByPeriodsSummaryDbIterator(
            PeriodSummary periodSummary) throws AdeException {
        return new IntervalByPeriodSummaryDbIterator(periodSummary);
    }

    public void createTables() throws AdeException {
        new TableManager().createAll();
    }

    @Override
    public void deleteAllContent() throws AdeException {
        final TableManager tm = new TableManager();
        tm.deleteAll();
        getAdeDictionaries().clearAll();

        // after the tables have been deleted, re-initialize them
        tm.initTables();
    }

    public DataStoreTextClusteringModelsImpl textClustering() {
        return m_dataStoreTextClustering;
    }

    @Override
    public IDataStoreDictionaryApi messages() throws AdeException {
        return new DataStoreDictionaryApiImpl(getAdeDictionaries().getMessageIdDictionary());
    }

}
