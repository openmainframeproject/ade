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

import java.util.Date;
import java.util.TimeZone;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.IAdeConfigProperties;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * An abstract base class for Spark log parsers. This class defines regular expressions
 * for the Spark log header fields while leaving additional parsing of the message body to 
 * concrete subclasses.
 */
public abstract class SparklogParserBase extends SparklogLineParser {
    /**
     * Main logger for this class.
     */
    static final Logger s_logger = LoggerFactory.getLogger(SparklogParserBase.class);

    /**
     * The end of today, when the parser was first loaded.
     */
    private static DateTime END_OF_TODAY = null;

    /**
     * The input time-zone specified in setup.props.
     */
    private static DateTimeZone INPUT_TIME_ZONE;
    /**
     * The output time-zone specified in setup.props.
     */
    private static DateTimeZone OUTPUT_TIME_ZONE;

    /**
     * LinuxAdeExtProperties object that contains properties and configurations information from the start
     * of AdeExt main class.
     */
    private static LinuxAdeExtProperties s_linuxAdeExtProperties = null;
    
    /**
     * Regular expression to extract the priority information from the header. Note: The priority is optional. 
     * This allows logs that have been written to disk without the priority to be post-processed.
     * protected static final String SPARK_PRI = "(?:<(\\d{1,3})>){0,1}";
     * // We're not using this at the moment
     */
    
    /**
     * Regular expression to extract the host name from the header.
     */
    protected static final String SPARK_HOSTNAME = "([\\-\\.:%_a-zA-Z0-9]{1,255})";
    /**
     * Regular expression to extract the time-stamp from the header.
     */
    public static final String SPARK_TIMESTAMP = "(\\d{2}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2})";
    /**
     * Regular expression to extract header information. (The priority, time-stamp, and host name)
     */
    protected static final String SPARK_HEADER = "^" + SPARK_TIMESTAMP + " " + SPARK_HOSTNAME + " ";

    /*
     * Within the SPARK_HEADER regex string above, identify the regex
     * capturing groups for the parts that we want to extract.
     */
    protected static final int SPARK_HEADER_TIMESTAMP_GROUP = 1;
    protected static final int SPARK_HEADER_HOSTNAME_GROUP = 2;

    /*
     * Define the number of capturing groups defined by the header. This is
     * useful to a subclass that wants to use the RFC3164_HEADER constant and
     * concatenate strings with additional capturing groups. The subclass can use
     * this constant to identify the start of its own capturing groups.
     */
    protected static final int SPARK_HEADER_GROUPS = 2;

    /**
     * The current year. (Spark logs already contain the year)
     */
    private final int curYear;
    
    /*
     * Setup an array of DateTimeFormatter objects that can parse the dates in a
     * 3164 style message.  Both are necessary because the DateTimeFormatter
     * parseDateTime() method doesn't handle a variable number of spaces
     * between the month and day.
     */
    protected static final DateTimeFormatter[] dt_formatters = {
            DateTimeFormat.forPattern("yy/MM/dd HH:mm:ss").withZoneUTC()
    };
    /**
     * Constructor for initializing the properties file and various time properties.
     * @param linuxAdeExtProperties Contains property and configuration information.
     * @throws AdeException
     */
    public SparklogParserBase(LinuxAdeExtProperties linuxAdeExtProperties) throws AdeException {
        this.curYear = new DateTime().getYear();

        if (linuxAdeExtProperties == null) {
            m_LinuxAdeExtProperties = s_linuxAdeExtProperties;
        } else {
            m_LinuxAdeExtProperties = linuxAdeExtProperties;
        }

        /* Set the start of today and timezone*/
        initializeTimeZoneAndStartOfToday();
    }
    
    /**
     * Default constructor that sets the properties file to null.
     * @throws AdeException
     */
    public SparklogParserBase() throws AdeException {
        this(null);
    }
    
    /**
     * Set the AdeExt properties file.
     * @param linuxAdeExtProperties The properties file that contains the configuration and properties information.
     */
    public static void setAdeExtProperties(LinuxAdeExtProperties linuxAdeExtProperties) {
        s_linuxAdeExtProperties = linuxAdeExtProperties;
    }

