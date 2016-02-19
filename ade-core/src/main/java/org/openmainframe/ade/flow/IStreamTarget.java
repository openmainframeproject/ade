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
import org.openmainframe.ade.exceptions.AdeFlowException;

/**
 * Interface for streaming-based processing of data. 
 * A {@link IStreamTarget} expects the following sequence of calls:</br>
 * <ol>
 * 		<li>a single call to {@link #beginOfStream()}</li>
 * 		<li>zero or more consecutive calls to {@link #incomingObject(Object)}</li>
 * 		<li>a single call to {@link #endOfStream()}</li>
 * </ol>
 * Generally speaking, the sequence above can be repeated more than once. It is up to the
 * implementation (of both the {@link IStreamTarget}&lt;OBJ&gt; and the {@link IStreamSource}&lt;OBJ&gt;)
 * to agree whether this is allowed.
 * However, it is forbidden to stray from the above described sequence (e.g. you should never have 2
 * calls to {@link #beginOfStream()} without a call to {@link #endOfStream()} between them). 
 *
 * @param <T> class of data objects provided to the {@link #incomingObject(&lt;OBJ&gt;))} method
 */
public interface IStreamTarget<T> {

    /**
     * Takes initiation actions to prepare for a new stream of objects of type &lt;OBJ&gt;.
     * @see {@link IStreamTarget}
     * @throws AdeFlowException if not in a state allowing initiation (e.g. 2 calls to 
     * {@link #beginOfStream()} without a call to {@link #endOfStream()} between them)
     * @throws AdeException if initiation failed due to other reasons  
     */
    public void beginOfStream() throws AdeException, AdeFlowException;

    /**
    * Adds an object to this {@link IStreamTarget}.
    * @see {@link IStreamTarget}
    * @param obj the incoming data object.
    * @throws AdeFlowException if the flow 'contract' was violated (e.g. {@link #incomingObject(Object)}
    * was called before {@link #beginOfStream()}) 
    * @throws AdeException if object insertion failed due to other reasons (e.g. I/O error)
    */
    public void incomingObject(T obj) throws AdeException, AdeFlowException;

    /**
     * Marks an end of a data objects stream.
     * @see {@link IStreamTarget}
     * @throws AdeFlowException if not in a state allowing a call to {@link #endOfStream()} (e.g. with
     * no preceding call to {@link #beginOfStream()})
     * @throws AdeException if initiation failed due to other reasons
     */
    public void endOfStream() throws AdeException, AdeFlowException;

}
