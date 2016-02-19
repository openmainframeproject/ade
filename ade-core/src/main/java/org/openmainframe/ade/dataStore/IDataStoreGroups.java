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
 * An interface for manipulating the GROUPS table.
 */
public interface IDataStoreGroups {
    
    /**
     * Returns the group internal id of the unassigned group by either using an existing unassigned group 
     * internal id or by inserting a new unassigned group.
     * @param analysisGroup this is the group id. For this particular method it should be the unofficial group 
     * id for unassigned groups.
     * @return the group internal id of the unassigned group.
     */
    int insertUnassignedGroup(int analysisGroup) throws AdeException;

}
