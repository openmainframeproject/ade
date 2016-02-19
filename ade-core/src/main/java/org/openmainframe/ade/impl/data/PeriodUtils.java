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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.data.IPeriod.PeriodMode;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.modules.ConsecutiveTimeFramer;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.openmainframe.ade.utils.LazyObj;

/**
 * A utility class for handling {@link IPeriod} object.
 */
public final class PeriodUtils {
    /**
     * A lazy instance of the {@link PeriodMode} taken from the setup properties.
     */
    private static LazyObj<PeriodMode> staticPeriodMode = new LazyObj<PeriodMode>() {
        @Override
        protected PeriodMode create() {
            try {
                return Ade.getAde().getConfigProperties().getPeriodMode();
            } catch (AdeException e) {
                throw new ObjectCreationException(e);
            }
        }
    };

    private PeriodUtils() {
        //Don't allow default construction
    }
 
    /**
     * Check to see if a date is within a given period.
     * @param date the date to check
     * @param periodStart the start of a period
     * @return true if the input date in inside the period starting at periodStart and spans as indicated 
     *     by staticPeriodMode
     * @throws AdeException
     */
    public static boolean isInPeriod(Date date, Date periodStart) throws AdeException {
        return getContainingPeriodStart(date).equals(periodStart);
    }

    /**
     * Get the containing period start date.
     * @param date the date for which to look up the containing period
     * @return the begin date of the period (as indicated by s_period mode) containing the input date
     * @throws AdeException if the period mode contains an unexpected value
     */
    public static Date getContainingPeriodStart(Date date) throws AdeException {
        final Calendar calendar = DateTimeUtils.getGmtGregorianCalendar();

        calendar.setTime(date);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        final PeriodMode periodMode = staticPeriodMode.get();
        // set day
        switch (periodMode) {
            case YEARLY:
                calendar.set(Calendar.MONTH, Calendar.JANUARY);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case MONTHLY:
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case WEEKLY:
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                break;
            case DAILY:
            case HOURLY:
                break;
            default:
                throw new AdeInternalException("Unknown period mode: " + periodMode);
        }

        // set hours and minutes
        switch (periodMode) {
            case YEARLY:
                // continue to next block
            case MONTHLY:
                // continue to next block
            case WEEKLY:
                // continue to next block
            case DAILY:
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                break;
            case HOURLY:
                calendar.set(Calendar.MINUTE, 0);
                break;
            default:
                throw new AdeInternalException("Unknown period mode: " + periodMode);
        }

        return calendar.getTime();
    }

    /**
     * Get the starting date of a period shifted by the input value. 
     * @param curPeriodStart the current period start date
     * @param periodsToShift the number of periods to shift. Negative value will shift the period backwards
     * @return the shifted period start date
     * @throws AdeException
     */
    public static Date getShiftedPeriod(Date curPeriodStart, int periodsToShift) throws AdeException {
        Date res = new Date(curPeriodStart.getTime());
        if (periodsToShift > 0) {
            for (int i = 0; i < periodsToShift; i++) {
                res = getNextPeriodStart(res);
            }
        } else if (periodsToShift < 0) {
            for (int i = 0; i < -1 * periodsToShift; i++) {
                res = getPrevPeriodStart(res);
            }
        }
        return res;
    }

    /**
     * Get the beginning of the next period.
     * @param curPeriodStart the beginning of the current period
     * @return the begin date of the next period (as indicated by period mode)
     * @throws AdeException if the period mode contains an unexpected value
     */
    public static Date getNextPeriodStart(Date curPeriodStart) throws AdeException {
        final PeriodMode periodMode = staticPeriodMode.get();

        final Calendar calendar = new GregorianCalendar(DateTimeUtils.GMT_TIMEZONE);

        calendar.setTime(curPeriodStart);

        if (periodMode == PeriodMode.HOURLY) {
            calendar.add(Calendar.HOUR, 1);
        } else if (periodMode == PeriodMode.DAILY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        } else if (periodMode == PeriodMode.WEEKLY) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        } else if (periodMode == PeriodMode.MONTHLY) {
            calendar.add(Calendar.MONTH, 1);
        } else if (periodMode == PeriodMode.YEARLY) {
            calendar.add(Calendar.YEAR, 1);
        } else {
            throw new AdeInternalException("Invalid period mode " + periodMode);
        }

