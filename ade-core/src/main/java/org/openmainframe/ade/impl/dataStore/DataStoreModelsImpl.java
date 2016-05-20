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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.dataStore.IDataStoreModels;
import org.openmainframe.ade.dataStore.IModelFileHandler;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.dbUtils.PreparedStatementWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.models.IModel;
import org.openmainframe.ade.models.IModelMetaData;
import org.openmainframe.ade.utils.AdeFileUtils;

public class DataStoreModelsImpl<T extends IModel> implements IDataStoreModels<T> {

    private final SQL m_modelsTable;
    private final ModelMetaDataLoader m_modelMetaDataLoader;
    private final IModelFileHandler<T> m_modelFileHandler;
    private final Map<String, T> m_modelsCache;
    private Map<String, Object> m_modelDataObject;
    private static final int DEFAULT_MODELS_CACHE_SIZE = 100;

    /**
     * Construct a new DataStoreModelsImpl specifying a model cache size.
     * 
     * @param modelsTable the model table description
     * @param modelFileHandler the ModelFileHandler
     * @param modelsCacheSize the cache size
     */
    public DataStoreModelsImpl(SQL modelsTable, IModelFileHandler<T> modelFileHandler, int modelsCacheSize) {
        m_modelsTable = modelsTable;
        m_modelMetaDataLoader = new ModelMetaDataLoader(modelsTable);
        m_modelFileHandler = modelFileHandler;
        m_modelsCache = Collections.synchronizedMap(new LruCachedMap<String, T>(modelsCacheSize));
        m_modelDataObject = new TreeMap<>();
    }

    /**
     * Construct a new DataStoreModelsImpl using the default cache size of 100.
     * 
     * @param modelsTable the model table description
     * @param modelFileHandler the ModelFileHandler
     */
    public DataStoreModelsImpl(SQL modelsTable, IModelFileHandler<T> modelFileHandler) {
        this(modelsTable, modelFileHandler, DEFAULT_MODELS_CACHE_SIZE);
    }

    @Override
    public final T loadDefaultModel() throws AdeException {
        return loadByMetaData(ModelRead.readDefaultNull(m_modelsTable));
    }

    @Override
    public final T loadDefaultModel(String analysisGroup) throws AdeException {
        T model = m_modelsCache.get(analysisGroup);
        final IModelMetaData defaultModelMetaData = loadDefaultModelMetaData(analysisGroup);
        if (model != null) {
            // compare cached model with latest in DB
            final int cachedModelId = model.getModelMetaData().getModelInternalId();
            final int defaultModelId = defaultModelMetaData.getModelInternalId();
            if (cachedModelId == defaultModelId) {
                return model;
            }
        }
        model = loadByMetaData(loadDefaultModelMetaData(analysisGroup));
        m_modelsCache.put(analysisGroup, model);
        return model;
    }

