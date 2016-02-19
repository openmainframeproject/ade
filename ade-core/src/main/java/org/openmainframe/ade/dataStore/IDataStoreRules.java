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
package org.openmainframe.ade.dataStore;

import org.openmainframe.ade.exceptions.AdeException;


/**
 * Interface for manipulating the RULES table.
 */
public interface IDataStoreRules {

    /**
     * Inserts a new unassigned rule in the database. 
     * @return The rule internal id for the unassigned group rule.
     * @throws AdeException
     */
    int insertUnassignedRule() throws AdeException;
}
