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

import java.util.regex.Pattern;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;

/**
 * An RFC3164 syslog parser that looks for a component id and process id
 * in the message body (Note, this class also handles log messages with only
 * the component id and not the process id).  If present, they are 
 * extracted and the remainder of the message is returned as the message body.
 */
public class NginxLogParser extends NginxLogParserBase {

    /**
     * Pattern object for matching against the base RFC3164 header, the optional BOM_AND_PRI which gets the
     * option UTF-8 Byte Order mark and priority values, and regex that finds the component id, process id,
     * and message body.
     */
        private static final Pattern pattern = Pattern.compile(NGINX_LOG);
//    private static final Pattern pattern = Pattern.compile(NGINX_LOG
//            + "([-_./a-zA-Z0-9]*[-_./a-zA-Z])?: (.*)$");

    /**
     * Default constructor to call its parent constructor.
     * @throws AdeException
     */
    public NginxLogParser() throws AdeException {
        super();
    }
    /**
     * Explicit-value constructor for getting the properties file and passing it to the parent explicit value 
     * constructor.
     * @param linuxAdeExtProperties Contains property and configuration information from the start of AdeExt main class.
     * @throws AdeException
     */
    public NginxLogParser(LinuxAdeExtProperties linuxAdeExtProperties)
            throws AdeException {
        super(linuxAdeExtProperties);
    }

    /**
     * Parses the line by calling the super class's parseLine with the positions of the captured groups.
     * As an example, it looks at the text following the RFC3164 header and tries to match text like:
     *      process-name[1234]: message body.  
     *  If found, the component is set to "process-name" and the pid is set to "1234".  The message body is 
     *  set to "message body."
     * @param line A line from the Linux syslog file.
     * @return true if the line was parsed successfully.
     */
    @Override
    public boolean parseLine(String line) {
        return parseLine(pattern, NGINX_LOG_REMOTE_ADDRESS_GROUP, NGINX_LOG_REMOTE_USER_GROUP,
                NGINX_LOG_TIMESTAMP_GROUP, NGINX_LOG_REQUEST_GROUP, NGINX_LOG_STATUS_GROUP,
                NGINX_LOG_BYTES_GROUP, line);
    }

}
