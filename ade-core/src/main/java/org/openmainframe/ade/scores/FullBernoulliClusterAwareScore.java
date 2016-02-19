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

import java.io.PrintStream;
import java.util.Map.Entry;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extension of the Bernoulli Scorer that calculates the raw bernoulli probability and 
 * a normalized version where the the average log-likelihood is about zero. Also calculates
 * associated statistics such as anomaly, log probabilities, and "out of context" probability.
 */
public class FullBernoulliClusterAwareScore extends BernoulliScore {

    /**
     * String constant for property key.
     */
    private static final String BASE_CLUSTERING_SCORER = "ClusteringScorer";

    /**
     * Default logger for class.
     */
    private static final Logger logger = LoggerFactory.getLogger(FullBernoulliClusterAwareScore.class);

    /**
     * The serialized ID.
     */
    private static final long serialVersionUID = 1L;

    @Property(key = BASE_CLUSTERING_SCORER, required = true, help = "Clustering Scorer used to determine "
            + "if message is in context")
    private String m_clusteringScorer;

    @Property(key = "FullProb", required = false, help = "Return Prob/(1-Prob) rather than Prob, to move the "
            + "average log-likelihood to be about zero")
    private boolean fullProbability = true;
    
    /**
     * Class for keeping track of the data for this message (Some of the data includes 
     * message count, score, probability, etc.)
     */
    static public class FullBernoulliMsgData extends BernoulliScore.MsgData {
        private static final long serialVersionUID = 1L;
        /**
         * Keeps track of this message's out of cluster count or in other words, when a cluster 
         * is NOT "IN CONTEXT" (ie. a cluster where a certain percentage of messages mapped 
         * to this cluster appear in this interval).
         */
        public int m_outOfClusterCount = 0;
        
        /**
         * The probability that this message is considered "out of cluster" (Determined by seeing how many
         * times this message was "out of cluster" over the total number of analyzed intervals)
         */
        public double m_outOfClusterProb = 0;
        
        /**
         * Determined by taking the negative log of the "out of cluster" probability.
         */
        public double m_outOfContextScore = 0;
        
        /**
         * toString for printing out the message data collected from "BernoulliScore" and this class.
         */
        public String toString() {
            return String.format("(count=%d, prob=%f, score=%f | outOfClusterCount=%d, OutOfClusterProb=%f, "
                    + "outOfContextScore=%f)", m_count, m_prob, m_score, m_outOfClusterCount, 
                    m_outOfClusterProb, m_outOfContextScore);
        }
    }
    
    /**
     * Extract all messages from an analyzed interval and retrieves each message's data (msg count, 
     * probability, and score) During this operation, we increment the current count for this 
     * particular message and we check if this message is in a cluster that is "IN CONTEXT"
     * (ie. a cluster where a certain percentage of messages mapped to this cluster appear
     * in this interval) If it is a cluster that is NOT "IN CONTEXT" then we increment
     * the "outOfClusterCount".
     * @param analyzedInterval contains a summary of the interval i.e. information such as time, number 
     * of message ids, etc.
     */
    @Override
    public void incomingObject(IAnalyzedInterval analyzedInterval) throws AdeException {
        for (IAnalyzedMessageSummary ams : analyzedInterval.getAnalyzedMessages()) {
            final IMessageSummary ms = ams.getMessageSummary();
            final String id = ms.getMessageId();
            FullBernoulliMsgData data = (FullBernoulliMsgData) m_msgData.get(id);
            if (data == null) {
                data = new FullBernoulliMsgData();
                m_msgData.put(id, data);
            }
            ++data.m_count;
            if (!AbstractClusteringScorer.ClusterStatus.IN_CONTEXT.name().equals(ams.getStatistics()
                    .getStringStat(m_clusteringScorer + "." + ClusteringContextScore.STATUS))) {
                ++data.m_outOfClusterCount;
            }
        }
        ++m_totalIntervalCount;
    }
    
