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
package org.openmainframe.ade.impl.dataStore;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IConfigurationData;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.dataStore.IDataStorePeriods;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.dbUtils.PreparedStatementWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.flow.IAdeIterator;
import org.openmainframe.ade.impl.data.IntervalImpl;
import org.openmainframe.ade.impl.data.PeriodImpl;
import org.openmainframe.ade.impl.dbUtils.DmlPreparedStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.QueryPreparedStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.TableGeneralUtils;
import org.openmainframe.ade.impl.flow.IntervalByPeriodsAndFramingFlowTypeDbIterator;
import org.openmainframe.ade.impl.flow.AdeIteratorAdaptor;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the implementation class for the Periods data store.
 * A Period is an amount of time for which message
 *     data was collected or analyzed.
 * A Period is associated with a source. 
 * 
 */
public class DataStorePeriodsImpl implements IDataStorePeriods {

    private static final Logger LOG = LoggerFactory.getLogger(DataStorePeriodsImpl.class);

    public DataStorePeriodsImpl() throws AdeException {
        // Nothing to initialize
    }


    @Override
    public final void deletePeriod(IPeriod period) throws AdeException {
        new PeriodDeleter(period).execute();
    }

    @Override
    public final Collection<IPeriod> getAllPeriods(ISource source, Date minTime,
            Date maxTime) throws AdeException {
        final PeriodLister pl = new PeriodLister(source, minTime, maxTime);
        pl.executeQuery();
        return new ArrayList<IPeriod>(pl.m_result);
    }

    public final PeriodImpl getOrAddPeriod(ISource source, Date startTime, Date endTime) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        PeriodImpl res = null;
        try {
            cw.startTransaction();
            cw.lockTableExclusive(SQL.PERIODS);
            res = getPeriodImpl(source, startTime, endTime);
            if (res == null) {
                addPeriod(cw, source, startTime, endTime);
                res = new PeriodImpl(cw.simpleQueries().getLastKey(), source, false, 0, null, startTime, endTime);
                LOG.info("Created a new period " + res);
            }
            cw.endTransaction();
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        return res;
    }

    @Override
    public final void updatePeriodMetaData(IPeriod period) throws AdeException {
        new PeriodUpdater(getPeriodImpl(period)).execute();
    }

    public final PeriodImpl getPeriodImpl(IPeriod period) throws AdeException {
        if (period instanceof PeriodImpl) {
            return (PeriodImpl) period;
        }
        return getPeriodImpl(period.getSource(), new Date(period.getStartTime().getTime()), period.getEndTime());
    }

    public final PeriodImpl getPeriodImpl(ISource source, Date startTime, Date endTime) throws AdeException {
        final PeriodLoader pl = new PeriodLoader(source, startTime, endTime);
        pl.executeQuery();
        if (pl.m_result.isEmpty()) {
            return null;
        }

        final PeriodImpl res = pl.m_result.get(0);
        if (pl.m_result.size() == 1 
                && res.getStartTime().equals(startTime) 
                && res.getEndTime().equals(endTime)) {
            return res;
        }

        final StringBuilder overlaps = new StringBuilder();
        
        overlaps.append("Found periods overlapping to time range [" 
                + DateTimeUtils.timestampToHumanDateAndTimeAde(startTime.getTime()) + "," 
                + DateTimeUtils.timestampToHumanDateAndTimeAde(endTime.getTime()) + "]: ");
        for (IPeriod p : pl.m_result) {
            overlaps.append(" " + p);
        }
        
        throw new AdeInternalException(overlaps.toString());
    }

    private static void addPeriod(ConnectionWrapper cw, ISource source, Date startTime, Date endTime) 
            throws SQLException {

        final StringBuilder sqlStm = new StringBuilder();
        
        sqlStm.append("insert into ");
        sqlStm.append(SQL.PERIODS);
        sqlStm.append("(source_internal_id,exclude_from_training,status,comment,start_time,end_time)");
        sqlStm.append(" values (?,?,?,?,?,?)");

        final PreparedStatementWrapper psw = cw.preparedStatement(sqlStm.toString());
        final PreparedStatement ps = psw.getPreparedStatement();
        int pos = 1;
        ps.setInt(pos++, source.getSourceInternalId());
        ps.setInt(pos++, 0);
        ps.setInt(pos++, 0);
        ps.setNull(pos++, Types.VARCHAR);
        psw.setTimestamp(pos++, startTime);
        psw.setTimestamp(pos++, endTime);
        psw.execute();
        psw.close();
    }

    @Override
    public IAdeIterator<IInterval> getPeriodIntervals(IPeriod period, FramingFlowType framingFlowType) 
            throws AdeException {
        return getPeriodIntervals(period, framingFlowType, true);
    }

    @Override
    public IAdeIterator<IInterval> getPeriodIntervals(IPeriod period, FramingFlowType framingFlowType, boolean verbose) 
            throws AdeException {
        final ArrayList<IPeriod> periods = new ArrayList<>();
        periods.add(period);
        final IAdeIterator<IntervalImpl> res = 
                new IntervalByPeriodsAndFramingFlowTypeDbIterator(periods, framingFlowType, verbose);
        return new AdeIteratorAdaptor<IInterval>(res);
    }

    public IntervalByPeriodsAndFramingFlowTypeDbIterator getPeriodIntervals(Collection<IPeriod> periods
                                                                            , FramingFlowType framingFlowType) 
            throws AdeException {
        return getPeriodIntervals(periods, framingFlowType, true);
    }

