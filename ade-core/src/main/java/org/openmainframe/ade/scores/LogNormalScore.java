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
import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.scoringApi.MessageScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.IStructuredOutputWriter;

/**
 * Calculate the log probability and related statistics for normally distributed message instances.
 */
public class LogNormalScore extends MessageScorer {

    /**
     * Constants for StatisticsChart.
     */
    public static final String IS_NEW = "isNew";
    public static final String MEAN = "mean";
    /**
     * The serial ID.
     */
    private static final long serialVersionUID = 1L;

    @Property(key = "baseScorer", required = false, help = "Clustering Scorer used to determine if message is in context - when active clustered messages are ignored in train and analysis")
    private String m_baseScorer = null;

    @Property(key = "badFitThreshold", required = false, help = "If in any interval in the train, the copy number score is above this, we declare the message un-learnable")
    private double m_badMessageCountScoreThreshold = 15.;

    /**
     * Hold message data for each message.
     */
    static public class MsgData implements Serializable {
        /**
         * The serial ID.
         */
        private static final long serialVersionUID = 1L;
        /**
         * The log of the total number of unclustered messages in all intervals.
         */
        public double m_totalLogCounts = 0;
        /**
         * The total number of intervals a message appears in and is not in a cluster.
         */
        public int m_intervalCount = 0;
        /**
         * The poisson parameter, lamba. In this case it is the average log value for the total 
         * number of messages over all intervals.
         */
        public double m_lambda = 0;
        /**
         * the total number of intervals a message appears in.
         */
        public int m_intervalAllCount = 0;
        /**
         * The number of appearances this message has appeared in all intervals.
         */
        public int m_allCount;
        /**
         * The max number this message has appeared in a single interval.
         */
        public int m_maxNumAppearance = 0;
        /**
         * Determine if bad fit based on log probability calculated.
         */
        public boolean m_badFit = false;

        /**
         * Print out the current state of message data.
         */
        public String toString() {
            return String.format("sumOfLogCounts=%f intervalCount=%d lambda=%f intervalAllCount=%d allCount=%d, maxAppear=%d, badFit=%b",
                    m_totalLogCounts, m_intervalCount, m_lambda, m_intervalAllCount, m_allCount, m_maxNumAppearance, m_badFit);
        }
    }

    /**
     * Mapping from message id to its message data.
     */
    private SortedMap<String, MsgData> m_msgData = null;

    /**
     * If we have used this scorer to train.
     */
    private boolean m_trained = false;

    /**
     * Total number of intervals this scorer has seen.
     */
    private int m_totalIntervalCount = 0;
    /**
     * The minimum lambda value. 
     */
    private double m_minLambda;

    static final String NEW_MESSAGE_EXPECTED_MEAN = "newMessageExpectedMean";
    @Property(key = NEW_MESSAGE_EXPECTED_MEAN, help = "Mean number of appearances for a \"typical\" message the first time it appears in the stream", required = false)
    private double m_newMessageMeanNumAppear = 2.0;
    @Property(key = "noPeneltyOnMean", help = "Scale up the score so that on when the number of messages matches the mean, the score is zero", required = false)
    private boolean m_scaleUpMean = false;

    /**
     * Log of the average number of appearances for a message that was shown for the first time.
     */
    private Double m_newMessageLambda = null;

    /**
     * Retrieves the property value for m_newMessageMeanNumAppear and use it to calculate
     * the new message lambda value.
     */
    @Override
    public void processProperties() throws AdeUsageException {
        if (m_newMessageMeanNumAppear <= 0) {
            throw new AdeUsageException("The scorer property " + NEW_MESSAGE_EXPECTED_MEAN + " must be set greater then zero");
        }
        m_newMessageLambda = Math.log(m_newMessageMeanNumAppear);

    }

    /**
     * Reset the object state to that of a newly created scorer.
     */
    @Override
    protected void reset() throws AdeException {
        super.reset();
        m_trained = false;
        m_msgData = null;
    }

