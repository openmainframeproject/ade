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

import java.io.IOException;

import org.openmainframe.ade.exceptions.AdeException;

public interface IAsyncLineReader {

    /** Starts helper threads.
     * NOTE: it's very important to call close() afterwards. Use a finally clause if necessary. */
    void start() throws AdeException;

    /** Reads the next line. If no line is avaialable, give up after given amount of milliseconds.
     * @return the next line, or null. Null can signify EOF or time-out. Use isEof() to figure out which.
     */
    String readLine(long waitTime) throws AdeException, IOException;

    /** Returns true if last call to readLine() returned NULL due to  EOF */
    boolean isEof();

    /** Closes object and all helper threads */
    void close() throws AdeException;
}