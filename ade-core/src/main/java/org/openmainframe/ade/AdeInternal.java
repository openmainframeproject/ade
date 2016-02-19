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
package org.openmainframe.ade;

import java.io.File;
import java.sql.Connection;

import org.openmainframe.ade.actions.IActionsFactory;
import org.openmainframe.ade.data.IDataFactory;
import org.openmainframe.ade.dataStore.IDataStore;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.AdeConfigPropertiesImpl;
import org.openmainframe.ade.impl.UserSpecifications;
import org.openmainframe.ade.impl.actions.ActionsFactoryImpl;
import org.openmainframe.ade.impl.data.DataFactoryImpl;
import org.openmainframe.ade.impl.dataStore.DataStoreSqlImpl;
import org.openmainframe.ade.impl.dataStore.AdeDictionaries;
import org.openmainframe.ade.impl.dbUtils.MyJDBCConnection;
import org.openmainframe.ade.impl.flow.factory.FlowFactory;
import org.openmainframe.ade.impl.summary.CriticalWordsScorer;
import org.openmainframe.ade.models.IModelMetaData;
import org.openmainframe.ade.output.IAnalysisResultMarshaller;
import org.openmainframe.ade.output.impl.AnalysisResultMarshallerImpl;
import org.openmainframe.ade.utils.patches.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdeInternal extends Ade {

    private static final Logger logger = LoggerFactory.getLogger(AdeInternal.class);
    private final Version ADE_VERSION;
    private static final Version ADE_REGRESSION_VERSION = new Version(1);
    private static final Version ADE_DEPLOYMENT_VERSION = new Version(3, 2, 1);
    private static final Version ADE_DB_VERSION = new Version(3, 2, 0);
    private static final Version LAST_ADE_VERSION_FOR_WHICH_MODEL_FILES_ARE_READABLE_BY_CURRENT_VERSION = new Version(3, 2, 1);

    private String m_setupFilePath;

    /**
     * saved for documentation.
     */
    private String[] m_commandLineArguments = null;

    // factories    
    private DataStoreSqlImpl m_dataStore;
    private AnalysisResultMarshallerImpl m_analyisResultMarshaller;
    private IDataFactory m_dataFactory;
    private IActionsFactory m_actionsFactory;
    private FlowFactory m_flowFactory;

    // configuration
    private IAdeConfigProperties m_configProps;

    private AdeDirectoriesManagerImpl m_directoriesManager;

    private UserSpecifications m_userSpecifications;
    
    static AdeInternal adeObjectImpl;
    
    /** Sets ade setup file path.
     * @param inputPropertyPath path for ade setup
     */
    public AdeInternal(String inputPropertyPath) throws AdeException {
        String propertyPath = inputPropertyPath;
        if (propertyPath == null) {
            propertyPath = System.getProperty(ADE_SETUP_FILE_PATH_PROPERTY);
            if ("".equals(propertyPath) || propertyPath == null) {
                throw new AdeUsageException("Missing property: " + ADE_SETUP_FILE_PATH_PROPERTY);
            }
        }
        m_setupFilePath = propertyPath;
        logger.info("Reading configuration from " + m_setupFilePath);
        m_configProps = new AdeConfigPropertiesImpl(propertyPath);

        ADE_VERSION = m_configProps.debug().getRegressionMode() ? ADE_REGRESSION_VERSION : ADE_DEPLOYMENT_VERSION;
        m_directoriesManager = new AdeDirectoriesManagerImpl(
                m_configProps.getOutputPath(), m_configProps.getAnalysisOutputPath(), m_configProps.getTempPath());

        m_userSpecifications = createUserSpecifications(m_configProps);
    }

    private final UserSpecifications createUserSpecifications(IAdeConfigProperties configProps) throws AdeException {
        CriticalWordsScorer criticalWordsScorer = new CriticalWordsScorer(configProps.getCriticalWordsFile());       
        return new UserSpecifications(criticalWordsScorer);
    }

    public AdeInternal() throws AdeException {
        this(null);
    }

    @Override
    public final synchronized IDataFactory getDataFactory()
            throws AdeInternalException {
        if (m_dataFactory == null) {
            m_dataFactory = new DataFactoryImpl();
        }
        return m_dataFactory;
    }

    @Override
    public final synchronized IActionsFactory getActionsFactory() {
        if (m_actionsFactory == null) {
            m_actionsFactory = new ActionsFactoryImpl();
        }
        return m_actionsFactory;
    }

    @Override
    public final IAdeConfigProperties getConfigProperties() {
        return m_configProps;
    }

    @Override
    public final String[] getCommandLineArguments() {
        return m_commandLineArguments.clone();
    }

    @Override
    public final void setCommandLineArguments(String[] args) {
        m_commandLineArguments = args.clone();
    }

    @Override
    public final void close() throws AdeException {
        if (m_dataStore != null) {
            m_dataStore.close();
        }
    }

    @Override
    public final void quietCleanup() {
        if (m_dataStore != null) {
            m_dataStore.quietCleanup();
        }
    }

    public final File getCriticalWordsFile() throws AdeInternalException {
        return new File(getConfigProperties().getCriticalWordsFile());
    }
    
    private void getDataStore(boolean create) throws AdeException {
        if (m_conn != null) {
            m_dataStore = new DataStoreSqlImpl(m_conn);
            return;
        }
        final String dbType = getConfigProperties().database().getDataStoreType();

        if (!"sql".equalsIgnoreCase(dbType)) {
            throw new AdeInternalException("Unsupported data store type " + dbType);
        }
        m_dataStore = new DataStoreSqlImpl(create);
    }

    @Override
    public final DataStoreSqlImpl getDataStore() throws AdeException {
        if (m_dataStore == null) {
            getDataStore(false);
        }
        return m_dataStore;
    }
    
    @Override
    public final DataStoreSqlImpl getDataStore(Connection conn) throws AdeException {
        if (m_dataStore != null) {
            return m_dataStore;
        } else {
            m_conn = conn;
            getDataStore(false);
            return m_dataStore;
        }

    }

    @Override
    public final void releaseDataStore() throws AdeException {
        if (m_dataStore != null) {
            m_dataStore.close();
            m_dataStore = null;
        }
    }

    /** Returns Ade's analysis result marshaller.*/
    @Override
    public final IAnalysisResultMarshaller getAnalysisResultMarshaller() throws AdeException {
        if (m_analyisResultMarshaller == null) {
            m_analyisResultMarshaller = new AnalysisResultMarshallerImpl();
        }
        return m_analyisResultMarshaller;
    }

    @Override
    public final String getSetupFilePath() {
        return m_setupFilePath;
    }

    public final AdeDictionaries getDictionaries() throws AdeException {
        return getDataStore().getAdeDictionaries();
    }

    @Override
    public final FlowFactory getFlowFactory() throws AdeException {
        if (m_flowFactory == null) {
            m_flowFactory = new FlowFactory();
        }
        return m_flowFactory;
    }

    @Override
    public final UserSpecifications getUserSpecifications() {
        return m_userSpecifications;
    }

    public static AdeInternal getAdeImpl() {
        return adeObjectImpl;
    }

    @Override
    public final IDataStore createDataStore() throws AdeException {

        if (m_dataStore != null) {
            throw new AdeInternalException("Datastore already opened");
        }
        getDataStore(true);
        return m_dataStore;
    }

    @Override
    public final Version getVersion() {
        return ADE_VERSION;
    }

    @Override
    public final Version getDbVersion() {
        return ADE_DB_VERSION;
    }

    @Override
    public final AdeDirectoriesManagerImpl getDirectoryManager() {
        return m_directoriesManager;
    }

    public static Connection getDefaultConnection() throws AdeException {
        return MyJDBCConnection.getConnection();
    }
    
    /** 
     * Verifies if the Model is valid for use.
     * @param metaData data used to compare to the last usable version
     * @throws AdeUsageException incorrect usage of ade
     */
    public final void verifyValidModelVersion(IModelMetaData metaData) throws AdeUsageException {
        if (metaData.getAdeVersion().compareTo(LAST_ADE_VERSION_FOR_WHICH_MODEL_FILES_ARE_READABLE_BY_CURRENT_VERSION) < 0) {
            throw new AdeUsageException(String.format("Model internal id %s is a "
                    + "version %s model, and can no longer be used. Only models from "
                    + "version %s can be used", metaData.getModelInternalId(), metaData.getAdeVersion(),
                    LAST_ADE_VERSION_FOR_WHICH_MODEL_FILES_ARE_READABLE_BY_CURRENT_VERSION));
        }
    }

}
