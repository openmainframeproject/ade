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
package org.openmainframe.ade.scores;

import java.util.SortedSet;
import java.util.TreeSet;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.scoringApi.StatisticsChart;

public class ClusteringUniquifyScore extends AbstractClusteringScorer {

    public ClusteringUniquifyScore() throws AdeException {
        super();
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary ams, IAnalyzedInterval analyzedInterval) throws AdeException {
        if (m_analyzer == null)
            m_analyzer = new Analyzer();
        return m_analyzer.getScore(ams, analyzedInterval);
    }

    class Analyzer extends AbstractClusteringScorer.Analyzer {
        transient private SortedSet<Integer> m_contextGoodClusters;
        transient private SortedSet<Integer> m_goodClustersRepresentative;
        transient private IInterval m_cachedInterval;

        @Override
        public StatisticsChart getScore(IAnalyzedMessageSummary ams, IAnalyzedInterval analyzedInterval) throws AdeException {
            boolean representatitve = false;
            Integer clusterId;
            setContext(analyzedInterval.getInterval());
            final int id = ams.getMessageSummary().getMessageInternalId();
            final StatisticsChart sc = new StatisticsChart();
            clusterId = m_model.m_msgInternalIdToCluster.get(id);
            boolean spike = false;
            if (clusterId == null) {
                clusterId = -1;
            } else {
                spike = m_contextGoodClusters.contains(clusterId);
                if (spike && !m_goodClustersRepresentative.contains(clusterId)) {
                    m_goodClustersRepresentative.add(clusterId);
                    representatitve = true;
                }
            }
            sc.setStat(CLUSTER_ID, clusterId);

            ClusteringUniquifyScore.ClusterStatus status;
            if (!m_model.m_seenMsgIds.containsKey(ams.getMessageId())) {
                status = AbstractClusteringScorer.ClusterStatus.NEW;
            } else if (clusterId == -1) {
                status = AbstractClusteringScorer.ClusterStatus.UNCLUSTERED;
            } else if (spike) {
                if (representatitve) {
                    status = AbstractClusteringScorer.ClusterStatus.REPRESENTATIVE;
                } else {
                    status = AbstractClusteringScorer.ClusterStatus.IN_CONTEXT;
                }
            } else {
                status = AbstractClusteringScorer.ClusterStatus.OUT_OF_CONTEXT;
            }

            sc.setStat(STATUS, status.name());
            sc.setStat(MAIN, (status == ClusterStatus.IN_CONTEXT ? 0.0 : 1.0));
            return sc;
        }

        /**
         * Analyze an interval. Figure out which clusters are spiking in it, and map
         * messages to clusters. A message-id that belongs to a cluster that does not
         * spike, is removed from the mapping. A cluster is considered spiking if at
         * least 20% of msgs mapped to this cluster (according to the clustering
         * training result) appear in the interval.
         * 
         * @param interval
         *          to profile
         * @return a map from message ids to spiking clusters
         */
        private void setContext(IInterval interval) {
            if (interval.equals(m_cachedInterval)) {
                return;
            }
            m_cachedInterval = interval;

            final int[] clusterSizeInInterval = new int[m_model.m_actualClusters];
            // first pass: calculate how many messages from each cluster appear in
            // interval
            for (IMessageSummary messageSummary : interval.getMessageSummaries()) {
                final Integer clusterId = m_model.m_msgInternalIdToCluster.get(messageSummary.getMessageInternalId());
                if (clusterId != null) {
                    clusterSizeInInterval[clusterId]++;
                }
            }

            m_contextGoodClusters = new TreeSet<Integer>();
            m_goodClustersRepresentative = new TreeSet<Integer>();
            // second pass: find spiking clusters    
            for (int i = 0; i < m_model.m_actualClusters; ++i) {
                m_contextGoodClusters.add(i);
            }
        }

    }

}