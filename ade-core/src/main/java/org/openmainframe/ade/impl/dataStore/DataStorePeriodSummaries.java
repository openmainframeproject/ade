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
import java.util.ArrayList;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.PeriodImpl;
import org.openmainframe.ade.impl.data.PeriodSummary;
import org.openmainframe.ade.impl.dbUtils.DmlPreparedStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.QueryPreparedStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.SpecialSqlQueries;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;

/**
 * Provides methods to read/update PeriodSummaries from the data-store.
 */
public class DataStorePeriodSummaries {


    private DataStorePeriodsImpl m_dsPeriodsImpl;

    /**
     * Constructor to create a DataStorePeriodSummaries object that accesses
     * periods from the given DataStorePeriodsImpl.
     *
     * @param dsPeriodsImpl the DataStorePeriodsImpl that periods should be accessed from
     */
    DataStorePeriodSummaries(DataStorePeriodsImpl dsPeriodsImpl) {
        m_dsPeriodsImpl = dsPeriodsImpl;
    }

    /**
     * Retrieves a PeriodSummary from the data-store for the given Period and
     * framing flow type.
     *
     * @param period the Period to retrieve the PeriodSummary for
     * @param framingFlowType the FramingFlowType that describes the type of
     *                          PeriodSummary that should be retrieved
     * @return the requested PeriodSummary if one exists, null otherwise
     * @throws AdeException when an error occurs accessing the data store
     */
    public final PeriodSummary getPeriodSummary(IPeriod period, FramingFlowType framingFlowType) throws AdeException {
        final PeriodImpl periodImpl = m_dsPeriodsImpl.getPeriodImpl(period);
        return getPeriodSummary(periodImpl, framingFlowType);
    }

    /**
     * Retrieves a PeriodSummary from the data-store for the given PeriodImpl and
     * framing flow type.
     *
     * @param period the PeriodImpl to retrieve the PeriodSummary for
     * @param framingFlowType the FramingFlowType that describes the type of
     *                          PeriodSummary that should be retrieved
     * @return the requested PeriodSummary if one exists, null otherwise
     * @throws AdeException when an error occurs accessing the data store
     */
    public final PeriodSummary getPeriodSummary(PeriodImpl period,
            FramingFlowType framingFlowType) throws AdeException {
        final PeriodSummaryFinder pf = new PeriodSummaryFinder(period, framingFlowType);
        pf.executeQuery();
        if (pf.m_result.isEmpty()) {
            return null;
        }
        if (pf.m_result.size() > 1) {
            throw new AdeInternalException(
                    "Multiple period summaries for period " + period + " and summary type " + framingFlowType);
        }
        return pf.m_result.get(0);
    }

    /**
     * Retrieves a PeriodSummary from the data-store for the given Period and
     * framing flow type if one exists. If an associated PeriodSummary
     * does not exist then one will be created in the data-store and this
     * new item will be returned.
     *
     * @param period the Period to retrieve the PeriodSummary for
     * @param framingFlowType the FramingFlowType that describes the type of
     *                          PeriodSummary that should be retrieved
     * @return the requested PeriodSummary if one exists, or a newly created one otherwise
     * @throws AdeException when an error occurs accessing the data store
     */
    public final PeriodSummary getOrAddPeriodSummary(PeriodImpl period,
            FramingFlowType framingFlowType) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        PeriodSummary result = null;
        try {
            /* Indicate that the connection should have auto commit off in case there is an error */
            cw.startTransaction();

            /* Lock the period tables so that it cannot be modified during this transaction */
            cw.lockTableShare(SQL.PERIODS);
            /* Lock the period summaries tables so that it cannot be modified during this transaction */
            cw.lockTableExclusive(SQL.PERIOD_SUMMARIES);
            result = getPeriodSummary(period, framingFlowType);
            /*
             * Check to see if a period summary was found, if not then we need to add one
             * to the data-store
             */
            if (result == null) {
                /* Create a PeriodSummaryAdder so that we can add a new PeriodSummary */
                final PeriodSummaryAdder psa = new PeriodSummaryAdder(period, framingFlowType);
                psa.execute();
                /* Get the internal id of the newly created PeriodSummary */
                final int iid = SpecialSqlQueries.getLastKey();
                result = new PeriodSummary(iid, period, framingFlowType);
            }

            /* Clean up the connection wrapper */
            cw.endTransaction();
            cw.close();

        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        return result;

    }

    /**
     * Internal class to read PeriodSummary's from the data-store.
     */
    private class PeriodSummaryFinder extends QueryPreparedStatementExecuter {

        private ArrayList<PeriodSummary> m_result = new ArrayList<PeriodSummary>();
        private PeriodImpl m_period;
        private FramingFlowType m_framingFlowType;

        PeriodSummaryFinder(PeriodImpl period, FramingFlowType framingFlowType) {
            super("select period_summary_internal_id from " + SQL.PERIOD_SUMMARIES
                    + " where period_internal_id=? and summary_type_internal_id=?");
            m_period = period;
            m_framingFlowType = framingFlowType;
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            int pos = 1;
            stmt.setInt(pos++, m_period.getInternalId());
            stmt.setInt(pos++, m_framingFlowType.getDatabaseId());
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            final int periodSummaryInternalId = rs.getInt(1);
            m_result.add(new PeriodSummary(periodSummaryInternalId, m_period, m_framingFlowType));
        }

    }

    /**
     * Internal class to add PeriodSummary's to the data-store.
     */
    private class PeriodSummaryAdder extends DmlPreparedStatementExecuter {

        private PeriodImpl m_period;
        private FramingFlowType m_framingFlowType;

        PeriodSummaryAdder(PeriodImpl period, FramingFlowType framingFlowType) {
            super("insert into " + SQL.PERIOD_SUMMARIES + "(period_internal_id,summary_type_internal_id) values (?,?)");
            m_period = period;
            m_framingFlowType = framingFlowType;
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            int pos = 1;
            stmt.setInt(pos++, m_period.getInternalId());
            stmt.setInt(pos++, m_framingFlowType.getDatabaseId());
        }

    }

}
