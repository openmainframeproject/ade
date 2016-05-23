/*
 
    Copyright IBM Corp. 2009, 2016
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

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IIntervalClassification;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.openmainframe.ade.utils.patches.Version;

/**
 * Basic implementation of the {@link IInterval} interface. This implementation
 * holds the {@link IMessageSummary}s in a Map between the
 * {@link IMessageSummary#getMessageInternalId()} to the summary instance.
 * 
 */
public class IntervalImpl implements IInterval, Comparable<IInterval> {

    /**
     * identifies the interval. the start interval time and {@link FramingFlowType}
     * The mStartIntervalTime is the number of milliseconds since January 1, 1970, 00:00:00 GMT
     */
    private long m_startIntervalTime;
    private long m_endIntervalTime;

    private FramingFlowType m_framingFlowType;
    private IIntervalClassification m_intervalClassification;
    private int m_serialNum;
    private Version a_adeVersion;
    private ISource m_source;
    /**
     * Map between the {@link IMessageSummary#getMessageInternalId()} to the
     * {@link IMessageSummary}.
     */
    private SortedMap<Integer, IMessageSummary> m_messageSummaryMap;
    private double m_coverageFactor;

    /**
     * This is used to build partial intervals.
     *
     * @param serialNum the interval's serial number.
     * @param startIntervalDateTime interval startTime
     * @param coverageFactor the fraction of the whole interval the messages cover.
     *     Legal values are between 0 (exclusive) and 1 (inclusive).
     * @param intervalDataType the {@link FramingFLowType} of the interval.
     * @param source source for this interval. All {@link IMessageSummary} objects (and the {@link IMessageInstance}
     *     objects within an interval are from the same source.
     * @param intervalClassification the classification for the interval.
     * @param adeVersion the ade version
     * @throws AdeInternalException for illegal input values
     */
    public IntervalImpl(int serialNum, long startIntervalDateTime, double coverageFactor,
            FramingFlowType intervalDataType, ISource source, IIntervalClassification intervalClassification,
            Version adeVersion) throws AdeInternalException {
        a_adeVersion = adeVersion;
        if (startIntervalDateTime < 0) {
            throw new IllegalArgumentException(String.format("Interval start time must be "
                    + "greater than 0! Failed creating new interval with parameters: serialNum=%d, " 
                    + "startIntervalDateTime=%d, coverageFactor=%s, intervalDataType=%s, source=%s, "
                    + "intervalClassification=%s, adeVersion=%s", serialNum, startIntervalDateTime,
                    coverageFactor, intervalDataType, source, intervalClassification, adeVersion));
        }

        m_startIntervalTime = startIntervalDateTime;

        m_framingFlowType = intervalDataType;
        m_serialNum = serialNum;

        m_intervalClassification = intervalClassification;
        if (m_intervalClassification == null) {
            m_intervalClassification = IntervalClassificationEnum.REGULAR;
        }
        m_messageSummaryMap = new TreeMap<>();
        m_endIntervalTime = m_startIntervalTime + m_framingFlowType.getDuration();
        m_source = source;

        if (coverageFactor <= 0 || coverageFactor > 1) {
            throw new AdeInternalException(
                    "Interval coverage factor must be between 0 (exclusive) and 1 (inclusive), but currently is: "
                            + coverageFactor);
        }
        m_coverageFactor = coverageFactor;
    }

    /**
     * This is used to build partial intervals. This is the same as the other constructor
     * but with a default version.
     * 
     * @param serialNum the interval's serial number
     * @param startIntervalDateTime interval startTime
     * @param coverageFactor the fraction of the whole interval the messages covers.
     *     Legal values are between 0 (exclusive) and 1 (inclusive).
     * @param intervalDataType the {@link FramingFLowType} of the interval.
     * @param source source for this interval. All {@link IMessageSummary} objects (and the {@link IMessageInstance}
     *     objects within an interval are from the same source.
     * @param intervalClassification the classification for the interval.
     */
    public IntervalImpl(int serialNum, long startIntervalDateTime, double coverageFactor,
            FramingFlowType intervalDataType, ISource source, IIntervalClassification intervalClassification)
                    throws AdeException {
        this(serialNum, startIntervalDateTime, coverageFactor, intervalDataType, source, intervalClassification,
                Ade.getAde().getVersion());

    }

    @Override
    public final FramingFlowType getIntervalFramingFlowType() {
        return m_framingFlowType;
    }

    @Override
    public final void setIntervalFramingFlowType(FramingFlowType flow) {
        m_framingFlowType = flow;
    }

    @Override
    public final IIntervalClassification getIntervalClassification() {
        return m_intervalClassification;
    }

    @Override
    public final long getIntervalStartTime() {
        return m_startIntervalTime;
    }

    @Override
    public final long getIntervalEndTime() {
        return m_endIntervalTime;
    }

    @Override
    public final long getIntervalSize() {
        return m_endIntervalTime - m_startIntervalTime;
    }

    @Override
    public final int getIntervalSizeInMins() {
        return (int) (getIntervalSize() / DateTimeUtils.MILLIS_IN_MINUTE);
    }

    @Override
    public final Collection<IMessageSummary> getMessageSummaries() {
        return m_messageSummaryMap.values();
    }

    @Override
    public final int getNumUniqueMessages() {
        return m_messageSummaryMap.size();
    }

