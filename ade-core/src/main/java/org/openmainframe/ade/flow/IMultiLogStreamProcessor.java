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
package org.openmainframe.ade.flow;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.data.TimeSeparator;

/** 
 * 
 * An interface for reading lines from a stream of mixed sources, in the format
 * Resulting from tail -F .. -F .. -F ..
 * 
 * The lines are multiplexed to given named targets
 */
public interface IMultiLogStreamProcessor {

    public static final String FLUSH = "flush";

    /** Set time in which no-activity will result with flushing the line-processors.
     * The actual time in which the flush occur may be up to 2-3 times the given amount.
     */
    void setFlushTime(long flushTime);

    /** Add a target for read lines. Lines will be served to the given block if preceeded with
     * ==> logName <==
     * @throws AdeException 
     */
    void addTarget(String logName, IFrameableTarget<String, TimeSeparator> target)
            throws AdeException;

    /** Start reading lines. This method blocks until all lines are read 
    */
    void run() throws AdeException;

    /** Turn a flag indicating the processor to stop sending lines. It may be a while before it actually stops */
    void setStopFlag();

}