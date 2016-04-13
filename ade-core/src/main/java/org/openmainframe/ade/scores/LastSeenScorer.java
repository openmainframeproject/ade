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
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.scoringApi.MessageScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculate scores for the deltas belonging to a message id. A delta is defined as the
 * time difference in seconds between two of the same message instances. (ie. two messages
 * with the same message id) For example, if we have some message instance X with id MSGID 
 * appearing at time T1 then the delta value is T1 - T2 where T2 < T1 and T2 is the time of
 * the MOST RECENT message instance Y with id MSGID. These deltas are used to compute log
 * probabilities which gets stored in the StatisticsChart.
 */
public class LastSeenScorer extends MessageScorer {
    /**
     * Default logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(LastSeenScorer.class);
    /**
     * The serialized ID.
     */    
    private static final long serialVersionUID = 1L;
    
    private static final double DEFAULT_FOR_NEW = 0d;
    @Property(key = "minimalValueForTallesBin", help = "Minimal count for most common bin.  Less than this and no model will be created.", required = false)
    private int m_minimalMax = 10;
    @Property(key = "minimalConcentration", help = "minimal required average points per bin", required = false)
    private double m_minimalConcentration = 2.0;

    /**
     * Given a sequence of points in R1 (e.g. times) x1 <= x2 ... <= xn (the
     * input is the distances [xi+1 - xi]) Look whether there are typical
     * differences [xi+1 - xi] (up to 3 modes) build clusters around the
     * mode. The likelihood in each cluster is then by Chebychev inequality
     * a new point will be considered anomaly if there is a significant
     * difference between the probability in the training points and the
     * point in question.
     */
    private class PerodicityBounder implements Serializable {
        
        /**
         * Stores the value of log(.5).
         */
        private final double m_logHalf = Math.log(0.5);
        /**
         * The serial ID.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Threshold for the minimal points per cluster.
         */
        int mMinimalPointsPerCluster;
        /**
         * For determining if we have used this scorer during training.
         */
        boolean mTrained;

        /**
         * Maps the time difference in seconds from the most recently seen message instance since some message 
         * instance x with the same message id to the number of times this time difference has been seen.
         */
        TreeMap<Integer, Integer> mPointDifferences;
        /**
         * The total number of points we have added.
         */
        int mPoints;

        /**
         * Maps point/delta/ values to its score where score is the log of the ratio between max value 
         * and a point's value. (max is the time difference that occurs the most in mPointDifferences and a 
         * point's value is the number of occurrences for this time difference/point).
         */
        private TreeMap<Integer, Double> m_pointScores;
        /**
         * Log of the max delta/point value.
         */
        private double m_lMax = 0;
        /**
         * The log of ratio between the max point/delta and the total number of points/deltas.
         */
        private double m_llMax = 0;

        /**
         * Default constructor for initializing member variables.
         */
        public PerodicityBounder() {
            mMinimalPointsPerCluster = 10;
            mTrained = false;
            mPointDifferences = new TreeMap<Integer, Integer>();
            m_pointScores = null;
            mPoints = 0;
        }

        /**
         * Add a set of points. i.e. the time difference in seconds between two of the same message
         * instances. 
         * @param deltas change in seconds between two of of the same message instances where if MSG1 
         * occurred at time T then the other message instance time is less than time T and is the 
         * MOST RECENTLY seen message instance with this message id.
         */
        public void addPoints(Integer[] deltas) {
            if (deltas != null) {
                for (int delta : deltas) {
                    addPoint(delta);
                }
            }
        }

        /**
         * Start training if the number if the total number of points
         * we have seen is greater than the minimal amount of points per cluster. 
         * When training is done we clear the point differences.
         * @param name the message id.
         */
        public void train(String name) {
            assert mTrained == false;
            assert mPointDifferences.size() > 0;
            if (mPoints >= mMinimalPointsPerCluster) {
                if (m_debugPrint) {
                    if (name != null) {
                        logger.info("trainig last seen model for "
                                + name);
                    }
                    for (Entry<Integer, Integer> pointDiff : mPointDifferences.entrySet()) {
                        logger.info("  " + pointDiff.getKey() + ", "
                                + pointDiff.getValue());
                    }
                }
                computeScores();
            }
            mPointDifferences = null; 
            mTrained = true;
        }

