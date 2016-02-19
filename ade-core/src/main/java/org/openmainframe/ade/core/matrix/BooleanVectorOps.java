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
package org.openmainframe.ade.core.matrix;

/**
 * A read-only vector with logical entries, that can be used to represent
 * a subset of entries in a vector, or of columns in a matrix.
 * 
 * The method get() returns true if an index belongs to the subset
 * The method getLength() returns the length of the underlying vector
 * find() returns an IntVector containing the indices, conversely
 * getViewOnIndices(int...) returns a LogicalVectorView representing the given indices
 * 
 * The basic boolean operators are implemented as methods:
 * result = inverse()  returns a LogicalVectorView satisfying result.get(i)==!get(i)
 * result = or(other)  returns a LogicalVectorView satisfying 
 *                     result.get(i)==get(i) || other.get(i)                             
 * result = and(other) returns a LogicalVectorView satisfying 
 *                     result.get(i)==get(i) && other.get(i)
 * get(i) returns false if i>=getLength()
 * or(other).getLength equals max{getLength(), other.getLength()}
 * and(other).getLength equals min{getLength(), other.getLength()}                    
 * 
 * For concretions of this abstract class see the class VectorIndexPredicate
 * 
 * 
 */
public final class BooleanVectorOps {
    
    private BooleanVectorOps() {
        // Private constructor to hide the implicit public one.
    }

    public static IIntVector find(final IBooleanVector arg) {
        final int[] result = new int[count(arg)];
        int size = 0;
        for (int i = 0; i < arg.getLength(); i++) {
            if (arg.get(i)) {
                result[size++] = i;
            }
        }
        return IntVector.wrapArray(result);
    }

    public static int count(final IBooleanVector arg) {
        int result = 0;
        for (int i = 0; i < arg.getLength(); i++) {
            if (arg.get(i)) {
                ++result;
            }
        }
        return result;
    }

}
