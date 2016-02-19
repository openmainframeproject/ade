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
import java.util.Collection;
import java.util.Date;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.data.TextClusterData;
import org.openmainframe.ade.impl.dbUtils.DmlPreparedStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.QueryStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.SpecialSqlQueries;
import org.openmainframe.ade.impl.dbUtils.TableGeneralUtils;

/**
 * Provides access to @link{TextClusteringModel} in the datastore. Allows for loading the models in the
 * datastore
 *
 */
public class DataStoreTextClusteringModelsImpl {

    /** Create the instance of the class.
     */
    public DataStoreTextClusteringModelsImpl() {
        //Purposefully empty constructor
    }
    /**
     * Place a text cluster in the data store.
     * @param representativeText text that represents the cluster
     * @param componentInternalId the component internal id
     * @param timeStamp time it was last observed
     * @return the last key of the data store
     * @throws AdeException
     */
    public final int storeTextCluster(final String representativeText, final int componentInternalId,
            final Date timeStamp) throws AdeException {
        final DmlPreparedStatementExecuter clusterStorer = new DmlPreparedStatementExecuter(
                "INSERT INTO " + SQL.TEXT_CLUSTERS + " "
                        + "(COMPONENT_INTERNAL_ID, TEXT_REPRESENTATIVE, LAST_OBSERVED )"
                        + " VALUES( ?, ?, ? ) ") {
            @Override
            protected void setParameters(PreparedStatement stmt) throws SQLException,
                    AdeException {
                int pos = 1;
                stmt.setInt(pos++, componentInternalId);
                stmt.setString(pos++, representativeText);
                TableGeneralUtils.setPreparedStatementTimestamp(stmt, pos++, timeStamp);

            }
        };

        clusterStorer.execute();
        return SpecialSqlQueries.getLastKey();
    }

    /**
     * Grab the maximum cluster id from the database for the given componentId.
     * @param componentId for which we are to get the max cluster id
     * @return the maximum cluster id
     * @throws AdeException
     */
    public final int getMaxClusterId(int componentId) throws AdeException {

        final Integer res = SpecialSqlQueries.executeIntQuery(

                "SELECT   MAX(TEXT_CLUSTER_INTERNAL_ID) "
                        + "FROM     " + SQL.TEXT_CLUSTERS + " "

                        + "WHERE    COMPONENT_INTERNAL_ID = " + componentId,
                AdeInternal.getDefaultConnection(), false);
        if (res == null) {
            return 0;
        }
        return res;
    }

    /**
     * Get the latest cluster data from the datastore.
     * @param componentId the component id to refresh the data about
     * @param currentMaxClusterId the current maximum cluster ID
     * @param clustersData a collection of clusters
     * @return the updated max cluster id
     * @throws AdeException
     */
    public final int refreshClustersFromDataStore(int componentId, int currentMaxClusterId,
            Collection<TextClusterData> clustersData) throws AdeException {
        final TextClusterReader textClustersReader = new TextClusterReader(componentId, currentMaxClusterId, 
                clustersData);

        textClustersReader.executeQuery();
        return textClustersReader.m_maxClusterId;
    }

    /**
     * Update the last observed time stamp for a given cluster.
     * @param clusterId the cluster that needs to be updated
     * @param timeStamp the new last observed time stamp
     * @throws AdeException
     */
    public final void updateTextClusterTimeStamp(final int clusterId, final Date timeStamp) throws AdeException {
        final DmlPreparedStatementExecuter timeStampUpdater = new DmlPreparedStatementExecuter(
                "UPDATE   " + SQL.TEXT_CLUSTERS + " "
                        + "SET      LAST_OBSERVED = ? "
                        + "WHERE    TEXT_CLUSTER_INTERNAL_ID = ? ") {
            @Override
            protected void setParameters(PreparedStatement stmt) throws SQLException,
                    AdeException {
                int pos = 1;
                TableGeneralUtils.setPreparedStatementTimestamp(stmt, pos++, timeStamp);
                stmt.setInt(pos++, clusterId);

            }
        };

        timeStampUpdater.execute();
    }
    
    /**
     * Private class to read new clusters from the datastore.
     */
    private class TextClusterReader extends QueryStatementExecuter {
        private Collection<TextClusterData> m_clustersData;
        private int m_maxClusterId;

        /**
         * Create a reader for new clusters from a given component.
         * @param componentId the component
         * @param currentMaxClusterId the current max cluster id
         * @param clustersData collection of the currently known clusters
         */
        public TextClusterReader(int componentId, int currentMaxClusterId,
                Collection<TextClusterData> clustersData) {
            super("SELECT   TEXT_CLUSTER_INTERNAL_ID, TEXT_REPRESENTATIVE, LAST_OBSERVED "
                    + "FROM     " + SQL.TEXT_CLUSTERS + " "
                    + "WHERE    COMPONENT_INTERNAL_ID = " + componentId + " "
                    + "AND      TEXT_CLUSTER_INTERNAL_ID > " + currentMaxClusterId);
            m_clustersData = clustersData;
            m_maxClusterId = currentMaxClusterId;
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            int pos = 1;
            final int clusterId = rs.getInt(pos++);
            final String textRepresentative = rs.getString(pos++);
            final Date lastObserved = rs.getTimestamp(pos++);
            final TextClusterData cluster = new TextClusterData(textRepresentative, lastObserved, clusterId);
            m_clustersData.add(cluster);
            if (m_maxClusterId < clusterId) {
                m_maxClusterId = clusterId;
            }
        }
    }

}