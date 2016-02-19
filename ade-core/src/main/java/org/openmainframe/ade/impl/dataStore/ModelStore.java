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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.dbUtils.PreparedStatementWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.models.IModelMetaData;

/** Utility class to contain methods for dealing with a model store.
 */
public final class ModelStore {
    
    private ModelStore() {
        //Nothing to construct here
    }

    /**method to store model metadata into the model store. 
     * 
     * @param table the model table
     * @param metaData the model meta data
     * @param setAsDefault boolean indicating whether or not to set this as the default
     * @return the model internal id
     * @throws AdeException
     */
    static int store(SQL table, IModelMetaData metaData, boolean setAsDefault) throws AdeException {
        final int modelInternalId = storeToModelsTable(table, metaData);
        metaData.setModelInternalId(modelInternalId);
        if (setAsDefault) {
            setAsDefault(table, modelInternalId);
        }
        return modelInternalId;
    }

    /**  Remove the default indicator from the other models for this analysis group and add it to the model with 
     * matching model id.
     * 
     * @param table the model table
     * @param modelInternalId the internal id of the model
     * @throws AdeException Either when the passed in model id is not present in the database, or the model 
     *     id is not with a model group
     */
    static void setAsDefault(SQL table, int modelInternalId) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        int analysisGroupId = 0;
        try {
            cw.startTransaction();
            cw.lockTableExclusive(table);
            final PreparedStatementWrapper psw = cw.preparedStatement("select ANALYSIS_GROUP from " + table 
                    + " where model_internal_id=" + modelInternalId);
            final ResultSet rs = psw.executeQuery();
            if (!rs.next()) {
                throw new AdeInternalException("Model internal id " + modelInternalId + " is missing from the db");
            }
            analysisGroupId = rs.getInt(1);
            psw.close();
            if (analysisGroupId == 0) {
                throw new AdeInternalException("Can't update an empty ANALYSIS_GROUP");
            }
            cw.executeDml("update " + table + " set IS_DEFAULT=0 where ANALYSIS_GROUP=" + analysisGroupId);
            cw.executeDml("update " + table + " set IS_DEFAULT=1 where MODEL_INTERNAL_ID=" + modelInternalId);

            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
    }

    /** Remove the default indicator from all models.
     * 
     * @param table the model table
     * @throws AdeException
     */
    static void setAllModelsNotDefault(SQL table) throws AdeException {
        ConnectionWrapper.executeDmlDefaultCon("update " + table + " set IS_DEFAULT=0");
    }

    /** Delete all the non default model meta data in the store for a given analysis group.
     * 
     * @param table the model table
     * @param analysisGroup the analysis group
     * @throws AdeException
     */
    static void deleteNonDefaultModelsMetaData(SQL table, String analysisGroup) throws AdeException {
        for (IModelMetaData modelMetaData : ModelRead.readNonDefault(table, analysisGroup)) {
            deleteModelMetaData(table, modelMetaData.getModelInternalId());
        }
    }

    /** Delete model meta data from the store for a given model id.
     * 
     * @param table the model table
     * @param modelInternalId the model id
     * @throws AdeException
     */
    static void deleteModelMetaData(SQL table, int modelInternalId) throws AdeException {
        ConnectionWrapper.executeDmlDefaultCon("delete from " + table + " where MODEL_INTERNAL_ID=" + modelInternalId);
    }

    private static int storeToModelsTable(SQL table, IModelMetaData metaData) throws AdeException {
        final String sql = "INSERT INTO " + table 
                + "(ANALYSIS_GROUP, START_TIME, END_TIME, IS_DEFAULT, EXTERNAL_FILE_NAME, "
                    + "ADE_VERSION, CREATION_TIME) " 
                + "VALUES(?,?,?,?,?,?,?)";

        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        int lastKey = 0;
        try {
            File modelFileName = metaData.getModelFileName();
            // we may get a modelFileName that is empty.  This is fine.  We will replace the value in the DB later.
            if (modelFileName == null) {
                modelFileName = new File("PLACE-HOLDER");
            }
            final PreparedStatementWrapper psw = cw.preparedStatement(sql);
            final PreparedStatement ps = psw.getPreparedStatement();
            int pos = 1;
            ps.setInt(pos++, metaData.getSourceGroupId());
            psw.setTimestamp(pos++, metaData.getStartTime());
            psw.setTimestamp(pos++, metaData.getEndTime());
            ps.setInt(pos++, 0);
            psw.setFileName(pos++, modelFileName.getAbsoluteFile());
            ps.setString(pos++, metaData.getAdeVersion().toString());
            psw.setTimestamp(pos++, metaData.getCreationDate());
            psw.execute();

            lastKey = cw.simpleQueries().getLastKey();

            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }

        return lastKey;
    }

}
