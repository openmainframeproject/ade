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
package org.openmainframe.ade.impl.actions;

import java.util.ArrayList;
import java.util.Collection;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.AnalyzedMessageSummaryImpl;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.openmainframe.ade.scoringApi.IScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.patches.Version;

public class AnalyzedIntervalImpl implements IAnalyzedInterval {

    private IInterval m_interval;
    private Collection<IAnalyzedMessageSummary> m_messages;

    private int m_numUniqueIds;
    private long m_startTime;
    private long m_endTime;
    private int m_serialNum;
    private Integer m_modelInternalId;
    private Version a_adeVersion;
    private StatisticsChart m_statistics = new StatisticsChart();

    public AnalyzedIntervalImpl(IInterval interval, Integer modelInternalId) throws AdeException {
        m_interval = interval;
        m_serialNum = interval.getSerialNum();
        m_startTime = interval.getIntervalStartTime();
        m_endTime = interval.getIntervalEndTime();
        m_numUniqueIds = interval.getNumUniqueMessages();
        a_adeVersion = interval.getAdeVersion();
        m_modelInternalId = modelInternalId;
        m_messages = new ArrayList<>();
        for (IMessageSummary ms : interval.getMessageSummaries()) {
            final AnalyzedMessageSummaryImpl ams = new AnalyzedMessageSummaryImpl(ms);
            addMessage(ams);
        }
    }

    public AnalyzedIntervalImpl(int serialNum, long startDate, long endDate, double score,
            int uniqueMessageIds, int modelInternalId, Version adeVersion) throws AdeInternalException {
        m_startTime = startDate;

        if (endDate < startDate) {
            throw new AdeInternalException("Illegal end date");
        }
        m_endTime = endDate;

        if (uniqueMessageIds < 0) {
            throw new AdeInternalException("Illegal unique number of messages");
        }
        m_numUniqueIds = uniqueMessageIds;

        m_messages = new ArrayList<>();
        m_serialNum = serialNum;
        m_modelInternalId = modelInternalId;
        a_adeVersion = adeVersion;
        setScore(score);
    }

    public AnalyzedIntervalImpl(int serialNum, long startDate, long endDate, double score,
            int uniqueMessageIds, int modelInternalId) throws AdeException {
        this(serialNum, startDate, endDate, score, uniqueMessageIds, modelInternalId, Ade.getAde().getVersion());
    }

    @Override
    public final int getSerialNum() {
        return m_serialNum;
    }

    @Override
    public final Collection<IAnalyzedMessageSummary> getAnalyzedMessages() {
        return m_messages;
    }

    @Override
    public final long getIntervalEndTime() {
        return m_endTime;
    }

    @Override
    public final int getNumUniqueMessageIds() {
        return m_numUniqueIds;
    }

    @Override
    public final double getScore() throws AdeInternalException {
        return m_statistics.getDoubleStatOrThrow(IScorer.ANOMALY);
    }

    public final void setScore(double anomaly) throws AdeInternalException {
        m_statistics.setStat(IScorer.ANOMALY, anomaly);
    }

    @Override
    public final long getIntervalStartTime() {
        return m_startTime;
    }

    public final void addMessage(IAnalyzedMessageSummary message) {
        m_messages.add(message);
    }

    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append("[AnalyzedIntervalImpl:\n");
        result.append("    serialNum=" + getSerialNum() + "\n");
        result.append("    time=" + DateTimeUtils.timestampToHumanDateAndTimeAndStampLocal(m_startTime) + " - "
                + DateTimeUtils.timestampToHumanDateAndTimeLocal(m_endTime) + "\n");
        result.append(String.format("    msgIds=%d \n", m_numUniqueIds));
        result.append("    stat=" + m_statistics);
        result.append("]");

        return result.toString();
    }

    @Override
    public final Version getAdeVersion() {
        return a_adeVersion;
    }

    @Override
    public final Integer getModelInternalId() {
        return m_modelInternalId;
    }

    @Override
    public final IInterval getInterval() {
        return m_interval;
    }

    @Override
    public final StatisticsChart getStatistics() {
        return m_statistics;
    }

}
