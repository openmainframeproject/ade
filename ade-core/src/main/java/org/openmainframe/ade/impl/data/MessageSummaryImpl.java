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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.summary.LevenshteinTextSummary;
import org.openmainframe.ade.impl.summary.MessageSummaryBuilder;

/**
 * Implements the {@link IMessageSummary} interface.
 * Each message summary holds data regarding message instance 
 * that share the same {@link IMessageInstance#getMessageId()}
 */
public class MessageSummaryImpl implements IMessageSummary {

    private short[] m_timeLine;
    private Set<Long> m_exactTimeLine = null;

    private int m_criticalWordsScore = -1;

    private Severity m_severity;

    protected int m_messageInternalId;
    // The amount of messageInstances that are summarized 
    protected int m_instanceCount;
    protected int m_instanceCountFailed;

    /**
     * Holds the summary text. 
     */
    protected String m_textSummary = null;

    protected String m_textSample = null;

    /**
     * Creates a new MessageSummary by the messageID parameter.
     * 
     * @param messageID  - id of the MessageSummary
     * @param severity severity for this {@link IMessageSummary}. Severity is set by the first message of this 
     *  {@link IMessageSummary}. 
     */

    public MessageSummaryImpl(int messageID, Severity severity) {
        m_messageInternalId = messageID;
        m_instanceCount = 0;
        m_instanceCountFailed = 0;
        m_severity = severity;
    }

    /**
     * Creates a new MessageSummary by the messageID parameter with no severity parameter.
     * 
     * @param messageID  - id of the MessageSummary
     */

    public MessageSummaryImpl(int messageID) {
        m_messageInternalId = messageID;
        m_instanceCount = 0;
        m_instanceCountFailed = 0;
        m_severity = Severity.UNKNOWN;
    }

    /**
     * Creates a new MessageSummary by the messageID parameter with an unknown severity, an initialized text,
     * the number of instance counts summarized by this {@link IMessageSummary}.
     * This would be used usually when uploading Summary Data from a data store.
     * 
     * @param messageID - id of the MessageSummary
     * @param instanceCount - number of instances summarized by this {@link IMessageSummary}
     * @param textSample - initialized text for the sample
     * @param textSummary - initialized text for the summary
     * @param criticalWordsScore - the critical words score
     */
    public MessageSummaryImpl(int messageID, int instanceCount, String textSample, String textSummary,
            int criticalWordsScore) {
        this(messageID, instanceCount, textSample, textSummary, criticalWordsScore, Severity.UNKNOWN);
    }

    /**
     * Creates a new MessageSummary by the messageID parameter with an unknown severity, an initialized text,
     * the number of instance counts summarized by this {@link IMessageSummary} and 
     * the number of failed instance counts summarized by this {@link IMessageSummary}.
     * This would be used usually when uploading Summary Data from a data store.
     * 
     * @param messageID - id of the MessageSummary
     * @param instanceCount - number of instances summarized by this {@link IMessageSummary}
     * @param instanceCountFailed - the number of failed instance counts summarized by this {@link IMessageSummary}
     * @param textSample - initialized text for the sample
     * @param textSummary - initialized text for the summary
     * @param criticalWordsScore - the critical words score
     */
    public MessageSummaryImpl(int messageID, int instanceCount, int instanceCountFailed, String textSample,
            String textSummary, int criticalWordsScore) {
        this(messageID, instanceCount, textSample, textSummary, criticalWordsScore, Severity.UNKNOWN);
        m_instanceCountFailed = instanceCountFailed;
    }

    /**
     * Creates a new MessageSummary by the messageID parameter, an initialized text
     * and the number of instance counts summarized by this {@link IMessageSummary}.
     * This would be used usually when uploading Summary Data from a data store.
     * 
     * @param messageID - id of the MessageSummary
     * @param instanceCount - number of instances summarized by this {@link IMessageSummary}
     * @param textSample - initialized text for the sample
     * @param textSummary - initialized text for the summary
     * @param criticalWordsScore - the critical words score
     * @param severity severity for this {@link IMessageSummary}. Severity is set by the first message of this 
     * {@link IMessageSummary}. 
     */
    public MessageSummaryImpl(int messageID, int instanceCount, String textSample, String textSummary,
            int criticalWordsScore, Severity severity) {
        m_messageInternalId = messageID;
        m_textSample = textSample;
        m_textSummary = textSummary;
        m_instanceCount = instanceCount;
        m_criticalWordsScore = criticalWordsScore;
        m_severity = severity;
    }

