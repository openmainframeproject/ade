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

/** Returns a view of a matrix with a selection of rows and columns 
 */
public class DoubleMatrixSelectView extends AbstractDoubleMatrix {
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    private IDoubleMatrix mMatrix;
    private int[] mSelectedRows;
    private int[] mSelectedColumns;

    public DoubleMatrixSelectView(IDoubleMatrix matrix, int[] rows, int[] cols) {
        mMatrix = matrix;
        mSelectedRows = rows;
        mSelectedColumns = cols;
    }

    @Override
    public double get(int i, int j) {
        return mMatrix.get(convertRow(i), convertCol(j));
    }

    @Override
    public int getColNum() {
        return mSelectedColumns != null ? mSelectedColumns.length : mMatrix.getColNum();
    }

    @Override
    public int getRowNum() {
        return mSelectedRows != null ? mSelectedRows.length : mMatrix.getRowNum();
    }

    @Override
    public void set(int i, int j, double val) {
        mMatrix.set(convertRow(i), convertCol(j), val);

    }

    private int convertRow(int i) {
        if (mSelectedRows == null) {
            return i;
        }
        DimAndSizesAsserts.assertLegalRowIndex(i, mSelectedRows.length);
        return mSelectedRows[i];
    }

    @Override
    public String toString() {
        return "DoubleMatrixSelectViews\n" + MatrixIO.toString(this);
    }

    private int convertCol(int j) {
        if (mSelectedColumns == null) {
            return j;
        }
        DimAndSizesAsserts.assertLegalColIndex(j, mSelectedColumns.length);
        return mSelectedColumns[j];
    }
}