    /**
     * Determine if the learner needs another iteration.
     */
    @Override
    public boolean needsAnotherIteration() throws AdeException {
        return !m_trained;
    }

    /**
     * Start the iteration and initialize variables.
     */
    @Override
    public void startIteration() throws AdeException {
        if (m_trained) {
            throw new AdeInternalException("Already trained");
        }
        m_msgData = new TreeMap<String, MsgData>();
        m_totalIntervalCount = 0;
        m_minLambda = 1.0;
    }

    /**
     * Create MsgData objects for each message id in an analyzed interval. If the message is not in a cluster, then we 
     * add to the total log value of the number of messages in an interval, and interval count. For all messages, we keep 
     * track of the total number of message instances, and the max number of appearances this message instance has appeared 
     * over all intervals.
     * @param analyzedInterval contains interval summary
     */
    @Override
    public void incomingObject(IAnalyzedInterval analyzedInterval) throws AdeException {
        for (IAnalyzedMessageSummary ams : analyzedInterval.getAnalyzedMessages()) {
            final String id = ams.getMessageId();

            MsgData data = m_msgData.get(id);
            if (data == null) {
                data = new MsgData();
                m_msgData.put(id, data);
            }
            if (!isClustered(ams)) {
                data.m_totalLogCounts += Math.log(ams.getNumberOfAppearances());
                data.m_intervalCount++;
            }
            data.m_allCount += ams.getNumberOfAppearances();
            data.m_intervalAllCount++;
            if (data.m_maxNumAppearance < ams.getNumberOfAppearances()) {
                data.m_maxNumAppearance = ams.getNumberOfAppearances();
            }
        }
        ++m_totalIntervalCount;
    }
    /**
     * At the end of the stream, for all the message data we've collected,
     * we calculate the lamba value and determine if the message data
     * is a "bad fit" i.e. the log probability is greater than or equal to some 
     * threshold.
     */
    @Override
    public void endOfStream() throws AdeException {
        for (MsgData data : m_msgData.values()) {
            if (data.m_totalLogCounts > 0) {
                data.m_lambda = data.m_totalLogCounts / data.m_intervalCount;
                m_minLambda = Math.min(data.m_lambda, m_minLambda);
                if (locateMismatchingDistributions(data)) {
                    data.m_badFit = true;
                }
            }
        }
        m_trained = true;
    }

    /**
     * Calculate the log probability of the log-normally distributed message. Determine if
     * the value is greater than a threshold value to determine if it is a bad fit.
     * @param data the message data.
     * @return true if the log probability is greater than or equal to threshold.
     * @throws AdeException
     */
    private boolean locateMismatchingDistributions(MsgData data) throws AdeException {

        final double maxScore = -calcLogProb(data.m_maxNumAppearance, data.m_lambda);
        return (maxScore >= m_badMessageCountScoreThreshold);
    }