    @Override
    public final int getMessageInternalId() {
        return m_messageInternalId;
    }

    @Override
    public final int getNumMessageInstances() {
        return m_instanceCount;
    }

    @Override
    public final int getNumFailedMessageInstances() {
        return m_instanceCountFailed;
    }

    @Override
    public final String getTextSummary() {
        return m_textSummary;
    }

    @Override
    public final String toString() {
        final StringBuilder result = new StringBuilder();
        result.append("MessageSummary:\n");
        result.append("   messageId=" + m_messageInternalId + "\n");
        result.append("   instanceCount=" + m_instanceCount + "\n");
        result.append("   textSample=" + m_textSample + "\n");
        result.append("   textSummary=" + m_textSummary + "\n");
        result.append("   criticalWordsScore=" + m_criticalWordsScore + "\n");
        result.append("   severity=" + m_severity + "\n");
        return result.toString();
    }

    @Override
    public final String getTextSample() {
        return m_textSample;
    }

    @Override
    public final void setTextSummary(String textSummary) {
        m_textSummary = textSummary;
    }

    public final void setTextSample(String text) {
        m_textSample = text;
    }

    /**
     * Increment the message counter by a passed in count.
     * 
     * @param count the amount to increment the message counter
     */
    public final void incrementMessageCounter(int count) {
        m_instanceCount += count;
    }

    public final void setMessageCounter(int count) {
        m_instanceCount = count;
    }

    /**
     * Increment the message failed counter by a passed in count.
     * 
     * @param count the amount to increment the message failed counter
     */
    public final void incrementMessageFailedCounter(int count) {
        m_instanceCountFailed += count;
    }

    /**
     * Set the message failed counter to a passed in count.
     * 
     * @param count the value to set the message failed counter to
     */
    public final void setMessageFailedCounter(int count) {
        m_instanceCountFailed = count;
    }

    @Override
    public final void setTimeLine(short[] timeLine) {
        m_timeLine = Arrays.copyOf(timeLine, timeLine.length);
    }

    @Override
    public final short[] getTimeLine() {
        return Arrays.copyOf(m_timeLine, m_timeLine.length);
    }

    @Override
    public final int getCriticalWordsScore() {
        return m_criticalWordsScore;
    }

    public final void setCriticalWordsScore(int val) {
        m_criticalWordsScore = val;
    }

    @Override
    public final String getMessageId() throws AdeException {
        return AdeInternal.getAdeImpl().getDictionaries().getMessageIdDictionary().
                getWordById(m_messageInternalId);
    }

    @Override
    public final Severity getSeverity() {
        return m_severity;
    }

    public final void setSeverity(Severity severity) {
        m_severity = severity;
    }

    @Override
    public final void join(IMessageSummary other, IInterval otherInterval, IInterval target) {

        if (other instanceof MessageSummaryImpl) {
            addAllTimeline((MessageSummaryImpl) other, otherInterval, target);
        } else {
            addAllTimeline(other.getTimeLine(), otherInterval, target);
        }
        m_criticalWordsScore = Math.max(m_criticalWordsScore, other.getCriticalWordsScore());

        if (this.getSeverity().ordinal() < other.getSeverity().ordinal()) {
            setSeverity(other.getSeverity());
        }

        m_instanceCount += other.getNumMessageInstances();
        m_instanceCountFailed += other.getNumFailedMessageInstances();

        if (other.getCriticalWordsScore() > this.getCriticalWordsScore()) {
            setCriticalWordsScore(other.getCriticalWordsScore());
            setTextSample(other.getTextSample());
        }

        if (this.getTextSample() == null) {
            this.setTextSample(other.getTextSample());
        }

        if (this.getTextSummary() == null) {
            this.setTextSummary(other.getTextSummary());
        } else if (other.getTextSummary() != null) {
            final String summary = LevenshteinTextSummary.summarizeStrings(this.getTextSummary(),
                    other.getTextSummary());
            this.setTextSummary(summary);
        }
    }