    /**
     * Calculate the probabilities and scores for each message's data after going through all the
     * analyzed intervals. Message probability is calculated by dividing the total number of times
     * the message appeared by the total number of analyzed intervals gone through. The message
     * score is calculated by taking the negative log of the message probability. Out of cluster 
     * probability and out of context score are also calculated.
     */
    @Override
    public void endOfStream() throws AdeException {
        double minProb = 1;
        // to avoid dividing by zero if no intervals were trained on
        if (m_totalIntervalCount == 0) {
            m_totalIntervalCount = 1;
        }
        for (Entry<String, BernoulliScore.MsgData> entry : m_msgData.entrySet()) {
            final FullBernoulliMsgData data = (FullBernoulliMsgData) entry.getValue();
            data.m_prob = ((double) data.m_count + 0.5) / (m_totalIntervalCount + 1.0);
            if (data.m_prob < 0 || data.m_prob >= 1.0) {
                logger.info(entry.getKey() + ":  bad m_prob: " + data.m_prob + "=" + data.m_count + "+0.5 / " 
                        + m_totalIntervalCount + "+1");
            }
            if (data.m_prob < minProb) {
                minProb = data.m_prob;
            }
            data.m_outOfClusterProb = ((double) data.m_outOfClusterCount + 0.5) / (m_totalIntervalCount + 1.0);
            if (data.m_outOfClusterCount < minProb && data.m_outOfClusterCount > 0.0) {
                minProb = data.m_outOfClusterCount;
            }
        }

        for (BernoulliScore.MsgData dataSuper : m_msgData.values()) {
            final FullBernoulliMsgData data = (FullBernoulliMsgData) dataSuper;
            final double probability = data.m_prob;
            data.m_score = -Math.log(probability);
            if (data.m_score > 1) {
                data.m_score = 1;
            }
            final double outOfContextProbability = data.m_outOfClusterProb;
            data.m_outOfContextScore = -Math.log(outOfContextProbability);
            if (data.m_outOfContextScore > 1) {
                data.m_outOfContextScore = 1;
            }

        }
        m_trained = true;
    }

    /**
     * Compute the final probability from the bernoulli message data and add various computations 
     * to the statistics chart. Calculate the anomaly by subtracting from 1 the probability a message
     * appears within the total number of analyzed intervals.
     * @param ams The analysis results of a MessageSummary object. Message summaries contain 
     * statistics and information on message instances. i.e. text body message, message id, severity, etc.
     * @param analyzedInterval contains a summary of the interval i.e. information such as time, number of 
     * message ids, etc.
     * @return The StatisticsChart for collecting double and string statistics.
     */
    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary ams, IAnalyzedInterval interval) throws AdeException {
        final StatisticsChart sc = new StatisticsChart();
        final FullBernoulliMsgData data = (FullBernoulliMsgData) m_msgData.get(ams.getMessageId());
        double prob = 1.0;
        double finalProb = 0;
        if (data == null) {
            prob = 0.5 / m_totalIntervalCount;
            if (fullProbability) {
                finalProb = prob / (1 - prob);
            } else {
                finalProb = prob;
            }
            sc.setStat(MAIN, 1.0);
        } else {
            if (ams.getStatistics().getStringStat(m_clusteringScorer + "." + ClusteringContextScore.STATUS).equals(
                    AbstractClusteringScorer.ClusterStatus.OUT_OF_CONTEXT.name())) {
                prob = data.m_outOfClusterProb;
            } else {
                prob = data.m_prob;
            }
            if (fullProbability) {
                finalProb = prob / (1 - prob);
            } else {
                finalProb = prob;
            }
            sc.setStat(MAIN, 1 - finalProb);
        }
        sc.setStat(PROBABILITY, finalProb);
        sc.setStat(LOG_PROB, -Math.log(finalProb));
        sc.setStat("rawLogProb", -Math.log(prob));
        sc.setStat("rawAnomaly", 1 - prob);
        sc.setStat("frequency", 1 / prob);
        return sc;
    }
    
    /**
     * Print method for outputting the Bernoulli and interval information.
     * @out the print stream for printing out member variable information.
     */
    @Override
    public void debugPrint(PrintStream out) throws AdeException {
        super.debugPrint(out);
        out.println("Trained=" + m_trained);
        out.println("Total interval count=" + m_totalIntervalCount);
        for (String key : m_msgData.keySet()) {
            final FullBernoulliMsgData value = (FullBernoulliMsgData) m_msgData.get(key);
            out.println(key + " : " + value);
        }
    }

}