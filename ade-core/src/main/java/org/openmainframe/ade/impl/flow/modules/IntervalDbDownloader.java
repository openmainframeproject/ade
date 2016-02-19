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
package org.openmainframe.ade.impl.flow.modules;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.core.statistics.TimingStatistics;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.data.IntervalImpl;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.IntervalByPeriodsAndFramingFlowTypeDbIterator;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.hub.HubFramingSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads intervals from the database and sends them to the specified target(s).
 */
public class IntervalDbDownloader extends HubFramingSource<IInterval, TimeSeparator> {


    /* Logger */
    private static final Logger logger = LoggerFactory.getLogger(IntervalDbDownloader.class.getName());

    /* An iterator over the PeriodIntervals in the database that satisfy the specified criteria */
    protected IntervalByPeriodsAndFramingFlowTypeDbIterator m_iterator;

    /**
     * Creates a IntervalDbDownloader that is configured to download the specified Periods of the given FrameFlowType.
     *
     * @param periods a list of Periods retrieve the Intervals to download
     * @param framingFlowType the FramingFlowType of the Intervals to be downloaded
     * @throws AdeException when there is an error accessing the database
     */
    public IntervalDbDownloader(Collection<IPeriod> periods, FramingFlowType framingFlowType) throws AdeException {
        m_iterator = AdeInternal.getAdeImpl().getDataStore().periods().getPeriodIntervals(
                periods, framingFlowType, false);
    }

    /**
     * Downloads the Intervals as specified by the constructor to the specified targets.
     *
     * @throws AdeException when there is an error accessing the database
     */
    public final void run() throws AdeException {
        try {
            sendBeginOfStream();
            m_iterator.open();
            IntervalImpl interval;
            Long lastIntervalEndTime = null;
            final TimeSeparator sep =
                    AdeInternal.getAdeImpl().getDataFactory().newTimeSeparator("Non Continuous intervals");
            /*
             * Initialize last source id to a value that will
             * automatically fail the first statement below
             */
            int lastSourceId = -1;
            /*
             * Iterate through the intervals, sending each one in turn and logging
             * when non-consecutive intervals are encountered
             */
            while ((interval = m_iterator.getNext()) != null) {
                /*
                 * Order here is critical - the method isConsecutiveIntervals will add a warning to the log if run.
                 */
                if (lastIntervalEndTime == null
                        || interval.getSource().getSourceInternalId() != lastSourceId
                        || !isConsecutiveIntevals(lastIntervalEndTime, interval)) {
                    sendSeparator(sep);
                    lastSourceId = interval.getSource().getSourceInternalId();
                }
                TimingStatistics.start("Interval processing");
                /* Send the actual interval object */
                sendObject(interval);
                TimingStatistics.end("Interval processing");
                lastIntervalEndTime = interval.getIntervalEndTime();
            }

            m_iterator.close();
        } finally {
            m_iterator.quietCleanup();
        }
        sendEndOfStream();
    }

    private boolean isConsecutiveIntevals(Long lastIntervalEndTime,
            IntervalImpl interval) {
        if (lastIntervalEndTime != null
                && lastIntervalEndTime.equals(interval.getIntervalStartTime())) {
            return true;
        } else {
            final SimpleDateFormat sdf = new SimpleDateFormat();
            logger.warn("Gap in log coverage: skipped from  "
            + sdf.format(new Date(lastIntervalEndTime != null ? lastIntervalEndTime : 0L)) + " to "
            + sdf.format(new Date(interval.getIntervalStartTime())));
            return false;
        }
    }
}