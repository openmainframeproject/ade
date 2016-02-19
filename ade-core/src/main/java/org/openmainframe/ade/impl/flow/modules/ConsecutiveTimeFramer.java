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
package org.openmainframe.ade.impl.flow.modules;

import java.util.Date;
import java.util.Properties;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IIntervalClassification;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.flow.FlowUtils;
import org.openmainframe.ade.impl.actions.Action;
import org.openmainframe.ade.impl.data.FileSeperator;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.summary.SummarizationProperties;

/**
 * A ade block which receives {@link IMessageInstance} objects and forwards them
 * with separators according to a given time duration.
 */
public class ConsecutiveTimeFramer extends IntervalFramer {

    /**
     * Starting time of interval as the number of seconds since January 1, 1970, 00:00:00 GMT. see {@link Date}
     */
    protected Long m_currentTimeFrameStartTime = null;
    private Long m_curMsgTime = null;

    protected long m_alignmentOffset;
    private FileSeperator m_fileSepReceived = null;

    public static final String ALIGNMENT_OFFSET = "alignmentOffset";
    public static final int TWO_INTERVALS = 2;

    /**
     * Construct a new ConsecutiveTimeFramer.
     * 
     * @param framingFlow Contains the properties for the interval time frame.
     * @throws AdeException see {@link IntervalFramer#IntervalFramer(Properties)}
     */
    public ConsecutiveTimeFramer(FramingFlowType framingFlow) throws AdeException {
        super(framingFlow);
        setAlighnmentOffset();
    }

    /**
     * Construct a new ConsecutiveTimeFramer.
     */
    public ConsecutiveTimeFramer() {
        super();
        setAlighnmentOffset();
    }

    protected final void setAlighnmentOffset() {
        String s;
        s = getProp(ALIGNMENT_OFFSET);
        if (s != null) {
            m_alignmentOffset = Long.parseLong(s);
        } else {
            m_alignmentOffset = 0L;
        }
    }

    @Override
    public final void beginOfStream() throws AdeException {
        sendBeginOfStream();
    }

    @Override
    public final void incomingObject(IMessageInstance msg) throws AdeException {
        final Long messagTime = msg.getDateTime().getTime();

        if (m_fileSepReceived != null && m_curMsgTime != null) {
            if (messagTime - m_curMsgTime > TWO_INTERVALS * m_framingFlow.getDuration()) {
                this.incomingSeparator(AdeInternal.getAdeImpl().getDataFactory().
                        newTimeSeparator(m_fileSepReceived.getSource(),
                                "File changed and time moved forward more than two intervals. "
                                        + m_fileSepReceived.getReason()));
            } else if (messagTime - m_curMsgTime < -1 * m_framingFlow.getDuration()) {
                this.incomingSeparator(AdeInternal.getAdeImpl().getDataFactory().
                        newTimeSeparator(m_fileSepReceived.getSource(),
                                "File changed and time moved backward more than one interval. "
                                        + m_fileSepReceived.getReason()));
            }
        }

        m_curMsgTime = messagTime;
        m_fileSepReceived = null;

        // the time frame start can only be null only if the incoming MessageInstance object is the first
        // message in a new File.
        if (m_currentTimeFrameStartTime == null) {
            setFirstMessageTimeAndSendSeparator(m_curMsgTime);
        }
        // Consecutive messages can be a long time apart. We need to fill the time gap with (sometimes) empty intervals 
        while (separatorRequired(m_curMsgTime)) {
            updateTime();
            final IntervalSeparator intervalSeparator = generateIntervalSeparator();
            sendSeparator(intervalSeparator);

        }

        // Now we know that this message is not delayed by more that m_duration from the current time frame beginning,
        // we can forward the incoming MessageInstance.
        sendObject(msg);
    }

    protected IntervalSeparator generateIntervalSeparator() {
        return new IntervalSeparator(m_currentTimeFrameStartTime);
    }

    @Override
    public void incomingSeparator(TimeSeparator sep) throws AdeException {
        if (sep instanceof FileSeperator) {
            m_fileSepReceived = (FileSeperator) sep;
        } else {
            sendSeparator(sep);
            wrapDuration();
        }
    }

    @Override
    public final void endOfStream() throws AdeException {
        wrapDuration();
        sendEndOfStream();
    }

    private void wrapDuration() {
        m_curMsgTime = null;
        // reset the beginning of the time frame so we know that the next incoming MessageInstance should begin a new
        // Interval regardless of the previous ones
        m_currentTimeFrameStartTime = null;
    }

    /**
     * @param firstMessageTime - the time (in milliseconds) of the first message in a file
     * @throws AdeException - see {@link FlowUtils#sendSeparator(Object, java.util.Collection)}
     * @throws AdeFlowException - see {@link FlowUtils#sendSeparator(Object, java.util.Collection)}
     */
    protected void setFirstMessageTimeAndSendSeparator(long firstMessageTime) throws AdeException {
        // set the beginning of the current time frame to the time of the first message
        // aligned with respect to the time framer's duration 
        m_currentTimeFrameStartTime = alignToTimeFrame(firstMessageTime);
        // send the separator to mark a new time frame.
        sendSeparator(generateIntervalSeparator());
    }

    /**
     * Returns true if the given message time requires a separator.
     * 
     * @param msgTime current message instance time in milliseconds
     * @return whether a separator is required (starting a new time-frame) 
     */
    protected final boolean separatorRequired(Long msgTime) {
        // open a new interval if this message's time is greater than this interval's end time.
        // Note: old messages (even if they do not belong to this interval) go into this interval.
        if (m_currentTimeFrameStartTime + m_framingFlow.getDuration() > msgTime) {
            return false;
        }
        return true;
    }

    /**
     * Increment the current time frame time by the time framer's duration.
     */
    protected final void updateTime() {
        m_currentTimeFrameStartTime += m_framingFlow.getDuration();
    }

    /**
     * Finds the start time of the time frame corresponding to time, aligned to the calendar in the time 
     * zone specified by the user in the config file.  
     * 
     * @param duration time duration of time frame in milliseconds
     * @param time time of current message
     * @return start time of the time frame corresponding to time
     */
    public final long alignToTimeFrame(long time) {
        return m_alignmentOffset + m_framingFlow.getDuration()
        * ((time - m_alignmentOffset) / m_framingFlow.getDuration());
    }

    public final long getAlignmentOffset() {
        return m_alignmentOffset;
    }

    @Override
    public IntervalBuilder getIntervalBuilder(
            ISource source, SummarizationProperties sumProps,
            FramingFlowType framingFlowType,
            IIntervalClassification intervalClassicication, Action action)
                    throws AdeException {
        return new ConsecutiveIntervalBuilder(source, sumProps, framingFlowType, intervalClassicication, action);
    }
}
