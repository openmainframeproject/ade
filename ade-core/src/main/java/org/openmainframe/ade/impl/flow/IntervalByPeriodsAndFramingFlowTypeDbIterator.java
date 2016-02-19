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

import java.util.ArrayList;
import java.util.Collection;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.flow.IAdeIterator;
import org.openmainframe.ade.impl.data.IntervalImpl;
import org.openmainframe.ade.impl.data.PeriodSummary;
import org.openmainframe.ade.impl.dataStore.DataStorePeriodSummaries;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntervalByPeriodsAndFramingFlowTypeDbIterator implements IAdeIterator<IntervalImpl> {

    private FramingFlowType m_framingFlowType;
    private IntervalByPeriodSummaryDbIterator m_byPeriodSummaryIterator;
    private boolean m_verbose;
    private ArrayList<IPeriod> m_periods;
    private int m_currentPeriod;
    private DataStorePeriodSummaries m_dsPeriodSummaries;
    private static final Logger logger = LoggerFactory.getLogger(IntervalByPeriodsAndFramingFlowTypeDbIterator.class);

    public IntervalByPeriodsAndFramingFlowTypeDbIterator(Collection<IPeriod> periods, FramingFlowType framingFlowType, 
            boolean verbose) throws AdeException {
        m_periods = new ArrayList<IPeriod>();
        m_periods.addAll(periods);
        m_framingFlowType = framingFlowType;
        m_verbose = verbose;
        m_currentPeriod = 0;
        m_dsPeriodSummaries = AdeInternal.getAdeImpl().getDataStore().periodSummaries();
    }

    @Override
    public final void open() throws AdeException {
        m_currentPeriod = 0;
        if (m_verbose) {
            logger.info("Start iterating over intervals of " + m_periods.size() + " periods summaries");
        }
        openCurrentBySummaryIterator();
    }

    @Override
    public final IntervalImpl getNext() throws AdeException {
        while (m_currentPeriod < m_periods.size()) {

            if (m_byPeriodSummaryIterator == null) {
                ++m_currentPeriod;
                openCurrentBySummaryIterator();
                continue;
            }

            final IntervalImpl res = m_byPeriodSummaryIterator.getNext();
            if (res != null) {
                return res;
            }
            m_byPeriodSummaryIterator.close();
            m_byPeriodSummaryIterator = null;
            ++m_currentPeriod;
            openCurrentBySummaryIterator();
        }
        return null;
    }

    @Override
    public final void close() throws AdeException {
        if (m_byPeriodSummaryIterator != null) {
            m_byPeriodSummaryIterator.close();
        }
    }

    @Override
    public final void quietCleanup() {
        if (m_byPeriodSummaryIterator != null) {
            m_byPeriodSummaryIterator.quietCleanup();
        }
    }

    private void openCurrentBySummaryIterator() throws AdeException {
        m_byPeriodSummaryIterator = null;
        if (m_currentPeriod >= m_periods.size()) {
            return;
        }
        final IPeriod period = m_periods.get(m_currentPeriod);
        final PeriodSummary periodSummary = m_dsPeriodSummaries.getPeriodSummary(period, m_framingFlowType);
        if (periodSummary == null) {
            return;
        }
        if (m_verbose) {
            logger.info("Scanning intervals of " + periodSummary);
        }
        m_byPeriodSummaryIterator = new IntervalByPeriodSummaryDbIterator(periodSummary);
        m_byPeriodSummaryIterator.open();
    }

}
