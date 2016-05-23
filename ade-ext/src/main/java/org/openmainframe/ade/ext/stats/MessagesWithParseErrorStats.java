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
package org.openmainframe.ade.ext.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.AdeExt;
import org.openmainframe.ade.ext.os.parser.LinuxSyslog3164ParserBase;
import org.openmainframe.ade.ext.os.parser.LinuxSyslog5424ParserBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class MessagesWithParseErrorStats {
    /**
     * A hashmap to keep track of stats objects
     */
    static private MessagesWithParseErrorStats stats = null;

    public static MessagesWithParseErrorStats getParserErrorStats() throws AdeException {
        if (stats == null) {
            stats = new MessagesWithParseErrorStats();
        }
        return stats;
    }

    /**
     * Output format
     */
    private static final DateTimeFormatter s_dateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy z");

    /**
     * Timestamp Patterns
     */
    private static final Pattern RFC3164_TIMESTAMP_PATTERN = Pattern.compile(LinuxSyslog3164ParserBase.RFC3164_TIMESTAMP);
    private static final String RFC3164_TIMESTAMP = "3164_TIME";
    private static final Pattern RFC5424_TIMESTAMP_PATTERN = Pattern.compile(LinuxSyslog5424ParserBase.RFC5424_TIMESTAMP);
    private static final String RFC5424_TIMESTAMP = "5424_TIME";

    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(MessagesWithParseErrorStats.class);

    /**
     * Logger for statistics
     */
    private static final Logger statslogger = LoggerFactory.getLogger(MessagesWithParseErrorStats.class);

    /**
     * Number of token we want as representation.
     * 
     * RFC5124 has 6 tokens of interest.  RFC3164 has 3 tokens of interest (after treating timestamp as 1 token)
     */
    final static int REPRESENTATION_TOKEN_COUNT_RFC3164 = 4;
    final static int REPRESENTATION_TOKEN_COUNT_RFC5424 = 7;
    final static int REPRESENTATION_TOKEN_COUNT_FOR_GENERAL = 10;

    /**
     * Maximum number of messages to keep, and the number of message
     * before cleanup will happen.
     */
    final static int DEFAULT_NUMBER_OF_PARSE_ERROR_TO_KEEP = 100;
    final static int DEFAULT_NUMBER_OF_PARSE_ERROR_ADDITIONAL_FOR_CLEANUP = 20;
    private int m_numberOfParseErrorToKeepMax = 100;
    private int m_numberOfParseErrorToRunCleanup = 110;

    /**
     * Number of days different before considering the message are different
     * during cleanup.
     */
    final static int DEFAULT_NUMBER_OF_DAYS_TO_CONSIDER_CLEANUP_PRIORITY = 2;
    static private int m_numberOfDaysToConsiderCleanupPriority = 2;

    /**
     * A mapping from MsgRep to Msg Stats
     */
    private HashMap<String, MessageStats> m_msgRepToMsgStatsMap;

    /**
     * Whether there are new errors
     */
    private boolean isThereNewErrors = true;

    /**
     * Constructor
     * @throws AdeException 
     */
    private MessagesWithParseErrorStats() throws AdeException {
        m_msgRepToMsgStatsMap = new HashMap<>();

        /* Set the numberOfParseErrorToKeep */
        m_numberOfParseErrorToKeepMax = AdeExt.getAdeExt().getConfigProperties().getParseErrorsToKeep();
        if (m_numberOfParseErrorToKeepMax < 0) {
            m_numberOfParseErrorToKeepMax = 100;
        }

        /* Set the number of Parse Error that will trigger cleanup */
        if (m_numberOfParseErrorToKeepMax == 0) {
            m_numberOfParseErrorToRunCleanup = 0;
        } else {
            m_numberOfParseErrorToRunCleanup = m_numberOfParseErrorToKeepMax
                    + DEFAULT_NUMBER_OF_PARSE_ERROR_ADDITIONAL_FOR_CLEANUP;
        }

        /* Set the numberOfParseErrorToKeep */
        m_numberOfDaysToConsiderCleanupPriority = AdeExt.getAdeExt().getConfigProperties().getParseErrorDaysToTolerate();
        if (m_numberOfDaysToConsiderCleanupPriority < 0) {
            m_numberOfDaysToConsiderCleanupPriority = DEFAULT_NUMBER_OF_DAYS_TO_CONSIDER_CLEANUP_PRIORITY;
        }

        logger.info("Tracking Parser Error Objects: "
                + ", parseErrorToKeep=" + m_numberOfParseErrorToKeepMax
                + ", parseErrorToRunCleanup=" + m_numberOfParseErrorToRunCleanup
                + ", numberOfDaysToTolerate=" + m_numberOfDaysToConsiderCleanupPriority);
    }

    /**
     * Add messages
     */
    public void addMessage(String line) {
        isThereNewErrors = true;

        /* Only keep parse error if the Max is > 0 */
        if (m_numberOfParseErrorToKeepMax > 0) {
            final String msgRep = getMessageRepresentation(line);
            MessageStats msgStats = m_msgRepToMsgStatsMap.get(msgRep);
            if (msgStats == null) {
                msgStats = new MessageStats(msgRep);
                m_msgRepToMsgStatsMap.put(msgStats.getMessageRepresentation(), msgStats);
            }

            msgStats.occurred();
            cleanupMapIfNeeded();
        }
    }

    /**
     * Clean up the map
     */
    private void cleanupMapIfNeeded() {
        final int numberOfMsgKept = m_msgRepToMsgStatsMap.size();

        if (numberOfMsgKept > m_numberOfParseErrorToRunCleanup) {
            /* During overflow, write it to the log */
            writeToLog();

            /* Perform cleanup */
            final ArrayList<MessageStats> statsArray = new ArrayList<>(m_msgRepToMsgStatsMap.values());
            Collections.sort(statsArray);

            /* Remove the first few Stats from the beginning of the list, until we reach the max */
            for (MessageStats stats : statsArray) {
                final String msgRep = stats.getMessageRepresentation();
                m_msgRepToMsgStatsMap.remove(msgRep);

                if (m_msgRepToMsgStatsMap.size() <= m_numberOfParseErrorToKeepMax) {
                    break;
                }
            }
        }
    }

    /**
     * Retrieve the message's representation.
     * @return
     */
    private String getMessageRepresentation(String origLine) {
        /* The n-th space we want to look for */
        final String stringWanted = " ";

        String line = origLine;
        String ret = "";
        boolean matchedRFC3164Time = false;
        boolean matchedRFC5424Time = false;

        /* look for the n-th space in the line */
        int tokensWanted = REPRESENTATION_TOKEN_COUNT_RFC3164 - 1;

        /* Replace the normal variable data, i.e. the timestamp, in a message */
        final Matcher rfc3164Matcher = RFC3164_TIMESTAMP_PATTERN.matcher(line);
        if (rfc3164Matcher.find()) {
            line = rfc3164Matcher.replaceFirst(RFC3164_TIMESTAMP);
            matchedRFC3164Time = true;
            tokensWanted = REPRESENTATION_TOKEN_COUNT_RFC3164;
        }

        final Matcher rfc5424Matcher = RFC5424_TIMESTAMP_PATTERN.matcher(line);
        if (rfc5424Matcher.find()) {
            line = rfc5424Matcher.replaceFirst(RFC5424_TIMESTAMP);
            matchedRFC5424Time = true;
            tokensWanted = REPRESENTATION_TOKEN_COUNT_RFC5424;
        }

        /* Extract more tokens if it doesn't match RFC3164 or RFC5424 timestamp */
        if (!matchedRFC3164Time && !matchedRFC5424Time) {
            tokensWanted = REPRESENTATION_TOKEN_COUNT_FOR_GENERAL;
        }

        int pos = line.indexOf(stringWanted, 0);
        int oldPos;
        while (tokensWanted > 0) {
            oldPos = pos;
            pos = line.indexOf(stringWanted, oldPos + 1);

            if (oldPos + 1 != pos) {
                tokensWanted--;

                /* The else case happen when multiple consecutive spaces are in the line */
            }

            if (pos == -1) {
                /* End of line */
                pos = line.length();
                break;
            }
        }

        ret = line.substring(0, pos);

        return ret;
    }

    /**
     * Write the content of this to a log file.
     */
    public void writeToLog() {
        if (isThereNewErrors) {
            if (m_msgRepToMsgStatsMap.size() == 0) {
                /* Don't need any message if there were no error */
                statslogger.info("No Parser Errors on: " + s_dateTimeFormatter.print(DateTime.now()));
            } else {
                /* Only output this if there are new errors */

                final ArrayList<MessageStats> statsArray = new ArrayList<>(m_msgRepToMsgStatsMap.values());
                Collections.sort(statsArray);

                statslogger.info("Parser Errors as of: " + s_dateTimeFormatter.print(DateTime.now()));
                StringBuilder bldout = new StringBuilder("");
                for (MessageStats stats : statsArray) {
                    final String newStr = stats.toString();
                    if ((newStr.length() + bldout.toString().length()) >= (Integer.MAX_VALUE / 2)) {
                        statslogger.info(bldout.toString());
                        bldout.append("");
                    }
                    bldout.append("\n" + newStr);
                }
                statslogger.info(bldout.toString());

            }

            /* Set isThereNewErrors to false. */
            isThereNewErrors = false;
        }
    }

    /**
     * 
     */
    static class MessageStats implements Comparable<MessageStats> {
        /**
         * The Message Representation
         */
        private String m_msgRep;

        /**
         * Number of times this Message Representation occurred.
         */
        private int m_occurrance;

        /**
         * Last time this message was added;
         */
        private long m_lastAdded;

        /**
         * Constructor
         * @param msgRep
         */
        public MessageStats(String msgRep) {
            m_msgRep = msgRep;
        }

        /**
         * Indicate that the message rep has occurred.
         */
        public void occurred() {
            m_lastAdded = System.currentTimeMillis();
            m_occurrance++;
        }

        /**
         * Return the occurrence
         */
        public int getOccurred() {
            return m_occurrance;
        }

        /**
         * Return the representation
         */
        public String getMessageRepresentation() {
            return m_msgRep;
        }

        /**
         * Return the last time this message was added
         * @return
         */
        public long getLastAdded() {
            return m_lastAdded;
        }

        /**
         * Compare the messageStats
         */
        @Override
        public int compareTo(MessageStats in) {
            final DateTime inDateTime = new DateTime(in.getLastAdded());
            final DateTime thisDateTime = new DateTime(getLastAdded());

            /* Allow some toleration */
            int days = Days.daysBetween(inDateTime, thisDateTime).getDays();

            if (m_numberOfDaysToConsiderCleanupPriority >= Math.abs(days)) {
                /* For example, if the days diff is less than 2 days, we
                 * will use the occurrence count for sorting.
                 */
                days = 0;
            }

            /* Do the compare */
            if (days > 0) {
                /* last occurrence of this message is after in */
                return 1;

            } else if (days < 0) {
                return -1;
            } else {
                if (getOccurred() > in.getOccurred()) {
                    return 1;
                } else if (getOccurred() < in.getOccurred()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            final MessageStats ms = (MessageStats) obj;
            
            return this.compareTo(ms) == 0;
        }

        @Override
        public int hashCode() {
            return this.getOccurred();
        }

        @Override
        public String toString() {
            return "lastAdded=" + s_dateTimeFormatter.print(m_lastAdded)
                    + ", count=" + m_occurrance
                    + ", msgRep=" + m_msgRep + "\n";
        }
    }

    /**
     * Main
     * @param argv
     */
    public static void main(String[] argv) {
        MessagesWithParseErrorStats stats = null;
        try {
            stats = getParserErrorStats();
        } catch (AdeException e) {
            logger.error("Parse error encountered.", e);
        }

        String str, result;

        str = "1 2 3 4 5 6 7 8 9 ";
        result = stats.getMessageRepresentation(str);
        logger.info(str + " : " + result);

        str = "1 2 3 4 5 6 7";
        result = stats.getMessageRepresentation(str);
        logger.info(str + " : " + result);

        str = "1 2 3 4 5 6";
        result = stats.getMessageRepresentation(str);
        logger.info(str + " : " + result);

        str = " 1 2 3 4 5 6 7";
        result = stats.getMessageRepresentation(str);
        logger.info(str + " : " + result);

        str = "1 2  3 4  5 6 7";
        result = stats.getMessageRepresentation(str);
        logger.info(str + " : " + result);

    }
}
