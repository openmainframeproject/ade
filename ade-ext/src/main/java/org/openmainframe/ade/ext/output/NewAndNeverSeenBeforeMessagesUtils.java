/*
 
    Copyright IBM Corp. 2012, 2016
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


package org.openmainframe.ade.ext.output;

import static org.openmainframe.ade.ext.output.StatisticsChartConstants.ClusteringContextScore_status;
import static org.openmainframe.ade.ext.output.StatisticsChartConstants.LastSeenLoggingScorerContinuous_LastTime;

import java.util.Collection;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.scoringApi.StatisticsChart;

/**
 * Class that is used for processing new and never seen before messages in analyzed intervals.
 */
public final class NewAndNeverSeenBeforeMessagesUtils {
    
    private NewAndNeverSeenBeforeMessagesUtils() {
        // Private constructor to hide the implicit public one.
    }
    
    /* 
     * Gets the number of new messages and number of never seen before messages by looping through the analyzed messages for the
     * given interval.
    */
    public static NewAndNeverSeenBeforeMessages processAnalyzedInterval(IAnalyzedInterval interval) throws AdeException{
        int numNewMessages = 0;
        int numNeverSeenBeforeMessages = 0;
        Collection<IAnalyzedMessageSummary> analyzedMessages = interval.getAnalyzedMessages();
        for(IAnalyzedMessageSummary analyzedMessage : analyzedMessages){
            StatisticsChart statChart = analyzedMessage.getStatistics();
            String clusterStatus = statChart.getStringStatOrThrow(ClusteringContextScore_status);
            String periodicityLastIssueString = statChart.getStringStat(LastSeenLoggingScorerContinuous_LastTime);         
            /**
             * A message is considered new if it is seen during analysis but is not in the current
             * model data.
             */
            if (clusterStatus.equalsIgnoreCase("New")){
                numNewMessages++;      
            }    
            /**
             * A message is considered never seen before if and only if it is seen for the first time
             * during analyze.
             */           
            if (periodicityLastIssueString == null){
                numNeverSeenBeforeMessages++;       
            }                 
        }
        return new NewAndNeverSeenBeforeMessages(numNewMessages, numNeverSeenBeforeMessages);
    }
       
}
