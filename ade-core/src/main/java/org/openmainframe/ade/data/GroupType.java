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
package org.openmainframe.ade.data;

import java.util.HashMap;
import java.util.Map;

import org.openmainframe.ade.exceptions.AdeInternalException;

/**
 * Enum class that contains the supported group types. 
 * 
 * The valid group types are:
 *   MODELGROUPS
 */
public enum GroupType {
    MODELGROUPS(1);
    
    private final int value;
    
    /**
     * A map between the group type short value and its enum type. 
     */
    private static final Map<Integer,GroupType> intToGroupType = new HashMap<Integer,GroupType>(1,1.01f);
    
    static {
        for (GroupType type : GroupType.values()) {
            intToGroupType.put(type.value, type);
        }
    }
    
    private GroupType(int value){
        this.value = value;
    }
    
    public int getValue(){
        return value;
    }
    /**
     * Uses the mapping between the group type short value and the enum groupType to retrieve the enum GroupType. 
     * @throws AdeInternalException
     */
    public static GroupType getGroupType(int i) throws AdeInternalException{
        GroupType type = intToGroupType.get(i);
        if (type == null){
            throw new AdeInternalException("No matching group type for value " + i);
        }
        return type;
    }
}
