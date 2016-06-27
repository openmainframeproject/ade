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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** AsyncLineReader implementation using a helper thread that fills a single-line buffer */
public class AsyncLineReaderThreadedImpl implements IAsyncLineReader {
    
    /** logger */
    private static final Logger logger = LoggerFactory.getLogger(AsyncLineReaderThreadedImpl.class);

    /** Underlying reader */
    private BufferedReader m_reader;
    /** A single line buffer */
    private String m_currentLine;
    /** Indicates whether the single line buffer m_currentLine is full */
    private boolean m_lineWaiting;
    /** Indicates whether an EOF has been reached */
    private boolean m_eof;
    /** The helper thread */
    private Thread m_thread;

    public AsyncLineReaderThreadedImpl(BufferedReader reader) {
        m_reader = reader;
        m_currentLine = null;
        m_eof = false;
        m_lineWaiting = false;
    }

    @Override
    public void start() {
        m_thread = new Thread(new Producer());
        m_thread.start();
    }

    @Override
    public void close() {
        final Thread t = m_thread;
        if (t != null) {
            // Setting m_thread to null indicates to the helper thread to stop
            m_thread = null;
            t.interrupt();
        }
    }

    synchronized private void put(String line) {
        // Wait for the line-buffer to be free.
        // If m_thread is null it signals an abort
        while (m_lineWaiting && m_thread != null) {
            try {
                wait();
            } catch (InterruptedException e) {
                logger.info("Caught InerruptedException", e);
            }
        }
        // Fill buffer with given line
        m_currentLine = line;
        m_lineWaiting = true;
        // Release main thread, if it was waiting for buffer to fill
        notifyAll();
    }

    /* (non-Javadoc)
     * @see org.openmainframe.ade.impl.flow.AsyncLineReader#readLine(long)
     */
    @Override
    synchronized public String readLine(long waitTime) {
        // Wait for the buffer to fill, or at most waitTime milliseconds
        // m_thread==null indicates abort
        long curTime = System.currentTimeMillis();
        final long endTime = curTime + waitTime;
        while (!m_lineWaiting && curTime < endTime && m_thread != null) {
            try {
                wait(waitTime);
                curTime = System.currentTimeMillis();
            } catch (InterruptedException e) {
                logger.info("Caught InerruptedException", e);
            }
        }
        // Return whatever is in the buffer, even if null.
        final String res = m_currentLine;
        // Release buffer
        m_lineWaiting = false;
        // Notify helper thread buffer is empty
        notifyAll();

        return res;
    }

    /* (non-Javadoc)
     * @see org.openmainframe.ade.impl.flow.AsyncLineReader#isEof()
     */
    @Override
    public boolean isEof() {
        return m_eof;
    }

    private class Producer implements Runnable {
        @Override
        public void run() {

            String line;
            try {
                // Put all lines in buffer sequentially
                // m_thread indicates abort
                // NOTE: call to readLine() is blocking and not release when interrupted. This may keep
                // The thread running when exceptions occur
                while (m_thread != null && (line = m_reader.readLine()) != null) {
                    put(line);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed reading from input", e);
            }
            // If not aborted, put in the before a null indicating eof to reader.
            if (m_thread != null) {
                m_eof = true;
                put(null);
            }
        }
    }

}
