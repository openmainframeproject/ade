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

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.scoringApi.StatisticsChart;

/**
 * Scorer to retrieve the maximum critical word score of all message instances in the summary 
 * passed into the getScore method. These critical words may indicate potential problems. 
 * Some examples of critical words include "failure", "warning", etc.
 */
public class CriticalWordCountReporter extends FixedMessageScorer {

    /**
     * The serialized ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The bulk of the logic. Here, we simply just retrieve the critical word score from Analyzed message summary
     * and set the statistics charts.
     * @param scoredElement The analysis results of a MessageSummary object. Message summaries contain statistics 
     * and information on message instances. i.e. text body message, message id, severity, etc.
     * @param contextElement contains a summary of the interval i.e. information such as time, number of message ids, etc.
     * @return The StatisticsChart for collecting double and string statistics.
     */
    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary scoredElement,
            IAnalyzedInterval contextElement) throws AdeException {
        final StatisticsChart sc = new StatisticsChart();
        sc.setStat(MAIN, scoredElement.getCriticalWordsScore());
        sc.setStat(LOG_PROB, 0.0);
        return sc;
    }

}