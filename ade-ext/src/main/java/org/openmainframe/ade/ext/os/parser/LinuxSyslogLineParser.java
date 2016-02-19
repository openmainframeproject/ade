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

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openmainframe.ade.actions.IParsingQualityReporter;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

/**
 * An abstract class for extracting data from a linux syslog message.
 * Subclasses are expected to implement the parseLine() method to parse
 * a line and set the instance variable values as appropriate.  A typical
 * subclass will call the parseLine method with a regex pattern and capturing 
 * groups for each of the instance variables it wants to extract.
 */
public abstract class LinuxSyslogLineParser {
    /**
     * Default logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(LinuxSyslogLineParser.class);

    /**
     * UTF8_BOM regex.
     */
    protected static final String UTF8_BOM = "\\xEF\\xBB\\xBF";

    /**
     * The optional BOM and PRI, this will be used in pattern searching.
     */
    protected static final String BOM_AND_PRI = "(?:" + UTF8_BOM + ")?" + "(?:<\\p{Digit}{1,2}>)?";
    /**
     * The component name when syslog-ng starts.
     */
    public static final String SYSLOG_NG_COMPONENT_NAME = "syslog-ng";
    /**
     * The pattern when syslog-ng is starting.
     */    
    public static final String SYSLOG_NG_STARTING = ".*(starting|configuration initialized|reloading configuration).*";
    public static final Pattern SYSLOG_NG_STARTING_PATTERN = Pattern.compile(SYSLOG_NG_STARTING);

    /**
     * The pattern when a message is suppressed.
     */
    public static final String SYSLOG_MESSAGE_SUPPRESSION = ".*last message repeated (\\d+) times.*";
    public static final Pattern SYSLOG_MESSAGE_SUPPRESSION_PATTERN = Pattern.compile(SYSLOG_MESSAGE_SUPPRESSION);
    
    /**
     * Process ID of the message.  Note: this is not always there.
     */
    protected String m_pid;

    /**
     * Body of the message.  Note: this doesn't have to be ASCII.
     */
    protected String m_text;
    

    /**
     * Whether the hostname truncation has already been logged.
     */
    private boolean isHostnameTruncationLogged = false;

    /**
     * The LinuxAdeExtProperties that contains configurations and properties from the start of
     * AdeExt main class.
     */
    protected LinuxAdeExtProperties m_LinuxAdeExtProperties;

    /**
     * An object used for monitoring parsing quality, or null if none.
     */
    protected IParsingQualityReporter m_parsingQualityReport = null;
    
    /**
     * Time of the message.
     */
    protected Date m_msgTime;

    /**
     * Source of the message.
     */
    protected String m_source;

    /**
     * Component of the message.  Note: this is not always there.
     */
    protected String m_component;

    /**
     * Severity of the message.
     */
    protected Severity m_severity = Severity.UNKNOWN;

