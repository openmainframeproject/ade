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

import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.impl.data.TimeSeparator;

/**
 * Extends {@link IFrameableTarget}&lt;{@link IMessageInstance},{@link TimeSeparator}&gt;.<br/>
 * And object implementing this interface expects the following sequence
 * of method invocations:
 * <ol>
 *  <li>{@link #beginOfStream()}</li>
 *  <li>zero or more of the following sequences:
 *    <ul>
 *      <li>{@link #incomingSeparator(TimeSeparator)}</li>
 *      <li> zero or more {@link #incomingObject(IMessageInstance)}</li>
 *    </ul>
 *  </li>
 *  <li>{@link #endOfStream()}</li>
 * </ol>
 * Where an incoming {@link IMessageInstance} object is related to the nearest
 * preceding (invoked before) incoming {@link TimeSeparator} object.
 */
public interface IMessageInstanceTarget extends
        IFrameableTarget<IMessageInstance, TimeSeparator> {

}
