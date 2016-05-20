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

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that keep track of messages from unexpected source, and perform logging/tracing.
 */

public final class MessagesWithUnexpectedSource {
    /**
     * Logger
     */
    private static final Logger s_logger = LoggerFactory.getLogger(MessagesWithUnexpectedSource.class);
    
    private MessagesWithUnexpectedSource() {
        // Private constructor to hide the implicit public one.
    }

    /**
     * Map from source name to the SourceStatisticsForMessage object
     */
    private static HashMap<String, SourceStatisticsForMessage> sourceToStatisticsMap = new HashMap<>();

    /**
     * Add a message that belongs to an unexpected source.
     * 
     * @param source
     * @param timestamp
     * @param message
     */
    public static void addMessage(String source, long msgTimestamp, String message) {
        /* Get the sourceStatisticsForMessage object.  If it doesn't expect, create one */
        SourceStatisticsForMessage sourceStatisticsForMessage = sourceToStatisticsMap.get(source);
        if (sourceStatisticsForMessage == null) {
            sourceStatisticsForMessage = new SourceStatisticsForMessage(source);
            sourceToStatisticsMap.put(source, sourceStatisticsForMessage);
        }

        /* Add the message to the sourceStatisticsForMessage object */
        sourceStatisticsForMessage.addMessage(msgTimestamp, message);
    }

    /** 
     * Class containing statistics for a source, and it is also responsible to write 
     * FFDC/trace information related to the unexpected source.
     */
    static class SourceStatisticsForMessage {
        /**
         * Name of the source 
         */
        private String m_source;

        /**
         * The most recent timestamp of logging about the unexpected source. 
         */
        private long m_lastLogTimestamp = 0;

        /**
         * The log interval, 60 minutes.
         * 
         * After the log interval has elapsed, the logging for a source will be enabled again. 
         */
        private static final long LOG_INTERVAL = 60 * 60 * 1000;

        /**
         * A count of number of log messages about the unexpected source.
         */
        private int m_intervalLogCount = 0;

        /**
         * Within a log interval, the number of times the message will be logged.
         */
        private static final int INTERVAL_MAX_LOG = 5;

        /**
         * A count of number of log messages about the unexpected source.
         */
        private long m_totalCount = 0;
        private long m_prevTotalCount = 0;

        /**
         * Constructor
         * @param source
         */
        SourceStatisticsForMessage(String source) {
            m_source = source;
        }

        /**
         * Handle a message from an unexpected source, and perform logging if needed.
         */
        void addMessage(long msgTimestamp, String message) {
            if (msgTimestamp - m_lastLogTimestamp > LOG_INTERVAL) {
                /* After LOG_INTERVAL has elapsed, it's considered a new interval.
                 * And, we will reset the intervalLogCount to 0.
                 */
                m_intervalLogCount = 0;

                /* Log information related to the unexpected source */
                s_logger.warn("Unexpected source " + m_source + ", statistics"
                        + ": totalCount=" + m_totalCount
                        + ", deltaCount=" + (m_totalCount - m_prevTotalCount)
                        + ", intervalSize=" + (msgTimestamp - m_lastLogTimestamp));

                /* Set the previous values */
                m_lastLogTimestamp = msgTimestamp;
                m_prevTotalCount = m_totalCount;
            }

            /* Log a warning message only if the amount of warning messages in this 
             * interval is less than INTERVAL_MAX_LOG */
            if (m_intervalLogCount < INTERVAL_MAX_LOG) {
                s_logger.warn("Unexpected source " + m_source + ", log message:" + message);
                m_intervalLogCount++;
            }

            /* Increment the total number of unexpected message count for this source */
            m_totalCount++;
        }
    }
}
