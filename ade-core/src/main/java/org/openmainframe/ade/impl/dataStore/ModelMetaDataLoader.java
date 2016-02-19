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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.dbUtils.PreparedStatementWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.models.ModelMetaDataImpl;
import org.openmainframe.ade.models.IModelMetaData;
import org.openmainframe.ade.utils.patches.Version;

class ModelMetaDataLoader {
    private final String m_basicSql;

    ModelMetaDataLoader(SQL table) {
        m_basicSql = "select MODEL_INTERNAL_ID, ANALYSIS_GROUP, "
                + "START_TIME, END_TIME, EXTERNAL_FILE_NAME, ADE_VERSION, "
                + "CREATION_TIME from " + table;
    }

    List<IModelMetaData> loadAll(String sourceGroup, Date startTime, Date endTime) throws AdeException {
        final List<IModelMetaData> res = new ArrayList<IModelMetaData>();
        res.addAll(loadAllImpl(sourceGroup, startTime, endTime));
        return res;
    }

    ArrayList<IModelMetaData> loadAll() throws AdeException {
        final ArrayList<IModelMetaData> res = new ArrayList<IModelMetaData>();
        res.addAll(loadAllImpl());
        return res;
    }

    List<ModelMetaDataImpl> loadAllImpl(String analysisGroup, Date startTime, Date endTime) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        List<ModelMetaDataImpl> result = null;
        try {
            if (analysisGroup == null) {
                throw new AdeInternalException("Can't load with analysis_group=null");
            }
            final String sql = m_basicSql + " where ANALYSIS_GROUP=? "
                    + ((startTime != null) ? " and end_time>?" : "")
                    + ((endTime != null) ? " and start_time<?" : "");

            final PreparedStatementWrapper psw = cw.preparedStatement(sql);
            final PreparedStatement ps = psw.getPreparedStatement();
            int pos = 1;
            ps.setString(pos++, analysisGroup);
            // NOTE: if min and max are null, they are not part of the where clause at all
            if (startTime != null) {
                psw.setTimestamp(pos++, startTime);
            }
            if (endTime != null) {
                psw.setTimestamp(pos++, endTime);
            }

            result = load(psw.executeQuery());
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        return result;
    }

    List<ModelMetaDataImpl> loadAllImpl() throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        List<ModelMetaDataImpl> result = null;
        try {
            final String sql = m_basicSql;
            final PreparedStatementWrapper psw = cw.preparedStatement(sql);
            result = load(psw.executeQuery());
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        return result;
    }

    ModelMetaDataImpl loadOne(int modelInternalId) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        List<ModelMetaDataImpl> result = null;
        try {
            final String sql = m_basicSql
                    + " where "
                    + " model_internal_id=" + modelInternalId;
            final PreparedStatementWrapper psw = cw.preparedStatement(sql);
            result = load(psw.executeQuery());
            if (result.size() != 1) {
                throw new AdeInternalException("Expected " + sql + " to return exactly one row. Instead found "
                        + result.size());
            }
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        return result.get(0);
    }

    private static List<ModelMetaDataImpl> load(ResultSet rs) throws SQLException, AdeException {
        final List<ModelMetaDataImpl> res = new ArrayList<ModelMetaDataImpl>();
        while (rs.next()) {
            res.add(createFromRs(rs));
        }
        return res;

    }

    private static ModelMetaDataImpl createFromRs(ResultSet rs) throws SQLException, AdeException {
        int pos = 1;

        final int modelInternalId = rs.getInt(pos++);
        final int analysisGroupId = rs.getInt(pos++);
        final Date startDate = PreparedStatementWrapper.getResultSetTimestamp(rs, pos++);
        final Date endDate = PreparedStatementWrapper.getResultSetTimestamp(rs, pos++);
        final File modelFile = new File(rs.getString(pos++));
        final Version adeVersion = Version.parse(rs.getString(pos++));
        final Date creationDate = PreparedStatementWrapper.getResultSetTimestamp(rs, pos++);

        final ModelMetaDataImpl result = new ModelMetaDataImpl(modelInternalId, analysisGroupId,
                startDate, endDate, adeVersion, creationDate);
        result.setExternalFileName(modelFile);

        return result;
    }
}