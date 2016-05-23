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

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.scoringApi.StatisticsChart;

/**
 * Class that determines the clustering context score of messages. Clustering context score for each
 * message is determined by the status of clusters in an interval and the cluster that the message is
 * mapped to.
 */
public class ClusteringContextScore extends AbstractClusteringScorer {
    /**
     * The serial version number.
     */
    private static final long serialVersionUID = 1L;

    /**
     * ClusteringContextScore constructor that calls the parent constructor,
     * AbstractClusteringScorer.
     */
    public ClusteringContextScore() throws AdeException {
        super();
    }

    /**
     * Add an additional FlowLayout.xml parameter for setting a threshold
     * on the number of message ids needed for a valid cluster.
     */
    class Configuration extends AbstractClusteringScorer.Configuration {

        private static final long serialVersionUID = 1L;
        @Property(key = "contextFraction", 
                help = "fraction of messages of cluster that must appear for cluster to shoot.", 
                required = false)
        protected double m_clusterContextFraction = 0.2;

        /**
         * Override toString for printing out the cluster context fraction.
         */
        @Override
        public String toString() {
            String res = super.toString();
            res += String.format("cluster context fraction: %f%n", m_clusterContextFraction);
            return res;
        }
    }

    /**
     * Determines if the message ids within the interval window are part of a cluster by calling
     * an Analyzer object's getScore method.
     * @param ams The analysis results of a MessageSummary object. Message summaries contain 
     * statistics and information on message instances. i.e. text body message, message id, severity, etc.
     * @param analyzedInterval contains a summary of the interval i.e. information such as time, number of 
     * message ids, etc.
     */
    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary ams, IAnalyzedInterval analyzedInterval)
            throws AdeException {
        if (m_analyzer == null) {
            m_analyzer = new Analyzer();
        }
        return m_analyzer.getScore(ams, analyzedInterval);
    }
    /**
     * Retrieve the number of clusters created.
     */
    @Override
    public int getNumClusters() {
        return m_model.getNumClusters();
    }
    /**
     * Retrieves the internal message ids that make up a cluster.
     */
    @Override
    public Map<Integer, Integer> getMessageClustersByInternalId() {
        return m_model.getMessageClustersByInternalId();
    }
    /**
     * Set cluster information in model.
     * @param seenIds the message ids that have been seen already.
     * @param clusterMap mapping from internal message id to cluster.
     * @param num number of clusters.
     * @throws AdeException
     */
    public void setClusters(SortedMap<String, Integer> seenIds, Map<Integer, Integer> clusterMap, int num) throws AdeException {
        m_config = new Configuration();
        m_model = new Model();
        m_model.setClusters(clusterMap, num);
        m_model.m_seenMsgIds = seenIds;
    }
    /**
     * Main class that does the bulk of the logic for determining cluster status.
     */
    private class Analyzer extends AbstractClusteringScorer.Analyzer {
        private transient SortedSet<Integer> m_contextGoodClusters;
        private transient IInterval m_cachedInterval;
        /**
         * First determine which clusters are spiking i.e. clusters where at least 20% of the
         * messages mapped to this cluster appear in the interval. Then determine the cluster status and cluster 
         * id of the current message. For instance, if the message is in a cluster that is spiking, the message
         * is considered IN_CONTEXT.
         * @param ams The analysis results of a MessageSummary object. Message summaries contain 
         * statistics and information on message instances. i.e. text body message, message id, severity, etc.
         * @param analyzedInterval contains a summary of the interval i.e. information such as time, number of 
         * message ids, etc.
         * @return The StatisticsChart for collecting double and string statistics.
        */
        @Override
        public StatisticsChart getScore(IAnalyzedMessageSummary ams,
                IAnalyzedInterval analyzedInterval) throws AdeException {
            setContext(analyzedInterval.getInterval());
            final int id = ams.getMessageSummary().getMessageInternalId();
            final StatisticsChart sc = new StatisticsChart();
            Integer clusterId = m_model.m_msgInternalIdToCluster.get(id);
            boolean spike = false;
            if (clusterId == null) {
                clusterId = -1;
            } else {
                spike = m_contextGoodClusters.contains(clusterId);
            }
            sc.setStat(CLUSTER_ID, clusterId);

            ClusteringContextScore.ClusterStatus status = null;
            if (!m_model.m_seenMsgIds.containsKey(ams.getMessageId())) {
                status = ClusteringContextScore.ClusterStatus.NEW;
            } else if (clusterId == -1) {
                status = ClusteringContextScore.ClusterStatus.UNCLUSTERED;
            } else if (spike) {
                status = ClusteringContextScore.ClusterStatus.IN_CONTEXT;
            } else {
                status = ClusteringContextScore.ClusterStatus.OUT_OF_CONTEXT;
            }

            sc.setStat(STATUS, status.name());
            sc.setStat(MAIN, status == ClusterStatus.IN_CONTEXT ? 0.0 : 1.0);
            return sc;
        }

        /**
         * Analyze an interval, figure out which clusters are spiking in it, and map
         * messages to clusters. A message id that belongs to a cluster that does not
         * spike is removed from the mapping. A cluster is considered spiking if at
         * least 20% of messages mapped to this cluster (according to the clustering
         * training result) appear in the interval.
         * @param interval to profile.
         */
        public void setContext(IInterval interval) {
            if (m_cachedInterval != null && m_cachedInterval.equals(interval)) {
                return;
            }
            m_cachedInterval = interval;

            final int[] clusterSizeInInterval = new int[m_model.m_actualClusters];
            // first pass: calculate how many messages from each cluster appear in
            // interval
            for (IMessageSummary messageSummary : interval.getMessageSummaries()) {
                final Integer clusterId = 
                        m_model.m_msgInternalIdToCluster.get(messageSummary.getMessageInternalId());
                if (clusterId != null) {
                    clusterSizeInInterval[clusterId]++;
                }
            }

            m_contextGoodClusters = new TreeSet<>();
            // second pass: find spiking clusters    
            for (int i = 0; i < m_model.m_actualClusters; ++i) {
                if (clusterSizeInInterval[i] > 1) {
                    final double fraction = (double) clusterSizeInInterval[i]
                            / m_model.getClusterSize(i);
                    if (fraction >= m_config.m_clusterContextFraction) {
                        m_contextGoodClusters.add(i);
                    }
                }
            }
        }

    }

}