/*
 
    Copyright IBM Corp. 2012, 2016
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

/**
 * A class that may have to repeat some functionality, and can notify
 * the client (parent process) of the required iteration type.
 * @param <T> The type returned by the {@link #getRequiredIterationType()}
 * method which indicates the type of the required iteration
 */
public interface IMultipleIterationsTarget<T> {
    /**
     * @return <code>true</code> if another iteration is required. The
     * type of the required iteration can be queried by {@link #getRequiredIterationType()}
     */
    public boolean requiresAnotherIteration() throws AdeException;

    /**
     * @return The type of the required iteration
     * @throws AdeException
     */
    public T getRequiredIterationType() throws AdeException;
}
