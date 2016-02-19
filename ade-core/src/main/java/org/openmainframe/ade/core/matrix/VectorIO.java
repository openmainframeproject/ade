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
package org.openmainframe.ade.core.matrix;

public final class VectorIO {
    
    private VectorIO() {
        // Private constructor to hide the implicit public one.
    }
    
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    public static void print(IDoubleVector vec) {
        System.out.printf("Vector %d\n", vec.getLength());
        for (int j = 0; j < vec.getLength(); j++) {
            System.out.format("%6.2f ", vec.get(j));
        }
        System.out.println();
    }

    public static String toString(IDoubleVector vec) {

        final StringBuilder res = new StringBuilder();
        for (int i = 0; i < vec.getLength(); ++i) {
            if (i > 0) {
                res.append(" ");
            }
            // Michal & Tamar: added this after some problem with BDM printing			
            res.append(String.format("%.8f", vec.get(i)));
        }
        return res.toString();

    }

    public static String toString(IIntVector vec) {
        final StringBuilder res = new StringBuilder();
        for (int i = 0; i < vec.getLength(); ++i) {
            if (i > 0) {
                res.append(" ");
            }
            res.append(vec.get(i));
        }
        return res.toString();
    }
}
