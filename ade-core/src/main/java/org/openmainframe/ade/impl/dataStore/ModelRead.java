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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.dbUtils.PreparedStatementWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dbUtils.SpecialSqlQueries;
import org.openmainframe.ade.impl.models.ModelMetaDataImpl;
import org.openmainframe.ade.models.IModelMetaData;

/** 
 * ModelRead provides methods to access and return model meta data
 * from the MODELS table.
 */
public final class ModelRead {

    private ModelRead() {
        
    }
    
    /**
     * readDefaultNull returns the model meta data for all 
     * default models where the ANALYSIS_GROUP is equal to NULL.
     * <br><br>
     * Null is returned if there are no models for a NULL
     * analysis group.
     * <br><br>
     * A ade internal exception is thrown if there is more
     * than 1 model for a NULL analysis group.
     * 
     * @param  table        database table MODELS
     * @return readMetaData model meta data
     * @see    readMetaData
     */
    public static IModelMetaData readDefaultNull(SQL table) throws AdeException {
        final String sql = String.format(
                "select MODEL_INTERNAL_ID from %s where IS_DEFAULT=1 and ANALYSIS_GROUP is null", table
                );
        final ArrayList<Integer> ids = SpecialSqlQueries.executeIntListQuery(sql);
        if (ids.size() != 1) {
            if (ids.isEmpty()) {
                return null;
            }
            if (ids.size() > 1) {
                throw new AdeInternalException("multiple trained models for ANALYSIS_GROUP=null");
            }
        }
        return readMetaData(table, ids.get(0));
    }

    /**
     * readNonDefault returns a list of model meta data for
     * all non default models for a select ANALYSIS_GROUP. 
     * <br><br>
     * A ade internal exception is thrown if the sql fails.
     * 
     * @param  table         database table MODELS
     * @param  analysisGroup name of analysis group
     * @return res           list of model meta data
     */
    public static List<IModelMetaData> readNonDefault(SQL table, String analysisGroup) throws AdeException {
        final List<IModelMetaData> res = new ArrayList<>();
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        try {
            final PreparedStatementWrapper psw = cw.preparedStatement("select MODEL_INTERNAL_ID from " 
                                                               + table + " where is_default=0 and ANALYSIS_GROUP=?");
            psw.getPreparedStatement().setString(1, analysisGroup);
            final ResultSet rs = psw.executeQuery();
            while (rs.next()) {
                res.add(readMetaData(table, rs.getInt(1)));
            }
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }

        return res;
    }

    /**
     * readDefault returns the model meta data for
     * a default model for a select ANALYSIS_GROUP.
     * <br><br>
     * Null is returned if there are no default models 
     * for the given analysis group.
     * <br><br>
     * A ade internal exception is thrown if there is more
     * than 1 default model for a given analysis group.
     * <br><br>
     * A ade internal exception is thrown if the sql fails.
     *  
     * @param  table         database table MODELS
     * @param  analysisGroup name of analysis group
     * @return readMetaData  model meta data
     * @see    readMetaData
     */
    public static IModelMetaData readDefault(SQL table, String analysisGroup) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        final ArrayList<Integer> ids = new ArrayList<>();
        try {
            final int groupId = GroupRead.getAnalysisGroupId(analysisGroup);
            final PreparedStatementWrapper psw = cw.preparedStatement("select MODEL_INTERNAL_ID from " 
                                                               + table + " where is_default=1 and ANALYSIS_GROUP=?");
            psw.getPreparedStatement().setInt(1, groupId);
            final ResultSet rs = psw.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }

        if (ids.isEmpty()) {
            return null;
        }
        
        if (ids.size() > 1) {
            throw new AdeInternalException("Analysis group " + analysisGroup + " has multiple trained models");
        }
        
        return readMetaData(table, ids.get(0));
    }

    /**
     * readMetaData returns the model meta data for a given MODEL_INTERNAL_ID. 
     *
     * @param  table           database table MODELS
     * @param  modelInternalId model internal ID MODEL_INTERNAL_ID
     * @return mmdl.loadOne    model meta data
     */
    public static ModelMetaDataImpl readMetaData(SQL table, int modelInternalId) throws AdeException {
        final ModelMetaDataLoader mmdl = new ModelMetaDataLoader(table);
        return mmdl.loadOne(modelInternalId);
    }

}
