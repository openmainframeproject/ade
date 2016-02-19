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

public final class DoubleMatrixViews {
    
    private DoubleMatrixViews() {
        // Private constructor to hide the implicit public one. 
    }
    
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    static public IDoubleVector row(IDoubleMatrix mat, int rowNum) {
        return new DoubleMatrixVectorViews(mat, rowNum, 0, 0, 1, mat.getColNum());
    }

    static public IDoubleVector column(IDoubleMatrix mat, int colNum) {
        return new DoubleMatrixVectorViews(mat, 0, colNum, 1, 0, mat.getRowNum());
    }

    static public IDoubleVector diagonal(IDoubleMatrix mat) {
        return new DoubleMatrixVectorViews(mat, 0, 0, 1, 1, Math.min(mat.getRowNum(), mat.getColNum()));
    }

    static public IDoubleMatrix selectRows(IDoubleMatrix mat, int... rows) {
        return new DoubleMatrixSelectView(mat, rows, null);
    }

    static public IDoubleMatrix selectCols(IDoubleMatrix mat, int... cols) {
        return new DoubleMatrixSelectView(mat, null, cols);
    }

    static public IDoubleMatrix selectRowsAndCols(IDoubleMatrix mat, int[] rows, int[] cols) {
        return new DoubleMatrixSelectView(mat, rows, cols);
    }

    /**
     * returns a view on the transpose; note that subsequent calls of set()
     * affect the original matrix, for a transposed copy use DoubleMatrixOps.transpose
     * @param mat the original matrix
     * @return transposed view on the matrix
     */
    static public IDoubleMatrix transpose(final IDoubleMatrix mat) {
        return new AbstractDoubleMatrix() {
            @Override
            public double get(int i, int j) {
                return mat.get(j, i);
            }

            @Override
            public int getColNum() {
                return mat.getRowNum();
            }

            @Override
            public int getRowNum() {
                return mat.getColNum();
            }

            @Override
            public void set(int i, int j, double val) {
                mat.set(j, i, val);
            }
        };
    }
}
