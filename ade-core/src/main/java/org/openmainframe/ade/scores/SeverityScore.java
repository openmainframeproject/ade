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

import java.util.HashMap;
import java.util.Map;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.scoringApi.StatisticsChart;

/**
 * Severity score sets a weight to each message according to its severity.
 * Weights are determined in the scorer's properties in the flow layout file.
 * @see Severity
 * @see AnalysisGrouptFlowType
 */
public class SeverityScore extends FixedMessageScorer {
    /**
     * The serial ID.
     */
    private static final long serialVersionUID = -5419323122708372487L;
    
    /**
     * Mapping from severity to weight.
     */
    private Map<Severity, Double> m_severityScore = new HashMap<Severity, Double>();

    @Property(key="weight_UNKNOWN",help= "Weight for 'unknown' sevirity",required=false)
    transient private Double m_weight_UNKNOWN=null;
    @Property(key="weight_INFO",help= "Weight for 'info' sevirity",required=false)
    transient private Double m_weight_INFO=null;
    @Property(key="weight_WARNING",help= "Weight for 'warning' sevirity",required=false)
    transient private Double m_weight_WARNING=null;
    @Property(key="weight_ERROR",help= "Weight for 'error' sevirity",required=false)
    transient private Double m_weight_ERROR=null;
    @Property(key="weight_FATAL",help= "Weight for 'fatal' sevirity",required=false)
    transient private Double m_weight_FATAL=null;

    /**
     * Set the severity to its weight as specified in the FlowLayout scorer properties.
     */
    @Override
    protected void processProperties() throws AdeException{
        setSeverity(Severity.UNKNOWN,m_weight_UNKNOWN);
        setSeverity(Severity.INFO,m_weight_INFO);
        setSeverity(Severity.WARNING,m_weight_WARNING);
        setSeverity(Severity.ERROR,m_weight_ERROR);
        setSeverity(Severity.FATAL,m_weight_FATAL);
    }

    /**
     * Sets the severity by adding a mapping in m_severityScore. If the severityWeight does not
     * exist, then set the severity to the default value of "weight"
     * @param severity The severity level value.
     * @param severityWeight The weight value for parameter "severity" to be set.
     * @throws AdeUsageException 
     */
    private void setSeverity( Severity severity, Double severityWeight) throws AdeUsageException{
        double weight = 1.0;
        if( severityWeight != null ){
            weight = severityWeight;
        }
        m_severityScore.put(severity, weight);
    }

    /**
     * Retrieves the severity score for the incoming message summary from the severity score map. Set
     * the statistics chart with the severity value.
     * @param scoredElement The analysis results of a MessageSummary object. Message summaries contain 
     * statistics and information on message instances. i.e. text body message, message id, severity, etc.
     * @param contextElement contains a summary of the interval i.e. information such as time, number of 
     * message ids, etc.
     * @return The StatisticsChart for collecting double and string statistics.
     */
    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary scoredElement,
            IAnalyzedInterval contextElement) throws AdeException {
        StatisticsChart sc=new StatisticsChart();
        double severity = m_severityScore.get(scoredElement.getSeverity());
        sc.setStat(MAIN, severity);
        return sc;
    }

}
