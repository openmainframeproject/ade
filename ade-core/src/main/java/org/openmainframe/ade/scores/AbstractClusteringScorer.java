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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.openmainframe.ade.core.ListSortedByKey;
import org.openmainframe.ade.core.MapCounter;
import org.openmainframe.ade.core.clustering.IClustExp;
import org.openmainframe.ade.core.clustering.IClustExp.IClustRunSummary;
import org.openmainframe.ade.core.clustering.IClustExp.Partition;
import org.openmainframe.ade.core.matrix.IDoubleMatrix;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.hub.HubFrameableFramingBlock;
import org.openmainframe.ade.impl.training.ClusteringPartition;
import org.openmainframe.ade.impl.training.MsgMutualInformation;
import org.openmainframe.ade.impl.training.MsgMutualInformationSmoothed;
import org.openmainframe.ade.impl.utils.FileUtils;
import org.openmainframe.ade.scoringApi.MessageScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.IStructuredOutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for clustering based on minimizing the mutual information between
 * the occurrence of message ids within an intervals.
 * 
 * <p>uses tree augmented naive Bayesian techniques</p> 
 */
public abstract class AbstractClusteringScorer extends MessageScorer implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ClusteringUniquifyScore.class);

    public static final String STATUS = "status";
    public static final String CLUSTER_ID = "clusterId";

    /**
     *  Defines the relation between this message-id's cluster and interval. 
     */
    public enum ClusterStatus {
        /** 
         * this message's cluster appears in this interval. 
         */
        IN_CONTEXT,
        /** 
         * this message's cluster does not appear in this interval. 
         */
        OUT_OF_CONTEXT,
        /** 
         * this message was not seen in the training data. 
         */
        NEW,
        /** 
         * no cluster was found for this message in the training data. 
         */
        UNCLUSTERED,
        /** 
         * there is no information regarding this message's cluster. 
         * Most probably clustering was not used. 
         */
        NO_INFO, INVALID, REPRESENTATIVE
    }

    protected Configuration m_config;
    protected Model m_model;
    private transient IFrameableTarget<IInterval, TimeSeparator> m_trainer;
    protected transient Analyzer m_analyzer;
    /**
     * A flag indicating whether this is the first iteration.
     */
    private boolean m_firstIteration = true;
    /**
     * A histogram used in the first iteration in order to filter out the
     * message IDs that appear in less intervals than the required threshold.
     */
    protected transient MapCounter<Integer> mMessageCounter;

    /**
     * Describes the cluster results - is it a valid cluster.
     *
     */
    public enum ClusterUsage {
        UNKNOWN,
        /**
         * This is a good cluster.
         */
        USED,
        /**
         * Fewer members (unique message ids) than  MIN_CLUSTER_SIZE (2) were found.
         */
        TOO_SMALL,
        /**
         * The smallest cluster (least helpful cluster) is discarded.
         */
        LOWEST_SCORE,
        /**
         * Any cluster that is below threshold (doesn't predict occurence of message id)
         * is not used.
         */
        TOO_NOISY
    }

    private ClusteringPartition m_clusteringPartition;

    /**
     * Processes the configuration information from flowlayout.xml.
     * 
     * <p>See toString or @Property for list of values</p>
     * <p.When adding a new parameter for the AbstractClusterinScorer update the
     * toString and @Property</p> 
     */
    public class Configuration implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Clusters with at most this number of message IDs will be removed
         * cluster must contain more than this number of message ids.
         */
        public static final int MIN_CLUSTER_SIZE = 2;

        /**
         * Parameters that control behavior of clustering.
         */
        @Property(key = "trace", help = "Report files about clustering performance are outputed to the trace output directory", required = false)
        private Boolean m_traceOn = false;
        @Property(key = "numClusters", help = "Number of clusters to learn", required = false)
        private int m_numClusters = 30;
        @Property(key = "numClustersSqrtNumMsgs", help = "Use square root of the number of messages as the number of clusters", required = false)
        private boolean m_numClustersSqrtNumMsgs = false;
        @Property(key = "numClustersSqrtNumMsgsFactor", help = "multily square root of the number of messages as the number of clusters", required = false)
        private double m_SqrtFactor = 1.0;
        @Property(key = "numRuns", help = "Number of cluster runs performed to find best partition", required = false)
        private int m_numClusteringRuns = 5;
        @Property(key = "seed", help = "Random seed for run", required = false)
        private int m_seed = 0;
        @Property(key = "maxTrials", help = "Number of attemtst to improve clustering.", required = false)
        private int m_maxTrials = -1;
        @Property(key = "maxIdleTrials", help = "Number of idle tries indecating convergence.", required = false)
        private int m_maxIdleTrials = -1;
        @Property(key = "alpha", help = "Alpha regulation parameter for clustering algorithm.", required = false)
        private double m_alpha = 0.1;
        @Property(key = "clusterContextFraction", help = "Fraction used to determine with a clustered message is in context or not", required = false)
        protected double m_clusterContextFraction = 0.5;
        @Property(key = "minAppearThresh", help = "Minimal number of appearences of a message needed to be considered for clustering.", required = false)
        private int m_minAppearThresh = 3;

        /** 
         * Define minimal allowed ratio between cluster mean info and m_meanInfo. 
         */
        @Property(key = "minAverageInformationRatio", help = "Minimal allowed ratio between cluster mean info and m_meanInfo ??!!!", required = false)
        private Double m_clusterMinAvgInfo = null;
        /** 
         * Use the number of Occurrences to initiate the partition of message ids.
         */
        @Property(key = "useInitialPartitionOccurrence", help = "use the number of occurences of the messages as basis for initial partition", required = false)
        private boolean m_initialPartitionOccurrence = false;
        /*
         * Use file to initialize clustering.
         */
        @Property(key = "initialPartitionFromFile", help = "filename to read initial partion from", required = false)
        private File m_initialPartitionFromFile = null;
        /**
         * Defines the value/price of a singleton cluster relevant only when allowEmptyClusters.
         */
        @Property(key = "singleElementValue", help = "value of empty cluster according to MDL principle", required = false)
        private double m_singleElementValue = -1.0;
        @Property(key = "allowEmptyClusters", help = "Allow empty clusters as mean to chose the number of clusters", required = false)
        private boolean m_allowEmptyClusters = false;
        @Property(key = "clusterSimilarityLevel", help = "value of empty cluster according to MDL principle", required = false)
        private double m_clusterSimilarityLevel = 0.5;
        @Property(key = "useTimelineForMutualInformation", help = "Use the Timeline of messages for persice mutual information calculation", required = false)
        private boolean m_useTimelineMICalculator = false;

        Configuration() {
        }

        @Override
        /**
         * Print out the configuration values found in flowlayout.xml and used for this
         * clustering run.
         */
        public String toString() {

            StringBuilder res = new StringBuilder();
            res.append(String.format("num clusters: %d%n", m_numClusters));
            res.append(String.format("max trials: %d%n", m_maxTrials));
            res.append(String.format("max idle trials: %d%n", m_maxIdleTrials));
            res.append(String.format("alpha: %f%n", m_alpha));
            res.append(String.format("seed: %d%n", m_seed));
            res.append(String.format("trace: %s%n", String.valueOf(m_traceOn)));
            res.append(String.format("num clustering runs: %d%n", m_numClusteringRuns));
            res.append(String.format("min appear thresh: %d%n", m_minAppearThresh));
            res.append(String.format("cluster min avg info: %f%n", m_clusterMinAvgInfo));
            res.append(String.format("cluster context fraction: %f%n", m_clusterContextFraction));
            res.append(String.format("miminal cluster size: %d%n", MIN_CLUSTER_SIZE));
            res.append(String.format("Use square root value of messages as number of clusters: %s%n",
                    String.valueOf(m_numClustersSqrtNumMsgs)));
            res.append(String.format("Cluster square root factor: %f%n", m_SqrtFactor));
            res.append(String.format("Minimal allowed ration between cluster mean and m_meanInfo: %d%n",
                    m_clusterMinAvgInfo));
            res.append(String.format("Use number of message occurences to prime clustering: %s%n",
                    String.valueOf(m_initialPartitionOccurrence)));
            res.append(String.format("Filename containing clusters to prime clustering: %s%n",
                    String.valueOf(m_initialPartitionFromFile)));
            res.append(String.format("Use timeline of messages for precise mutal information: %s%n",
                    String.valueOf(m_useTimelineMICalculator)));
            return res.toString();

        }

        /**
         * Returns of the maximum number of clusters allowed.
         */
        public int getNumClusters() {

            return m_numClusters;
        }

        /**
         * Sets the maximum number of clusters allowed.
         */
        public final void setNumClusters(int num) {

            m_numClusters = num;
        }
    }

    @Override
    /**
     * Check complex property relationship (relationships between property keys).
     */
    protected final void processProperties() throws AdeException {
        if (m_config.m_initialPartitionFromFile != null && m_config.m_initialPartitionOccurrence) {
            throw new AdeUsageException("Two kinds of initial partition were defined: " + m_config.toString());
        }
    }

    /**
     * Using the training set create a model of expected behavior to be compared by
     * analyze to detect unusual behavior in the log stream.
     * <p>Behavior of clustering is controlled by properties set in the flowlayout.xml file
     * for the clustering scorer being used.</p>
     */
    class Model implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 
         * Define Mean information in information matrix. 
         * */
        private double m_meanInfo;

        protected Map<Integer, Integer> m_msgInternalIdToCluster;
        protected Map<Integer, String> m_clusterNames = null;
        private int[] m_clusterSizes;
        protected int m_actualClusters;
        private int m_clustersBeforeFiltering;
        private int m_totalIntervalCount;
        private int m_clusteredMsgCount;
        protected SortedMap<String, Integer> m_seenMsgIds = new TreeMap<>();;
        private int m_msgCountAboveThreshold;
        private int m_msgAppearThreshold;
        private boolean m_converged;

        private ArrayList<String> m_runsSummary;

        /**
         * Constructor for Model - use reset() instead.
         */
        Model() {
            //Take default construction
        }

        /**
         * Creates a summary of the statistics generated by the clustering code.
         * 
         * @param iclust
         * @param clusteredMsgCount - number of messages captured in clusters
         * @param clustersBeforeFiltering - number of cluster created
         * @param totalIntervalCount - number of intervals
         * @param msgAppearThreshold - number of intervals in which a message must appear
         * @param msgCountAboveThreshold - number of messages above threshold
         * @param converged - did clustering converge
         */
        void setRunsSummary(IClustExp iclust, int clusteredMsgCount,
                int clustersBeforeFiltering, int totalIntervalCount,
                int msgAppearThreshold, int msgCountAboveThreshold,
                boolean converged) {
            final List<IClustRunSummary> runsSummary = iclust.getRunsSummary();
            m_runsSummary = new ArrayList<>();
            for (int i = 0; i < runsSummary.size(); ++i) {
                m_runsSummary.add(runsSummary.get(i).toString());
            }
            m_clusteredMsgCount = clusteredMsgCount;
            m_clustersBeforeFiltering = clustersBeforeFiltering;
            m_totalIntervalCount = totalIntervalCount;
            m_msgAppearThreshold = msgAppearThreshold;
            m_msgCountAboveThreshold = msgCountAboveThreshold;
            m_converged = converged;
        }

        /** 
         * Output various summary details about runs of the clustering code. 
         * @param out - print stream handle
         * */
        void printUserSummary(PrintStream out) throws AdeUsageException, AdeInternalException {

            if (!Double.isNaN(m_meanInfo)) {
                out.printf("Mean information in similary matrix: %f%n", m_model.m_meanInfo);
            }    
            out.printf("Runs:%n");
            if (m_runsSummary == null) {
                out.println("No data");
            }
            else 
            {
                for (int i = 0; i < m_runsSummary.size(); ++i) {
                    out.printf("  run %d: %s%n", i, m_runsSummary.get(i));
                }
            }
            out.printf("Resulting clusters: %d%n", m_clustersBeforeFiltering);
            out.printf("Remaining after filtering clusters: %d%n", m_actualClusters);
            out.printf("Total intervals scanned: %d%n", m_totalIntervalCount);
            out.printf("Total msg-ids found: %d%n", m_seenMsgIds.size());
            out.printf("Total msg-ids above min appear threshold %d: %d%n",
                    m_msgAppearThreshold,
                    m_msgCountAboveThreshold);
            out.printf("Total msg-ids clustered: %d%n", m_clusteredMsgCount);
            out.printf("Converged: %s%n", String.valueOf(m_converged));
        }

        void setClusters(Map<Integer, Integer> idToCluster, int num) {
            setClusters(idToCluster, num, null);

        }

        /**
         * SetCluster creates the definition of a cluster.
         * @param idToCluster - internal message id within cluster
         * @param num - number of clusters
         * @param clusterNames - name of cluster (optional)
         */
        public void setClusters(Map<Integer, Integer> idToCluster,
                int num, Map<Integer, String> clusterNames) {
            m_msgInternalIdToCluster = idToCluster;
            m_actualClusters = num;
            m_clusterSizes = new int[m_actualClusters];
            for (int id : m_msgInternalIdToCluster.keySet()) {
                m_clusterSizes[m_msgInternalIdToCluster.get(id)]++;
            }
            m_clusterNames = clusterNames;
        }

        protected int getClusterSize(int i) {
            return m_clusterSizes[i];
        }

        /**
         * Prints the how messages ids have been assigned to a cluster.
         * @param out - output object
         * @param msgId - message id
         * @throws Exception
         */
        public final void printMessageUserData(IStructuredOutputWriter out, String msgId)
                throws Exception {
            final Integer iid = m_seenMsgIds.get(msgId);

            if (iid == null) {
                out.simpleChild("status", "new");
                return;
            }

            final Integer clusterId = m_msgInternalIdToCluster.get(iid);
            if (clusterId == null || clusterId < 0) {
                out.simpleChild("status", "unclusterd");
                return;
            }

            out.simpleChild("status", "clustered");
            out.simpleChild("clusterId", clusterId);
            out.simpleChild("clusterName", getClusterName(clusterId));
            out.simpleChild("clusterSize", getClusterSize(clusterId));
        }

        /**
         * Return number of clusters created.
         */
        public int getNumClusters() {
            return m_actualClusters;
        }

        /**
         * Returns the internal message ids that make up a cluster.
         */
        public final Map<Integer, Integer> getMessageClustersByInternalId() {
            return m_msgInternalIdToCluster;
        }

        /**
         * Returns the cluster name associated with a cluster id.
         */
        public String getClusterName(int key) {
            if (m_clusterNames == null) {
                return null;
            }
            else 
            {
                return m_clusterNames.get(key);
            }
        }
    }

    /**
     * A trainer used for counting the number of intervals each message ID appeared in,
     * And filtering message IDs that didn't appear more than the required threshold of times.
     */
    class MessageIdCounterTrainer implements IFrameableTarget<IInterval, TimeSeparator> {

        MapCounter<Integer> mCounter = new MapCounter<>();

        /**
         * Constructor - use reset() instead of constructor.
         */
        MessageIdCounterTrainer() {
        // Take default construction
        }

        @Override
        public final void incomingObject(IInterval interval) throws AdeException {
            final ArrayList<IMessageSummary> msgSummaries =
                    new ArrayList<>(interval.getMessageSummaries());
            for (IMessageSummary messageSummary : msgSummaries) {
                mCounter.add(messageSummary.getMessageInternalId());
            }
        }

        @Override
        public final void endOfStream() throws AdeException {
            mCounter.removeLessThanThreshold(m_config.m_minAppearThresh + 1);
            mMessageCounter = mCounter;
        }

        @Override
        public void beginOfStream() throws AdeException, AdeFlowException {
            throw new AdeFlowException("should never have been called");
        }

        @Override
        public void incomingSeparator(TimeSeparator sep)
                throws AdeException, AdeFlowException {
            // nothing to do.

        }
    }

    /**
     * A trainer used for calculating the mutual information matrix, and for clustering
     * the message IDs based on it.
     * <p>For memory optimization the illegal messages that appeared less than the required 
     * threshold are filtered (ignored).</p>
     */
    class MITrainer extends HubFrameableFramingBlock<IInterval, TimeSeparator, IInterval, TimeSeparator> {
        private static final int MIN_NUM_CLUSTERS = 2;

        /**
         * An interval target for creating mutual information matrix.
         */
        MsgMutualInformation m_mutualInformationMatrixBuilder;

        /**
         * Matrix to hold the data for clustering.
         */
        IDoubleMatrix m_informationMat = null;

        private int m_totalIntervalCount = 0;

        MITrainer() throws AdeFlowException {
            if (m_config.m_useTimelineMICalculator) {
                m_mutualInformationMatrixBuilder = new MsgMutualInformationSmoothed(mMessageCounter.keySet());
            } else {
                m_mutualInformationMatrixBuilder = new MsgMutualInformation(mMessageCounter.keySet());
            }
            addTarget(m_mutualInformationMatrixBuilder);
            m_totalIntervalCount = 0;
        }

        @Override
        public void incomingObject(IInterval interval) throws AdeException {
            sendObject(interval);
            for (IMessageSummary ms : interval.getMessageSummaries())
            {
                m_model.m_seenMsgIds.put(ms.getMessageId(), ms.getMessageInternalId());
            }
            ++m_totalIntervalCount;
        }

        @Override
        public void endOfStream() throws AdeException {
            sendEndOfStream();
            if (m_config.m_traceOn) {
                FileUtils.createDir(m_scorerEnvironment.m_traceOutputPath);
                m_mutualInformationMatrixBuilder.printReport(new File(m_scorerEnvironment.m_traceOutputPath, 
                        "clustering.matrix.txt"));
            }

            final int numUniqueMsgIds = m_mutualInformationMatrixBuilder.getMutualInformationMatrix().getRowNum();
            if (m_config.m_maxIdleTrials == -1){
                m_config.m_maxIdleTrials = numUniqueMsgIds;
            }
            if (m_config.m_maxTrials == -1){
                m_config.m_maxTrials = numUniqueMsgIds * 100;
            }

            if (m_config.m_numClustersSqrtNumMsgs) {
                m_config.m_numClusters = Math.max(MIN_NUM_CLUSTERS, 
                        (int) (m_config.m_SqrtFactor * Math.sqrt((double) numUniqueMsgIds)));
            }
            int[] initialPartition = null;
            if (m_config.m_initialPartitionOccurrence) {
                m_clusteringPartition.createInitialPartitionByOccurrences(mMessageCounter, 
                        MIN_NUM_CLUSTERS, numUniqueMsgIds);
                initialPartition = m_clusteringPartition.getInitialPartition();
                m_config.m_numClusters = m_clusteringPartition.getNumClusters();
            }

            if (m_config.m_initialPartitionFromFile != null) {
                m_clusteringPartition.createInitialPartitionFromFile(new File(m_scorerEnvironment.m_traceOutputPath, 
                        "clustering.clusterMembers.txt"),
                        m_mutualInformationMatrixBuilder);
                initialPartition = m_clusteringPartition.getInitialPartition();
                m_config.m_numClusters = m_clusteringPartition.getNumClusters();
            }
            if (numUniqueMsgIds <= m_config.m_numClusters) {
                logger.warn("number of unique message IDs (" + 
                        numUniqueMsgIds + ") is not greater than the number of clusters (" + 
                        m_config.m_numClusters + ")");
                m_config.m_numClusters = Math.max(numUniqueMsgIds - 1, 0);
            }
            // if less than three message Ids, put all in one cluster
            if (numUniqueMsgIds < 3) { 
                logger.info("Fewer than 3 messages: setting one cluster per msg-id");
                final SortedMap<Integer, Integer> clustersPerItem = new TreeMap<>();
                for (int msgId : m_mutualInformationMatrixBuilder.getMatIndexToMsgInternalId()) {
                    clustersPerItem.put(msgId, 0);
                }
                m_model.setClusters(clustersPerItem, Math.min(1, numUniqueMsgIds));

            } else {
                performClustering(numUniqueMsgIds, initialPartition);
            }
            m_mutualInformationMatrixBuilder = null;
        }

        /**
         * Internal method to invoke the clustering / pattern recognition code using
         * the following parameters.
         * <p>Clustering works to maximize the mutual information contained within the clusters.</p> 
         * @param numUniqueMsgIds - number of message ids that meet selection criteria
         * @param initialPartition - starting set of clusters
         * @throws AdeException
         */
        private void performClustering(int numUniqueMsgIds, int[] initialPartition) throws AdeException {
            
            logger.info(String.format("Clustering: msg-ids: %d num-clusters:%d", numUniqueMsgIds, 
                    m_config.m_numClusters));

            final IClustExp c = new IClustExp();
            c.setClusterNum(m_config.m_numClusters);
            c.setSeed(m_config.m_seed);
            c.collectRunsSummary();
            c.setMaxIdleTrialNum(m_config.m_maxIdleTrials);
            c.setNumOfMaxIterationsPerRun(m_config.m_maxTrials);
            c.setAlpha(m_config.m_alpha);
            m_informationMat = m_mutualInformationMatrixBuilder.getMutualInformationMatrix();
            c.setVerbosity(System.out, 0);
            c.setRunNum(m_config.m_numClusteringRuns);
            c.setInitialPartition(initialPartition);
            c.setEnableEmptyClusters(m_config.m_allowEmptyClusters);
            c.setSingleElementScore(m_config.m_singleElementValue);
            IClustExp.Partition clusterPartition;
            clusterPartition = c.run(m_informationMat);
            storePartition(c, clusterPartition, m_mutualInformationMatrixBuilder.getMatIndexToMsgInternalId());
        }

        /**
         * Internal method used to write the results of clustering removes clusters which do not meet 
         * threshold and converts internal message id to message id.
         */
        private void storePartition(IClustExp c, Partition clusterPartition, 
                int[] matIndexToMsgInternalId) throws AdeException {
            //remove small and junk cluster (the one with the smallest score)
            m_model.m_meanInfo = Double.NaN;
            ArrayList<ClusterData> clusterData;
            clusterData = calcClusterData(clusterPartition);
            //generate a mapping from msgIDs to their cluster
            int goodClusters = 0;
            final HashMap<Integer, Integer> msgId2Cluster = new HashMap<>();
            final Map<Integer, String> clusterNames = new HashMap<>();

            int clusteredMsgCount = 0;
            for (int i = 0; i < clusterData.size(); ++i) {
                final ClusterData cd = clusterData.get(i);
                if (cd.m_clusterUsage == ClusterUsage.UNKNOWN){
                    throw new AdeInternalException("Internal bug");
                }
                if (cd.m_clusterUsage != ClusterUsage.USED)
                    continue;
                cd.m_id = goodClusters;
                for (int matIndex : cd.m_cluster) {
                    msgId2Cluster.put(matIndexToMsgInternalId[matIndex], cd.m_id);
                    ++clusteredMsgCount;
                }
                ++goodClusters;
            }
            m_clusteringPartition.updateClusters(clusterData, matIndexToMsgInternalId);

            m_model.setClusters(msgId2Cluster, goodClusters, clusterNames);

            if (m_config.m_traceOn) {
                printReport(new File(m_scorerEnvironment.m_traceOutputPath, "clustering.clusters.txt"), clusterData);
                outputClusterMembership(matIndexToMsgInternalId, clusterData);
                m_clusteringPartition.printClusteringDetails(new File(m_scorerEnvironment.m_traceOutputPath, 
                        "clustering.clusteringDetails.txt"));
                m_clusteringPartition.printInitialClusteringChanges(new File(m_scorerEnvironment.m_traceOutputPath, 
                        "clustering.clusteringChanges.txt"), clusterData, matIndexToMsgInternalId);
            }

            m_model.setRunsSummary(c, clusteredMsgCount, clusterPartition.getNumClusters(), m_totalIntervalCount,
                    m_config.m_minAppearThresh,
                    m_mutualInformationMatrixBuilder.getMutualInformationMatrix().getRowNum(),
                    c.isConverged());
        }

        protected void outputClusterMembership(int[] matIndexToMsgInternalId,
                ArrayList<ClusterData> clusterData)
                throws AdeUsageException, AdeException {
            final PrintWriter out = FileUtils.openPrintWriterToFile(new File(m_scorerEnvironment.m_traceOutputPath, 
                    "clustering.clusterMembers.txt"), true);
            m_clusteringPartition.printClusters(out, clusterData, matIndexToMsgInternalId);
            out.close();
        }

        /** Print clustering report to file.
         * 
         * @param file report file to be generated
         * @param clusterPartition raw clustering partition reported
         * @param survivingClusters  subset of the clusters actually used
         */
        private void printReport(File file, ArrayList<ClusterData> clusterData) throws AdeException {
            final PrintWriter out = FileUtils.openPrintWriterToFile(file, true);

            if (!Double.isNaN(m_model.m_meanInfo)){
                out.printf("Mean information in similarity matrix: %f%n", m_model.m_meanInfo);
            }
            out.println("Clusters:");
            final ListSortedByKey<Double, ClusterData> clusterList = new ListSortedByKey<>();
            for (ClusterData cd : clusterData) {
                final double sm = m_mutualInformationMatrixBuilder.calcSimilaritySum(cd.m_cluster);
                clusterList.add(sm, cd);
            }
            clusterList.invertKeyOrdering();

            for (int i = 0; i < clusterList.size(); ++i) {
                final ClusterData cd = clusterList.getValue(i);
                final TreeSet<Integer> c = cd.m_cluster;
                out.printf("%d. Cluster id=%d, Used=%s, Size=%d, ", i, cd.m_id, cd.m_clusterUsage.toString(), c.size());
                if (!Double.isNaN(cd.m_score)){
                    out.printf("Score=%f", cd.m_score);
                }
                if (!Double.isNaN(cd.m_meanInfo)){
                    out.printf(", meanInformation=%f", cd.m_meanInfo);
                }
                out.println();
                int counter = 0;
                for (int m : c) {
                    out.printf("\t%d. ", ++counter);
                    m_mutualInformationMatrixBuilder.printMemberReport(out, m, cd.m_cluster);
                }
            }
            out.close();

        }

        private ArrayList<ClusterData> calcClusterData(Partition partition) throws AdeInternalException {
            final ArrayList<ClusterData> clusterData = new ArrayList<>(partition.getNumClusters());

            calcMeanInfo();

            double lowestScore = Double.MAX_VALUE;
            int smallestScoreClusterI = -1;

            final double threshold = m_config.m_clusterMinAvgInfo == null ? -1 : m_config.m_clusterMinAvgInfo * m_model.m_meanInfo;

            for (int i = 0; i < partition.getNumClusters(); ++i) {
                final TreeSet<Integer> clusterElements = partition.getClusterElements(i);
                final ClusterData cd = new ClusterData();
                cd.m_cluster = clusterElements;
                clusterData.add(cd);
                if (clusterElements.size() < Configuration.MIN_CLUSTER_SIZE) {
                    cd.m_clusterUsage = ClusterUsage.TOO_SMALL;
                    continue;
                }

                cd.m_meanInfo = calcClusterMeanInfo(clusterElements);
                if (cd.m_meanInfo < threshold){
                    cd.m_clusterUsage = ClusterUsage.TOO_NOISY;
                }
                else
                {
                    cd.m_clusterUsage = ClusterUsage.USED;
                }
                cd.m_score = partition.getClusterScore(i);
                if (cd.m_score < lowestScore) {
                    smallestScoreClusterI = i;
                    lowestScore = cd.m_score;
                }
            }
            if (smallestScoreClusterI >= 0){
                clusterData.get(smallestScoreClusterI).m_clusterUsage = ClusterUsage.LOWEST_SCORE;
            }
            return clusterData;
        }

        private double calcClusterMeanInfo(TreeSet<Integer> members) throws AdeInternalException {
            if (members.size() == 1){
                return 0;
            }
            final SummaryStatistics sumS = new SummaryStatistics();
            for (int i : members){
                for (int j : members) {
                    if (i <= j){
                        continue;
                    }
                    double v;
                    v = m_informationMat.get(i, j);
                    if (!Double.isNaN(v)){
                        sumS.addValue(v);
                    }
                }
            }
            final double res = sumS.getMean();
            if (Double.isNaN(res)){
                return 0;
            }
            return res;

        }

        private void calcMeanInfo() throws AdeInternalException {
            final SummaryStatistics sumS = new SummaryStatistics();
            for (int i = 0; i < m_informationMat.getRowNum(); ++i){
                for (int j = i + 1; j < m_informationMat.getColNum(); ++j) {
                    double v;
                    v = m_informationMat.get(i, j);
                    if (!Double.isNaN(v)){
                        sumS.addValue(v);
                    }
                }
            }
            m_model.m_meanInfo = sumS.getMean();
        }

        @Override
        /**
         * Should not be invoked.
         */
        public void beginOfStream() throws AdeException, AdeFlowException {
            throw new AdeFlowException("should never have been called");
        }

        @Override
        /**
         * Use separator to end the interval - frame.
         */
        public void incomingSeparator(TimeSeparator sep) throws AdeException,
                AdeFlowException {
            sendSeparator(sep);
        }

    }

    @Override
    /**
     * Reset of public variables used instead of constructor at the end of the interval or end
     * of stream.
     */
    protected final void reset() throws AdeException {
        m_config = new Configuration();
        super.reset();
        m_analyzer = null;
        m_trainer = null;
        m_model = null;
        m_clusteringPartition = null;
        m_firstIteration = true;
    }

    @Override
    public final Configuration getConfigurableObject() {
        logger.info("configurableObjectInternal in " + this.getClass().getSimpleName() + "=" + m_config);
        if (m_config == null){
            throw new RuntimeException("m_config is null.  How did we get here?");
        }
        return m_config;
    }

    @Override
    public final boolean needsAnotherIteration() throws AdeException {
        return m_model == null;
    }

    @Override
    /**
     *  The first iteration only counts the number of intervals in which each message ID 
     *  Appear. When the iteration is completed, the set of (legal) messages that appeared more
     *  than the required threshold is passed to the following iteration for processing.
     */
    public void startIteration() throws AdeException {

        if (m_firstIteration) {
            logger.info("Starting first iteration");
            m_trainer = new MessageIdCounterTrainer();
            m_firstIteration = false;
        } else {
            logger.info("Starting non-first iteration");
            m_trainer = new MITrainer();
            if (m_model != null){
                throw new AdeInternalException("Already trained");
            }
            m_model = new Model();
        }
        m_clusteringPartition = new ClusteringPartition(m_config.m_numClusters, m_config.m_clusterSimilarityLevel);
    }

    @Override
    public void incomingObject(IAnalyzedInterval analyzedInterval) throws AdeException {
        m_trainer.incomingObject(analyzedInterval.getInterval());
    }

    @Override
    public void endOfStream() throws AdeException {
        m_trainer.endOfStream();
        m_trainer = null;
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        //  Apparently, there is nothing to do here.  Move along.   
    }

    @Override
    /**
     * Provides information about the behavior of cluster.
     * configuration used and 
     * clusters that resulted.
     */
    public final void debugPrint(PrintStream out) throws AdeException {
        super.debugPrint(out);
        out.println("Configuration");
        out.println(m_config.toString());
        out.println("Model summary");
        m_model.printUserSummary(out);
        out.println("clustering members");
        for (Entry<String, Integer> msgEntry : m_model.m_seenMsgIds.entrySet()) {
            String clusterName = "-";
            Integer clusterNumber = null;
            final Integer MsgInternalId = msgEntry.getValue();
            if (MsgInternalId != null) {
                clusterName = m_model.getClusterName(MsgInternalId);
                clusterNumber = m_model.m_msgInternalIdToCluster.get(MsgInternalId);
            }
            if (clusterNumber != null) {
                if (clusterName != null) {
                    out.println(msgEntry.getKey() + "\t" + MsgInternalId + "\t" + clusterNumber + "\t" + clusterName);
                } else {
                    out.println(msgEntry.getKey() + "\t" + MsgInternalId + "\t" + clusterNumber);
                }
            } else {
                out.println(msgEntry.getKey() + "\t" + MsgInternalId + "\t" + -1 + "\t" + "unclustered");
            }
        }
    }

    @Override
    /**
     * Print out information about the cluster for a message id.
     */
    public void printMessageUserData(IStructuredOutputWriter out, String msgId)
            throws Exception {
        if (m_model == null){
            throw new AdeInternalException("No trained model");
        }
        m_model.printMessageUserData(out, msgId);
    }

    @Override
    public final void printGeneralUserData(IStructuredOutputWriter out) throws Exception {
        super.printGeneralUserData(out);
        out.simpleChild("config", "%n" + m_config.toString());
        final ByteArrayOutputStream temp = new ByteArrayOutputStream();
        final PrintStream ps = new PrintStream(temp, false, StandardCharsets.UTF_8.name());
        m_model.printUserSummary(ps);
        ps.close();
        out.simpleChild("summary", "%n" + temp.toString(StandardCharsets.UTF_8.name()));
    }
    /**
     * Initialize cluster data.
     */
    public static class ClusterData {
        public ClusterUsage m_clusterUsage = ClusterUsage.UNKNOWN;
        public TreeSet<Integer> m_cluster = null;
        public int m_id = -1;
        public double m_meanInfo = Double.NaN;
        public double m_score = Double.NaN;
    }

    /**
     * Setup cluster parameters from properties file. 
     */
    public AbstractClusteringScorer() {
        super();
        m_config = new Configuration();
    }

    /**
     * Return number of clusters.
     */
    public int getNumClusters() {
        return m_model.getNumClusters();
    }

    /**
     * Returns cluster information for a message internal id.
     */
    public Map<Integer, Integer> getMessageClustersByInternalId() {
        return m_model.getMessageClustersByInternalId();
    }

    /**
     * Invoked to analyze an interval from a stream of message to determine
     * if the message ids within the interval window are part of a cluster found
     * during training.
     */
    abstract protected class Analyzer {

        abstract public StatisticsChart getScore(IAnalyzedMessageSummary ams,
                IAnalyzedInterval analyzedInterval) throws AdeException;

    }
}