        return calendar.getTime();
    }

    /**
     * Get the beginning date of the previous period. 
     * @param curPeriodStart the current period start date
     * @return the begin date of the previous period (as indicated by staticPeriodMode)
     * @throws AdeException if the period mode contains an unexpected value
     */
    public static Date getPrevPeriodStart(Date curPeriodStart) throws AdeException {
        final PeriodMode periodMode = staticPeriodMode.get();

        final Calendar calendar = new GregorianCalendar(DateTimeUtils.GMT_TIMEZONE);

        calendar.setTime(curPeriodStart);

        if (periodMode == PeriodMode.HOURLY) {
            calendar.add(Calendar.HOUR, -1);
        } else if (periodMode == PeriodMode.DAILY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        } else if (periodMode == PeriodMode.WEEKLY) {
            calendar.add(Calendar.WEEK_OF_YEAR, -1);
        } else if (periodMode == PeriodMode.MONTHLY) {
            calendar.add(Calendar.MONTH, -1);
        } else if (periodMode == PeriodMode.YEARLY) {
            calendar.add(Calendar.YEAR, -1);
        } else {
            throw new AdeInternalException("Invalid period mode " + periodMode);
        }

        return calendar.getTime();
    }

    /**
     * Get the start of the closest aligned interval.
     * @param framingFlowType the framing flow type
     * @param periodStart the first day of the period
     * @param someIntervalStart the start date of some arbitrary interval. If null
     *     then the framingFlowType will be queried for this interval start.
     * @return the start date of the first interval beginning in the input period time and type.
     *     The calculation is based on some arbitrary input interval start (not necasarilly in the 
     *     period).
     * @throws AdeException
     */
    public static Date getFirstConsecutiveIntervalStartInPeriod(FramingFlowType framingFlowType, Date periodStart, 
            Long someIntervalStart) throws AdeException {
        Long intervalStart = someIntervalStart;
        if (intervalStart == null) {
            assertIsConsecutiveTimeFramer(framingFlowType);
            final ConsecutiveTimeFramer ctf = new ConsecutiveTimeFramer(framingFlowType);
            intervalStart = ctf.getAlignmentOffset();
        }
        final long numOfIntervalsTojump = (intervalStart - periodStart.getTime()) / framingFlowType.getDuration();
        // in the case we got an interval happening before the period, we need to add one interval to 
        // cross over to the correct period
        long closestAlignedIntervalStart = intervalStart - numOfIntervalsTojump * framingFlowType.getDuration();
        if (closestAlignedIntervalStart < periodStart.getTime()) {
            closestAlignedIntervalStart += framingFlowType.getDuration();
        }
        return new Date(closestAlignedIntervalStart);
    }

    /**
     * Get the serial number of an interval within the period.
     * @param framingFlowType the framing flow type
     * @param someIntervalStart the start of an interval
     * @return the serial number of the given interval in its period
     * @throws AdeException
     */
    public static int getIntervalSerialNumInPeriod(FramingFlowType framingFlowType, Date someIntervalStart) 
            throws AdeException {
        final Date containigPeriodStartDate = getContainingPeriodStart(someIntervalStart);
        final Date firstIntervalStartInPeriod = getFirstConsecutiveIntervalStartInPeriod(framingFlowType, 
                containigPeriodStartDate, someIntervalStart.getTime());
        final long diff = someIntervalStart.getTime() - firstIntervalStartInPeriod.getTime();
        final long intervalDuration = framingFlowType.getDuration();
        return (int) (diff / intervalDuration);
    }

    /**
     * Get the number of intervals per period.
     * @param framingFlowType the framing flow type
     * @param periodStart the beginning of the period
     * @return the number of intervals per period
     * @throws AdeException
     */
    public static int getIntervalsPerPeriod(FramingFlowType framingFlowType, Date periodStart) throws AdeException {
        return getIntervalsPerPeriod(framingFlowType, periodStart, null);
    }

    /**
     * Get the number of intervals per period.
     * @param framingFlowType the framing flow type
     * @param periodStart the beginning of the period
     * @param someIntervalStart the start of an interval 
     * @return the number of intervals per period
     * @throws AdeException if the difference between periods is not evenly divisible by the interval duration 
     *     defined in the framing flow type
     */
    public static int getIntervalsPerPeriod(FramingFlowType framingFlowType, Date periodStart, Long someIntervalStart) 
            throws AdeException {
        final Date firstIntervalStartInPeriod = getFirstConsecutiveIntervalStartInPeriod(framingFlowType, periodStart, 
                someIntervalStart);
        final Date firstIntervalStartInNextPeriod = getFirstConsecutiveIntervalStartInPeriod(framingFlowType, 
                PeriodUtils.getNextPeriodStart(periodStart), someIntervalStart);
        final long diff = firstIntervalStartInNextPeriod.getTime() - firstIntervalStartInPeriod.getTime();
        final long intervalDuration = framingFlowType.getDuration();
        if (diff % intervalDuration != 0 || diff < intervalDuration) {
            throw new AdeInternalException(String.format(
                    "The difference between: intervals %s must be an integral multiplication of the duration: %s", 
                    diff, intervalDuration));
        }
        return (int) (diff / intervalDuration);
    }

    private static void assertIsConsecutiveTimeFramer(FramingFlowType framingFlowType) throws AdeException {
        if (!framingFlowType.isConsecutive()) {
            throw new AdeInternalException("Currently Ade only supports consecutive time framers");
        }
    }

    /**
     * Class to document a period range. Contains a first and a last period. They may be the same.
     */
    public static final class PeriodRange implements Iterable<Date> {
        private Date m_firstPeriodStart;
        private Date m_lastPeriodStart;

        private PeriodRange(Date firstPeriodStart, Date lastPeriodStart) {
            m_firstPeriodStart = new Date(firstPeriodStart.getTime());
            m_lastPeriodStart = new Date(lastPeriodStart.getTime());
        }

        /**
         * Get the period range for a given set of dates.
         * @param dateInFirstPeriod a date in the first period
         * @param dateInLastPeriod a date in the last period
         * @return the period range containing the dates
         * @throws AdeException
         */
        public static PeriodRange create(Date dateInFirstPeriod, Date dateInLastPeriod) throws AdeException {
            final Date firstPeriodStart = PeriodUtils.getContainingPeriodStart(dateInFirstPeriod);
            final Date lastPeriodStart = PeriodUtils.getContainingPeriodStart(dateInLastPeriod);
            return new PeriodRange(firstPeriodStart, lastPeriodStart);
        }

        /**
         * Get the period range for a given date.
         * @param dateInSinglePeriod the date within a period
         * @return the period range containing the date
         * @throws AdeException
         */
        public static PeriodRange create(Date dateInSinglePeriod) throws AdeException {
            final Date singlePeriodStart = PeriodUtils.getContainingPeriodStart(dateInSinglePeriod);
            return new PeriodRange(singlePeriodStart, singlePeriodStart);
        }

        /**
         * Get the period range for a date and number of periods.
         * @param dateInSomePeriod the date
         * @param numPeriods the number of periods
         * @return the period range containing the date and given number of periods
         * @throws AdeException
         * @throws {@link IllegalArgumentException} if numPeriods is 0
         */
        public static PeriodRange create(Date dateInSomePeriod, int numPeriods) throws AdeException {
            final Date somePeriodStart = PeriodUtils.getContainingPeriodStart(dateInSomePeriod);
            if (numPeriods > 0) {
                //Get the last period date by looking forward "numPeriods" periods
                final Date lastPeriodStart = PeriodUtils.getShiftedPeriod(somePeriodStart, numPeriods - 1);
                return new PeriodRange(somePeriodStart, lastPeriodStart);
            } 
            if (numPeriods < 0) {
                //Get the first period date by looking backward "numPeriods" periods
                final Date firstPeriodStart = PeriodUtils.getShiftedPeriod(somePeriodStart, numPeriods + 1);
                return new PeriodRange(firstPeriodStart, somePeriodStart);
            }
            //numPeriods must be zero to get here as either a positive or negative value would have returned above
            throw new IllegalArgumentException("The input number of periods must differ from zero!");
            
        }

        public int getPeriodsDifference() throws AdeException {
            int countDiff = 0;
            Date curPeriodStart = m_firstPeriodStart;
            while (curPeriodStart.before(m_lastPeriodStart)) {
                countDiff++;
                curPeriodStart = PeriodUtils.getNextPeriodStart(curPeriodStart);
            }
            return countDiff;
        }

        public int getNumOfPeriods() throws AdeException {
            return getPeriodsDifference() + 1;
        }

        @Override
        public String toString() {
            final Calendar beginCal = Calendar.getInstance();
            beginCal.setTime(m_firstPeriodStart);
            final Calendar endCal = Calendar.getInstance();
            endCal.setTime(m_lastPeriodStart);

            if (beginCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR)) {
                return DateTimeUtils.timestampToHumanDateAndTimeAde(m_firstPeriodStart.getTime()) 
                        + " - " + DateTimeUtils.timestampToHumanDateAndTimeAde(m_lastPeriodStart.getTime());
            }
            if (beginCal.get(Calendar.MONTH) != endCal.get(Calendar.MONTH)) {
                final SimpleDateFormat beginSdf = new SimpleDateFormat("dd MMM");
                return beginSdf.format(m_firstPeriodStart) + " - " 
                        + DateTimeUtils.timestampToHumanDateAndTimeAde(m_lastPeriodStart.getTime());
            }
            if (beginCal.get(Calendar.DAY_OF_MONTH) != endCal.get(Calendar.DAY_OF_MONTH)) {
                final SimpleDateFormat beginSdf = new SimpleDateFormat("dd");
                return beginSdf.format(m_firstPeriodStart) + " - " 
                        + DateTimeUtils.timestampToHumanDateAndTimeAde(m_lastPeriodStart.getTime());
            }
            return DateTimeUtils.timestampToHumanDateAndTimeAde(m_firstPeriodStart.getTime());
        }

        @Override
        public Iterator<Date> iterator() {
            try {
                return new DateIterator();
            } catch (AdeException e) {
                throw new RuntimeException("Ade Exception occured: ", e);
            }
        }

        /**
         * Private class to iterate through period dates.
         */
        private class DateIterator implements Iterator<Date> {
            private Date m_curPeriodStart;

            /**
             * Create a new date iterator starting with the beginning of the first period.
             * @throws AdeException
             */
            public DateIterator() throws AdeException {
                m_curPeriodStart = new Date(m_firstPeriodStart.getTime());
            }

            @Override
            public boolean hasNext() {
                return !m_curPeriodStart.after(m_lastPeriodStart);
            }

            @Override
            public Date next() {
                if (!hasNext()) {
                    return null;
                }
                final Date curPeriodStart = m_curPeriodStart;
                try {
                    m_curPeriodStart = PeriodUtils.getNextPeriodStart(m_curPeriodStart);
                } catch (AdeException e) {
                    throw new RuntimeException("AdeException occured: ", e);
                }
                return curPeriodStart;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("operation not supported");
            }
        }
    }
}
