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

package org.openmainframe.ade.impl.flow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.PriorityQueue;
import java.util.Random;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.IAsyncLineReader;
import org.openmainframe.ade.impl.flow.MultiLogStreamProcessorImpl;
import org.openmainframe.ade.impl.utils.DateTimeUtils;

import junit.framework.TestCase;

public class TestMultiLogTracker extends TestCase {

    static final long SEED = 609281;
    static int m_numberOfSources = 4;
    static int m_numberOfMessagesPerSource = 20;
    static int m_maxTimeBetweenMessageLines = 500;

    static int m_minTimeBetweenMessages = 1000 * 0;
    static int m_maxTimeBetweenMessages = 1000 * 15;
    static int m_maxLinesInMessage = 25;
    static final long FLUSH_TIME = 1000 * 3;

    /** Proxy for a real AsyncLineReader.
     * 
     *  Generates data from m_numberOfSources Generators (see inner class)
     *  The source names are G<index>
     *  A row looks like this:  
     *  
     *  <sourceName> M=<messageIndex> LEN=<totalRowNum> POS=<rowIndex>
     */
    private class DummyReader implements IAsyncLineReader {

        private Random m_random = new Random(SEED);
        private boolean m_eof;
        private String m_lastGenName;

        /** Simulates a single source.
         * Generates a total of m_numberOfMessagesPerSource messages with:
         * 
         * U[1,m_maxLinesInMessage] rows
         * U[m_minTimeBetweenMessages,m_maxTimeBetweenMessages) milliseconds between messages
         * U[0,m_maxTimeBetweenMessageLines] milliseconds between rows of the same message
         * 
         * A message looks like:
         * <sourceName> M=<messageIndex> LEN=<totalRowNum> POS=<rowIndex>
         */
        class Generator implements Comparable<Generator> {
            String m_name;
            int m_curMessage = 0;
            int m_curMessageLen;
            int m_curMessagePos;
            long m_nextActionTime;

            Generator(String name) {
                m_name = name;
                drawNextMessage();
            }

            private void drawNextMessage() {
                m_nextActionTime = System.currentTimeMillis() + m_minTimeBetweenMessages +
                        m_random.nextInt(m_maxTimeBetweenMessages - m_minTimeBetweenMessages);
                m_curMessagePos = 0;
                m_curMessageLen = m_random.nextInt(m_maxLinesInMessage) + 1;
            }

            public boolean isDone() {
                return m_curMessage >= m_numberOfMessagesPerSource;
            }

            public String getNextLine() throws AdeInternalException {
                if (m_curMessage >= m_numberOfMessagesPerSource)
                    throw new AdeInternalException("bug");
                while (System.currentTimeMillis() < m_nextActionTime)
                    ;
                String res = m_name + " M=" + m_curMessage + " LEN=" + m_curMessageLen + " POS=" + m_curMessagePos;
                ++m_curMessagePos;
                if (m_curMessagePos >= m_curMessageLen) {
                    ++m_curMessage;
                    drawNextMessage();
                } else {
                    m_nextActionTime = System.currentTimeMillis() + m_random.nextInt(m_maxTimeBetweenMessageLines);
                }
                return res;
            }

            @Override
            public int compareTo(Generator o) {
                return new Long(m_nextActionTime).compareTo(o.m_nextActionTime);
            }
        }

        PriorityQueue<Generator> m_generators = new PriorityQueue<>();
        Deque<String> m_waitingLines = new ArrayDeque<>();

        public DummyReader() {
            for (int i = 0; i < m_numberOfSources; ++i)
                m_generators.add(new Generator("G" + i));
        }

        @Override
        public String readLine(long waitTime) throws AdeInternalException {
            if (m_waitingLines.size() > 0)
                return m_waitingLines.removeFirst();

            if (m_generators.size() == 0) {
                m_eof = true;
                return null;
            }

            long nextGenTime = m_generators.peek().m_nextActionTime;
            long endTime = Math.min(System.currentTimeMillis() + waitTime, nextGenTime);
            while (System.currentTimeMillis() < endTime)
                ;
            if (System.currentTimeMillis() < nextGenTime)
                return null;
            Generator g = m_generators.remove();

            if (m_lastGenName == null || !m_lastGenName.equals(g.m_name)) {
                if (m_lastGenName != null)
                    m_waitingLines.add("");
                m_lastGenName = g.m_name;
                m_waitingLines.add("==> " + g.m_name + " <==");
            }

            m_waitingLines.add(g.getNextLine());
            if (!g.isDone())
                m_generators.add(g);
            return m_waitingLines.removeFirst();
        }

