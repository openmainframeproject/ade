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
package org.openmainframe.ade.impl.dataStore;

import java.util.LinkedHashMap;

public class LruCachedMap<T1, T2> extends LinkedHashMap<T1, T2> {

    private static final long serialVersionUID = -5089918216578324138L;
    private int m_cacheMaxSize;

    /**
     * Construct a new LruCachedMap.
     * 
     * @param cacheMaxSize the maximum size for the cache.
     */
    public LruCachedMap(int cacheMaxSize) {
        super();
        m_cacheMaxSize = cacheMaxSize;
    }

    @Override
    protected final boolean removeEldestEntry(java.util.Map.Entry<T1, T2> eldest) {
        return size() > m_cacheMaxSize;
    }

}
