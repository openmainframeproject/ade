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

import java.util.Collection;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.data.IntervalClassificationEnum;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.utils.patches.Version;

/**
 * An Interval object contains summary data over a certain period of time.
 */
public interface IInterval extends IBasicInterval {

    /** 
     * @return the Source this interval is associated with. 
     * */
    public ISource getSource();

    /**Returns the version of Ade by which this interval was produced.
     * @return the ade version at the time of this interval creation 
     */
    Version getAdeVersion();

    /**Returns the position of the interval in the period based on its start time.
     * @return the serial on the interval in the period
     */
    int getSerialNum();

    /**
     * Returns the {@link FramingFlowType} of the interval.
     * @return interval's Framing Flow type
     */
    FramingFlowType getIntervalFramingFlowType();

    /**
     * Returns the {@link FramingFlowType} of the interval.
     * @param flow the framing flow type
     * @return interval's Framing Flow type
     */
    void setIntervalFramingFlowType(FramingFlowType flow);

    /**
     * Return the {@link IIntervalClassification} of the interval.
     * @return interval's classification. Intervals of each {@link FramingFlowType} can be classified 
     *     into one of a number of classes.
     * @see IntervalClassificationEnum
     */
    IIntervalClassification getIntervalClassification();

    /**
     * Retrieves all the {@link IMessageSummary}s contained in the Interval.
     * @return Collection of {@link IMessageSummary}s
     */
    Collection<IMessageSummary> getMessageSummaries();

    /**
     * The number of unique message ids in the interval.
     * @return number of unique mesages.
     */
    int getNumUniqueMessages();

    /**
     * Returns the {@link IMessageSummary} matching the given msgId.
     * @param msgId msgId to match
     * @return the {@link IMessageSummary} in this interval that matches the given msgId
     * @throws AdeException if no {@link IMessageSummary} in this Interval matches the given msgId
     */
    IMessageSummary getMessageSummaryByMessageId(String msgId) throws AdeException;

    /**
     * Returns this interval's size. This is equivalent to 
     *     <code>{@link IInterval#getIntervalEndTime()} - {@link IInterval#getIntervalStartTime()} </code>. 
     * @return interval size in milliseconds. 
     */
    long getIntervalSize();

    /**
     * Returns this interval's size in minutes. This is equivalent to 
     *     <code>{@link IInterval#getIntervalSize} / 60000 </code>.
     * @return interval size in minutes. 
     */
    int getIntervalSizeInMins();

    /**
     * @return the coverage factor.
     */
    double getCoverageFactor();

    /**
     * Join two intervals together.
     */
    void join(IInterval i);

    /**
     * Fix up interval start and end times.
     */
    void alignIntervalTimes() throws AdeException;

    /**
     * Fix up interval serial number. 
     */
    void fixIntervalSerialNum() throws AdeException;

}
