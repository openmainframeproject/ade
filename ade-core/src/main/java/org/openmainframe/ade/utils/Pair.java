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
package org.openmainframe.ade.utils;

import java.io.Serializable;

/** A generic pair of objects */
public class Pair<S, T> implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public S m_first;
    public T m_second;

    public Pair() {
        m_first = null;
        m_second = null;

    }

    public Pair(S s, T t) {
        m_first = s;
        m_second = t;
    }
}
