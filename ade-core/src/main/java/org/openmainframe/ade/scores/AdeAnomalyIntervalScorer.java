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
import java.util.ArrayList;
import java.util.Collections;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.impl.scoringApi.MainScorerImpl;
import org.openmainframe.ade.scoringApi.IScorer;
import org.openmainframe.ade.scoringApi.IntervalAnomalyScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.IStructuredOutputWriter;

/*
 * Uses the messageAnomalyScore's results, logProb, to compute the interval total logProb by summing
 * up the message instances log probabilities in an interval. We store this information in the StatisticsChart
 * and using this value we calculate percentile information to also add to the StatisticsChart.
 */
public class AdeAnomalyIntervalScorer extends IntervalAnomalyScorer {
    /**
     * The serial ID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Number of buckets/bins for calculating percentiles.
     */
    private static final int NUM_BUCKETS = 1000;
    @Property(key = "percentilesWithEmptyIntervals", help = "use empty intervals to compute percentiles.", required = false)
    private boolean m_percentilesWithEmptyIntervals = false;
    
    /**
     * Contain the log probability score for intervals.
     */
    private ArrayList<Double> m_rawScores;
    
    /**
     * Determines whether we've trained using this scorer.
     */
    private boolean m_trained = false;
    
    /**
     * The values for determining what percentile a rawScore falls in.
     */
    private double[] m_percentiles;
    
    /**
     * The number of intervals this scorer has seen.
     */
    private int m_intervalCount;
    
    /**
     * Default constructor that calls the parent constructor.
     * @throws AdeException
     */
    public AdeAnomalyIntervalScorer() throws AdeException {
        super();
    }
    
    /**
     * Reset scorer state to the equivalent of a newly created object.
     */
    @Override
    protected void reset() throws AdeException {
        super.reset();
        m_trained = false;
        m_rawScores = null;
    }
    /**
     * Determines if this learner needs another iteration.
     * @return true if we haven't trained using this scorer yet.
     */
    @Override
    public boolean needsAnotherIteration() throws AdeException {
        return !m_trained;
    }

    /**
     * Checks to see if we've already trained and initialized variables.
     */
    @Override
    public void startIteration() throws AdeException {
        if (m_trained) {
            throw new AdeInternalException("Already trained");
        }
        m_rawScores = new ArrayList<Double>();
        m_intervalCount = 0;
    }

    /**
     * If we do not use empty intervals to compute percentiles and there are no unique messages
     * for this interval then we simply return. Otherwise, we calculate the log probability score 
     * and add it to the list of raw scores as long as the probability isn't a large valued probability
     * as determined by "MainScorerImpl."
     * @param analyzedInterval contains summary results of an interval.
     */
    @Override
    public void incomingObject(IAnalyzedInterval analyzedInterval) throws AdeException {
        if (!m_percentilesWithEmptyIntervals && analyzedInterval.getNumUniqueMessageIds() == 0) {
            return;
        }
        final double rawScore = calcRawScore(analyzedInterval);

        if (!SeenHugeLogProb(analyzedInterval)) {
            m_rawScores.add(rawScore);
            ++m_intervalCount;
        }
    }

    /**
     * Get the log probability for the interval by summing up all the message instance's log 
     * probability in the interval.
     * @param analyzedInterval contains summary results of an interval.
     * @return the interval log probability value.
     * @throws AdeException
     */
    private double calcRawScore(IAnalyzedInterval analyzedInterval) throws AdeException {
        double sum = 0;
        for (IAnalyzedMessageSummary ams : analyzedInterval.getAnalyzedMessages()) {
            final StatisticsChart sc = ams.getStatistics();
            final double logProb = sc.getDoubleStatOrThrow(IScorer.LOG_PROB);
            sum += logProb;
        }
        return sum;
    }

