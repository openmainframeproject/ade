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
package org.openmainframe.ade.impl.utils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.utils.LazyObj;

public final class DateTimeUtils {


    public static final long SECONDS_IN_MINUTE = 60L;
    public static final long MINUTES_IN_HOUR   = 60L;
    public static final long HOURS_IN_DAY = 24L;
    public static final long DAYS_IN_WEEK = 7L;
    public static final long MILLIS_IN_SECOND = 1000L;
    public static final long HALF_MILLIS_IN_SECOND = 500L;
    public static final long MILLIS_IN_MINUTE = MILLIS_IN_SECOND * SECONDS_IN_MINUTE;
    public static final long MILLIS_IN_HOUR = MILLIS_IN_MINUTE * MINUTES_IN_HOUR;
    public static final long MILLIS_IN_DAY = MILLIS_IN_HOUR * HOURS_IN_DAY;
    public static final long MILLIS_IN_WEEK = MILLIS_IN_DAY * DAYS_IN_WEEK;
    
    private static final long REGRESSION_MODE_FAKE_CURRENT_DATE = 1300096126000L;
    private static final int  MONDAY = 2;
    

    static public final TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");
    static public final LazyObj<TimeZone> ADE_TIMEZONE = new LazyObj<TimeZone>() {
        @Override
        protected TimeZone create() throws ObjectCreationException {
            try {
                return Ade.getAde().getConfigProperties().getOutputTimeZone();
            } catch (AdeException e) {
                throw new ObjectCreationException(e);
            }
        }
    };

    private static final String s_dateAndTimeStr = "d MMM yyyy HH:mm:ss";
    private static final DateFormat s_dateAndTimeLocal = new SimpleDateFormat(s_dateAndTimeStr, new Locale("en", "US"));
    private static final DateFormat s_dateAndTimeUTC = new SimpleDateFormat(s_dateAndTimeStr, new Locale("en", "US"));
    private static final LazyObj<DateFormat> s_dateAndTimeAde = new LazyObj<DateFormat>() {
        @Override
        protected DateFormat create() throws ObjectCreationException {
            DateFormat res = new SimpleDateFormat(s_dateAndTimeStr, new Locale("en", "US"));
            res.setTimeZone(ADE_TIMEZONE.get());
            return res;
        }
    };

    static {
        s_dateAndTimeUTC.setTimeZone(GMT_TIMEZONE);
    }

    private DateTimeUtils() {
        //private constructor
    }
    
    public static String timestampToHumanDateAndTimeLocal(long timestamp) {
        synchronized (s_dateAndTimeLocal) {
            return s_dateAndTimeLocal.format(new Date(timestamp));
        }
    }

    public static String timestampToHumanDateAndTimeAde(long timestamp) {
        synchronized (s_dateAndTimeAde) {
            return s_dateAndTimeAde.get().format(new Date(timestamp));
        }
    }

    public static String timestampToHumanDateAndTimeUTC(long timestamp) {
        synchronized (s_dateAndTimeUTC) {
            return s_dateAndTimeUTC.format(new Date(timestamp));
        }
    }

    public static String millisecondsToHumanTime(long period) {
        if (period % MILLIS_IN_SECOND >= HALF_MILLIS_IN_SECOND) {
            period += MILLIS_IN_SECOND;
        }
        period /= MILLIS_IN_SECOND;
        long secs = period % SECONDS_IN_MINUTE;
        period /= SECONDS_IN_MINUTE;
        long mins = period % MINUTES_IN_HOUR;
        period /= MINUTES_IN_HOUR;
        long hours = period;
        return String.format("%02d:%02d:%02d (hh:mm:ss)", hours, mins, secs);
    }

    public static String millisecondsToDecimalHumanTime(long period) {
        double periodD = period;
        NumberFormat numFormat = new DecimalFormat("#.##");

        if (period < MILLIS_IN_SECOND) {
            return String.format("%s milliseconds", numFormat.format(periodD));
        } else if (period < MILLIS_IN_MINUTE) {
            return String.format("%s seconds", numFormat.format(periodD / MILLIS_IN_SECOND));
        } else if (period < MILLIS_IN_HOUR) {
            return String.format("%s minutes", numFormat.format(periodD / MILLIS_IN_MINUTE));
        } else if (period < MILLIS_IN_DAY) {
            return String.format("%s hours", numFormat.format(periodD / MILLIS_IN_HOUR));
        } else {
            return String.format("%s days", numFormat.format(periodD / MILLIS_IN_DAY));
        }
    }