    /**
     * Perform the calculations to get the log probability for a normally distributed message instance.
     * Add this information in the statistics chart. LOG_PROB is the log probability. IS_NEW is determined by 
     * if the message data was seen during train. MEAN is the average of the lognormal distribution. MAIN is
     * the normal distributed probability value subtracted from 1.
     * @param ams The analysis results of a MessageSummary object. Message summaries contain 
     * statistics and information on message instances. i.e. text body message, message id, severity, etc.
     * @param interval contains a summary of the interval i.e. information such as time, number of 
     * message ids, etc.
     * @return The StatisticsChart for collecting double and string statistics.
     */
    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary ams, IAnalyzedInterval interval) throws AdeException {
        final StatisticsChart sc = new StatisticsChart();

        final MsgData data = m_msgData.get(ams.getMessageId());
        final boolean seenInTraining = (data != null);
        final boolean noLambda = (data == null || data.m_intervalCount == 0);

        double mu = 0;
        if (noLambda) {
            if (m_newMessageLambda != null) {
                mu = m_newMessageLambda;

            } else {
                mu = m_minLambda;
            }
        } else {
            mu = data.m_lambda;
        }

        final int numInstances = ams.getMessageSummary().getNumMessageInstances();
        double logProb = 0;
        double anomaly = 0;

        if (m_totalIntervalCount > 0 && !isClustered(ams) && seenInTraining && !data.m_badFit) {
            logProb = calcLogProb(numInstances, mu);
            if (m_scaleUpMean) {
                logProb -= calcLogProbAtMu(mu);
            }

            anomaly = 1.0 - Math.exp(logProb);
        }

        sc.setStat(LOG_PROB, -logProb);
        sc.setStat(IS_NEW, seenInTraining ? 0.0 : 1.0);
        sc.setStat(MEAN, Math.exp(mu));
        sc.setStat(MAIN, anomaly);

        return sc;
    }

    /**
     * Determines whether the message's cluster appears in the interval or not.
     * @param ams message summary for all message instances. 
     * @return true if the message's cluster appears in the interval (i.e. IN_CONTEXT).
     */
    private boolean isClustered(IAnalyzedMessageSummary ams) {
        if (m_baseScorer != null) {
            final Double context = ams.getStatistics().getDoubleStat(m_baseScorer + "." + MAIN);
            return context != null && context == 0;
        } else {
            return false;
        }
    }

    /**
     * Part of the lognormal distribution equation.
     */
    static final double s_logSqrtTwoPi = Math.log(Math.sqrt(2 * Math.PI));

    /**
     * Calculate the log probability given the message's logarithm is normally distributed. Note, here we 
     * take the natural logarithm of the normal distribution (lognormal distribution) which using log properties 
     * and rules turns into the equation whose value gets stored in res.
     * @param numAppear the number of times this message appears.
     * @param mu mean or average value.
     * @return the log of the normal distribution.
     */
    static private double calcLogProb(int numAppear, double mu) {

        final double sigma = calcSigmaFromMu(mu);

        final double logNumAppear = Math.log(numAppear);

        // we do not want to complain about smaller numbers than expected.
        final double overMu = Math.max(logNumAppear - mu, 0.0);

        final double res = -(1.0 / 2) * (overMu * overMu) / (sigma * sigma) - s_logSqrtTwoPi - Math.log(sigma);

        assert (res <= 0);
        return res;
    }

    /**
     * Calculate the log probability for a normally distributed message centered at mu.
     * @param mu the mean/average.
     * @return The lognormal probability for X = mu.
     */
    static private double calcLogProbAtMu(double mu) {

        final double sigma = calcSigmaFromMu(mu);

        return (-s_logSqrtTwoPi - Math.log(sigma));
    }

    /**
     * Calculate the standard deviation (sigma). Note that for Poisson distributions the
     * variance is equal to expected value (mu).
     * @param mu the mean or average value.
     * @return the standard deviation.
     */
    protected static double calcSigmaFromMu(double mu) {
        double sigma = Math.sqrt(mu);
        if (sigma < 0.5) {
            sigma = 0.5;
        }
        return sigma;
    }

    /**
     * Print out object state for debugging purposes.
     * @param out the print stream for printing out object state.
     */
    @Override
    public void debugPrint(PrintStream out) throws AdeException {
        super.debugPrint(out);
        out.println("Trained=" + m_trained);
        out.println("Total interval count=" + m_totalIntervalCount);
        out.println("min lambda=" + m_minLambda);
        for (String key : m_msgData.keySet()) {
            final MsgData value = m_msgData.get(key);
            out.println(key + " : " + value);
        }
    }

    /**
     * Print out general summary of the model.
     * @param out An output stream for printing out structured information.
     */
    @Override
    public void printGeneralUserData(IStructuredOutputWriter out) throws Exception {
        out.simpleChild("intervalCount", m_totalIntervalCount);
        out.simpleChild("uniqueMsgIds", m_msgData.size());
    }

    /**
     * Prints out model information for a specific message id.
     * @param out output stream for printing out structured information.
     * @param msdId the message id we want to print information on.
     */
    @Override
    public void printMessageUserData(IStructuredOutputWriter out, String msgId) throws Exception {
        final MsgData data = m_msgData.get(msgId);
        if (data == null) {
            out.simpleChild("totalInstances", 0);
            return;
        }
        out.simpleChild("intervalSeenIn", data.m_intervalAllCount);
        out.simpleChild("totalInstances", data.m_allCount);
        out.simpleChild("intervalsSeenInOutOfContext", data.m_intervalCount);
        out.simpleChild("totalInstancesOutOfContext", data.m_totalLogCounts);
        if (data.m_totalLogCounts > 0) {
            out.simpleChild("estimatedLambda", data.m_lambda);
            out.simpleChild("instanceCountForAnomaly9", calcThreshold(0.9, data.m_lambda));
            out.simpleChild("instanceCountForAnomaly99", calcThreshold(0.99, data.m_lambda));
        }
    }

    /** 
     *  Calculates the minimal number of instances required to achieve a score as high as the
     *  given scoreThreshold for the given lambda.
     *  @param scoreThreshold the threshold value for determining minimum number of message
     *  instances for the given lamba.
     *  @param lambda the lamdba value as in the Poisson distribution.
     *  @return The minimal number of instances.
     */
    static private String calcThreshold(double scoreThreshold, double lambda) {
        // Convert the score threshold to a threshold on log(P) by inverting the score formula
        // score=0.999*(1-P)
        // which gives:
        // 0.999*(1-P)>=scoreThreshold
        // P<=1-scoreThreshold/0.999
        // log(P)<=log(1-scoreThreshold/0.999)
        final double logProbThreshold = Math.log(1 - scoreThreshold / 0.999);
        int maxNum = 1000000;
        // If 1e6 is not enough to pass the score threshold, return ">1e6"
        if (calcLogProb(maxNum, lambda) > logProbThreshold) {
            return ">1e6";
        }
        // Use binary search to find the number
        int minNum = 1;
        while (minNum < maxNum) {
            final int midNum = (maxNum + minNum) / 2;
            // Invariant:
            // maxNum passes the threshold
            // minNum<=midNum<maxNum
            final boolean passThreshold = calcLogProb(midNum, lambda) <= logProbThreshold;
            if (passThreshold) {
                maxNum = midNum;
            } else {
                // midNum==minNum if maxNum=minNum+1, in which case maxNum is the lowest that passes the threshold
                if (midNum == minNum) {
                    break;
                }
                minNum = midNum;
            }
        }
        return Integer.toString(maxNum);
    }

    /**
     * Retrieves the size of the message ID to MsgData mapping.
     * @return the number of message id's we have mapped to MsgData.
     */
    public int getNumRecords() {
        return m_msgData.size();
    }

    /**
     * Get the msg data record for the specified message id.
     * @param msgId the message id.
     * @return The msgData retrieved from specific message id.
     */
    public MsgData getRecord(String msgId) {
        return m_msgData.get(msgId);
    }

    /**
     * Add in a new record i.e. a new MsgData into the mapping.
     * @param messageId the message id to add into the mapping.
     * @param i the total log count for a message over all intervals.
     * @param d lambda value.
     */
    public void addRecord(String messageId, int i, double d) {
        if (m_msgData == null) {
            m_msgData = new TreeMap<String, LogNormalScore.MsgData>();
            m_minLambda = 1;
        }
        final MsgData data = new MsgData();
        data.m_totalLogCounts = i;
        data.m_lambda = d;
        m_msgData.put(messageId, data);
        m_minLambda = Math.min(data.m_lambda, m_minLambda);
        // Artificially set number of intervals to 1.
        // If it remains 0, it will shut down calculation in getScore
        m_totalIntervalCount = 1;
    }

    /**
     * Retrieve the number of message instances that are not part of a
     * cluster by using the "m_totalLogCounts" variable (This is ONLY calculated
     * if a message instance is NOT in a cluster)
     * @return the number of message instances not in a cluster.
     */
    public int getNumOutOfContextRecords() {
        int res = 0;
        for (MsgData d : m_msgData.values()) {
            if (d.m_totalLogCounts > 0) {
                res++;
            }
        }
        return res;
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
    }
}