    /**
     * Determines if any of the message instances in the interval have "huge-valued" log 
     * probabilities. If this is the case, then return true, otherwise return false.
     * @param analyzedInterval contains summary results of an interval.
     * @return true if any of the message instances in the interval is considered a 
     * "huge-valued" probability.
     * @throws AdeException
     */
    private boolean SeenHugeLogProb(IAnalyzedInterval analyzedInterval) throws AdeException {
        for (IAnalyzedMessageSummary ams : analyzedInterval.getAnalyzedMessages()) {
            final StatisticsChart sc = ams.getStatistics();
            if (sc.getDoubleStatOrThrow(IScorer.LOG_PROB) == MainScorerImpl.HUGELOGPROB) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method to calculate the percentiles, clear out the log probability scores, and set the
     * trained value to true at the end of stream.
     */
    @Override
    public void endOfStream() throws AdeException {
        calcPercentiles();
        m_rawScores = null;
        m_trained = true;
    }

    /**
     * Calculate the values to determine percentiles by first sorting the log probabilities then 
     * multiply the number of intervals by a percentile (i/NUM_BUCKETS) to retrieve the index at which 
     * to get the log probability score. This log probability score is the value for determining the
     * percentile.
     */
    private void calcPercentiles() {
        m_percentiles = new double[NUM_BUCKETS];
        // To support the case of empty model
        if (m_rawScores.size() == 0) {
            return;
        }
        Collections.sort(m_rawScores);
        final int numIntervals = m_rawScores.size();
        for (int i = 0; i < NUM_BUCKETS; i++) {
            final int index = ((numIntervals * i) / NUM_BUCKETS);
            m_percentiles[i] = m_rawScores.get(index);
        }

    }

    /**
     * Calculate the total log probability for the interval, the percentile, and set the 
     * statistics chart with the log probability for the interval, the percentage value for where
     * the rawScore falls in the percentile (percentile / (double) NUM_BUCKETS), and the ratio of 
     * the rawScore to the highest valued percentile.
     * @param analyzedInterval contains a summary of the interval i.e. information such as time, 
     * number of message ids, etc. Will be used for calculating the log probability score for the
     * interval.
     * @param interval contains a summary of the interval i.e. information such as time, number of 
     * message ids, etc.
     * @return The StatisticsChart for collecting double and string statistics.
     */
    @Override
    public StatisticsChart getScore(IAnalyzedInterval analyzedInterval, IAnalyzedInterval interval)
            throws AdeException {
        final StatisticsChart st = new StatisticsChart();
        final double rawScore = calcRawScore(analyzedInterval);
        st.setStat(IScorer.LOG_PROB, rawScore);
        final int percentile = convertScoreToPercentile(rawScore);
        st.setStat(MAIN, (double) percentile / (double) NUM_BUCKETS);
        st.setStat("FactorFromOne", rawScore / m_percentiles[NUM_BUCKETS - 1]);

        return st;
    }
    
    /**
     * Calculate the percentile for the score by seeing where the score falls in the
     * m_percentiles array. Once the score is less than or equal to a value in m_percentiles
     * then we return the bucket number it falls in. If the score is always larger, then
     * we return the max bucket value as the percentile. If the score is above a certain threshold
     * then we return a value higher than the number of buckets.
     * @param score the value to be converted into a percentile.
     * @return The percentile the score falls in.
     * @throws AdeInternalException
     */
    private int convertScoreToPercentile(double score) throws AdeInternalException {
        final double highestScore = m_percentiles[m_percentiles.length - 1];
        if (score > highestScore * 1.5) {
            return (int) Math.ceil(NUM_BUCKETS * 1.01);
        }
        // convert to a percentile
        for (int i = 0; i < NUM_BUCKETS; ++i) {
            if (score <= m_percentiles[i]) {
                return i;
            }
        }
        return NUM_BUCKETS;
    }

    /**
     * Print out object state for debugging purposes.
     * @param out the print stream for printing out object state.
     */
    @Override
    public void debugPrint(PrintStream out) throws AdeException {
        super.debugPrint(out);
        out.println("consideringEmptyIntervals = " + m_percentilesWithEmptyIntervals);
        out.println("intervalCount = " + m_intervalCount);
        out.println("percentiles:");
        for (int i = 0; i < NUM_BUCKETS; ++i) {
            out.printf("  %3d: %10.5f\n", i, m_percentiles[i]);
        }
    }

    /**
     * Prints summary of model for the user.
     * @param out Output stream for printing out a summary of structured data.
     */
    @Override
    public void printGeneralUserData(IStructuredOutputWriter out) throws Exception {
        out.simpleChild("consideringEmptyIntervals", m_percentilesWithEmptyIntervals);
        out.simpleChild("intervalCount", m_intervalCount);
        final IStructuredOutputWriter child = out.child("percentiles");
        for (int i = 0; i < NUM_BUCKETS; ++i) {
            child.simpleChild("per" + i, m_percentiles[i]);
        }
        child.close();
    }

    /**
     * Set the values for calculating percentiles.
     * @param table the values for determining percentile membership.
     */
    public void setPercentiles(double[] table) {
        m_percentiles = table;
    }

    /**
     * Get the values for calculating percentiles.
     * @return a double array for determining percentile membership.
     */
    public double[] getPercentiles() {
        return m_percentiles;
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
    }
}
