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
package org.openmainframe.ade.impl.utils;

import java.util.ArrayList;

public class MathUtils {

    public static final int INIT_TBL_CAPACITY = 1000;

    private ArrayList<Double> m_logFactTbl;
    private int m_maxFactComputed;

    public MathUtils() {
        m_logFactTbl = new ArrayList<>(INIT_TBL_CAPACITY);
        m_logFactTbl.add(0, new Double(0));
        m_maxFactComputed = 0;
    }

    /**
     * Computes log factorials.
     * 
     * @param n 
     * @return log(n!)
     */
    public double computeLogFactorial(int n) {
        //compute log factorials of values that we don't have
        if (n > m_maxFactComputed) { 
            for (int i = m_maxFactComputed + 1; i <= n; i++) {
                m_logFactTbl.add(m_logFactTbl.get(i - 1) + Math.log(i));
            }
            m_maxFactComputed = n;
        }

        return m_logFactTbl.get(n);
    }
}