    @Override
    public final int storeModel(T model, boolean setAsDefault) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        File modelFile = null;
        try {
            cw.startTransaction();
            cw.lockTableExclusive(m_modelsTable);
            final int modelId = ModelStore.store(m_modelsTable, model.getModelMetaData(), setAsDefault);
            modelFile = m_modelFileHandler.store(model);
            model.getModelMetaData().setExternalFileName(modelFile);
            updateExternalFile(modelId, modelFile);
            return modelId;
        } catch (Exception e) {
            Exception rollbackException = null;
            try {
                cw.rollback();
            } catch (SQLException e1) {
                rollbackException = e1;
            }

            boolean deleteSuccess = true;
            if (modelFile != null) {
                deleteSuccess = modelFile.delete();
            }

            if (rollbackException != null || !deleteSuccess) {
                String msg = "Failed cleanup after trying to store a model: " + model.getModelMetaData();
                if (modelFile != null && !deleteSuccess) {
                    msg += "\nFailed deleting model file: " + modelFile.getAbsolutePath();
                }
                if (rollbackException != null) {
                    throw new AdeInternalException(msg, rollbackException);
                }
                throw new AdeInternalException(msg);
            }

            throw new AdeUsageException("Failed storing model: " + model.getModelMetaData(), e);
        } finally {
            try {
                cw.close();
            } catch (SQLException e) {
                throw new AdeInternalException("Failed closing the connection wrapper after writing the model: " 
                        + model.getModelMetaData(), e);
            }
        }
    }

    @Override
    public final T loadModel(int modelInternalId) throws AdeException {
        return loadByMetaData(ModelRead.readMetaData(m_modelsTable, modelInternalId));
    }

    @Override
    public final IModelMetaData loadModelMetaData(int modelInternalId)
            throws AdeException {
        return ModelRead.readMetaData(m_modelsTable, modelInternalId);
    }

    @Override
    public final List<IModelMetaData> getModelList() throws AdeException {
        return m_modelMetaDataLoader.loadAll();
    }

    @Override
    public final List<IModelMetaData> getModelListByAnalysisGroup(String analysisGroup,
            Date startTime, Date endTime) throws AdeException {
        return m_modelMetaDataLoader.loadAll(analysisGroup, startTime, endTime);
    }

    @Override
    public final void deleteModel(int modelInternalId, boolean deleteModelFile)
            throws AdeException {
        IModelMetaData metaData = null;
        if (deleteModelFile) {
            metaData = loadModelMetaData(modelInternalId);
        }
        ModelStore.deleteModelMetaData(m_modelsTable, modelInternalId);
        if (metaData != null && deleteModelFile) {
            if (metaData.getModelFileName() == null) {
                throw new AdeInternalException("Model " + modelInternalId
                        + " has no associated file");
            }
            AdeFileUtils.deleteFileOrLog(metaData.getModelFileName());
        }
    }

    @Override
    public final void setAsDefault(int modelInternalId) throws AdeException {
        ModelStore.setAsDefault(m_modelsTable, modelInternalId);
    }

    @Override
    public final void setAllModelsNotDefault() throws AdeException {
        ModelStore.setAllModelsNotDefault(m_modelsTable);
    }

    @Override
    public final int importModelFromFile(File file, boolean setAsDefault)
            throws AdeException {
        final T model = readModelFromFile(file);
        final IModelMetaData metaData = model.getModelMetaData();
        metaData.setExternalFileName(file);
        return ModelStore.store(m_modelsTable, metaData, setAsDefault);
    }

    @Override
    public final T readModelFromFile(File file) throws AdeException {
        T model;
        try {
            model = m_modelFileHandler.load(file);
        } catch (IOException e) {
            throw new AdeUsageException("Error accessing model file "
                    + file.getAbsolutePath(), e);
        }
        return model;
    }

    @Override
    public final IModelMetaData loadDefaultModelMetaData(String sourceGroup) throws AdeException {
        return ModelRead.readDefault(m_modelsTable, sourceGroup);
    }

    @Override
    public final IModelMetaData loadDefaultModelMetaData() throws AdeException {
        return ModelRead.readDefault(m_modelsTable, null);
    }

    private void updateExternalFile(int modelInternalId, File externalFile)
            throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        try {
            final PreparedStatementWrapper psw = cw.preparedStatement("update "
                    + m_modelsTable + " set external_file_name=? where model_internal_id="
                    + modelInternalId);
            psw.setFileName(1, externalFile);
            psw.execute();
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
    }

    private T loadByMetaData(IModelMetaData metaData)
            throws AdeException {
        if (metaData == null) {
            return null;
        }
        final File fileName = metaData.getModelFileName();
        AdeInternal.getAdeImpl().verifyValidModelVersion(metaData);
        if (fileName == null) {
            throw new AdeInternalException("Model with internal id "
                    + metaData.getModelInternalId() + " has no external file specified");
        }
        try {
            return m_modelFileHandler.load(fileName);
        } catch (IOException e) {
            throw new AdeUsageException("Error accessing model file "
                    + fileName.getAbsolutePath(), e);
        }
    }

    @Override
    public final Object getModelDataObject(String name) {
        return m_modelDataObject.get(name);
    }

    @Override
    public final void setModelDataObject(String name, Object data) {
        m_modelDataObject.put(name, data);
    }

    @Override
    public final Set<String> getModelDataObjectKeys() {
        return m_modelDataObject.keySet();
    }
}