        /**
         * Calculate the scores for each point in "mPointDifferences" where the score is the log
         * of the ratio between max value and a point's value. (max is the time difference that occurs the
         * most in mPointDifferences and a point's value is the number of occurrences for 
         * this time difference/point). Only attempt this calculation if it satistifies the minimal
         * requirements as specified by the Property values and if the value is greater than 1.
         */
        private void computeScores() {
            final int max = getMaxValue();
            m_lMax = Math.log(max);
            m_llMax = -Math.log((double) max / mPoints);
            if (max > m_minimalMax
                    && ((double) mPoints) / mPointDifferences.size() > m_minimalConcentration) {
                m_pointScores = new TreeMap<Integer, Double>();
                for (Entry<Integer, Integer> entry : mPointDifferences
                        .entrySet()) {
                    final Integer v = entry.getValue();
                    if (v > 1) {
                        m_pointScores.put(entry.getKey(), m_lMax - Math.log(v));
                    }
                }
            }

        }

        /**
         * Get the time difference in seconds that occurred the most between message instances.
         * @return the point/delta/time difference in seconds between messages that occurred the most. 
         */
        private int getMaxValue() {
            int max = 0;
            for (Entry<Integer, Integer> e : mPointDifferences.entrySet()) {
                final int v = e.getValue();
                if (v > max) {
                    max = v;
                }
            }
            return max;
        }
        /**
         * Retrieves the score for each delta by using the scores calculated by "computerScores()." If the
         * score is not stored for a particular delta then set the score as skipScore where skipScore is retrieved
         * by obtaining the score at the current delta added to the previous delta. If this value isn't null, then 
         * use it otherwise, call getNeverSeenScore() to retrieve the score.
         * @param deltas change in seconds between two of of the same message instances where if MSG1 
         * occurred at time T then the other message instance time is less than time T and is the 
         * MOST RECENTLY seen message instance with this message id.
         * @return the scores for each delta.
         */
        public double[] getScore(Integer[] deltas) {
            if (deltas == null) {
                return new double[0];
            }
            assert (deltas.length > 0);
            if (m_pointScores == null) {
                return new double[0];
            }
            final double[] scores = new double[deltas.length];
            for (int idx = 0; idx < deltas.length; ++idx) {
                Double score = m_pointScores.get(deltas[idx]);
                if (idx > 0) { // if we have one extra message, 
                    final Double skipScore = m_pointScores.get(deltas[idx - 1] + deltas[idx]);
                    if (score == null || (skipScore != null && skipScore < score)) {
                        score = skipScore;
                    }
                }
                scores[idx] = (score != null) ? score : getNeverSeenScore();
            }
            return scores;

        }
        /**
         * Retrieve the m_llMax variable.
         * @return The log of the ratio between the max point and the total number of points.
         */
        public double getLLMax() {
            return m_llMax;
        }

        /**
         * The score for a delta value that isn't in m_pointScores.
         * @return log(.5) subtracted from the log of the max delta/value.
         */
        public double getNeverSeenScore() {
            return m_lMax - m_logHalf;
        }

        /**
         * Add a new point to the sequence. We add to m_PointDifferences the difference in seconds from the 
         * last point and the count for how many of this time difference we have seen between any two
         * consecutive message instances.
         * @param distanceFromLastPoint The number of seconds distance from the last point. i.e. the last
         * time this message instance occurred.
         */
        private void addPoint(int distanceFromLastPoint) {
            assert distanceFromLastPoint >= 0;
            Integer count = mPointDifferences.get(distanceFromLastPoint);
            if (count == null) {
                count = 0;
            }
            mPointDifferences.put(distanceFromLastPoint, count + 1);
            mPoints++;
        }

        /**
         * Print out the object state for debugging purposes.
         * @param name the message id
         * @param out the output stream for printing out object state.
         * @throws AdeException
         */
        public void debugPrint(String name, PrintStream out) throws AdeException {
            if (name != null) {
                out.println("Last seen model for " + name + ": " + getLLMax());
            }
            if (m_pointScores != null && !m_pointScores.isEmpty()) {
                for (Entry<Integer, Double> pointScore : m_pointScores.entrySet()) {
                    out.println("  " + pointScore.getKey() + ", "
                            + pointScore.getValue());
                }
                out.println("  missing, "
                        + -m_logHalf);
            }
        }
    }

    /**
     * Maps message id to its periodicity bounder.
     */
    private TreeMap<String, PerodicityBounder> m_lastSeen;
    
    /**
     * For determining if we have used this scorer during training.
     */
    transient private boolean m_trained = false;

    @Property(key = "printDebug", required = false, help = "Print debug information to system.out")
    private boolean m_debugPrint = false;

