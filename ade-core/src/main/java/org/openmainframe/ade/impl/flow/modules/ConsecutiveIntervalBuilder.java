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

import java.util.SortedMap;
import java.util.TreeMap;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IIntervalClassification;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.actions.Action;
import org.openmainframe.ade.impl.data.IntervalClassificationEnum;
import org.openmainframe.ade.impl.data.IntervalImpl;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.dataStore.AdeDictionaries;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.summary.MessageSummaryBuilder;
import org.openmainframe.ade.summary.SummarizationProperties;

public class ConsecutiveIntervalBuilder extends IntervalBuilder {

    private boolean m_closed = true;

    private boolean m_inInterval = false;
    private boolean m_isFirstInterval = true;
    private Long m_curIntervalStartTime;
    private Long m_firstMsgTime;
    private Long m_lastMsgTime;

    private SortedMap<Integer, MessageSummaryBuilder> m_messageSummaryBuildersMap;
    private AdeDictionaries m_dictionaries;

    private boolean m_isTempInterval = false;

    private Long m_newIntervalStartTime = -1L;

    public ConsecutiveIntervalBuilder(ISource source, SummarizationProperties sumProps,
            FramingFlowType framingFlowType, IIntervalClassification intervalClassification,
            Action action) throws AdeException {
        super();
        setSource(source);
        setSumProps(sumProps);
        setFramingFlowType(framingFlowType);
        setIntervalClassification(intervalClassification);
        setAction(action);

        init();
    }

    /**
     * Remember to run init after constructor and before use.
     * @throws AdeException if there is a problem getting the Ade Dictionaries
     */
    @Override
    public final void init() throws AdeException {
        m_messageSummaryBuildersMap = new TreeMap<>();
        m_dictionaries = AdeInternal.getAdeImpl().getDictionaries();
    }

    private void assertOpen() throws AdeInternalException {
        if (m_closed) {
            throw new AdeInternalException("Interval builder is closed!");
        }
    }

    private void assertInInterval() throws AdeInternalException {
        if (!m_inInterval) {
            throw new AdeInternalException("not in interval, but should be");
        }
    }

    @Override
    public final void beginOfStream() throws AdeException {
        m_closed = false;
        sendBeginOfStream();
    }

    @Override
    public final void endOfStream() throws AdeException {
        assertOpen();
        wrapIntervalIfNeeded(true);
        m_inInterval = false;
        m_closed = true;
        sendEndOfStream();
    }

    @Override
    public final void incomingObject(IMessageInstance msg) throws AdeException {
        assertOpen();
        assertInInterval();

        final long msgTime = msg.getDateTime().getTime();
        if (m_firstMsgTime == null) {
            m_firstMsgTime = msgTime;
        }
        if (m_lastMsgTime == null || msgTime > m_lastMsgTime) {
            m_lastMsgTime = msgTime;
        }

        final String msgId = msg.getMessageId();
        final int msgInternalId = m_dictionaries.getMessageIdDictionary().addWord(msgId);
        MessageSummaryBuilder builder = m_messageSummaryBuildersMap.get(msgInternalId);
        if (builder == null) {
            final Severity severity = msg.getSeverity();
            builder = new MessageSummaryBuilder(m_sumProps, msgInternalId, severity,
                    m_curIntervalStartTime, m_framingFlowType);
            m_messageSummaryBuildersMap.put(msgInternalId, builder);
        }
        builder.incomingObject(msg);
    }

    @Override
    public final void incomingSeparator(Separator sep) throws AdeException {
        assertOpen();

        switch (sep.getType()) {
            case INTERVAL:
                handleIntervalSeparator(sep);
                break;
            case TIME:
                handleTimeSeparator(sep);
                break;
            default:
                throw new AdeInternalException("Unknown Time.getType(): " + sep.getType());
        }
    }

    private void handleIntervalSeparator(Separator sep) throws AdeException {
        m_newIntervalStartTime = ((IntervalSeparator) sep).getTime();
        wrapIntervalIfNeeded(false);

        m_inInterval = true;
        m_curIntervalStartTime = m_newIntervalStartTime;
        m_isTempInterval = TemporaryIntervalSeparator.class.isInstance(sep);
    }

    private void handleTimeSeparator(Separator sep) throws AdeException {
        wrapIntervalIfNeeded(true);
        sendSeparator((TimeSeparator) sep);

        m_inInterval = false;
        m_isFirstInterval = true;
        m_firstMsgTime = null;
        m_lastMsgTime = null;
    }

    private void wrapIntervalIfNeeded(boolean isLastInterval) throws AdeException {
        if (m_inInterval) {
            wrapInterval(isLastInterval);
        }
    }

    private void wrapInterval(boolean isLastInterval) throws AdeException {
        assertInInterval();

        double coverageFactor = 1d;

        if (m_isFirstInterval && m_firstMsgTime != null) {
            final double frontNonCoverageFactor = (double) (m_firstMsgTime - m_curIntervalStartTime) / m_framingFlowType.getDuration();
            coverageFactor -= frontNonCoverageFactor;
        }

        if (isLastInterval && m_lastMsgTime != null) {
            final double endNonCoverageFactor = 1d - (double) (m_lastMsgTime + 1L - m_curIntervalStartTime) / m_framingFlowType.getDuration();
            coverageFactor -= endNonCoverageFactor;
        }

        IIntervalClassification currentIntervalType = m_intervalClassification;
        if (m_isTempInterval) {
            currentIntervalType = IntervalClassificationEnum.CONTINUOUS;
        }
        IntervalImpl interval;
        m_numFinder.setIntervalStartTime(m_curIntervalStartTime);
        final int serialNum = m_numFinder.getLastSerialNum();

        interval = new IntervalImpl(serialNum, m_curIntervalStartTime, coverageFactor, m_framingFlowType,
                m_source, currentIntervalType);

        for (MessageSummaryBuilder msgSummaryBuilder : m_messageSummaryBuildersMap.values()) {
            msgSummaryBuilder.endOfStream();
            final IMessageSummary msgSummary = msgSummaryBuilder.getMessageSummary();
            interval.addMessageSummary(msgSummary);
        }
        sendObject(interval);

        m_messageSummaryBuildersMap.clear();
        m_curIntervalStartTime = null;
        m_isFirstInterval = false;
    }

}
