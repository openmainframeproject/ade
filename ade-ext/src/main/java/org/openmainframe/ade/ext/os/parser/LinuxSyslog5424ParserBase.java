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
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Generic parser for RFC5424 style syslog messages.
 */
public class LinuxSyslog5424ParserBase extends LinuxSyslogLineParser {
    /*
     * Define some constant regular expressions used to construct various parts of the syslog message.
     *
     * An example of an RFC5424 formatted syslog message:
     * <46>1 2014-05-30T08:52:40.620950-04:00 host-name rsyslogd  - - rsyslogd's groupid changed to 103
     * -   - -                                -         -        -- - -
     * |   | |                                |         |        || | |--message
     * |   | |                                |         |        || |----structured data
     * |   | |                                |         |        ||------message id
     * |   | |                                |         |        |-------proc id (NOTE: MISSING!)
     * |   | |                                |         |----------------app name
     * |   | |                                |--------------------------hostname
     * |   | |-----------------------------------------------------------timestamp
     * |   |-------------------------------------------------------------version
     * |-----------------------------------------------------------------priority
     *
     * Notes:
     *  - Fractional seconds on the time-stamp are optional.
     *  - We do not parse structured data and it is not included in the message body.
     *  - rsyslogd violates the RFC by providing an empty string instead of the nil character "-" for 
     *  a missing proc id.  The RFC5424_PROCID regex below compensates for that by accepting a 0 
     *  character procid.
     *
     */
    protected static final String RFC5424_PRI = "<(\\d{1,3})>";
    protected static final String RFC5424_VERSION = "[1-9]{0,1}";
    protected static final String RFC5424_HOSTNAME = "([\\-\\.:%_a-zA-Z0-9]{1,255})";
    protected static final String RFC5424_APPNAME = "(-|\\p{Graph}{1,48})";
    protected static final String RFC5424_PROCID = "(-|\\p{Graph}{0,128})";
    protected static final String RFC5424_MSGID = "(-|\\p{Graph}{1,32})";
    protected static final String RFC5424_STRUCTURED_DATA = "(-|\\[.*?\\])";
    public static final String RFC5424_TIMESTAMP = "(\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,6}){0,1}(?:Z|[\\-+]\\d{2}:{0,1}\\d{2}))"; 
    protected static final String RFC5424_OPTIONAL_FRAMING = "(?:\\d{1,5} ){0,1}";
    protected static final String RFC5424_OPTIONAL_PREFIX = "(?:" + RFC5424_PRI + RFC5424_VERSION + " ){0,1}";
    protected static final String RFC5424_HEADER = "^"
            + RFC5424_OPTIONAL_FRAMING
            + RFC5424_OPTIONAL_PREFIX
            + RFC5424_TIMESTAMP
            + " " + RFC5424_HOSTNAME
            + " " + BOM_AND_PRI + RFC5424_APPNAME
            + " " + RFC5424_PROCID
            + " " + RFC5424_MSGID
            + " " + RFC5424_STRUCTURED_DATA
            + " ";

    /*
     * Within the RFC5424_HEADER regex string above, identify the regex
     * capturing groups for the parts that we want to extract.
     */
    protected static final int RFC5424_HEADER_PRI_GROUP = 1;
    protected static final int RFC5424_HEADER_TIMESTAMP_GROUP = 2;
    protected static final int RFC5424_HEADER_HOSTNAME_GROUP = 3;
    protected static final int RFC5424_HEADER_APPNAME_GROUP = 4;
    protected static final int RFC5424_HEADER_PROCID_GROUP = 5;
    protected static final int RFC5424_HEADER_MSGID_GROUP = 6;
    protected static final int RFC5424_HEADER_STRUCTURED_DATA_GROUP = 7;

    /*
     * Define the number of capturing groups defined by the header. This is
     * useful to a subclass that wants to use the RFC5424_HEADER constant and
     * concatenate strings with additional capturing groups. The subclass can use
     * this constant to identify the start of its own capturing groups.
     */
    protected static final int RFC5424_HEADER_GROUPS = 7;

    /*
     * Setup an array of DateTimeFormatter objects that can parse the dates in a
     * 5424 style message. RFC5424 allows for some flexibility in the formatting
     * of the timestamp, so we must account for multiple formats.
     *
     * First, fractional seconds are optional in the timestamp, so we have
     * formatters that account for both the presence and absence of fractional
     * seconds. Second, the timezone can be specified as either a literal "Z"
     * for GMT time, or an explicit +-hour:minute specification.
     */
    protected static final DateTimeFormatter[] dt_formatters = {
            DateTimeFormat.forPattern("yyyy-MM-dd'T'H:m:s.SSSSSS'Z'").withZoneUTC(),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'H:m:sZ").withZoneUTC(),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'H:m:s.SSSSSSZZ"),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'H:m:sZZ"),
    };

    /*
     * Define the pattern for RFC5424 messages to be matched by this parser.
     */
    private static final Pattern pattern = Pattern.compile(RFC5424_HEADER + "(.*)$");
    /**
     * Position at which to capture the message text.
     */
    private static final int MSG_GROUP = RFC5424_HEADER_GROUPS + 1;

    /**
     * The dateTimeZone of the last date parsed in the toDate() method.
     */
    private DateTime m_dateTime = null;

    /**
     * This class converts the extracted time-stamp to a Date object by iterating
     * over the possible DateTimeFormatter objects until one is able to
     * successfully parse it.
     * @param source the source name string value.
     * @param s the date and time string value.
     * @return Date object with date/time-stamp of the Linux log.
     */
    @Override
    public Date toDate(String source, String s) {
        for (DateTimeFormatter fmt : dt_formatters) {
            try {
                final DateTime dt = fmt.withOffsetParsed().parseDateTime(s);
                m_dateTime = dt;
                return dt.toDate();
            } catch (IllegalArgumentException e) {
                // Ignore and continue to the next formatter.
            }
        }
        throw new IllegalArgumentException("Failed to parse date " + s);
    }

    /**
     * Return the DateTimeZone determined from toDate(String source, String s) method.
     * @return the date object with date/time-stamp of the Linux log passed in to toDate.
     */
    @Override
    public DateTime getLastDeterminedDateTime() {
        return m_dateTime;
    }

    /**
     * Invoke the base class parseLine() routine, which, given a regex
     * pattern and the capturing group number for each field, will extract
     * the values into the object instance variables.
     * @param line A line from the Linux syslog file.
     * @return true if the line was successfully parsed.
     */
    @Override
    public boolean parseLine(String line) {
        if (parseLine(pattern, RFC5424_HEADER_TIMESTAMP_GROUP,
                RFC5424_HEADER_HOSTNAME_GROUP, RFC5424_HEADER_APPNAME_GROUP,
                RFC5424_HEADER_PROCID_GROUP, MSG_GROUP, line)) {
            /* Convert any nil values ("-") to the empty string */
            if (m_component.equals("-")) {
                m_component = "";
            }
            if (m_pid.equals("-")) {
                m_pid = "";
            }
            return true;
        }
        ;
        return false;
    }
}
