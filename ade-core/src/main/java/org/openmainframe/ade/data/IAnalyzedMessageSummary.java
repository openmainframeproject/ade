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
package org.openmainframe.ade.data;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.scoringApi.StatisticsChart;

/**
 * Holds the result of the analysis of a single {@link IMessageSummary} object, based on a given model
 * and analysis flow.
 */
public interface IAnalyzedMessageSummary {

    /**
     * @return the underlying {@link IMessageSummary} analyzed to get this {@link IAnalyzedMessageSummary}.
     */
    public IMessageSummary getMessageSummary();

    /** Returns this summary's message id 
     * @throws AdeException */
    public String getMessageId() throws AdeException;

    /** Returns number of message instances in this summary*/
    public int getNumberOfAppearances();

    /** Returns number of failed message instances in this summary*/
    public int getNumberOfFailedAppearances();

    /** 
     * Returns the timesatamps of message instances in this summary
     * @return an array of timestamps (shouldn't it be long?!) of message instances in this summary, 
     * or <code> null </code> if not applicable. 
     */
    public short[] getTimeLine();

    /**
     * Returns this summary text sample
     * @return First non-null text encountered in summary, or the text with highest critical words score where applicable 
     */
    public String getTextSample();

    /**
     * Returns this summary's summary of all texts encountered.
     * @return this summary's summary or <code> null </code> if not applicable. 
     */
    public String getTextSummary();

    /** Returns the maximal critical words score of all message instances in this summary */
    public int getCriticalWordsScore();

    /** @return The analysis results: statistics added by the scorers for this interval */
    public StatisticsChart getStatistics();

    /** @return The analysis final result: the overall anomaly score */
    public double getFinalAnomaly() throws AdeInternalException;

    /**
     * Return the severity of this message summary.
     * 
     * @return the severity for this message or {@link IMessageInstance#UNKNOWN} if no severity is indicated.
     */
    public IMessageInstance.Severity getSeverity();

}
