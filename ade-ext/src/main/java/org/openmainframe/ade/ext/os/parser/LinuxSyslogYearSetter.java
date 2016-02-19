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
package org.openmainframe.ade.ext.os.parser;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.openmainframe.ade.impl.utils.DateTimeUtils;

/**
 * Class that handles year setting.
 */
public class LinuxSyslogYearSetter {
    /**
     * Allows message logs to go back in time at most 1 day.
     */
    static final private long DECREMENT_DAYS_ALLOWANCE_IN_MILLIS = 1L * 86400000L;

    /**
     * Allows message logs to go forward in time at most 364 days.
     */
    static final private long INCREMENT_DAYS_LIMIT_IN_MILLIS = 365L * 86400000L - DECREMENT_DAYS_ALLOWANCE_IN_MILLIS;

    /**
     * The Hashmap that maps source to yearSetter. Each source has its own yearSetter.
     */
    static private Map<String, LinuxSyslogYearSetter> map = new HashMap<String, LinuxSyslogYearSetter>();

    /**
     * The date of the last seen message.
     */
    private DateTime m_lastSeenMessageDate;

    /**
     * The current year, this year will only move forward.
     */
    private int m_currentYear;

    /**
     * The source name associated with this yearSetter.
     */
    private String m_source;

    /**
     * Constructor for setting some member variables.
     * @param source the source name string value.
     * @param startingYear the starting year int value.
     */
    public LinuxSyslogYearSetter(String source, int startingYear) {
        m_currentYear = startingYear;
        m_source = source;
    }
    
    /**
     * Returns the LinuxSyslogYearSetter object. If there is no existing yearSetter for the 
     * passed in source then a new one is made otherwise, return what is in the hashMap.
     * @param source the source name string value.
     * @return the LinuxSyslogYearSetter object.
     */
    static public LinuxSyslogYearSetter getYearSetter(String source) {
        LinuxSyslogYearSetter yearSetter = map.get(source);
        if (yearSetter == null) {
            yearSetter = new LinuxSyslogYearSetter(source,
                    LinuxSyslog3164ParserBase.getAdeExtPropertiesYear());
            map.put(source, yearSetter);
        }
        return yearSetter;
    }

    /**
     * Sets the starting year.
     * @param startingYear the starting year int value.
     */
    public final void setYear(int startingYear) {
        m_currentYear = startingYear;
    }

    /**
     * This method returns the year of the current message. First it makes a clone of the current date.
     * Then we obtain the milliseconds for the current date and the last message's date and calculate 
     * the message time difference. Depending on the difference, return the year appropriately.
     * @param currentMessageDate the current date of the message.
     * @return the year of the message.
     */
    public final int getDesiredYear(DateTime currentMessageDate) {
        int yearToReturn = 10;
        DateTime curMessageDate = new DateTime(currentMessageDate);
        curMessageDate = curMessageDate.withYear(m_currentYear);
        if (m_lastSeenMessageDate == null) {
            m_lastSeenMessageDate = curMessageDate;
        }
        final long curDateMillis = curMessageDate.getMillis();
        final long lastDateMillis = m_lastSeenMessageDate.getMillis();
        final long messageTimeDiff = curDateMillis - lastDateMillis;

        if (messageTimeDiff < 0) {
            /* cur message timestamp < last message timestamp.  There are two cases: */
            if (Math.abs(messageTimeDiff) < DECREMENT_DAYS_ALLOWANCE_IN_MILLIS) {
                /* Case: Message time goes backwards (maybe by a few minutes).   
                 * The year is set to current year. */
                yearToReturn = m_currentYear;
            } else {
                /* Case: Year N to Year N+1.  
                 * - Message time goes from Year N to Year N+1 (e.g. actual situation is 12/31/2001 to 1/1/2002, 
                 *   and our test will compare 12/31/2001 and 1/1/2001)
                 */
                m_currentYear++;
                curMessageDate = curMessageDate.withYear(m_currentYear);
                yearToReturn = m_currentYear;
                m_lastSeenMessageDate = curMessageDate;
                final String msg = m_source + ": Year increase by 1, to " + m_currentYear;
                LinuxSyslog3164ParserBase.s_logger.info(msg);
            }
        } else {
            /* last message timestamp < cur message timestamp. There are two cases:
             */
            if (messageTimeDiff < INCREMENT_DAYS_LIMIT_IN_MILLIS) {
                /* Case: The log time is increasing normally. In case of a missing log, we allow a specific limit, 
                 * such as 300 days. */
                yearToReturn = m_currentYear;
                m_lastSeenMessageDate = curMessageDate;
            } else {
                /* Case: Message time goes from Year N to Year N-1 (e.g. actual situation is 1/1/2002 to 
                 * 12/31/2001, and out test will compare 1/1/2002 and 12/31/2002.)
                 * In this case, we don't change the m_currentYear.
                 */
                yearToReturn = m_currentYear - 1;
            }
        }
        return yearToReturn;
    }

    /**
     * Override the toString method to print out member variables in human readable format.
     * @return String containing source name, current year, and last seen message date information. 
     */
    @Override
    public String toString() {
        String str = "";
        str += " source=" + m_source;
        str += " year=" + m_currentYear;
        str += " lastSeenMessage=" + DateTimeUtils.timestampToHumanDateAndTimeAndStampUTC(m_lastSeenMessageDate.getMillis());

        return str;
    }
}