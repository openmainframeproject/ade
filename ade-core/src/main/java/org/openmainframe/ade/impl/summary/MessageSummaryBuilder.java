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
package org.openmainframe.ade.impl.summary;

import java.util.Set;
import java.util.TreeSet;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.flow.IStreamTarget;
import org.openmainframe.ade.impl.data.MessageSummaryImpl;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.summary.SummarizationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a message summary for a specific message id, based on message instances streamed in.
 */
public class MessageSummaryBuilder implements IStreamTarget<IMessageInstance> {

    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private MessageSummaryImpl m_messageSummary;
    private SummarizationProperties m_sumProps;
    private Set<Short> m_timeLine;
    private boolean m_messageSummaryReady = false;
    private CriticalWordsScorer m_textScore = null;
    private long m_intervalStartTime;
    private FramingFlowType m_intervalDataType;

    public MessageSummaryBuilder(SummarizationProperties sumProps, int msgId, Severity severity,
            long intervalStartTime, FramingFlowType intervalDataType) {
        m_intervalStartTime = intervalStartTime;
        m_intervalDataType = intervalDataType;
        m_sumProps = sumProps;
        m_messageSummary = new MessageSummaryImpl(msgId, severity);
        if (sumProps.m_summarizeTimeLine) {
            m_timeLine = new TreeSet<Short>();
        }
        if (sumProps.m_calculateCriticalWordsScore) {
            m_textScore = AdeInternal.getAdeImpl().getUserSpecifications().getCriticalWordsScorer();
        }
        clear();
    }

    public MessageSummaryBuilder(SummarizationProperties sumProps, int msgId, long intervalStartTime, FramingFlowType intervalDataType) {
        this(sumProps, msgId, Severity.UNKNOWN, intervalStartTime, intervalDataType);
    }

    public static short timeToIndex(long time, long intervalStartTime, long intervalDuration) {
        double value = (double) (time - intervalStartTime) / intervalDuration;
        if (value < -1.0) {
            value = -1.0;
        } else if (value > 2.0) {
            value = 2.0;
        }
        return (short) (value * SummarizationProperties.TIMELINE_RESOLUTION);
    }

    public static long indexToTime(int idx, long intervalStartTime, long intervalDuration) {
        return (long) (intervalStartTime + intervalDuration / SummarizationProperties.TIMELINE_RESOLUTION * (0.5 + idx));
    }

    @Override
    public void incomingObject(IMessageInstance msg) throws AdeInternalException {
        m_messageSummaryReady = false;
        m_messageSummary.incrementMessageCounter(msg.getCount());
        m_messageSummary.incrementMessageFailedCounter(msg.getCountFailed());

        final String text = msg.getText();

        if (m_messageSummary.getTextSample() == null) {
            m_messageSummary.setTextSample(text);
        }

        if (m_sumProps.m_summarizeText && text != null) {
            if (m_messageSummary.getTextSummary() == null) {
                m_messageSummary.setTextSummary(text);
            } else {
                m_messageSummary.setTextSummary(
                        LevenshteinTextSummary.summarizeStrings(m_messageSummary.getTextSummary(), text));
            }
        }

        if (m_sumProps.m_summarizeTimeLine) {
            int idx = timeToIndex(msg.getDateTime().getTime(), m_intervalStartTime, m_intervalDataType.getDuration());
            if (idx < 0) {
                logger.warn("in msg=" + msg.getMessageId() + " from " + msg.getDateTime() + " got negative idx (" + idx + ") relative to the interval starting at " + m_intervalStartTime);
                idx = 0;
            }
            if (idx >= SummarizationProperties.TIMELINE_RESOLUTION) {
                throw new AdeInternalException("Should not have recieved messages exceeding interval end time: (" + idx + ")" + msg);
            }

            m_timeLine.add((short) idx);
        }

        if (m_sumProps.m_calculateCriticalWordsScore && text != null) {
            final int cwScore = m_textScore.calcScore(text);
            if (cwScore > m_messageSummary.getCriticalWordsScore()) {
                m_messageSummary.setTextSample(text);
                m_messageSummary.setCriticalWordsScore(cwScore);
            }
        }
    }

    public void merge(MessageSummaryBuilder otherBuilder) throws AdeInternalException {
        final IMessageSummary other = otherBuilder.getMessageSummary();
        m_messageSummaryReady = false;
        final int messageID = m_messageSummary.getMessageInternalId();

        if (messageID != other.getMessageInternalId()) {
            throw new AdeInternalException("cannot merge summaries of different msg ids");
        }

        Severity severity = m_messageSummary.getSeverity();

        if (severity.ordinal() < other.getSeverity().ordinal()) {
            // set the severity of the merged message summary to be the max of two summaries. If severity
            // is indicated only for one of the two message summaries, the severity will be set to this severity.
            // (it works because Severity.UNKNOWN is always the first one in its order
            severity = other.getSeverity();
        }

        m_messageSummary.setSeverity(severity);

        // Sum instances
        m_messageSummary.setMessageCounter(m_messageSummary.getNumMessageInstances() + other.getNumMessageInstances());
        m_messageSummary.setMessageFailedCounter(m_messageSummary.getNumFailedMessageInstances() + other.getNumFailedMessageInstances());

        if (m_sumProps.m_calculateCriticalWordsScore && other.getCriticalWordsScore() > m_messageSummary.getCriticalWordsScore()) {
            m_messageSummary.setCriticalWordsScore(other.getCriticalWordsScore());
            m_messageSummary.setTextSample(other.getTextSample());
        }
        if (m_messageSummary.getTextSample() == null) {
            m_messageSummary.setTextSample(other.getTextSample());
        }

        if (m_sumProps.m_summarizeText) {
            if (m_messageSummary.getTextSummary() == null) {
                m_messageSummary.setTextSummary(other.getTextSummary());
            } else if (other.getTextSummary() != null) {
                final String summary = LevenshteinTextSummary.summarizeStrings(m_messageSummary.getTextSummary(), other.getTextSummary());
                m_messageSummary.setTextSummary(summary);
            }
        }

        if (m_sumProps.m_summarizeTimeLine) {
            m_timeLine.addAll(otherBuilder.m_timeLine);
        }
    }

    @Override
    public void endOfStream() throws AdeException {
    }

    public IMessageSummary getMessageSummary() {
        prepareMessageSummary();
        return m_messageSummary;
    }

    private void prepareMessageSummary() {
        if (m_messageSummaryReady) {
            return;
        }

        if (m_sumProps.m_summarizeTimeLine) {
            final short[] timeLine = new short[m_timeLine.size()];
            int pos = 0;
            for (short t : m_timeLine) {
                timeLine[pos++] = t;
            }
            m_messageSummary.setTimeLine(timeLine);
        }

        m_messageSummaryReady = true;
    }

    public void clear() {
        if (m_timeLine != null) {
            m_timeLine.clear();
        }
        m_messageSummary.setMessageCounter(0);
        m_messageSummary.setMessageFailedCounter(0);
        if (m_sumProps.m_calculateCriticalWordsScore) {
            m_messageSummary.setCriticalWordsScore(0);
        } else {
            m_messageSummary.setCriticalWordsScore(-1);
        }
        m_messageSummary.setTextSample(null);
        m_messageSummary.setTextSummary(null);
        m_messageSummaryReady = false;
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        // TODO Auto-generated method stub

    }
}
