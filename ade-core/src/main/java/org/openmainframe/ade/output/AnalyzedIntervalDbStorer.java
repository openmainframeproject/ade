/*
 
    Copyright IBM Corp. 2012, 2016
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
package org.openmainframe.ade.output;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IBasicInterval;
import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.PeriodImpl;
import org.openmainframe.ade.impl.dataStore.DataStorePeriodSummaries;
import org.openmainframe.ade.impl.dataStore.DatastorePeriodAndSerialNumFinder;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.impl.dbUtils.DmlPreparedStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.QueryPreparedStatementExecuter;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;

/**
 * Stores the event log analysis data in the database. The database stores general information on each {@link IAnalyzedInterval}
 * All {@link IAnalyzedInterval} objects that are stored should be from the same {@link ISource}.
 */
public class AnalyzedIntervalDbStorer extends AnalyzedIntervalOutputer {

    protected ISource m_source;
    protected DatastorePeriodAndSerialNumFinder m_periodFinder;
    protected FramingFlowType m_framingFlowType;
    protected boolean[] m_intervals;
    protected PeriodImpl m_cachedPeriod;
    protected DataStorePeriodSummaries m_dsPeriodSummaries;

    @Override
    public void setupSourceAndFlowType(ISource source, FramingFlowType framingFlowType) throws AdeException {
        m_source = source;
        m_periodFinder = new DatastorePeriodAndSerialNumFinder(m_source, framingFlowType);
        m_framingFlowType = framingFlowType;
        m_dsPeriodSummaries = AdeInternal.getAdeImpl().getDataStore().periodSummaries();
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        // TODO Auto-generated method stub

    }

    @Override
    public void incomingObject(IAnalyzedInterval interval) throws AdeException,
            AdeFlowException {
        setPeriodForInterval(interval);

        final int serialNum = getIntervalIndex(interval);
        if (serialNum < 0 || serialNum >= m_intervals.length) {
            throw new AdeInternalException("Interval serial num mismatches its period: serial_num="
                    + serialNum + " period serial num range=0..." + (m_intervals.length - 1));
        }

        try {
            if (m_intervals[serialNum]) {
                new UpdateAnalyzedInterval(m_cachedPeriod, serialNum, interval).execute();
            } else {
                new InsertAnalyzedInterval(m_cachedPeriod, serialNum, interval).execute();
            }
        } catch (AdeException e) {
            final Throwable ce = e.getCause();
            if (ce != null && SQLException.class.isInstance(ce)) {
                String fullReason = "serialNum=" + serialNum + "\n" + Arrays.toString(m_intervals);
                readAnalyzedIntervals();
                fullReason += "\n\n After exception the stat of the DB is:\n" + Arrays.toString(m_intervals);
                throw new AdeInternalException(fullReason, e);
            }
            throw e;
        }

    }
    
    /**
     * Determine the index for the interval.
     */
    protected int getIntervalIndex(IAnalyzedInterval interval) throws AdeException {
        return m_periodFinder.getLastSerialNum();
    }

    @Override
    public final void endOfStream() throws AdeException, AdeFlowException {
        m_intervals = null;
        m_cachedPeriod = null;
    }

    public final void setPeriodForInterval(IBasicInterval ai)
            throws AdeException {
        m_periodFinder.setIntervalStartTime(ai.getIntervalStartTime());
        final PeriodImpl period = m_periodFinder.getLastPeriod();
        setCachedPeriod(period, ai);
        setPeriodSummary(period);

    }

    private void setPeriodSummary(PeriodImpl period) throws AdeException {
        m_dsPeriodSummaries.getOrAddPeriodSummary(period, m_framingFlowType);
    }

    private void setCachedPeriod(PeriodImpl period, IBasicInterval ai) throws AdeException {
        //replace period
        if (!period.equals(m_cachedPeriod)) {
            m_cachedPeriod = period;

            readAnalyzedIntervals();
        }
    }

