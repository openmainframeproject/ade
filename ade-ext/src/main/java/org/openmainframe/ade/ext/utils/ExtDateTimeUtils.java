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
package org.openmainframe.ade.ext.utils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;

/**
 *  Utilities for processing dates and timestamps.
 */

public final class ExtDateTimeUtils {

    /**
     * OutputTimeZone defined in setup.props
     */
    private static DateTimeZone outputTimeZone;

    public static final long MILLIS_IN_SECOND = 1000;
    public static final long MILLIS_IN_MINUTE = MILLIS_IN_SECOND * 60;
    public static final long MILLIS_IN_HOUR = MILLIS_IN_MINUTE * 60;
    public static final long MILLIS_IN_DAY = MILLIS_IN_HOUR * 24;
    
    public static final int LAST_HOUR_OF_DAY = 23;
    public static final int LAST_MINUTE = 59;
    public static final int LAST_SECOND = 59;
    public static final int LAST_MILLISECOND = 999;

    private static final ThreadLocal<DateFormat> m_dateAndTime = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("d MMM yyyy HH:mm:ss", new Locale("en", "US"));
        }
    };
    
    private static ThreadLocal<DateFormat> m_date = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("d MMM yyyy", new Locale("en", "US"));
        }
    };

    private ExtDateTimeUtils() {
        //private constructor
    }
    
    /**
     * Convert a timestamp to a humanly readable date and time.
     * 
     * @param timestamp - date and time in timestamp format
     * @return m_dateAndTime - a humanly readable format for the date and time input
     */
    public static String timestampToHumanDateAndTime(long timestamp) {
        return m_dateAndTime.get().format(new Date(timestamp));
    }

    /**
     * Convert a date to a humanly readable date and time.
     * 
     * @param date - date and time in date format
     * @return string - The date and time in string format
     */
    public static String dateToHumanDateAndTime(Date date) {
        if (date == null) {
            return "missing";
        }
        return timestampToHumanDateAndTime(date.getTime());
    }

    /**
     * Convert a date to a humanly readable date.
     * 
     * @param date - date in date format
     * @return string - The date in string format
     */
    public static String dateToHuman(Date date) {
        if (date == null) {
            return "missing";
        }
        return m_date.get().format(date);
    }

    /** 
     * Produce an instance of Timestamp which can produce a string 
     * representation understood by Derby, and which reflects the 
     * current Date and Time.
     * 
     * @return - instance of java.sql.Timestamp
     */
    public static Timestamp getCurrentTimeStamp() {

        final Calendar cal = new GregorianCalendar();
        final Date currDate = cal.getTime();
        return new Timestamp(currDate.getTime());

    } 

    /**
     * Method to return a Date instance which is exactly numDays
     * days before the point in time designated by the input Date.
     *  
     * @param dateInst - instance of Date
     * @param numDays - positive integer
     * @return - instance of Date as described
     */
    public static Date daysBefore(Date dateInst, int numDays) {

        if ((numDays <= 0) || (dateInst == null)) {
            throw new IllegalArgumentException();
        }
        final Calendar cal = new GregorianCalendar();
        cal.setTime(dateInst);
        cal.add(Calendar.DAY_OF_YEAR, -numDays);
        return cal.getTime();

    } 

    /**
     * Method to return a Date instance which is exactly numDays
     * days before the current point in time. 
     *  
     * @param numDays - positive integer representing the
     *                  number of days before "now" you want
     *                  a date for.
     * @return - instance of Date as described
     */
    public static Date daysBeforeNow(int numDays) {

        if (numDays <= 0) {
            throw new IllegalArgumentException();
        }
        final Calendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, -numDays);
        return cal.getTime();

    } 

    /**
    * Method to return a Date instance which is exactly numDays
    * days after the point in time designated by the input Date
    *  
    * @param dateInst - instance of Date
    * @param numDays  - positive integer
    * @return - instance of Date as described
    */
    public static Date daysAfter(Date dateInst, int numDays) {

        if ((numDays <= 0) || (dateInst == null)) {
            throw new IllegalArgumentException();
        }
        final Calendar cal = new GregorianCalendar();
        cal.setTime(dateInst);
        cal.add(Calendar.DAY_OF_YEAR, numDays);
        return cal.getTime();

    } 

    /**
     * Return the days After without impact of timezone.
     *  
     * @param dateInst - an instance of Date
     * @param numDays  - positive integer representing the nunmber of days
     *                   after the input date
     * @return a Date object representing the number of days after the 
     *                the input date, without the timezone.
     */
    public static Date daysAfterWithoutTimeZone(Date dateInst, int numDays) {

        if ((numDays <= 0) || (dateInst == null)) {
            throw new IllegalArgumentException();
        }
        final long daysInMillis = numDays * MILLIS_IN_DAY;
        final Date ret = new Date(dateInst.getTime() + daysInMillis);
        return ret;

    } 

    /**
    * Method to return a Date instance which is exactly numDays
    * days after the current point in time.
    *  
    * @param numDays - positive integer
    * @return - instance of Date as described
    */
    public static Date daysAfterNow(int numDays) {

        if (numDays <= 0) {
            throw new IllegalArgumentException();
        }
        final Calendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, numDays);
        return cal.getTime();

    } 

    /**
     * Method to return a "normalized" version of the input Date
     * whose time is reset to the absolute start of that same day
     * (first millisecond of first second of first minute of first hour).
     *    
     * @param dateInst - instance of Date
     * @return - instance of Date as described
     */
    public static Date startOfDay(Date dateInst) {

        if (dateInst == null) {
            throw new IllegalArgumentException();
        }
        final Calendar cal = new GregorianCalendar();
        cal.setTime(dateInst);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();

    } 

    /**
     * Method to return an "anti-normalized" version of the input Date
     * whose time is set to the absolute end of that same day
     * (last millisecond of last second of last minute of last hour).
     *    
     * @param dateInst - instance of Date
     * @return - instance of Date as described
     */
    public static Date endOfDay(Date dateInst) {

        if (dateInst == null) {
            throw new IllegalArgumentException();
        }
        final Calendar cal = new GregorianCalendar();
        cal.setTime(dateInst);
        cal.set(Calendar.HOUR_OF_DAY, LAST_HOUR_OF_DAY);
        cal.set(Calendar.MINUTE, LAST_MINUTE);
        cal.set(Calendar.SECOND, LAST_SECOND);
        cal.set(Calendar.MILLISECOND, LAST_MILLISECOND);
        return cal.getTime();

    } 

    /**
     * Method to return a "normalized" version of the input Date
     * whose time is reset to the absolute start of that same day
     * (first millisecond of first second of first minute of first hour).
     *    
     * @param dateInst - instance of Date
     * @return - instance of Date as described
     * @throws AdeException 
     */
    public static Date startOfDayUsingOutputTimeZone(Date dateInst) throws AdeException {

        if (outputTimeZone == null) {
            final TimeZone outputTimezone = Ade.getAde().getConfigProperties().getOutputTimeZone();
            outputTimeZone = DateTimeZone.forOffsetMillis(outputTimezone.getRawOffset());
        }

        if (dateInst == null) {
            throw new IllegalArgumentException();
        }

        /* Set start of today */
        DateTime startOFDay = new DateTime(dateInst);
        startOFDay = startOFDay.withZone(outputTimeZone);
        startOFDay = startOFDay.withTimeAtStartOfDay();

        return startOFDay.toDate();

    } 

    /**
     * Method to return a "normalized" version of the input Date
     * whose time is reset to the absolute start of that same day
     * (first millisecond of first second of first minute of first hour).
     *    
     * @param dateInst - instance of Date
     * @return - instance of Date as described
     * @throws AdeException 
     */
    public static Date endOfDayUsingOutputTimeZone(Date dateInst) throws AdeException {

        /* 
         * Note: This method generates a different end of day compare to endOfDay().
         * endOfDay() would generate timestamp: 10/10/2013 23:59:59
         * endOfDayUsingOutputTimeZone() would generate timestampe: 10/11/2013 00:00:00
         */

        if (outputTimeZone == null) {
            final TimeZone outputTimezone = Ade.getAde().getConfigProperties().getOutputTimeZone();
            outputTimeZone = DateTimeZone.forOffsetMillis(outputTimezone.getRawOffset());
        }

        if (dateInst == null) {
            throw new IllegalArgumentException();
        }

        /* Set end of today */
        DateTime startOFDay = new DateTime(dateInst);
        startOFDay = startOFDay.withZone(outputTimeZone);
        startOFDay = startOFDay.plusDays(1);
        startOFDay = startOFDay.withTimeAtStartOfDay();

        return startOFDay.toDate();

    } 

} 
