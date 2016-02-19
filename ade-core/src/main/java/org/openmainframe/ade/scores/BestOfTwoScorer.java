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
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.scoringApi.StatisticsChart;

/**
 * Sets the log likelihood value in the Statistics Chart to whichever scorer gives the best log 
 * likelihood score.
 */
public class BestOfTwoScorer extends FixedMessageScorer {

    /**
     * The serial ID.
     */
    private static final long serialVersionUID = 1L;

    @Property(key="firstScoreString", help="name of first score.  Assuming score is LogLikelihood (smaller is better) ",required=true)
    private String m_firstScoreString=null;

    @Property(key="secondScoreString", help="name of second score.  Assuming score is LogLikelihood (smaller is better) ",required=true)
    private String m_secondScoreString=null;

    /**
     * Chooses between two scores which one has a "better" log likelihood value. (The smaller of the two is 
     * chosen) Then set the statistics chart to use the better score of the two. Note: The scorer properties
     * are set in the FlowLayout.xml.
     * @param analyzedMessageSummary The analysis results of a MessageSummary object. Message summaries contain 
     * statistics and information on message instances. i.e. text body message, message id, severity, etc.
     * @param analyzedInterval contains a summary of the interval i.e. information such as time, number of 
     * message ids, etc.
     * @return The StatisticsChart for collecting double and string statistics.
     */
    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary scoredElement,
            IAnalyzedInterval contextElement) throws AdeException {
        Double s1=scoredElement.getStatistics().getDoubleStat(m_firstScoreString);
        Double s2=scoredElement.getStatistics().getDoubleStat(m_secondScoreString);
        Double best=s1;
        String chosen=m_firstScoreString;
        if (s1==null || (s1!=null && s2!=null && s2<s1)){
            best=s2;
            chosen=m_secondScoreString;
        }
        if (best==null){
            best=0.0;
            chosen="none";
        }
        double prob=1.0;
        if (best>0.0){
            prob=Math.exp(0-best);
        }
        StatisticsChart sc = new StatisticsChart();
        sc.setStat("used", chosen);
        sc.setStat(LOG_PROB, best);
        sc.setStat(MAIN, prob);
        sc.setStat(ANOMALY, 1-prob);
        return sc;
    }

}
