/*
 
    Copyright IBM Corp. 2012, 2016
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

/**
 * String constants for Reader. These messages provide statistics for the user to let them know if the corresponding
 * ANALYZE/UPLOAD was successful. 
 */
public final class ReaderLoggerMessages {
    /**
     * private constructor since this is a utilities class.
     */
    private ReaderLoggerMessages() {}
    
    /**
     * The parsing statistics.
     */
    public static final String PARSED_DATA_STATS_MSG = "Finished parsing message data %d lines of which, %d non-wrapper messages, "
                + " %d suppressed non-wrapper messages, %d total wrapper messages, %d lines in error, %d lines without "
                + "component name, %d lines with unexpected source, in total time = %f seconds";
    /**
     * Message when an upload is considered "good" i.e. the number of successfully parsed messages is above the threshold.
     */
    public static final String GOOD_UPLOAD_MSG = "Starting on %s the Anomaly Detection Engine successfully received %d lines of data from %s in "
            + "which %d lines contained messages. The received data can be either current %s data from a monitored "
            + "client or priming data for one or more clients.";
    /**
     * Message when an upload is considered "bad" i.e. the number of successfully parsed messages is below the threshold.
     */
    public static final String BAD_UPLOAD_MSG = "Starting on %s the Anomaly Detection Engine received %d lines of data from %s in which %d lines "
            + "contained messages. A significant number of lines in the received data do not contain valid message IDs."
            + " The received data can be either current %s data from a monitored client or priming data for one or "
            + "more clients. Check the data being sent to ensure that it meets the Anomaly Detection Engine formatting requirements.";
    /**
     * Message when an analyze is considered "good" i.e. the number of successfully parsed messages is above the threshold.
     */
    public static final String GOOD_ANALYZE_MSG = "Starting on %s, the Anomaly Detection Engine successfully processed %d lines of "
            + "%s data from %s in which %d lines contained messages.";
    /**
     * Message when none of the messages were successfully parsed during an analyze.
     */
    public static final String NO_MSGS_PARSED_MSG = "Starting on %s, the Anomaly Detection Engine processed %d lines of %s data from " 
            + "%s. None of these lines contained messages, so this data could not be used to produce analysis results.";
    /**
     * Message when an analyze is considered "bad" i.e. the number of successfully parsed messages is below the threshold.
     */
    public static final String BAD_ANALYZE_MSG = "Starting on %s the Anomaly Detection Engine processed %d lines of %s data from %s in which " 
            + "%d lines contained messages. Although the Anomaly Detection Engine used the messages for analysis, the low number of lines "
            + "containing messages might indicate a problem. Consider checking the log data and system configuration "
            + "for potential problems.";
}
