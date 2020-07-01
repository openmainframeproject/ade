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
 * An RFC3164 syslog parser that looks for a component id and process id
 * in the message body (Note, this class also handles log messages with only
 * the component id and not the process id).  If present, they are 
 * extracted and the remainder of the message is returned as the message body.
 */
public class SparklogParser extends SparklogParserBase {

    /**
     * Pattern object for matching against the base RFC3164 header, the optional BOM_AND_PRI which gets the
     * option UTF-8 Byte Order mark and priority values, and regex that finds the component id, process id,
     * and message body. Within a Linux log, COMPONENT name "([-_./a-zA-Z0-9]*[-_./a-zA-Z]+[-_./a-zA-Z0-9]*)"
     * is expected to contain at least one non-digit character.  This requirement is driven by some Linux
     * logs that have unique numeric value followed by ":" at the position of the component name.  These numeric
     * values does not help group the messages together, and causes the database to run out of assign-able unique
     * IDs quickly.
     */
    private static final Pattern pattern = Pattern.compile(SPARK_HEADER
            + BOM_AND_PRI + "([-_./a-zA-Z0-9]*[-_./a-zA-Z]+[-_./a-zA-Z0-9]*)(?:\\[([0-9]*)\\])?: (.*)$");

    /*
     * Identifies the regex capturing groups for the parts that we want to extract. The component id,
     * process id, and the message text are additional capturing groups on top of the header groups.
     */
    private static final int COMP_GROUP = SPARK_HEADER_GROUPS + 1;
    private static final int MSG_GROUP = SPARK_HEADER_GROUPS + 2;
    
    /**
     * Default constructor to call its parent constructor.
     * @throws AdeException
     */
    public SparklogParser() throws AdeException {
        super();
    }
    /**
     * Explicit-value constructor for getting the properties file and passing it to the parent explicit value 
     * constructor.
     * @param linuxAdeExtProperties Contains property and configuration information from the start of AdeExt main class.
     * @throws AdeException
     */
    public SparklogParser(LinuxAdeExtProperties linuxAdeExtProperties)
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
        return parseLine(pattern, SPARK_HEADER_TIMESTAMP_GROUP,
                SPARK_HEADER_HOSTNAME_GROUP, COMP_GROUP, MSG_GROUP, line);
    }

}
