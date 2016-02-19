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

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.impl.scoringApi.MainScorerImpl;
import org.openmainframe.ade.scoringApi.IScorer;
import org.openmainframe.ade.scoringApi.MessageScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.IStructuredOutputWriter;

/**
 * Adjust log normal scoring according to message weight as determined by the severity score.
 */
public class AdeWeightedMessageAnomalyScorerLogNormal extends MessageScorer {

    /**
     * The serialized ID.
     */
    private static final long serialVersionUID = 1L;

    @Property(key = "baseScorer", required = false, help = "Clustering Scorer used to determine if message is in context")
    protected String m_baseScorer = null;
    @Property(key = "severityScorer", required = true, help = "message severity Scorer used for integrated score")
    protected String m_severityScorer;
    @Property(key = "rarityScorer", required = true, help = "message rarity Scorer used for integrated score")
    protected String m_rarityScorer;
    @Property(key = "countScorer", required = true, help = "message count Scorer used for integrated score")
    protected String m_countScorer;

    /**
     * Override the reset method so we do nothing.
     */
    @Override
    protected void reset() {

    }

    /**
     * Determine if this learner needs another iterator. Unconditionally return false.
     * (Does not require more iterations).
     */
    @Override
    public boolean needsAnotherIteration() throws AdeException {
        return false;
    }
    
    @Override
    public void startIteration() throws AdeException {
        throw new AdeInternalException("not implemented");
    }

    @Override
    public void endOfStream() throws AdeException {
        throw new AdeInternalException("not implemented");
    }

    @Override
    public void incomingObject(IAnalyzedInterval trainElement) throws AdeException {
        throw new AdeInternalException("not implemented");
    }

    /**
     * Adjusts the log normal score to take into consideration weighted messages. (weight is determined by
     * severity of message). Set the statistics chart with the new weighted values.
     * @param scoredElement The analysis results of a MessageSummary object. Message summaries contain 
     * statistics and information on message instances. i.e. text body message, message id, severity, etc.
     * @param interval contains a summary of the interval i.e. information such as time, number of 
     * message ids, etc.
     * @return The StatisticsChart for collecting double and string statistics.
     */
    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary scoredElement, IAnalyzedInterval interval)
            throws AdeException {
        final StatisticsChart res = new StatisticsChart();

        final double weight = scoredElement.getStatistics().getDoubleStatOrThrow(m_severityScorer + "." + IScorer.MAIN);
        final double logBernouli = scoredElement.getStatistics().getDoubleStatOrThrow(m_rarityScorer + "." + LogNormalScore.LOG_PROB);
        final double logLogNormal = scoredElement.getStatistics().getDoubleStatOrThrow(m_countScorer + "." + LogNormalScore.LOG_PROB);
        final boolean isClustered = isClustered(scoredElement);
        double logProb = 0;

        if (!isClustered) {
            logProb = (0.2 * logLogNormal + 0.8 * logBernouli) * weight;
        }
        final double weightedProb = Math.exp(-logProb);
        final double weightedAnomaly = 1 - weightedProb;

        res.setStat(IScorer.MAIN, weightedAnomaly);
        res.setStat(IScorer.ANOMALY, weightedAnomaly);
        if (!Double.isInfinite(logProb)) {
            res.setStat(IScorer.LOG_PROB, logProb);
        } else {
            res.setStat(IScorer.LOG_PROB, MainScorerImpl.HUGELOGPROB);
        }
        return res;
    }
    /**
     * Determines if the message instances are in context. If the message is in
     * context i.e. this message's cluster appears in this interval then this message is
     * considered clustered.
     * @param ams The analysis results for all messages with the same message id.
     * @return true if the messages are clustered.
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
     * Print object state for debugging.
     * @out print stream for printing out object state.
     */
    @Override
    public void debugPrint(PrintStream out) throws AdeException {
        super.debugPrint(out);
        out.println("Ade 1 message anomaly scorer.");
    }

    /**
     * Print out the general summary of the model for user.
     * @out output stream for printing out general summary.
     */
    @Override
    public void printGeneralUserData(IStructuredOutputWriter out) throws Exception {
        super.printGeneralUserData(out);
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
    }

}
