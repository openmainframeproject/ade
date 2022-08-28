/*

    Copyright Contributors to the ADE Project.

    SPDX-License-Identifier: GPL-3.0-or-later

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
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

/**
 * An abstract class for extracting data from a Nginx log message.
 * Subclasses are expected to implement the parseLine() method to parse
 * a line and set the instance variable values as appropriate. A typical
 * subclass will call the parseLine method with a regex pattern and capturing
 * groups for each of the instance variables it wants to extract.
 * The features we consider for Nginx logs are:
 * 1. m_timestamp : Timestamp on the message
 * 2. m_remoteAddress : Remote Address of the message
 * 3. m_remoteUser : Remote User of the message
 * 4. m_request : The request field on the message line
 * 5. m_status: The status of the request
 * 6. m_bytes: The number of bytes sent
 */
public abstract class NginxLogLineParser {

    /**
     * Default logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(NginxLogLineParser.class);

    /**
     * UTF8_BOM regex.
     */
    protected static final String UTF8_BOM = "\\xEF\\xBB\\xBF";

    /**
     * The optional BOM and PRI, this will be used in pattern searching.
     */
    protected static final String BOM_AND_PRI = "(?:" + UTF8_BOM + ")?" + "(?:<\\p{Digit}{1,2}>)?";

    /**
     * Component of the message
     */
    protected String m_component;

    /**
     * Whether the hostname truncation has already been logged.
     */
    private boolean isHostnameTruncationLogged = false;

    /**
     * The LinuxAdeExtProperties that contains configurations and properties from
     * the start of AdeExt main class.
     */
    protected LinuxAdeExtProperties m_LinuxAdeExtProperties;

    /**
     * An object used for monitoring parsing quality, or null if none.
     */
    protected IParsingQualityReporter m_parsingQualityReport = null;

    /**
     * Time of the message.
     */
    protected Date m_timestamp;

    /**
     * The remote address of the request.
     */
    protected String m_remoteAddress;

    /**
     * The remote user of the request.
     */
    protected String m_remoteUser;

    /**
     * The message request.
     */
    protected String m_request;

    /**
     * Status code of the request.
     */
    protected int m_status;

    /**
     * Number of bytes of the request.
     */
    protected int m_bytes;

    /**
     * Parses a line and sets the instance variables from it.
     * 
     * @param line The line to parse.
     * @return false if the line could not be parsed.
     */
    public abstract boolean parseLine(String line);

    /**
     * Returns the property object containing configurations from the start of
     * AdeExt main class.
     * 
     * @return The AdeExtProperties object.
     */
    public final LinuxAdeExtProperties getLinuxAdeExtProperties() {
        return m_LinuxAdeExtProperties;
    }

    /**
     * Converts a date from String format to a Date object.
     * 
     * @param source the source name.
     * @param dateTimeString the date and time string value.
     * @return Date object with date/time-stamp of the Linux log.
     */
    public abstract Date toDate(String source, String dateTimeString);

    /**
     * Returns the DateTimeZone determined from the toDate(String source, String dateTimeString)
     * method.
     * 
     * @return The date object with date/time-stamp of the Linux log.
     */
    public abstract DateTime getLastDeterminedDateTime();

    /**
     * Parses a line based on a regex Pattern. For each capturing group
     * number that is non-zero, the corresponding instance variable
     * is set. (Assigns m_component = master, remove this once we have newer logs)
     * NOTE: There is no pid present in nginx logs.
     * 
     * @param pattern   - The pattern to parse.
     * @param timestamp - Capturing group number https://quest.squadcast.tech/api/RA1911003010323/emailsfor the timestamp.
     * @param remoteAddress  - Capturing group number for the remote address.
     * @param remoteUser - Capturing group number for the remote user.
     * @param request - Capturing group for the request.
     * @param status - Capturing group for the status code.
     * @param bytes - Capturing group for the number of bytes sent.
     * @return false if the line could not be parsed.
     */
    protected final boolean parseLine(Pattern pattern, int remoteAddress,
            int remoteUser, int timestamp, int request, int status, int bytes, String line) {
        final Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            try {
                String msgTimeString = toString(matcher, timestamp);
                System.out.println(msgTimeString);
                m_timestamp = toDate("m_remoteAddress", "msgTimeString");
                m_remoteAddress = toString(matcher, remoteAddress);
                m_remoteUser = toString(matcher, remoteUser);
                m_request = toString(matcher, request);
                m_status = Integer.parseInt(toString(matcher, status));
                m_bytes = Integer.parseInt(toString(matcher, bytes));
                // m_component = toString(matcher, comp);
                m_component = "master";
                System.out.println("PARSED SUCCESSFULLY");
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
     * 
     * @param m     Matcher to compare against a pattern.
     * @param group The capturing group value.
     * @return empty string if the capturing group is 0 otherwise the pattern
     *         captured by the passed in group.
     */
    private String toString(Matcher m, int group) {
        return (group == 0) ? "" : m.group(group);
    }

    /**
     * Returns the message time-stamp
     * 
     * @return the time-stamp.
     */
    public final Date getMsgTime() {
        return m_timestamp;
    }

    /**
     * Returns the component of the message.
     * 
     * @return the component
     */
    public final String getComponent() {
        return m_component;
    }

    /**
     * Returns the remote address.
     * 
     * @return the remote address string value.
     */
    public final String getRemoteAddress() {
        return m_remoteAddress;
    }

    /**
     * Returns the remote user.
     * 
     * @return the remote user string value.
     */
    public final String getRemoteUser() {
        return m_remoteUser;
    }

    /**
     * Returns the status code.
     * 
     * @return the status code int value.
     */
    public final int getStatus() {
        return m_status;
    }

    /**
     * Returns the number of bytes.
     * 
     * @return the number of bytes.
     */
    public final int getBytes() {
        return m_bytes;
    }

    /**
     * Returns the request text.
     * 
     * @return the request text string value.
     */
    public final String getRequest() {
        return m_request;
    }

    /**
     * Sets the parsingQualityReport for monitoring parsing quality of Linux logs.
     * 
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
        return String.format("timestamp=(%s) remote_address=(%s) remote_user=(%s) request=(%s) status=(%s) bytes=(%s)",
                m_timestamp, m_remoteAddress, m_remoteUser, m_request, m_status, m_bytes);
    }
}
