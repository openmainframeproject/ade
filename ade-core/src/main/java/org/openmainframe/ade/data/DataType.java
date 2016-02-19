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
package org.openmainframe.ade.data;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.openmainframe.ade.exceptions.AdeInternalException;

/**
 * Defines available data types for Ade analytics and
 * the corresponding filesystem path.
 * 
 * Valid data types are:
 *   SYSLOG
 */
public enum DataType {
    SYSLOG("SYSLOG", "syslog",1);

    private String type;
    private String path;
    private final short value;
    
    /**
     * A map between the data type short value and its enum type. 
     */
    private static final Map<Short,DataType> shortToDataType = new HashMap<Short,DataType>();
    
    static {
        for (DataType type : DataType.values()) {
            shortToDataType.put(type.value, type);
        }
    }
    
    /**
     * A lenient (ie. case-insensitive) version of valueOf().
     * @param name
     * @return
     */
    public static DataType fromString(String name) {
        return DataType.valueOf(name.toUpperCase(Locale.ENGLISH));
    }

    private DataType(String type, String path, int value) {
        this.type = type;
        this.path = path;
        this.value = (short) value;
    }
    
    public short getValue(){
        return value;
    }
    

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }
    
    /**
     * Uses the mapping between the data type short value and the enum dataType to retrieve the enum dataType.
     * @throws AdeInternalException
     */
    public static DataType getDataType(short i) throws AdeInternalException{
        DataType type = shortToDataType.get(i);
        if (type == null){
            throw new AdeInternalException("No matching data type for value " + i);
        }
        return type;
    }
}