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
package org.openmainframe.ade.core.matrix;

/** An interface to a vector of doubles */
public interface IDoubleVector {

    /** Returns the i'th element */
    double get(int i);

    /** Sets the i'th element to val */
    void set(int i, double val);

    /** Returns length of vector */
    int getLength();

    /** Sets all vector elements to val */
    void setAll(double val);

    /** Sums values. If one value is nan, returns nan */
    double sum();

    /** Normalizes vector. If one value is nan, does nothing. If sum is zero, sets all values to nan 
     * @return original sum*/
    double normalize();

    double max();

    double min();

    double mean();

    double var();

    double std();

    void normalizeScore();

}