    public static long getDayStartTimeLocal(long startTime) {
        Calendar gc = new GregorianCalendar();
        gc.setTimeInMillis(startTime);
        gc.set(Calendar.HOUR_OF_DAY, 0);
        gc.set(Calendar.MINUTE, 0);
        gc.set(Calendar.SECOND, 0);
        gc.set(Calendar.MILLISECOND, 0);
        return gc.getTimeInMillis();
    }

    public static String timestampToHumanDateAndTimeAndStampLocal(long time) {
        return String.format("%s(%d)", timestampToHumanDateAndTimeLocal(time), time);
    }

    public static String timestampToHumanDateAndTimeAndStampUTC(long time) {
        return String.format("%s(%d)", timestampToHumanDateAndTimeUTC(time), time);
    }

    public static long getTimeStampLocal(int year, int month, int day, double frac) {
        GregorianCalendar gc = new GregorianCalendar(year, month - 1, day);

        return gc.getTimeInMillis() + (int) (frac * 24L * 3600L * 1000L);
    }

    /**
     * Returns a new Date(), which is initialized with current time.
     * If regressionMode=true in configuration file, returns some fixed constant date.
     * @return
     * @throws AdeException
     */
    static public Date getCurrentDate() throws AdeException {
        if (Ade.getAde().getConfigProperties().debug().getRegressionMode()) {
            return new Date(REGRESSION_MODE_FAKE_CURRENT_DATE);
        }
        return new Date();
    }

    static private final String GMT1 = "GMT";
    static private final String GMT2 = "GMT+0";

    static public TimeZone parseTimeZone(String id) throws AdeUsageException {
        if (id.equals(GMT1) || id.equals(GMT2)) {
            return TimeZone.getTimeZone(GMT1);
        }

        TimeZone res = TimeZone.getTimeZone(id);
        if (res.equals(TimeZone.getTimeZone(GMT1))) {
            throw new AdeUsageException("time zone id '" + id + "' is unrecognized.");
        }
        return res;
    }

    static public SimpleDateFormat getNewGmtSimpleDateFormat(String format) {
        return getNewGmtSimpleDateFormat(format, true);
    }

    static public SimpleDateFormat getNewGmtSimpleDateFormat(String format, boolean lenient) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(GMT_TIMEZONE);
        sdf.setLenient(lenient);
        return sdf;
    }

    /**
     * @return a {@link GregorianCalendar} with its {@link TimeZone} set to GMT,
     *      and it's first day of the week to Monday
     */
    static public Calendar getGmtGregorianCalendar() {
        Calendar res = new GregorianCalendar(DateTimeUtils.GMT_TIMEZONE);
        res.setFirstDayOfWeek(MONDAY);
        return res;
    }

    static public Calendar getGmtGregorianCalendar(int year, int month, int dayOfMonth) {
        Calendar res = getGmtGregorianCalendar();
        res.set(Calendar.YEAR, year);
        res.set(Calendar.MONTH, month);
        res.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        res.set(Calendar.HOUR_OF_DAY, 0);
        res.set(Calendar.MINUTE, 0);
        res.set(Calendar.SECOND, 0);
        res.set(Calendar.MILLISECOND, 0);
        return res;
    }

    /**
     * Returns the the duration of the given string 
     * @param strDuration a duration in the format 'nW', 'nD' ,'nH', 'nm' or 
     * a concatenation of the above, separated with a white space 
     * @return the duration of the give string in milliseconds
     */
    static public long parseDuration(String strDuration) {
        String[] durations = strDuration.split("\\s");
        long duration = 0;
        for (String singleDuration : durations) {
            StringBuilder bldvalue = new StringBuilder("");
            for (int i = 0; i < singleDuration.length(); i++) {
                char c = strDuration.charAt(i);
                if (Character.isDigit(c)) {
                    bldvalue.append(c);
                } else if (Character.isLetter(c) && !bldvalue.toString().isEmpty()) {
                    duration += calcDuration(Integer.parseInt(bldvalue.toString()), c);
                    bldvalue.setLength(0);
                    bldvalue.append("");
                    break;
                }
            }
        }
        return duration;
    }

    private static long calcDuration(int value, char unit) {
        switch (unit) {
            case 'm':
                return value * MILLIS_IN_MINUTE;
            case 'H':
                return value * MILLIS_IN_HOUR;
            case 'D':
                return value * MILLIS_IN_DAY;
            case 'W':
                return value * MILLIS_IN_WEEK;
        }
        throw new IllegalArgumentException("Unknown unit char: " + unit);
    }

}
