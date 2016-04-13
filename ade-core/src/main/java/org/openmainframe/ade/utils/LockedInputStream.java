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
package org.openmainframe.ade.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.openmainframe.ade.exceptions.AdeUsageException;

/**
 * an input stream with fcntl type file locking.
 */
public class LockedInputStream extends InputStream {
    private RandomAccessFile m_raf;
    private FileLock m_lock;
    private String m_filename;

    /**
     * Opens an input stream and locks the underlying file.
     * Locking is done via the @java.nio.channels.FileLock class.
     * On windows, this is just like a normal input stream.  
     * On Linux and the likes it uses an "fcntl" type lock.
     * REMEMBER TO CLOSE THE INPUT STREAM.
     * @param filename to open
     * @throws FileNotFoundException
     * @throws AdeUsageException
     */
    public LockedInputStream(final String filename) throws FileNotFoundException, AdeUsageException {
        m_lock = null;
        m_filename = filename;
        // Get a file channel for the file
        m_raf = new RandomAccessFile(filename, "rw");
        FileChannel channel = m_raf.getChannel();

        // Use the file channel to create a lock on the file.
        // This method blocks until it can retrieve the lock.
        try {
            m_lock = channel.lock();
        } catch (IOException e) {
            throw new AdeUsageException("Failed locking " + filename, e);
        }

    }

    /**
     * close and unlock the file
     */
    public void unlockAndClose() throws AdeUsageException {
        try {
            if (m_lock != null) {
                m_lock.release();
                m_lock = null;
            }
            if (m_raf != null) {
                m_raf.close();
                m_raf = null;
            }
        } catch (IOException e) {
            throw new AdeUsageException("Failed unlocking " + m_filename, e);
        }
    }

    @Override
    public int read() throws IOException {
        return (m_raf.read());
    }

}