    /**
     * Get the {@link IMessageSummary} by its internal ID.
     * 
     * @param msgId the internal message ID
     * @return the {@link IMessageSummary}
     */
    public final IMessageSummary getMessageSummaryByMessageInternalId(int msgId) {
        return m_messageSummaryMap.get(msgId);
    }

    @Override
    public final IMessageSummary getMessageSummaryByMessageId(String msgId) throws AdeException {
        final int id = AdeInternal.getAdeImpl().getDictionaries().getMessageIdDictionary().getWordId(msgId);
        return getMessageSummaryByMessageInternalId(id);
    }

    /**
     * Add the {@link IMessageSummary} to the sorted map using its ID.
     * 
     * @param msgSum the {@link IMessageSummary}
     */
    public final void addMessageSummary(IMessageSummary msgSum) {
        m_messageSummaryMap.put(msgSum.getMessageInternalId(), msgSum);
    }

    @Override
    /**
     * Convert this IntervalImpl into a string.
     */
    public final String toString() {
        final StringBuilder result = new StringBuilder();
        result.append("[IntervalImpl:\n");
        result.append("    startIntervalTime=" + DateTimeUtils.
                timestampToHumanDateAndTimeAndStampUTC(m_startIntervalTime) + "\n");
        result.append("      endIntervalTime=" + DateTimeUtils.
                timestampToHumanDateAndTimeAndStampUTC(m_endIntervalTime) + "\n");
        result.append("    dataType=" + m_framingFlowType + "\n");
        result.append("    intervalClassification=" + m_intervalClassification + "\n");
        result.append("    adeVersion=" + a_adeVersion + "\n");
        result.append("    messageSummaryMap=" + "\n");
        for (Map.Entry<Integer, IMessageSummary> entry : m_messageSummaryMap.entrySet()) {
            result.append("        " + entry.getKey() + ": " + entry.getValue() + "\n");
        }
        result.append("]");
        return result.toString();
    }

    public final void setIntervalClassification(IntervalClassificationEnum classification) {
        m_intervalClassification = classification;
    }

    @Override
    public final int getSerialNum() {
        return m_serialNum;
    }

    @Override
    public final int compareTo(IInterval o) {
        final long l1 = getIntervalStartTime();
        final long l2 = o.getIntervalStartTime();
        if (l1 > l2) {
            return 1;
        }
        if (l1 < l2) {
            return -1;
        }
        return 0;
    }
    
    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final IntervalImpl intervalImpl = (IntervalImpl) obj;
        return this.getIntervalStartTime() == intervalImpl.getIntervalStartTime();
    }

    @Override
    public final int hashCode() {
        return Long.valueOf(this.getIntervalStartTime()).hashCode();
    }
    
    @Override
    public final Version getAdeVersion() {
        return a_adeVersion;
    }

    @Override
    public final double getCoverageFactor() {
        return m_coverageFactor;
    }

    @Override
    public final ISource getSource() {
        return m_source;
    }

    @Override
    public final void join(IInterval other) {

        assert m_source.equals(other.getSource());
        assert m_framingFlowType.equals(other.getIntervalFramingFlowType());
        final long newStart = Math.min(m_startIntervalTime, other.getIntervalStartTime());
        final long newEnd = Math.max(m_endIntervalTime, other.getIntervalEndTime());
        setTime(newStart, newEnd);

        m_coverageFactor = (getCoverageFactor() + other.getCoverageFactor()) / 2.0;

        for (IMessageSummary s : other.getMessageSummaries()) {
            addOrJoinMessageSummery(s, other);
        }

    }

    private void setTime(long newStart, long newEnd) {
        final long newDuration = newEnd - newStart;
        for (IMessageSummary s : getMessageSummaries()) {
            s.adjustTimeline(m_startIntervalTime, getIntervalSize(), newStart, newDuration);
        }
        m_startIntervalTime = newStart;
        m_endIntervalTime = newEnd;
    }

    private void addOrJoinMessageSummery(IMessageSummary s, IInterval other) {
        final IMessageSummary old = m_messageSummaryMap.get(s.getMessageInternalId());
        if (old != null) {
            old.join(s, other, this);
        } else {
            final IMessageSummary sclone = s.clone();
            sclone.adjustTimeline(other.getIntervalStartTime(), other.getIntervalSize(), this.getIntervalStartTime(),
                    this.getIntervalSize());
            addMessageSummary(sclone);
        }
    }

    @Override
    public final void fixIntervalSerialNum() throws AdeException {
        m_serialNum = PeriodUtils.getIntervalSerialNumInPeriod(m_framingFlowType, new Date(getIntervalEndTime() - 1));
    }

    @Override
    public final void alignIntervalTimes() throws AdeException {
        final Date periodStartDate = PeriodUtils.getContainingPeriodStart(new Date(m_endIntervalTime - 1));
        final long newStart = periodStartDate.getTime() + m_serialNum * m_framingFlowType.getDuration();
        final long newEnd = periodStartDate.getTime() + (1 + m_serialNum) * m_framingFlowType.getDuration();
        if (newStart != getIntervalStartTime() || newEnd != getIntervalEndTime()) {
            m_coverageFactor = m_coverageFactor * getIntervalSize() / m_framingFlowType.getDuration();
            setTime(newStart, newEnd);
        }
        clearLazyInternals();
    }

    private void clearLazyInternals() {
        for (IMessageSummary s : getMessageSummaries()) {
            s.streamline();
        }
    }

}