        @Override
        public boolean isEof() {
            return m_eof;
        }

        @Override
        public void start() throws AdeException {
        }

        @Override
        public void close() throws AdeException {
        }
    }

    /** A line-processor-block that reads the messages of a specific source and verifies:
     * 
     *  - They all arrive correctly
     *  - Waiting time indicates multi-log-processor flushed them correctly.
     *  - No messages of other sources arrive
     */
    private class DummyProcessor implements IFrameableTarget<String, TimeSeparator> {

        String m_name;

        /** A data class representing a message.
         */
        class Message {
            /** Index of message */
            int m_num;
            /** Number of rows already arrived*/
            int m_pos;
            /** Number of rows expected */
            int m_len;
            /** Time of first row */
            long m_startTime;
            /** Time of last row that arrived */
            long m_endTime;
            /** Time of flush, or when next message arrived (time when this message was declared done) */
            long m_flushOrNext;

            /** Verify this record is complete and ok:
             * 
             * Message index is as expected (num)
             * All expected rows arrived
             * Message flushed 
             * startTime<=endTime<=flushTime<=endTime+FLUSH_TIME*4
             */
            void verify(int num) throws AdeInternalException {
                double secsToEnd = (m_endTime - m_startTime) / 1000.0;
                double secsToNext = (m_flushOrNext - m_endTime) / 1000.0;
                System.out.printf("[%s]: time %5.3f, till next %5.3f", toString(), secsToEnd, secsToNext);
                System.out.flush();
                if (m_num != num)
                    throw new AdeInternalException("Wrong num");
                if (m_pos != m_len - 1)
                    throw new AdeInternalException("Not fully read");
                if (m_flushOrNext == 0)
                    throw new AdeInternalException("Message not flushed");
                if (m_flushOrNext < m_endTime)
                    throw new AdeInternalException("Message flushed before end");
                if (m_endTime < m_startTime)
                    throw new AdeInternalException("Message end before start");
                long diff = m_flushOrNext - m_endTime - FLUSH_TIME * 4;
                if (diff > 1000)
                    throw new AdeInternalException("Flush time too large by " + diff + " millis");
                System.out.println(": verified ok");
            }

            public String toString() {
                return String.format("%s: Message %d length %d",
                        m_name, m_num, m_len);
            }
        }

        private int m_curNum;

        private int m_curPos;

        private int m_curLen;

        ArrayList<Message> m_messages = new ArrayList<>();
        Message m_lastMessage;

        public DummyProcessor(String name) {
            m_name = name;
        }

        @Override
        public void incomingObject(String line) throws AdeInternalException {
            processLine(line);
        }

        private void processLine(String line) throws AdeInternalException {
            // Verify message is addressed to this source
            if (!line.startsWith(m_name + " "))
                throw new AdeInternalException("Line " + line + " does not start with name " + m_name);

            // Extract message details
            line = line.substring(m_name.length() + 1);
            String parts[] = line.split(" ");
            if (parts.length != 3)
                throw new AdeInternalException("Expecting 3 parts in " + line);
            m_curNum = extractKey(parts[0], "M");
            m_curLen = extractKey(parts[1], "LEN");
            m_curPos = extractKey(parts[2], "POS");

            // Sanity check
            if (m_curPos >= m_curLen)
                throw new AdeInternalException("Bug: invalid pos");

            // A new row to a previous message
            if (m_lastMessage != null && m_lastMessage.m_num == m_curNum) {
                // Sanity checks
                if (m_lastMessage.m_len != m_curLen)
                    throw new AdeInternalException("Bug: different lengths");
                if (m_curPos != m_lastMessage.m_pos + 1)
                    throw new AdeInternalException("Bug: not consecutive pos");
                // Update new row in current message record
                m_lastMessage.m_endTime = System.currentTimeMillis();
                m_lastMessage.m_pos = m_curPos;
            } else {
                // If previous message exists, flush and verify it.
                // Otherwise, just make sure this is message number 0
                if (m_lastMessage != null) {
                    if (m_curNum != m_lastMessage.m_num + 1)
                        throw new AdeInternalException("Bug: not consecutive num");
                    if (m_lastMessage.m_flushOrNext == 0)
                        m_lastMessage.m_flushOrNext = System.currentTimeMillis();
                    m_lastMessage.verify(m_lastMessage.m_num);
                } else if (m_curNum != 0)
                    throw new AdeInternalException("Bug: wrong first num");
                // Make sure this is the first row
                if (m_curPos != 0)
                    throw new AdeInternalException("Bug: wrong first pos");
                // Create a new message record                
                Message m = new Message();
                m.m_startTime = System.currentTimeMillis();
                m.m_endTime = m.m_startTime;
                m.m_pos = 0;
                m.m_len = m_curLen;
                m.m_num = m_curNum;
                m_messages.add(m);
                m_lastMessage = m;
            }
        }

