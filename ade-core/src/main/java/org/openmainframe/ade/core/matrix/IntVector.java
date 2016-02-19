/*
 
    Copyright IBM Corp. 2016
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

import java.io.Serializable;
import java.util.Arrays;

public class IntVector implements IIntVector, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    private int[] mValues;

    private IntVector() {
    }

    /** Create an all zero vector of specified length */
    public IntVector(int len) {
        mValues = new int[len];
    }

    /** Create a vector with specified values */
    public IntVector(int... vals) {
        mValues = vals.clone();
    }

    /** Creates a copy of the given vector */
    public IntVector(IIntVector vec) {
        mValues = new int[vec.getLength()];
        for (int i = 0; i < vec.getLength(); ++i) {
            mValues[i] = vec.get(i);
        }
    }

    @Override
    public int get(int i) {
        return mValues[i];
    }

    @Override
    public void set(int i, int val) {
        mValues[i] = val;
    }

    @Override
    public int getLength() {
        return mValues.length;
    }

    @Override
    public void setAll(int val) {
        Arrays.fill(mValues, val);
    }

    /** Creates a vector with the given values without another memory allocation.
     *  The vector will use the given array object as is for storing the data, therefore
     *  any changes to the vector will affect the array and vice versa. 
     */
    public static IntVector wrapArray(int[] vals) {
        final IntVector v = new IntVector();
        v.mValues = vals;
        return v;
    }

    @Override
    public String toString() {
        return "IntVector " + VectorIO.toString(this);
    }

    /**
     * This returns a reference to the underlying array; subsequent changes
     * to it will also affect the vector. 
     * 
     * @return the vector as a double array
     */
    public int[] getArray() {
        return mValues;
    }
}
