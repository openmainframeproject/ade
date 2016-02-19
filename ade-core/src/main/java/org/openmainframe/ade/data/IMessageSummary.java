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

import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.summary.SummarizationProperties;

/**
 * Contains a summary of {@link IMessageInstance}s
 * All message instances share the same message id  
 */
public interface IMessageSummary extends Cloneable {

    /** Returns the message id shared by all message instances of this message summary 
     * @throws AdeException */
    public String getMessageId() throws AdeException;

    /**
     * Returns internal numeric id of the message id 
     * of this MessageSummary
     * @return message numeric ID
     */
    public int getMessageInternalId();

    /**
     * Returns the text summary 
     * The text summary contains the text common to all message instances, with *
     * replacing the different parts.
     * This field may be null if text summary is disabled.
     * @return Summarized text  
     */
    public String getTextSummary();

    /**
     * Returns a text sample
     * The text sample is the text of the first message instance in this summary.
     * If critical words scoring is enabled, it is the text with the highest critical words score
     * @return Sampled text 
     */
    public String getTextSample();

    /**
     * Returns the number of MessageInstances summarized by the MessageSummary
     * @return number of MessageInstances
     */
    public int getNumMessageInstances();

    /**
     * Returns the number of FailedMessageInstances summarized by the MessageSummary
     * @return number of FailedMessageInstances
     */
    public int getNumFailedMessageInstances();

    /**
     * Returns the maximal critical words score seen
     * @return the maximal critical words score seen
     */
    int getCriticalWordsScore();

    /**
     * Returns the severity of this message summary 
     * @return severity level if available or {@link Severity#UNKNOWN} if no severity parameter is available,   
     */
    public IMessageInstance.Severity getSeverity();

    /** 
     * Returns an array of timestamps of all message instances in this summary
     * @return a sorted array of timestamps in seconds since the epoch or null if timeline summary is disabled.
     * @see SummarizationProperties#m_summarizeTimeLine
     * */
    short[] getTimeLine();

    /** 
     * Sets the timeline in seconds since the epoch.
     * 
     * @param timeLine the timeLine value in seconds
     * */
    public void setTimeLine(short[] timeLine);

    public void setTextSummary(String summary);

    void join(IMessageSummary other, IInterval src, IInterval target);

    void adjustTimeline(long oldStartTime, long oldSize, long newStartTime, long newSize);

    public IMessageSummary clone();

    /**
     * clear any non-critical internal storage items.  Call this once you expect the message summary will not be manipulated any more.
     * This must always be safe - calling it may increase function time or reduce accuracy mildly, but not render any code unusable.  
     */
    public void streamline();

}
