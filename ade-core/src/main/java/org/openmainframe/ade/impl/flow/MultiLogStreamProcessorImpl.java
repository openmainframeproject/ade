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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.flow.IMultiLogStreamProcessor;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiLogStreamProcessorImpl implements IMultiLogStreamProcessor {

    /** Underlying reader from which lines are pulled */
    private IAsyncLineReader m_asyncLineReader;
    /** Name of last log from which lines were read */
    private String m_lastLogName;

    private boolean m_stop = false;

    private static Logger logger = LoggerFactory.getLogger(MultiLogStreamProcessorImpl.class);

    private final static long DEFAULT_FLUSH_TIME = DateTimeUtils.MILLIS_IN_SECOND * 30;

    /** Basic time unit in which to perform flush. Actual time until flush may be 2-3 times larger */
    private long m_flushTime;
    /** Normal waiting time for a line to arrive */
    private long m_waitTime;
    /** Waiting time time for a line to arrive after a flush has been perfomred.
     * In this case there is no reason to wait a short time
     */
    private long m_longWaitTime;
    /** Map of targets that wait for lines. The keys are log names */
    private Map<String, Target> m_targets = new TreeMap<>();
    /** Target matching m_lastLogName */
    private Target m_currentTarget;
    /** Next time in which flush will be checked.
     * This doesn't mean flush will actually be performed - this also depends on individual targets timings 
     */
    private long m_nextGlobalFlushTime;
    /** Indicates whether last line was empty and therefore ignored.
     * This is used to ignore last lines prior to the lines that cause a log switch */
    private boolean m_lastLineEmpty;

    /** An envelope over actual target. */
    private class Target {
        /** Actual target */
        IFrameableTarget<String, TimeSeparator> m_processor;
        /** Last time it received a line */
        long m_lastMessageTime;
        /** Was it flushed since it last received a line */
        boolean m_flushed;

        /** Add line to target, record time and reset flush flag */
        public void addLine(String line) throws AdeException {
            m_processor.incomingObject(line);
            m_lastMessageTime = System.currentTimeMillis();
            m_flushed = false;
        }

        /** Flush target if not already flushed and if m_flushTime time passed since last time */
        public void flushIfNeeded(long curTime) throws AdeException {
            if (m_flushed || curTime < m_lastMessageTime + m_flushTime) {
                return;
            }
            m_processor.incomingSeparator(Ade.getAde().getDataFactory().newTimeSeparator("flush"));
            m_flushed = true;
        }
    }

    public MultiLogStreamProcessorImpl(BufferedReader reader) {
        this(new AsyncLineReaderThreadedImpl(reader));
    }

    MultiLogStreamProcessorImpl(IAsyncLineReader reader) {
        m_asyncLineReader = reader;
        setFlushTime(DEFAULT_FLUSH_TIME);
        m_nextGlobalFlushTime = System.currentTimeMillis() + m_flushTime;
    }

    /* (non-Javadoc)
     * @see org.openmainframe.ade.impl.flow.MultiLogStreamProcessor#setFlushTime(long)
     */
    @Override
    public final void setFlushTime(long flushTime) {
        m_flushTime = flushTime;
        m_waitTime = m_flushTime * 2;
        m_longWaitTime = m_waitTime * 10;
        m_nextGlobalFlushTime = Math.min(m_nextGlobalFlushTime, System.currentTimeMillis() + m_flushTime);
    }

    /* (non-Javadoc)
     * @see org.openmainframe.ade.impl.flow.MultiLogStreamProcessor#addTarget(java.lang.String, org.openmainframe.ade.flow.LineProcessorBlock)
     */

    @Override
    public final void addTarget(String logName,
            IFrameableTarget<String, TimeSeparator> target) throws AdeException {
        if (m_targets.containsKey(logName)) {
            throw new AdeInternalException("Duplicate log name " + logName);
        }
        final Target t = new Target();
        t.m_processor = target;
        t.m_lastMessageTime = 0;
        t.m_flushed = true;
        m_targets.put(logName, t);
    }

    /* (non-Javadoc)
     * @see org.openmainframe.ade.impl.flow.MultiLogStreamProcessor#run()
     */
    @Override
    public final void run() throws AdeException {
        long waitTime = m_waitTime;

        if (m_targets.size() == 0) {
            throw new AdeInternalException("No targets");
        }
        // tail -F with a single target does not produce the ==> logName <== lines, so we need
        // to set the currentTarget in advance
        if (m_targets.size() == 1) {
            m_currentTarget = m_targets.values().iterator().next();
        } else {
            m_currentTarget = null;
        }

        try {
            m_asyncLineReader.start();

            while (!m_stop) {

                final String line = m_asyncLineReader.readLine(waitTime);

                /** If line==null && eof => stop the loop
                 *  If line==null because of time-out => flush will surely take place, so we can start waiting for longer periods
                 *  If line!=null =>  the line is processed, and waitTime is set back to normal waiting period
                 */
                if (line == null) {
                    if (m_asyncLineReader.isEof()) {
                        break;
                    } else {
                        waitTime = m_longWaitTime;
                    }
                } else {
                    waitTime = m_waitTime;
                    processLine(line);
                }

                // Invariant:
                //  If line==null, it means we waited at least WAIT_TIME=2*FLUSH_TIME.
                //  It means all non-flushed targets will now be flushed.
                flushSelectedTargets();
            }
            // Send last empty line if exists
            if (m_lastLineEmpty && m_currentTarget != null) {
                m_currentTarget.addLine("");
            }
            // Last flush && eof
            flushSelectedTargets();
            for (Target t : m_targets.values()) {
                t.m_processor.endOfStream();
            }
        } catch (IOException ex) {
            throw new AdeInternalException("Failed reading from stream", ex);
        } finally {
            if (m_asyncLineReader != null) {
                m_asyncLineReader.close();
            }
        }
    }

    private void processLine(String line) throws AdeException {
        // Handle log switch lines
        // If last line was empty, it is ignored in this case
        if (line.startsWith("==> ") && line.endsWith(" <==")) {
            changeLogName(line);
            m_lastLineEmpty = false;
            return;
        }
        // Current target may be null if
        // (Unlikely) In multi-log file there are lines before the first log-switch line
        // (More likely) After a log-switch line to an unrecognized log
        if (m_currentTarget == null) {
            return;
        }

        // If previous line was empty, we send it now
        if (m_lastLineEmpty) {
            m_currentTarget.addLine("");
            m_lastLineEmpty = false;
        }

        // If line is empty, record but not send. Otherwise, send it to target
        if (line.length() == 0) {
            m_lastLineEmpty = true;
        } else {
            m_currentTarget.addLine(line);
        }
    }

    private void changeLogName(String line) {
        m_lastLogName = line.substring(4, line.length() - 4);
        m_currentTarget = m_targets.get(m_lastLogName);

        if (m_currentTarget == null) {
            logger.warn("Discarding message for unknown log " + m_lastLogName);
        }
    }

    private void flushSelectedTargets() throws AdeException {
        final long curTime = System.currentTimeMillis();

        if (curTime < m_nextGlobalFlushTime) {
            return;
        }
        m_nextGlobalFlushTime = curTime + m_flushTime;

        for (Target t : m_targets.values()) {
            t.flushIfNeeded(curTime);
        }
    }

    @Override
    public final void setStopFlag() {
        m_stop = true;
    }

}