    @Override
    public final void adjustTimeline(long oldStartTime, long oldSize, long newStartTime, long newSize) {
        if (m_timeLine == null) {
            // nothing to do here, move along.
            return;
        }
        maybeFillExactTimeLine(oldStartTime, oldSize);
        rebuildTimelineFromExactTimeline(newStartTime, newSize);
    }

    private void addAllTimeline(MessageSummaryImpl other, IInterval otherInterval, IInterval dst) {
        final Set<Long> otherExactTimeLine = other.getExactTimeLine(otherInterval.getIntervalStartTime(),
                otherInterval.getIntervalSize());
        if (m_timeLine == null) {
            return;
        }

        maybeFillExactTimeLine(otherInterval.getIntervalStartTime(), otherInterval.getIntervalSize());

        if (otherExactTimeLine != null) {
            if (m_exactTimeLine == null) {
                m_exactTimeLine = new TreeSet<Long>();
            }
            m_exactTimeLine.addAll(otherExactTimeLine);
        }
        rebuildTimelineFromExactTimeline(dst.getIntervalStartTime(), dst.getIntervalSize());
    }

    private void addAllTimeline(short[] timeLine, IInterval src, IInterval dst) {
        if (m_timeLine == null && timeLine == null) {
            return;
        }
        final Set<Short> set = new TreeSet<Short>();
        if (m_timeLine != null) {
            for (short i : m_timeLine) {
                set.add(i);
            }
        }
        if (timeLine != null) {
            for (short i : timeLine) {
                final long time = MessageSummaryBuilder.indexToTime(i, src.getIntervalStartTime(),
                        src.getIntervalSize());
                final short newIndex = MessageSummaryBuilder.timeToIndex(time, dst.getIntervalStartTime(),
                        dst.getIntervalSize());
                set.add(newIndex);
            }
        }
        rebuildTimeline(set);
    }

    private Set<Long> getExactTimeLine(long intervalStartTime, long intervalSize) {
        maybeFillExactTimeLine(intervalStartTime, intervalSize);
        return new TreeSet<Long>(m_exactTimeLine);
    }

    private void maybeFillExactTimeLine(long intervalStartTime, long intervalSize) {
        if (m_exactTimeLine == null && m_timeLine != null) {
            m_exactTimeLine = new TreeSet<Long>();
            for (short i : m_timeLine) {
                m_exactTimeLine.add(MessageSummaryBuilder.indexToTime(i, intervalStartTime, intervalSize));
            }
        }
    }

    private void rebuildTimelineFromExactTimeline(long intervalStartTime, long intervalSize) {
        final ArrayList<Short> newTimeLine = new ArrayList<Short>(m_exactTimeLine.size());
        int pos = 0;
        short lastIndex = Short.MIN_VALUE;
        for (Long time : m_exactTimeLine) {
            final short index = MessageSummaryBuilder.timeToIndex(time, intervalStartTime, intervalSize);
            if (index != lastIndex) {
                newTimeLine.add(index);
                lastIndex = index;
            }
        }
        m_timeLine = new short[newTimeLine.size()];
        for (pos = 0; pos < newTimeLine.size(); ++pos) {
            m_timeLine[pos] = newTimeLine.get(pos);
        }

    }

    private void rebuildTimeline(Set<Short> set) {
        m_timeLine = new short[set.size()];
        final Iterator<Short> it = set.iterator();
        int pos = 0;
        while (it.hasNext()) {
            m_timeLine[pos++] = it.next();
        }
        m_exactTimeLine = null;
    }

    @Override
    /**
     * Create a copy of this MessageSummaryImpl object.
     */
    public final MessageSummaryImpl clone() {
        final MessageSummaryImpl res = new MessageSummaryImpl(
                m_messageInternalId,
                m_instanceCount,
                m_textSample,
                m_textSummary,
                m_criticalWordsScore,
                m_severity);
        final short[] timeLine = getTimeLine(); 
        if (timeLine != null) {
            res.setTimeLine(Arrays.copyOf(timeLine, timeLine.length));
        }
        if (m_exactTimeLine != null) {
            res.m_exactTimeLine = new TreeSet<Long>(m_exactTimeLine);
        }
        return res;
    }

    @Override
    public final void streamline() {
        m_exactTimeLine = null;
    }

}
