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

/** Interface to a double matrix object.
 *  A matrix can be accesses as a vector as well using the convention that the first index 
 *  is running first.  
 *
 */
public interface IDoubleMatrix extends IDoubleVector {

    /** Get the element at (i,j) */
    double get(int i, int j);

    /** Set the element at (i,j) to val */
    void set(int i, int j, double val);

    /** Returns number of rows (first dimension) */
    int getRowNum();

    /** Returns number of columns (second dimension) */
    int getColNum();

    /** Returns the i'th row as a vector. The resulting interface can used to read or 
     * write to the row which will update the matrix as well.
     */
    IDoubleVector getRow(int i);

    /** Returns the i'th column as a vector. The resulting interface can used to read or 
     * write to the column which will update the matrix as well.
     */
    IDoubleVector getCol(int i);

    /** Returns a 2d-array representation of the matrix.
     * NOTE: this may be or may be not a reference to the actual data of the matrix
     */
    double[][] get2dArray();

}
