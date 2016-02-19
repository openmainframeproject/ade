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

import java.util.Arrays;

/** A vector of double values */
public class DoubleVector extends AbstractDoubleVector {
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    private double[] mValues;

    private DoubleVector() {
    }

    /** Create an all zero vector of specified length */
    public DoubleVector(int len) {
        mValues = new double[len];
    }

    /** Create a vector with specified values */
    public DoubleVector(double... vals) {
        mValues = vals.clone();
    }

    /** Creates a copy of the given vector */
    public DoubleVector(IDoubleVector vec) {
        mValues = new double[vec.getLength()];
        for (int i = 0; i < vec.getLength(); ++i) {
            mValues[i] = vec.get(i);
        }
    }

    @Override
    public double get(int i) {
        return mValues[i];
    }

    /** Return the range from (inclusive) to (exclusive) */
    public double[] getRange(int from, int to) {
        return Arrays.copyOfRange(mValues, from, to);
    }

    @Override
    public void set(int i, double val) {
        mValues[i] = val;
    }

    @Override
    public int getLength() {
        return mValues.length;
    }

    @Override
    public void setAll(double val) {
        Arrays.fill(mValues, val);
    }

    /** Creates a vector with the given values without another memory allocation.
     *  The vector will use the given array object as is for storing the data, therefore
     *  any changes to the vector will affect the array and vice versa. 
     */
    public static DoubleVector wrapArray(double[] vals) {
        final DoubleVector v = new DoubleVector();
        v.mValues = vals;
        return v;
    }

    @Override
    public String toString() {
        return "DoubleVector " + VectorIO.toString(this);
    }

    /**
     * This returns a reference to the underlying array; subsequent changes
     * to it will also affect the vector. 
     * 
     * @return the vector as a double array
     */
    public double[] getArray() {
        return mValues;
    }

}