    public IntervalByPeriodsAndFramingFlowTypeDbIterator getPeriodIntervals(Collection<IPeriod> periods
                                                                            , FramingFlowType framingFlowType
                                                                            , boolean verbose) throws AdeException {
        return new IntervalByPeriodsAndFramingFlowTypeDbIterator(periods, framingFlowType, verbose);
    }

    @Override
    public void exportPeriodToJsonFile(IPeriod period, String fileName)
            throws AdeException {
        throw new AdeInternalException("Not Implemented");
    }

    @Override
    public void importPeriodFromJsonFile(String fileName) throws AdeException {
        throw new AdeInternalException("not implemented");
    }

    @Override
    public Collection<String> getAllMessageIds() throws AdeException {
        return AdeInternal.getAdeImpl().getDictionaries().getMessageIdDictionary().getWords();
    }

    @Override
    public IAdeIterator<IConfigurationData> getPeriodConfigurations(Collection<PeriodImpl> periods) {
        return null;
    }
    
    private static class PeriodDeleter extends DmlPreparedStatementExecuter {
        private IPeriod m_period;

        PeriodDeleter(IPeriod period) {
            super("delete from " + SQL.PERIODS + " where source_internal_id=? and start_time=?");
            m_period = period;
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            int pos = 1;
            stmt.setInt(pos++, m_period.getSource().getSourceInternalId());
            TableGeneralUtils.setPreparedStatementTimestamp(stmt, pos++, m_period.getStartTime());
        }

    }

    private static class PeriodLoader extends QueryPreparedStatementExecuter {

        private ArrayList<PeriodImpl> m_result = new ArrayList<>();
        private ISource m_source;
        private Date m_end;
        private Date m_start;

        PeriodLoader(ISource source, Date start, Date end) {
            super("select period_internal_id,exclude_from_training,status,comment from " + SQL.PERIODS
                    + " where source_internal_id=? "
                    + " and start_time=?"
                    + " and end_time=?");
            
            m_source = source;
            setEnd(end);
            setStart(start);
        }

        private void setStart(Date start) { 
            m_start = new Date(start.getTime());
        }
        
        private void setEnd(Date end) {
            m_end = new Date(end.getTime());
        }
        
        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            int pos = 1;
            stmt.setInt(pos++, m_source.getSourceInternalId());
            TableGeneralUtils.setPreparedStatementTimestamp(stmt, pos++, m_start);
            TableGeneralUtils.setPreparedStatementTimestamp(stmt, pos++, m_end);

        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException, AdeException {
            int pos = 1;
            final int periodInternalId = rs.getInt(pos++);
            final int temp = rs.getInt(pos++);
            boolean excludeFromTraining = (temp > 0);
            final int status = rs.getInt(pos++);
            final String comment = rs.getString(pos++);
            
            if (rs.wasNull()) {
                excludeFromTraining = false;
            }
            
            m_result.add(new PeriodImpl(periodInternalId
                                        , m_source
                                        , excludeFromTraining
                                        , status
                                        , comment
                                        , m_start
                                        , m_end));
        }

    }

    private static class PeriodLister extends QueryPreparedStatementExecuter {

        private List<PeriodImpl> m_result = new ArrayList<>();
        private ISource m_source;
        private Date m_max;
        private Date m_min;

        PeriodLister(ISource source, Date min, Date max) {
                       
            super("select period_internal_id,exclude_from_training,status,comment,start_time,end_time from "
                    + SQL.PERIODS
                    + " where source_internal_id=? "
                    + ((min != null) ? " and start_time>=?" : "")
                    + ((max != null) ? " and end_time<=?" : ""));
            
            m_source = source;
            setMax(max);
            setMin(min);
        }

        private void setMin(Date min) {            
            if (min != null) {
                m_min = new Date(min.getTime());
            }
        }

        private void setMax(Date max) {
            if (max != null) {
                m_max = new Date(max.getTime());
            }
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            int pos = 1;
            stmt.setInt(pos++, m_source.getSourceInternalId());
            if (m_min != null) {
                TableGeneralUtils.setPreparedStatementTimestamp(stmt, pos++, m_min);
            }
            if (m_max != null) {
                TableGeneralUtils.setPreparedStatementTimestamp(stmt, pos++, m_max);
            }

        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            int pos = 1;
            final int periodInternalId = rs.getInt(pos++);
            final int temp = rs.getInt(pos++);
            boolean excludeFromTraining = (temp > 0);
            final int status = rs.getInt(pos++);
            final String comment = rs.getString(pos++);
            final Date startTime = PreparedStatementWrapper.getResultSetTimestamp(rs, pos++);
            final Date endTime = PreparedStatementWrapper.getResultSetTimestamp(rs, pos++);
            
            if (rs.wasNull()) {
                excludeFromTraining = false;
            }
            
            m_result.add(new PeriodImpl(periodInternalId
                                        , m_source
                                        , excludeFromTraining
                                        , status
                                        , comment
                                        , startTime
                                        , endTime));
        }

    }

    private static class PeriodUpdater extends DmlPreparedStatementExecuter {

        private PeriodImpl m_period;

        PeriodUpdater(PeriodImpl period) {
            super("update " 
                  + SQL.PERIODS 
                  + " set exclude_from_training=?,status=?,comment=? where period_internal_id=?");
            m_period = period;
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            int pos = 1;
            int paramValue = 0;
            
            if (m_period.getExcludeFromTraining()) {
                paramValue = 1;
            }
            
            stmt.setInt(pos++, paramValue);
            stmt.setInt(pos++, m_period.getStatus());
            stmt.setString(pos++, m_period.getComment());
            stmt.setInt(pos++, m_period.getInternalId());
        }

    }

}
