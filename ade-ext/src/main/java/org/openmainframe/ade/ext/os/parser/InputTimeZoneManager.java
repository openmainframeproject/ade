/*
 
    Copyright IBM Corp. 2015, 2016
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
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.IAdeConfigProperties;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;

/**
 * Class that keeps track of log's input time-zone for different sources. On Linux, the time-zone is always GMT for RFC3164 logs.  
 * For RFC5124, the time-zone is determined from the input log message, and this class keeps track of the latest known time-zone 
 * when the log message's time-zone changes.
 */
public final class InputTimeZoneManager {
    /**
     * A map from the source ID to dateTime object.
     */
    private static Map<String, DateTime> s_sourceToTimeZoneMap = new HashMap<String, DateTime>();

    /**
     * The default GMT Offset from setup.props.
     */
    static String s_configuredInputTimeZone = null;
    
    /**
     * Set of time constants.
     */
    public static final long SECONDS_IN_MINUTE = 60L;
    public static final long MINUTES_IN_HOUR   = 60L;
    public static final long EXCESS_MINUTES    = 60L;
    public static final long MILLIS_IN_SECOND = 1000L;
    public static final long MILLIS_IN_MINUTE = MILLIS_IN_SECOND * SECONDS_IN_MINUTE;
    public static final long MILLIS_IN_HOUR = MILLIS_IN_MINUTE * MINUTES_IN_HOUR;

    private InputTimeZoneManager() {
        //private constructor
    }

    /**
     * Update the latest time-zone for a system.
     * @param sourceId contains the name of the system.
     * @param dateTime the time extracted from parsing the logs. Note: for RFC3164, the dateTime
     * value will be null since the time-zone is always GMT.
     */
    public static void updateTimezone(String sourceId, DateTime dateTime) {
        if (dateTime != null) {
            s_sourceToTimeZoneMap.put(sourceId, dateTime);
        }
    }

    /**
     * Retrieves the time-zone for a system by getting the offset from the the DateTime object. If there is
     * no time-zone for the log messages from "sourceId", then we will use the time-zone in the setup.props file.
     * Note that when the time-zone offset changes, such as due to DST, Ade will be restarted with the new
     * time-zone settings in setup.props.
     * @param sourceId The name of the system.
     * @return returns the time-zone in string format.
     * @throws AdeException 
     */
    public static String getTimezone(String sourceId) throws AdeException {
        final DateTime dateTime = s_sourceToTimeZoneMap.get(sourceId);
        if (dateTime != null) {
            final long offsetInMillis = dateTime.getZone().getOffset(dateTime.getMillis());
            String timeZoneString = String.format("%02d:%02d",
                    Math.abs(offsetInMillis / MILLIS_IN_HOUR), 
                        Math.abs((offsetInMillis / MILLIS_IN_MINUTE) % EXCESS_MINUTES));
            if (offsetInMillis < 0) {
                timeZoneString = "GMT-" + timeZoneString;
            } else {
                timeZoneString = "GMT+" + timeZoneString;
            }
            return timeZoneString;
        } else {
            if (s_configuredInputTimeZone == null) {
                try {
                    final IAdeConfigProperties config = Ade.getAde().getConfigProperties();
                    s_configuredInputTimeZone = config.getInputTimeZone().getID();
                } catch (Exception e) {
                    throw new AdeInternalException("Unexpected Exception getting Input TimeZone from Ade", e);
                }
            }
            return s_configuredInputTimeZone;
        }
    }
}
