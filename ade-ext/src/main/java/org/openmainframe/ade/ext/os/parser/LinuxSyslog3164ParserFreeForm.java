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

import java.util.regex.Pattern;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;

/**
 * An RFC3164 syslog parser that accepts everything after the header as
 * message text.
 */
public class LinuxSyslog3164ParserFreeForm extends LinuxSyslog3164ParserBase {

    /**
     * Pattern object for matching against the base header regex and everything else after it.
     */
    private static final Pattern pattern = Pattern.compile(RFC3164_HEADER + "(.*)$");
    
    /**
     * Add another capturing group, MSG which contains the message text, to the base number of captured groups.
     */
    private static final int MSG_GROUP = RFC3164_HEADER_GROUPS + 1;
    /**
     * Default constructor to call its parent constructor.
     * @throws AdeException
     */
    public LinuxSyslog3164ParserFreeForm() throws AdeException {
        super();
    }
    /**
     * Explicit-value constructor for getting the properties file and passing it to the parent explicit value 
     * constructor.
     * @param linuxAdeExtProperties Contains property and configuration information from the start of AdeExt main class.
     * @throws AdeException
     */
    public LinuxSyslog3164ParserFreeForm(LinuxAdeExtProperties linuxAdeExtProperties) throws AdeException {
        super(linuxAdeExtProperties);
    }
    /**
     * Parses the line by calling the super class's parseLine with the positions of the captured groups.
     * Note: The MSG_GROUP captures everything pass the header as message text.
     * @param line A line from the Linux syslog file.
     * @return true if the line was parsed successfully.
     */
    @Override
    public final boolean parseLine(String line) {
        return parseLine(pattern, RFC3164_HEADER_TIMESTAMP_GROUP,
                RFC3164_HEADER_HOSTNAME_GROUP, 0, 0,
                MSG_GROUP, line);
    }

}
