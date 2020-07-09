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
import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

/**
 * An abstract class for extracting data from a Spark log message.
 * Subclasses are expected to implement the parseLine() method to parse
 * a line and set the instance variable values as appropriate.  A typical
 * subclass will call the parseLine method with a regex pattern and capturing 
 * groups for each of the instance variables it wants to extract.
 * The features we consider for Spark logs are:
 * 1. m_msgTime : Timestamp on the message
 * 2. m_source : Origin of the message
 * 3. m_text : The text field on the message line
 */
public abstract class SparklogLineParser {
    /**
     * Default logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(SparklogLineParser.class);

    /**
     * UTF8_BOM regex.
     */
    protected static final String UTF8_BOM = "\\xEF\\xBB\\xBF";

    /**
     * The optional BOM and PRI, this will be used in pattern searching.
     */
    protected static final String BOM_AND_PRI = "(?:" + UTF8_BOM + ")?" + "(?:<\\p{Digit}{1,2}>)?";   
    /**
     * Body of the message.  Note: this doesn't have to be ASCII.
     */
    protected String m_text;

    /**
     * Component of the message
     */
    protected String m_component;

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
     * Severity of the message.
     */
    protected Severity m_severity = Severity.UNKNOWN;
    // Not considering this field for the moment

    
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
     * is set. (Assigns m_component = master, remove this once we have newer logs)
     * NOTE: There is no pid present in spark logs.
     * @param pattern  - The pattern to parse.
     * @param timestamp - Capturing group number for the timestamp.
     * @param hostname - Capturing group number for the hostname.
     * @param msg - Capturing group number for the message body.
     * @param line - The line to parse.
     * @return false if the line could not be parsed.
     */
    protected final boolean parseLine(Pattern pattern, int timestamp, int hostname,
            int comp, int msg, String line) {
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
                m_text = toString(matcher, msg);
                //m_component = toString(matcher, comp);
                m_component = "master";
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
     * Returns the component of the message.
     * @return the component
     */
    public final String getComponent(){
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
     * Returns the source name.
     * @return the source name string value.
     */
    public final String getSource() {
        return m_source;
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
        return String.format("timestamp=(%s) hostname=(%s) msg=(%s)",
                m_msgTime, m_source, m_text);
    }
}