    /**
     * Gets the score for each delta as computed by the "getScore" method in PeriodicityBounder. Then set the
     * statistics chart with the scores obtained, The log of ratio between the max delta and the total number of deltas,
     * and get the max score seen in the set of scores for each delta to compute the "LobProbGiveLast" statistics
     * chart value. Then using the max score found calculate the probability and anomaly statistics. The max score,
     * probability and anomaly statistics are then added to the StatisticsChart.
     * @param analyzedMessageSummary The analysis results of a MessageSummary object. Message summaries contain 
     * statistics and information on message instances. i.e. text body message, message id, severity, etc.
     * @param analyzedInterval contains a summary of the interval i.e. information such as time, number of 
     * message ids, etc.
     * @return The StatisticsChart for collecting double and string statistics.
     */
    @Override
    public StatisticsChart getScore(
            IAnalyzedMessageSummary analyzedMessageSummary,
            IAnalyzedInterval analyzedInterval) throws AdeException {
        final StatisticsChart sc = new StatisticsChart();
        final String messageId = analyzedMessageSummary.getMessageId();

        double score = 0d;
        final PerodicityBounder omm = m_lastSeen.get(messageId);
        if (omm != null) {
            final double[] scores = omm
                    .getScore(extractDelta(analyzedMessageSummary));
            if (scores.length != 0) {
                sc.setStat("PerTickScores", Arrays.toString(scores));
                for (int i = 0; i < scores.length; ++i) {
                    if (scores[i] > score) {
                        score = scores[i];
                    }
                }
                sc.setStat("LogProbGivenLast", score + omm.getLLMax());
                sc.setStat("maxl", omm.getLLMax());
            }
        } else {
            score = DEFAULT_FOR_NEW;
        }
        final Double prob = Math.exp(0 - score);
        sc.setStat(MAIN, prob);
        sc.setStat(ANOMALY, 1 - prob);
        sc.setStat(LOG_PROB, score);
        return sc;
    }

    /**
     * Determine if the learner needs another iteration by checking
     * if its been trained already.
     */
    @Override
    public boolean needsAnotherIteration() throws AdeException {
        return !m_trained;
    }

    /**
     * Start the learner iteration by initializing necessary variables.
     */
    @Override
    public void startIteration() throws AdeException {
        m_trained = false;
        m_lastSeen = new TreeMap<String, PerodicityBounder>();
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
    }

    /**
     * Go through each message and get the delta values between each instance for this interval.
     * Then add the points where the points are the delta values extracted.
     * @param analyzedInterval contains summary results of an interval.
     */
    @Override
    public void incomingObject(IAnalyzedInterval analyzedInterval)
            throws AdeException, AdeFlowException {
        for (IAnalyzedMessageSummary ms : analyzedInterval.getAnalyzedMessages()) {
            final String messageId = ms.getMessageId();
            final Integer[] delta = extractDelta(ms);
            PerodicityBounder perodicityBounder = m_lastSeen.get(messageId);
            if (perodicityBounder == null) {
                perodicityBounder = new PerodicityBounder();
                m_lastSeen.put(messageId, perodicityBounder);
            }
            perodicityBounder.addPoints(delta);
        }
    }
    /**
     * Gets the delta values (change in seconds between each message instance) calculated by the 
     * LastSeenLogginScoreContinuous class. Reformats the delta values so it the deltas are in an integer
     * array.
     * @param analyzedMessageSummary The analysis results of a MessageSummary object. Message summaries contain 
     * statistics and information on message instances. i.e. text body message, message id, severity, etc.
     * @return The delta values in an integer array.
     */
    protected Integer[] extractDelta(IAnalyzedMessageSummary ms) {
        final String rawDelta = ms.getStatistics().getStringStat(
                LastSeenLoggingScorerContinuous.class.getSimpleName() + "."
                        + "res");
        if (rawDelta.equals("[]")) {
            return null;
        }
        final List<String> stringDelta = Arrays.asList(StringUtils.split(
                StringUtils.substringBetween(rawDelta, "[", "]"), ", "));
        final Integer[] delta = new Integer[stringDelta.size()];
        for (int i = 0; i < stringDelta.size(); ++i) {
            delta[i] = Integer.decode(stringDelta.get(i));
        }
        return delta;
    }

    /**
     * At the end of the stream, get all the message ids and run a train for each
     * message id.
     */
    @Override
    public void endOfStream() throws AdeException, AdeFlowException {
        for (Entry<String, PerodicityBounder> last : m_lastSeen.entrySet()) {
            final PerodicityBounder pb = last.getValue();
            pb.train(last.getKey());
        }
        m_trained = true;
    }
    /**
     * Print full object state for debug purposes.
     * @param out output stream to print out object state.
     */
    @Override
    public void debugPrint(PrintStream out) throws AdeException {
        super.debugPrint(out);
        for (Entry<String, PerodicityBounder> last : m_lastSeen.entrySet()) {
            last.getValue().debugPrint(last.getKey(), out);
        }

    }

}
