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
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.scoringApi.IScorer;
import org.openmainframe.ade.scoringApi.ScorerEnvironment;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.IStructuredOutputWriter;

public class PercentileScorer implements IScorer<Double, Double> {
    private static final long serialVersionUID = 1L;

    private String m_name;
    private String m_id = null;

    /**
     * statistic name for {@link PercentileScorer} score;
     */
    public static final String PERCENTILE = "percentile";
    public static final String OVER_THE_TOP_FACTOR = "overTheTopFactor";

    /**
     * Final scores are between 0 and {@link PercentileScorer#SCORE_FACTOR}. We may have
     * a larger number of percentiles in the model for higher accuracy, but the actual calculation outputs a 
     * number between 0 and 1 which is multiplied by {@link PercentileScorer#SCORE_FACTOR}.
     * @see PercentileScorer#NUM_PERCENTILE_BINS
     */
    public static final double SCORE_FACTOR = 100.0;

    /**
     * Number of bins used to store the percentiles model. Determines the percentile resolution of the
     * model
     */
    public static final int NUM_PERCENTILE_BINS = 100;

    private class GetVecMemberFromFullIndex {
        ArrayList<Double> m_vec;
        int m_totalNumIntervals;
        int m_numIntervalsWithZeroOccurrences;

        public GetVecMemberFromFullIndex(ArrayList<Double> vec,
                int totalNumIntervals) {
            super();
            this.m_vec = vec;
            this.m_totalNumIntervals = totalNumIntervals;
            this.m_numIntervalsWithZeroOccurrences = m_totalNumIntervals - m_vec.size();
        }

        public Double get(int i) {
            if (i < m_numIntervalsWithZeroOccurrences) {
                return 0.0;
            } else {
                return m_vec.get(i - m_numIntervalsWithZeroOccurrences);
            }
        }

        public Double getLast() {
            return m_vec.get(m_vec.size() - 1);
        }
    }

    /**
     * Maps a value to its percentile
     * If the value is in between two percentiles, a weighted average of the too bounding percentiles is
     * returned.
     */
    private NavigableMap<Double, Integer> m_percentiles;

    // array at index i contains the value observed at the i'th non-zero observation   
    private ArrayList<Double> m_observations;

    private int m_totalNumObservations;

    public PercentileScorer() {
        m_name = PercentileScorer.class.getSimpleName();
        m_observations = new ArrayList<Double>();
    }

    /**
     * returns percentile table for this message.
     * @return an ordered array of size N, where the i'th entry
     *  holds the occurrence count for i/N*100%
     *  for example:  if the array is of size 100, then 
     *  entry number 0 represents the XXXXXXX  
     */
    public ArrayList<Double> getPercentiles() {
        final ArrayList<Double> res = new ArrayList<Double>();
        int lastPercIndex = -1;
        for (Entry<Double, Integer> percentileEntry : m_percentiles.entrySet()) {
            final int currPercIndex = percentileEntry.getValue();

            for (int i = lastPercIndex + 1; i <= currPercIndex; i++) {
                res.add(percentileEntry.getKey());
            }
            lastPercIndex = currPercIndex;
        }
        return res;
    }

    /**
     * Returns the matching percentile for the given number of occurrences. 
     * @param value number of occurrences 
     * @return the percentage of intervals exhibiting a number of occurrences smaller or equal to the given value. 
     */
    public double getPercentile(double value) {
        final double percentileStepSize = 1.0 / NUM_PERCENTILE_BINS;
        final Entry<Double, Integer> predEntry = m_percentiles.floorEntry(value);
        // we are at below the lowest value, return 0
        if (predEntry == null) {
            return 0;
        }

        final int pred = predEntry.getValue();
        final double predVal = predEntry.getKey();

        if (predVal == value) {
            return pred * percentileStepSize * SCORE_FACTOR;
        }
        final Entry<Double, Integer> succEntry = m_percentiles.ceilingEntry(value);
        if (succEntry == null) {
            return m_percentiles.lastEntry().getValue() + 1;
        }
        final double succVal = succEntry.getKey();
        return (pred + (value - predVal) / (succVal - predVal)) * percentileStepSize * SCORE_FACTOR;
    }

    @Override
    public StatisticsChart getScore(Double value, Double contextElement)
            throws AdeException {
        final StatisticsChart sc = new StatisticsChart();
        final double percentileValue = getPercentile(value);
        sc.setStat(PERCENTILE, percentileValue);
        sc.setStat(OVER_THE_TOP_FACTOR, value / m_percentiles.lastKey());

        return sc;
    }

    @Override
    public void incomingObject(Double observation) throws AdeException {
        m_observations.add(observation);
    }

    @Override
    public void debugPrint(PrintStream out) throws AdeException {
        for (Entry<Double, Integer> percentileEntry : m_percentiles.entrySet()) {
            out.println(percentileEntry.getValue() + ": " + percentileEntry.getKey());
        }
    }

    public void setTotalNumOfObservations(int totalNumObservations) {
        m_totalNumObservations = totalNumObservations;
    }

    @Override
    public void endOfStream() throws AdeException {
        java.util.Collections.sort(m_observations);

        final GetVecMemberFromFullIndex vec = new GetVecMemberFromFullIndex(m_observations, m_totalNumObservations);
        m_percentiles = new TreeMap<Double, Integer>();
        final double step = ((double) m_totalNumObservations + 1.0) / (NUM_PERCENTILE_BINS);
        for (int i = 0; i < NUM_PERCENTILE_BINS; ++i) {
            double tmp_score = 0.0;
            final double exactIndex = (step * i);
            final int index = (int) Math.floor(exactIndex);
            if (index == 0) { //  -> 7.2.5.2 case 2 

                tmp_score = vec.get(0);
            } else if (index == m_totalNumObservations) { // last value - 7.2.5.2 case 3 in estimation of percentiles
                tmp_score = vec.getLast(); // the last value.
            } else {
                final double offset = exactIndex - index;
                // normal situation - 7.2.5.2 case 1 
                tmp_score = (1.0 - offset) * vec.get(index - 1).doubleValue() + (offset) * vec.get(index).doubleValue();
            }
            m_percentiles.put(tmp_score, i);
        }
        // at this point 1/m_percentiles.lastValue() contains the percentiles step size 
        m_observations = null;
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public boolean needsAnotherIteration() throws AdeException {
        return m_observations != null;
    }

    @Override
    public void printGeneralUserData(IStructuredOutputWriter out) throws Exception {
        final IStructuredOutputWriter perWriter = out.child("percentiles");
        for (Entry<Double, Integer> percentileEntry : m_percentiles.entrySet()) {
            perWriter.simpleChild("entry", percentileEntry.getKey(), "per", Integer.toString(percentileEntry.getValue()));
        }
        perWriter.close();
    }

    @Override
    public void initTraining(ScorerEnvironment globals) throws AdeException {
        throw new AdeInternalException(getName() + " does not allow init training");
    }

    @Override
    public void startIteration() throws AdeException {
        throw new AdeInternalException(getName() + " does not allow start iteration");

    }

    @Override
    public void setName(String name) {
        m_name = name;
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setArguments(Map<String, Object> props) throws AdeException {
        throw new AdeInternalException(getName() + " does not allow argument setting");
    }

    @Override
    public String getId() {
        return m_id;
    }

    @Override
    public void setId(String id) {
        m_id = id;
    }

    @Override
    public void incomingSeparator(TimeSeparator sep) throws AdeException,
            AdeFlowException {
        // Default is do nothing.  If the scorer keeps a history, it will need to override this.

    }

    @Override
    public void wakeUp() {
        // nothing to do
    }

}
