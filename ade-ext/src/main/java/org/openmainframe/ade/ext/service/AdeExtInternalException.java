/*
 
    Copyright IBM Corp. 2015, 2016
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

package org.openmainframe.ade.ext.service;

import org.openmainframe.ade.exceptions.AdeInternalException;

/**
 * This is an Ade Ext internal exception.
 * This exception class extends from Ade. There are two advantage of doing
 * this:
 * <ul> 
 * <li>Flexibility to change how exception is handled - perhaps, add the
 * exception message to the logger. 
 * <li>Reduce dependency from AdeCore. If
 * adeCore change their exception structure, all AdeExt need is to change
 * the super class used.
 * </ul>
 */
public class AdeExtInternalException extends AdeInternalException {

    /**
     * Serialized ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor inputting the message to appear with the exception.
     * 
     * @param message - The message you want to appear with the exception
     */
    public AdeExtInternalException(String message) {
        super(message);
    }

    /**
     * Constructor inputting message and rootCause to appear with the exception.
     * 
     * @param   message    The message you want to appear with the exception
     * @throws  rootCause  The root cause of this exception 
     */
    public AdeExtInternalException(String message, Throwable rootCause) {
        super(message, rootCause);
    }
}
