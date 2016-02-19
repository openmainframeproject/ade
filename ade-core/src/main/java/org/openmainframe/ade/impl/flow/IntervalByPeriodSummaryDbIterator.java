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
package org.openmainframe.ade.impl.flow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.flow.IAdeIterator;
import org.openmainframe.ade.impl.data.IntervalClassificationEnum;
import org.openmainframe.ade.impl.data.IntervalImpl;
import org.openmainframe.ade.impl.data.MessageSummaryImpl;
import org.openmainframe.ade.impl.data.PeriodSummary;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.impl.dbUtils.QueryStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.QueryStatementIterator;
import org.openmainframe.ade.summary.SummarizationProperties;
import org.openmainframe.ade.utils.patches.Version;

public class IntervalByPeriodSummaryDbIterator implements IAdeIterator<IntervalImpl> {

    private PeriodSummary m_periodSummary;

    static class IntervalData implements Comparable<IntervalData> {
        long m_intervalStartTime;
        int m_classificationId;
        int m_serialNum;
        Version a_adeVersion;
        double m_coverageFactor;

        public IntervalData(long st, int cid, int serialNum, Version adeVersion, double coverageFactor) {
            m_intervalStartTime = st;
            m_classificationId = cid;
            m_serialNum = serialNum;
            a_adeVersion = adeVersion;
            m_coverageFactor = coverageFactor;
        }

        @Override
        public int compareTo(IntervalData o) {
            return m_serialNum - o.m_serialNum;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            final IntervalData intervalData = (IntervalData) obj;
            return this.m_serialNum == intervalData.m_serialNum;
        }
        
        @Override
        public int hashCode() {
            return m_serialNum;
        }

    }

    private ArrayList<IntervalData> m_intervalData = new ArrayList<IntervalData>();

    private QueryStatementIterator m_queryIterator;

    private ResultSet m_curResultSet;

    private int m_currentIntervalIndex;

    public IntervalByPeriodSummaryDbIterator(PeriodSummary periodSummary) throws AdeException {
        m_periodSummary = periodSummary;
    }

    @Override
    public void open() throws AdeException {
        loadIntervalData();
        m_currentIntervalIndex = 0;
        final String sql = "select INTERVAL_SERIAL_NUM, MESSAGE_INTERNAL_ID, SEVERITY, NUM_MESSAGES, TEXT_SUMMARY, TEXT_SAMPLE, CRITICAL_WORDS_SCORE, ENCODED_TIME_VECTOR "
                + " from " + SQL.MESSAGE_SUMMARIES + " where PERIOD_SUMMARY_INTERNAL_ID=" + m_periodSummary.getInternalId() + " order by INTERVAL_SERIAL_NUM";
        m_queryIterator = new QueryStatementIterator(sql);
        m_queryIterator.open();
        m_curResultSet = m_queryIterator.getNext();
    }

    @Override
    public IntervalImpl getNext() throws AdeException {

        if (m_currentIntervalIndex >= m_intervalData.size()) {
            return null;
        }
        final IntervalData intervalData = m_intervalData.get(m_currentIntervalIndex);
        final int serialNum = intervalData.m_serialNum;
        final Version adeVersion = intervalData.a_adeVersion;
        final ISource source = m_periodSummary.getPeriod().getSource();
        final IntervalImpl res = new IntervalImpl(intervalData.m_serialNum, intervalData.m_intervalStartTime, intervalData.m_coverageFactor,
                m_periodSummary.getFramingFlowType(), source, IntervalClassificationEnum.values()[intervalData.m_classificationId], adeVersion);
        try {
            while (m_curResultSet != null && m_curResultSet.getInt(1) == serialNum) {
                int pos = 2;
                final int msgInternalId = m_curResultSet.getInt(pos++);
                final Severity severity = Severity.values()[m_curResultSet.getInt(pos++)];
                final int numMessages = m_curResultSet.getInt(pos++);
                final String textSum = m_curResultSet.getString(pos++);
                final String textSample = m_curResultSet.getString(pos++);
                final int criticalWordsScore = m_curResultSet.getInt(pos++);
                final String encodedTimeVector = m_curResultSet.getString(pos++);

                final MessageSummaryImpl msi = new MessageSummaryImpl(msgInternalId, numMessages, textSample, textSum, criticalWordsScore, severity);
                msi.setTimeLine(SummarizationProperties.decodeTimeLine(encodedTimeVector));
                res.addMessageSummary(msi);
                m_curResultSet = m_queryIterator.getNext();
            }
        } catch (SQLException e) {
            throw new AdeInternalException("Db access error", e);
        }
        ++m_currentIntervalIndex;
        return res;
    }

    private void loadIntervalData() throws AdeException {
        m_intervalData.clear();
        final String sql = "select INTERVAL_SERIAL_NUM,INTERVAL_START_TIME,CLASSIFICATION_INTERNAL_ID,ADE_VERSION,COVERAGE_FACTOR from " + SQL.INTERVALS
                + " where PERIOD_SUMMARY_INTERNAL_ID=" + m_periodSummary.getInternalId();
        new QueryStatementExecuter(sql) {
            @Override
            protected void handleResultSet(ResultSet rs) throws SQLException,
                    AdeException {
                int pos = 1;
                final int serialNum = rs.getInt(pos++);
                final long startTime = rs.getLong(pos++);
                final int classId = rs.getInt(pos++);
                final Version adeVersion = Version.parse(rs.getString(pos++));
                final double coverageFactor = rs.getDouble(pos++);

                final IntervalData id = new IntervalData(startTime, classId, serialNum, adeVersion, coverageFactor);
                m_intervalData.add(id);
            }
        }.executeQuery();

        sortAndVerifyIntervalData();
    }

    private void sortAndVerifyIntervalData() throws AdeInternalException {
        Collections.sort(m_intervalData);
        for (int i = 0; i < m_intervalData.size() - 1; ++i) {
            if (m_intervalData.get(i).m_serialNum >= m_intervalData.get(i + 1).m_serialNum) {
                throw new AdeInternalException("Duplicate or out of order intervals in period summary " + m_periodSummary);
            }
        }
    }

    @Override
    public void close() throws AdeException {
        if (m_queryIterator != null) {
            m_queryIterator.close();
        }
    }

    @Override
    public void quietCleanup() {
        if (m_queryIterator != null) {
            m_queryIterator.quietCleanup();
        }
    }

}
