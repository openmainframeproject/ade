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
package org.openmainframe.ade.impl.data;

import java.util.Date;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.DateTimeUtils;

/** A class for converting time stamps to (Period,serial_number) pairs.
 * 
 * <p>The class is optimized for repeated calls with time stamps of the same period, in which
 * case it doesn't require database access.
 */
public class PeriodAndSerialNumFinder {

    private long m_intervalSizeInMillis;
    private long m_analyzedIntervalSizeInMillis;
    private int m_lastSerialNum;
    private long m_lastPeriodStartTime = -1;
    private long m_lastPeriodEndTime = -1;
    /**
     *  Create a finder for the given source and {@link FramingFlowType}.
     *  
     *  @param framingFlowType the framing flow type
     */
    public PeriodAndSerialNumFinder(FramingFlowType framingFlowType) throws AdeException {
        this(framingFlowType.getDuration(), framingFlowType.getDuration());
    }
    
    public PeriodAndSerialNumFinder(long duration, long intervalSizeInMillis) throws AdeException{
        m_intervalSizeInMillis = intervalSizeInMillis;
        m_analyzedIntervalSizeInMillis = duration;
    }

    /**
     * Returns the number of intervals in a period.
     * @return number of intervals per period - note that this is the expected number of
     *         intervals per period as it may change from period to period.
     */
    public int getIntervalsPerPeriod() {
        return (int) ((m_lastPeriodEndTime - m_lastPeriodStartTime) / m_intervalSizeInMillis);
    }

    /**
     * Set the finder with the interval starting at the given time stamp.
     * The period and serial number of an interval are defined as follows:
     * An interval is aligned if its start time equals to period_start+serial_num*interval_size
     *   for some period_start and serial_num.
     *   In this case period_start and serial_num define its period and serial number
     *   
     * <p>If it is not aligned, its period and serial number are determined by the first aligned interval
     *   that follows it, i.e., has a larger start time. 
     *  
     * <p>Return true if the period of this intervals has changed since last call to setIntervalStartTime or 
     *    setToAlignedIntervalThatContainsTimeStamp.
     *    @param startTime the time stamp for the start of the interval
     *    @throws AdeException see setToAlignedIntervalThatContainsTimeStamp method
     */
    public boolean setIntervalStartTime(long startTime) throws AdeException {       
        return setToAlignedIntervalThatContainsTimeStamp(startTime + m_analyzedIntervalSizeInMillis - 1);
    }

    /**
     * Sets the finder to an aligned interval that contains the given timeStamp.
     * 
     * <p>An interval is aligned if it's start time equals to period_start+serial_num*interval_size
     * for some period_start and serial_num.
     * 
     * <p>An interval contains a time stamp if intervalStartTime <= timeStamp < intervalEndTime.
     * 
     * @param timeStamp the time stamp used to search for the aligned interval
     * @return true if the period of this interval has changed since the last call to setIntervalStartTime
     * @throws AdeInternalException if the passed in time stamp does not fall between the last period start time
     *         and the last period end time.
     */
    public boolean setToAlignedIntervalThatContainsTimeStamp(long timeStamp) throws AdeException {
        boolean periodChanged = false;

        // Check if needs to reset period range
        if (m_lastPeriodStartTime < 0
                || !(m_lastPeriodStartTime <= timeStamp && m_lastPeriodEndTime > timeStamp)) {

            m_lastPeriodStartTime = PeriodUtils.getContainingPeriodStart(new Date(timeStamp)).getTime();
            if (m_lastPeriodStartTime > timeStamp) {
                throw new AdeInternalException("Failed using Java Calendar: timestamp="
                        + DateTimeUtils.timestampToHumanDateAndTimeAndStampLocal(timeStamp)
                        + " resulted with period start time of "
                        + DateTimeUtils.timestampToHumanDateAndTimeAndStampLocal(m_lastPeriodStartTime));
            }

            m_lastPeriodEndTime = PeriodUtils.getNextPeriodStart(new Date(m_lastPeriodStartTime)).getTime();
            if (m_lastPeriodEndTime <= timeStamp) {
                throw new AdeInternalException("Failed using Java Calendar: timestamp="
                        + DateTimeUtils.timestampToHumanDateAndTimeAndStampLocal(timeStamp)
                        + " resulted with period end time of "
                        + DateTimeUtils.timestampToHumanDateAndTimeAndStampLocal(m_lastPeriodEndTime));

            }

            periodChanged = true;
        }

        final long elapsedMillis = timeStamp - m_lastPeriodStartTime;
        m_lastSerialNum = (int) (elapsedMillis / m_intervalSizeInMillis);
        return periodChanged;
    }

    /** Gets the serial number of an interval whose start time was supplied using last call to setTime().*/
    public int getLastSerialNum() throws AdeInternalException {
        return m_lastSerialNum;
    }

    /** Gets the start time of the period containing time that was supplied using last call to setTime().*/
    public long getLastPeriodStartTime() throws AdeInternalException {
        return m_lastPeriodStartTime;
    }

    /** Gets the end time of the period containing the time that was supplied using last call to setTime().*/
    public long getLastPeriodEndTime() throws AdeInternalException {
        return m_lastPeriodEndTime;
    }

    public long getLastAlignedIntervalStartTime() {
        return m_lastPeriodStartTime + m_lastSerialNum * m_intervalSizeInMillis;
    }

    /**
     * Create a finder using the passed in {@link FramingFlowType} and set it to an aligned interval that
     * contains the passed in time stamp.
     * 
     * @param framingFlowType the {@link FramingFlowType} that describes the interval
     * @param time the time stamp used to search for the aligned interval
     * @return the last aligned interval start time
     */
    public static long alignToInterval(FramingFlowType framingFlowType, long time) throws AdeException {
        final PeriodAndSerialNumFinder finder = new PeriodAndSerialNumFinder(framingFlowType);
        finder.setToAlignedIntervalThatContainsTimeStamp(time);
        return finder.getLastAlignedIntervalStartTime();
    }
}