        private int extractKey(String val, String key) throws AdeInternalException {
            if (!val.startsWith(key + "="))
                throw new AdeInternalException("Value " + val + " does not start with " + key);
            String numVal = val.substring(key.length() + 1);
            try {
                return Integer.valueOf(numVal);
            } catch (NumberFormatException e) {
                throw new AdeInternalException("Failed converting " + numVal + " from " + val);
            }
        }

        @Override
        public void incomingSeparator(TimeSeparator sep) throws AdeInternalException {
            flush();

        }

        private void flush() throws AdeInternalException {
            if (m_lastMessage != null && m_lastMessage.m_flushOrNext == 0) {
                m_lastMessage.m_flushOrNext = System.currentTimeMillis();
                m_lastMessage.verify(m_lastMessage.m_num);
            }
        }

        @Override
        public void endOfStream() throws AdeInternalException {
            flush();
            for (Message m : m_messages)
                System.out.println("" + m);
            if (m_messages.size() != m_numberOfMessagesPerSource)
                throw new AdeInternalException("Not all messages arrived");
            for (int i = 0; i < m_numberOfMessagesPerSource; ++i)
                m_messages.get(i).verify(i);
        }

        @Override
        public void beginOfStream() throws AdeException, AdeFlowException {

        }

    }

    public void setParams(int numberOfSources, int messagesPerSource, int maxLinesInMessage, int maxWaitBetweenMessages) {
        m_numberOfSources = numberOfSources;
        m_numberOfMessagesPerSource = messagesPerSource;
        m_maxLinesInMessage = maxLinesInMessage;
        m_maxTimeBetweenMessages = maxWaitBetweenMessages;
    }

    public void testShortRun() throws AdeException {
        setParams(2, 4, 8, 1000);
        myRun();
    }

    private void longRun() throws AdeException {
        setParams(4, 20, 25, 1000 * 15);
        myRun();
    }

    private void myRun() throws AdeException {
        System.out.println("Starting run:");
        System.out.println("Number of sources       : " + m_numberOfSources);
        System.out.println("Messages per source     : " + m_numberOfMessagesPerSource);
        System.out.printf("Seconds between messages: U[%f,%f)\n", m_minTimeBetweenMessages / 1000.0, m_maxTimeBetweenMessages / 1000.0);
        System.out.printf("Seconds between rows    : U[0,%f)\n", m_maxTimeBetweenMessageLines / 1000.0);
        System.out.printf("Flush time (secs)       : %f\n", FLUSH_TIME / 1000.0);
        DummyReader dr = new DummyReader();
        MultiLogStreamProcessorImpl mlt = new MultiLogStreamProcessorImpl(dr);
        mlt.setFlushTime(FLUSH_TIME);
        for (int i = 0; i < m_numberOfSources; ++i)
            mlt.addTarget("G" + i, new DummyProcessor("G" + i));
        mlt.run();
        System.out.println("All ok");
    }

    static public void main(String args[]) throws AdeException {
        new TestMultiLogTracker().longRun();
    }

    static public void printTsLn(String msg) {
        System.out.println(DateTimeUtils.timestampToHumanDateAndTimeAndStampLocal(System.currentTimeMillis()) + " " + msg);
    }
}