    /**
     * Checks to see if syslog-ng has been restarted by comparing against
     * the component name and the matching against the SYSLOG_NG_STARTING_PATTERN
     * regex.
     * @param compId the component name.
     * @param body the message text.
     * @return true if syslog-ng has been restarted.
     */
    public static boolean isSyslogNgRestarted(String compId, String body) {
        if (compId.equalsIgnoreCase(SYSLOG_NG_COMPONENT_NAME)) {
            final Matcher sysLogNgMatcher = SYSLOG_NG_STARTING_PATTERN.matcher(body);
            if (sysLogNgMatcher.matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wrapper method for checking if syslog-ng has restarted. This method
     * extracts the component name and the message text and passes it to
     * isSyslogNgRestarted(String compId, String body) method.
     * @param mi Represents single message from Linux log.
     * @return true if syslog-ng has restarted.
     */
    public static boolean isSyslogNgRestarted(IMessageInstance mi) {
        final String compId = mi.getComponentId();
        final String body = mi.getText();
        return isSyslogNgRestarted(compId, body);
    }

    /**
     * Getter method for retrieving SysLogSuppressiongPattern.
     * @return The pattern for matching against regex to determining if a message has been suppressed.
     */
    public static Pattern getSysLogSuppressionPattern() {
        return SYSLOG_MESSAGE_SUPPRESSION_PATTERN;
    }

    /**
     * Parses a line and sets the instance variables from it.
     * @param line The line to parse.
     * @return false if the line could not be parsed.
     */
    public abstract boolean parseLine(String line);

    /**
     * Returns the property object containing configurations from the start of 
     * AdeExt main class.
     * @return The AdeExtProperties object.
     */
    public final LinuxAdeExtProperties getLinuxAdeExtProperties() {
        return m_LinuxAdeExtProperties;
    }

    /**
     * Converts a date from String format to a Date object.
     * @param source the source name.
     * @param s the date and time string value.
     * @return Date object with date/time-stamp of the Linux log.
     */
    public abstract Date toDate(String source, String s);

    /**
     * Returns the DateTimeZone determined from the toDate(String source, String s) method.
     * @return The date object with date/time-stamp of the Linux log.
     */
    public abstract DateTime getLastDeterminedDateTime();

    /**
     * Parses a line based on a regex Pattern.  For each capturing group
     * number that is non-zero, the corresponding instance variable
     * is set.
     * @param pattern  - The pattern to parse.
     * @param timestamp - Capturing group number for the timestamp.
     * @param hostname - Capturing group number for the hostname.
     * @param comp - Capturing group number for the component name.
     * @param pid - Capturing group number for the process id.
     * @param msg - Capturing group number for the message body.
     * @param line - The line to parse.
     * @return false if the line could not be parsed.
     */
    protected final boolean parseLine(Pattern pattern, int timestamp, int hostname,
            int comp, int pid, int msg, String line) {
        final Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            try {
                final String msgTimeStr = toString(matcher, timestamp);

                final String source255Chars = toString(matcher, hostname);
                if (source255Chars.length() > 200) {
                    m_source = source255Chars.substring(0, 200);
                    if (!isHostnameTruncationLogged) {
                        logger.info("Hostname : \"" + source255Chars + "\""
                                + " is truncated from " + source255Chars.length()
                                + " to " + m_source.length() + " characters: \""
                                + m_source + "\"");

                        /* Set it to true to prevent further logging */
                        isHostnameTruncationLogged = true;
                    }
                } else {
                    m_source = source255Chars;
                }

                m_source = m_source.toLowerCase();
                m_msgTime = toDate(m_source, msgTimeStr);
                m_component = toString(matcher, comp);
                m_pid = toString(matcher, pid);
                m_text = toString(matcher, msg);

                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            }
        }

        return false;
    }

    /**
     * Captures the group passed in by matching against a pattern.
     * @param m Matcher to compare against a pattern.
     * @param group The capturing group value.
     * @return empty string if the capturing group is 0 otherwise the pattern
     * captured by the passed in group.
     */
    private String toString(Matcher m, int group) {
        return (group == 0) ? "" : m.group(group);
    }

    /**
     * Returns the message time-stamp
     * @return the time-stamp.
     */
    public final Date getMsgTime() {
        return m_msgTime;
    }

    /**
     * Returns the source name.
     * @return the source name string value.
     */
    public final String getSource() {
        return m_source;
    }

    /**
     * Returns the component name.
     * @return the component name string value.
     */
    public final String getComponent() {
        return m_component;
    }

    /**
     * Returns the severity level of the log message.
     * @return the severity value.
     */
    public final Severity getSeverity() {
        return m_severity;
    }

    /**
     * Returns the process id.
     * @return the process id string value.
     */
    public final String getPid() {
        return m_pid;
    }

    /**
     * Returns the message body text.
     * @return the message body text string value.
     */
    public final String getMessageBody() {
        return m_text;
    }

    /**
     * Sets the parsingQualityReport for monitoring parsing quality of Linux logs.
     * @param parsingQualityReport the ParsingQualityReporter object to be used.
     */
    public final void setParseQualityReport(IParsingQualityReporter parsingQualityReport) {
        m_parsingQualityReport = parsingQualityReport;
    }

    /**
     * The overridden toString method for this class. Prints out the captured groups
     * from the message.
     */
    @Override
    public String toString() {
        return String.format("timestamp=(%s) hostname=(%s) comp=(%s) pid=(%s) msg=(%s)",
                m_msgTime, m_source, m_component, m_pid, m_text);
    }
}
