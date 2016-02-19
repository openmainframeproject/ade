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

import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.scoringApi.IScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;

public class AnalyzedMessageSummaryImpl implements IAnalyzedMessageSummary {

    private IMessageSummary m_messageSummary;

    private StatisticsChart m_statistics = new StatisticsChart();

    /**
     * Construct a new AnalyzedMessageSummaryImpl.
     * 
     * @param messageSummary the {@link IMessageSummary}
     */
    public AnalyzedMessageSummaryImpl(IMessageSummary messageSummary) throws AdeException {
        m_messageSummary = messageSummary;
    }

    @Override
    public final int getCriticalWordsScore() {
        return m_messageSummary.getCriticalWordsScore();
    }

    @Override
    public final String getMessageId() throws AdeException {
        return m_messageSummary.getMessageId();
    }

    @Override
    public final int getNumberOfAppearances() {
        return m_messageSummary.getNumMessageInstances();
    }

    @Override
    public final int getNumberOfFailedAppearances() {
        return m_messageSummary.getNumFailedMessageInstances();
    }

    @Override
    public final String getTextSample() {
        return m_messageSummary.getTextSample();
    }

    @Override
    public final String getTextSummary() {
        return m_messageSummary.getTextSummary();
    }

    @Override
    public final short[] getTimeLine() {
        return m_messageSummary.getTimeLine();
    }

    @Override
    public final String toString() {
        try {
            return getMessageId() + ": stats=" + m_statistics;
        } catch (AdeException ex) {
            throw new RuntimeException("ade error", ex);
        }
    }

    @Override
    public final IMessageSummary getMessageSummary() {
        return m_messageSummary;
    }

    @Override
    public final StatisticsChart getStatistics() {
        return m_statistics;
    }

    public final void setStatistics(StatisticsChart stats) {
        m_statistics = stats;
    }

    @Override
    public final double getFinalAnomaly() throws AdeInternalException {
        return m_statistics.getDoubleStatOrThrow(IScorer.ANOMALY);
    }

    @Override
    public final Severity getSeverity() {
        return m_messageSummary.getSeverity();
    }

}