    protected final void readAnalyzedIntervals() throws AdeException {
        final int numOfIntervals = m_periodFinder.getIntervalsPerPeriod(); 
        m_intervals = new boolean[numOfIntervals];
        Arrays.fill(m_intervals, false);
        new LoadAnalyzedPeriod().executeQuery();
    }

    public final IPeriod getPeriod() {
        return m_cachedPeriod;
    }	

    /**inner class for storing a new analyzed interval 
     */
    static protected class InsertAnalyzedInterval extends DmlPreparedStatementExecuter {
        int m_periodId;
        IAnalyzedInterval m_interval;
        private int m_serialNum;

        public InsertAnalyzedInterval(PeriodImpl period, int serialNum, IAnalyzedInterval interval) {
            super("INSERT INTO " + SQL.ANALYSIS_RESULTS 
                    + "(PERIOD_INTERNAL_ID, INTERVAL_SERIAL_NUM, NUM_UNIQUE_MESSAGE_IDS, "
                    + "START_TIME, INTERVAL_SCORE,  MODEL_INTERNAL_ID, ADE_VERSION ) "
                    + " VALUES(?,?,?,?,?,?,?)");
            m_periodId = period.getInternalId();
            m_interval = interval;
            m_serialNum = serialNum;
        }

        @Override
        protected final void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            int pos = 1;
            stmt.setInt(pos++, m_periodId);
            stmt.setInt(pos++, m_serialNum);
            stmt.setInt(pos++, m_interval.getNumUniqueMessageIds());
            stmt.setLong(pos++, m_interval.getIntervalStartTime());
            stmt.setDouble(pos++, m_interval.getScore());
            stmt.setInt(pos++, m_interval.getModelInternalId());
            stmt.setString(pos++, m_interval.getAdeVersion().toString());
        }
    }

    /**inner class for storing a new analyzed interval 
     */
    static protected class UpdateAnalyzedInterval extends DmlPreparedStatementExecuter {
        int m_periodId;
        IAnalyzedInterval m_interval;
        private int m_serialNum;

        public UpdateAnalyzedInterval(PeriodImpl period, int serialNum, IAnalyzedInterval interval) {
            super("UPDATE " + SQL.ANALYSIS_RESULTS + " SET"
                    + " NUM_UNIQUE_MESSAGE_IDS=?, START_TIME=?, INTERVAL_SCORE=?,  MODEL_INTERNAL_ID=?, ADE_VERSION=?"
                    + " WHERE PERIOD_INTERNAL_ID=? and INTERVAL_SERIAL_NUM=?");
            m_periodId = period.getInternalId();
            m_interval = interval;
            m_serialNum = serialNum;
        }

        @Override
        protected final void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            int pos = 1;
            stmt.setInt(pos++, m_interval.getNumUniqueMessageIds());
            stmt.setLong(pos++, m_interval.getIntervalStartTime());
            stmt.setDouble(pos++, m_interval.getScore());
            stmt.setInt(pos++, m_interval.getModelInternalId());
            stmt.setString(pos++, m_interval.getAdeVersion().toString());
            stmt.setInt(pos++, m_periodId);
            stmt.setInt(pos++, m_serialNum);
        }
    }

    private class LoadAnalyzedPeriod extends QueryPreparedStatementExecuter {

        public LoadAnalyzedPeriod() {
            super("SELECT INTERVAL_SERIAL_NUM FROM " + SQL.ANALYSIS_RESULTS + " where PERIOD_INTERNAL_ID = ?");
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            stmt.setInt(1, m_cachedPeriod.getInternalId());
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            int pos = 1;
            final int serialNum = rs.getInt(pos++);

            // if we change the size of the train/analysis intervals, we may get more then we can handle.
            if (serialNum < m_intervals.length) {
                m_intervals[serialNum] = true;
            }

        }
    }
}
