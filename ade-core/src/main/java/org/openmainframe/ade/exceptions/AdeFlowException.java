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
package org.openmainframe.ade.exceptions;

import org.openmainframe.ade.flow.IStreamSource;
import org.openmainframe.ade.flow.IStreamTarget;

/**
 * An exception signaling a bug in the flow, meaning that the
 * 'contract' between the {@link IStreamSource} and the {@link IStreamTarget}
 * has been violated (e.g. a call to {@link IStreamTarget#incomingObject(Object)}
 * without a preceding call to {@link IStreamTarget#beginOfStream()}.
 */
public class AdeFlowException extends AdeException {

    private static final long serialVersionUID = 1L;

    public AdeFlowException(String message) {
        super(message);
    }

    public AdeFlowException(String message, Throwable rootCause) {
        super(message, rootCause);
    }
}