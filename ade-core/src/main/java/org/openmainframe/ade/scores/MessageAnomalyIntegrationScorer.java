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
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.scoringApi.IScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.IStructuredOutputWriter;

public class MessageAnomalyIntegrationScorer extends FixedMessageScorer {

    private String[] m_scoreList;
    @Property(key = "baseScorers", required = true, help = "List of scorers to choose max score from")
    private String m_scorers;

    @Override
    protected void processProperties() throws AdeException {
        m_scoreList = m_scorers.split(",");
    }

    public static final String MOST_ABNORMAL_SCORING = "mostAbnormalScoring";
    private static final long serialVersionUID = 1L;

    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary scoredElement, IAnalyzedInterval interval) throws AdeException {
        final StatisticsChart res = new StatisticsChart();
        final StatisticsChart statistics = scoredElement.getStatistics();
        double worse = 0;
        String mostAbnormal = null;
        double score;
        for (String className : m_scoreList) {
            score = statistics.getDoubleStatOrThrow(className + "." + IScorer.MAIN);
            if (score > worse) {
                worse = score;
                mostAbnormal = className;
            }
        }
        res.setStat(MOST_ABNORMAL_SCORING, mostAbnormal);
        res.setStat(IScorer.MAIN, worse);
        return res;
    }

    @Override
    public void debugPrint(PrintStream out) throws AdeException {
        super.debugPrint(out);
        out.println("Ade message anomaly intergrating scorer.");
    }

    @Override
    public void printGeneralUserData(IStructuredOutputWriter out) throws Exception {
        super.printGeneralUserData(out);
    }

}
