/*
 
    Copyright IBM Corp. 2009, 2016
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
package org.openmainframe.ade.impl.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Used to map between a <b>String</b>  an a unique numerical ID. 
 *
 */
public class NumStringMap {
    private SortedMap<Integer, String> m_numToStringMap;
    private Map<String, Integer> m_stringToNumMap;
    public static final int InvalidID = -1;

    public NumStringMap() {
        super();
        m_numToStringMap = new TreeMap<>();
        m_stringToNumMap = new HashMap<>();
    }

    public NumStringMap(Set<Entry<Integer, String>> entrySet) {
        this();
        if (!entrySet.isEmpty()) {
            for (Entry<Integer, String> entry : entrySet) {
                m_numToStringMap.put(entry.getKey(), entry.getValue());
                m_stringToNumMap.put(entry.getValue(), entry.getKey());
            }
        }

    }

    /**
     * Associates the specified messageID with the specified message in this map 
     * @param message the message text to be retrieved
     * @param messageID the id of the message
     */
    public final void put(String message, int messageID) {
        m_stringToNumMap.put(message, messageID);
        m_numToStringMap.put(messageID, message);
    }

    /**
     * Returns the number of Elements in the data store
     * @return number of elements
     */
    public final int getMappingCount() {
        return m_numToStringMap.size();
    }

    public final String getStringFromID(int id) {
        if (m_numToStringMap.containsKey(id)) {
            return m_numToStringMap.get(id);
        }
        return null;
    }

    public final int getIDFromString(String str) {
        if (m_stringToNumMap.containsKey(str)) {
            return m_stringToNumMap.get(str);
        }
        return InvalidID;
    }

    public final void removeEntry(int id) {
        if (m_numToStringMap.containsKey(id)) {
            m_stringToNumMap.remove(m_numToStringMap.get(id));
            m_numToStringMap.remove(id);
        }
    }

    public final Set<Integer> getIds() {
        return m_numToStringMap.keySet();
    }

    public final Set<String> getWords() {
        return m_stringToNumMap.keySet();
    }

    public final void clear() {
        m_numToStringMap.clear();
        m_stringToNumMap.clear();
    }

}