    /**
     * Returns the year stored in AdeExt properties file.
     * @return the year as an int value.
     */
    public static int getAdeExtPropertiesYear() {
        return s_linuxAdeExtProperties.getYear();
    }
    /**
     * Returns the input time zone specified in setup.props
     * @return The input time zone.
     */
    public static DateTimeZone getInputTimeZone() {
        return INPUT_TIME_ZONE;
    }
    /**
     * Returns the output time zone specified in setup.props
     * @return The output time zone.
     */
    public static DateTimeZone getOutputTimeZone() {
        return OUTPUT_TIME_ZONE;
    }

    /**
     * Retrieves the date parsed from the header of a log. Unlike Syslog, Spark logs come with year defined.
     * Redefining the year in setup file is ineffective.
     * After parsing the date, we need to correct the time-zone. 
     * Then we set the dateTime to the current year. Now we need to check the dateTime and see if it's after today.
     * The logic is as follows:
     *      - If Log time-stamp < End of day of today 
     *          (comparing Month, Day, Hour, Minutes, Seconds, with year missing), 
     *          assume it's this year.
     *      - If Log time-stamp > End of day of today 
     *          (comparing Month, Day, Hour, Minutes, Seconds, with year missing), 
     *          assume it's previous year.
     * 
     * The following restrictions will be made to customer for BulkLoad:
     *      - Cannot upload logs older than 11 months.
     *      - Cannot upload logs that are continuous for more than 11 months.
     * 
     * Note: END OF TODAY is purposely chosen instead of START OF TODAY in case a user bulk loads logs that 
     * belongs to today.  It's not possible/likely that a user would bulk load logs from last year of the 
     * same day with the restriction we specified above.
     * @param source the source name string value.
     * @param s the date and time string value.
     * @return Date object with date/time-stamp of the Linux log.
     */
    @Override
    public final Date toDate(String source, String s) {
        DateTime dt = null;
        for (DateTimeFormatter fmt : dt_formatters) {
            try {
                dt = fmt.parseDateTime(s);
                dt = dt.withZoneRetainFields(INPUT_TIME_ZONE);
                dt = dt.withZone(OUTPUT_TIME_ZONE);
                /* AdeCore will take the Java Date object, and convert 
                 * it to the output time-zone, then extract the hour. */
                return dt.toDate();
            } catch (IllegalArgumentException e) {
                /* This exception can occur normally when iterating
                 * through the DateTimeFormatter objects. It is only 
                 * an error worth noting when the dt object is not null.
                 */
                if (dt != null) {
                    s_logger.error("Invalid argument encountered.", e);
                }
            }
        }
        throw new IllegalArgumentException("Failed to parse date " + s);
    }

    /**
     * Set the END_OF_TODAY value and time-zone values. The time-zone values are taken from the Ade
     * configuration properties. End_OF_TODAY value is retrieved by getting the current date-time, 
     * adjust time-zone, add an additional day and set the time to the start of the day.
     * Note: These only need to be set once.
     * @throws AdeException
     */
    private static void initializeTimeZoneAndStartOfToday() throws AdeException {
        synchronized (SparklogParserBase.class) {
            if (END_OF_TODAY == null) {
                final IAdeConfigProperties adeConfig = Ade.getAde().getConfigProperties();
                final TimeZone timeZone = adeConfig.getInputTimeZone();
                final TimeZone outputTimezone = adeConfig.getOutputTimeZone();
                INPUT_TIME_ZONE = DateTimeZone.forOffsetMillis(timeZone.getRawOffset());
                OUTPUT_TIME_ZONE = DateTimeZone.forOffsetMillis(outputTimezone.getRawOffset());
                END_OF_TODAY = DateTime.now();
                END_OF_TODAY = END_OF_TODAY.withZone(OUTPUT_TIME_ZONE);
                END_OF_TODAY = END_OF_TODAY.plusDays(1);
                END_OF_TODAY = END_OF_TODAY.withTimeAtStartOfDay();
            }
        }
    }

    /**
     * Return the DateTimeZone determined from toDate(String source, String s) method.
     * For 3164 messages, the DateTimeZone is not included in the log.
     * @return null since DateTimeZone is not included in the log.
     */
    public final DateTime getLastDeterminedDateTime() {
        return null;
    }

}
