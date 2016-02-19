/*
 
    Copyright IBM Corp. 2011, 2016
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
package org.openmainframe.ade.data;

import java.io.Serializable;

import org.openmainframe.ade.exceptions.AdeException;

/** Represents a source.
 *  A Source is a high-level abstract entity that produces log-messages.
 *  Currently each source is managed independently.
 *
 */
public interface ISource extends Comparable<ISource>, Serializable {

    /**
     * Returns an id internally allocated by Ade
     * @return source internal id
     */
    int getSourceInternalId();

    /**
     * Returns user-defined source id
     * @return source-id
     * @throws AdeException 
     */
    String getSourceId() throws AdeException;

}
