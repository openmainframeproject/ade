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

import java.io.Serializable;
import java.util.Arrays;

import org.openmainframe.ade.core.exceptions.AdeCoreIllegalArgumentException;

/** A matrix of doubles
 */
public class DoubleMatrix extends AbstractDoubleMatrix implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private double[][] mValues;
    private int mNumCols;

    public enum Dimension {
        ROW, COL
    };

    /** Create an all zero matrix with m rows and n columns */
    public DoubleMatrix(int m, int n) {
        mValues = new double[m][n];
        mNumCols = n;
    }

    /** Create a matrix with m rows and n columns with the specified values.
     *  values.length is expected to be m*n
     *  The matrix is initialized by running the row index first, e.g., values[0..m-1] initialize the first column,
     *  values[m..2m-1] the second column etc...  
     *   */
    public DoubleMatrix(int m, int n, double... values) {
        this(m, n);
        this.setMatrix(Dimension.ROW, values);
    }

    /** Create a matrix with m rows and n columns with the specified values.
     *  values.length is expected to be m*n
     *  if dim==1 The matrix is initialized by running the row index first,
     *  e.g., values[0..m-1] initialize the first column, values[m..2m-1] the second column etc
     *  if dim==2 The matrix is initialized by running the column index first,
     *   */
    public DoubleMatrix(int m, int n, Dimension dim, double... values) {
        this(m, n);
        this.setMatrix(dim, values);
    }

    /** Create a copy of the given matrix */
    public DoubleMatrix(IDoubleMatrix matrix) {
        this(matrix.getRowNum(), matrix.getColNum());
        for (int i = 0; i < getRowNum(); ++i) {
            for (int j = 0; j < mNumCols; ++j) {
                mValues[i][j] = matrix.get(i, j);
            }
        }
    }

    public DoubleMatrix(DoubleMatrix matrix) {
        mValues = cloneArray(matrix.mValues);
        mNumCols = matrix.mNumCols;
    }

    private DoubleMatrix() {
    }

    public static DoubleMatrix wrapArray(double[][] values) {
        final DoubleMatrix result = new DoubleMatrix();
        result.mValues = values;
        result.mNumCols = values[0].length;
        return result;
    }

    public static DoubleMatrix copyArray(double[][] values) {
        final DoubleMatrix result = new DoubleMatrix();
        result.mValues = cloneArray(values);
        result.mNumCols = values[0].length;
        return result;
    }

    public void setMatrix(Dimension dim, double... values) {
        final int m = getRowNum();
        final int n = getColNum();
        if (values != null) {
            if (values.length != m * n) {
                throw new AdeCoreIllegalArgumentException("Expecting intialization of " + (m * n) + " elements. Received " + values.length + " elements");
            }
            int counter = 0;
            switch (dim) {
                case ROW:
                    for (int j = 0; j < n; ++j) {
                        for (int i = 0; i < m; ++i) {
                            mValues[i][j] = values[counter++];
                        }
                    }
                    break;
                case COL:
                    for (int i = 0; i < m; ++i) {
                        for (int j = 0; j < n; ++j) {
                            mValues[i][j] = values[counter++];
                        }
                    }
                    break;
                }
        }

    }

    public void appendCol(DoubleVector v) {
        addCol(v, mNumCols);
    }

    public void prependCol(DoubleVector v) {
        addCol(v, 0);
    }

    public void addCol(DoubleVector v, int pos) {

        final int n = mValues.length;
        final double[][] values2 = new double[n][mNumCols + 1];

        for (int i = 0; i < n; ++i) {

            System.arraycopy(mValues[i], 0, values2[i], 0, pos);
            values2[i][pos] = v.get(i);
            System.arraycopy(mValues[i], pos, values2[i], pos + 1, mNumCols - pos);
        }

        mValues = values2;
        mNumCols = mNumCols + 1;
    }

    public void appendCols(DoubleMatrix mat) {
        final int n = mValues.length;
        if (mat.mValues.length != n) {
            throw new AdeCoreIllegalArgumentException("Rows dimension differes between two matrices");
        }
        final double[][] values2 = new double[n][mNumCols + mat.mNumCols];
        for (int i = 0; i < n; ++i) {
            System.arraycopy(mValues[i], 0, values2[i], 0, mNumCols);
            System.arraycopy(mat.mValues[i], 0, values2[i], mNumCols, mat.mNumCols);
        }
        mValues = values2;
        mNumCols = mNumCols + mat.mNumCols;
    }

    @Override
    public double get(int i, int j) {
        return mValues[i][j];
    }

    @Override
    public int getColNum() {
        return mNumCols;
    }

    @Override
    public int getRowNum() {
        return mValues.length;
    }

    @Override
    public void set(int i, int j, double val) {
        mValues[i][j] = val;
    }

    @Override
    public void setAll(double val) {
        for (int i = 0; i < getRowNum(); ++i) {
            Arrays.fill(mValues[i], val);
        }

    }

    @Override
    public double[][] get2dArray() {
        return mValues;
    }

    @Override
    public IDoubleVector getRow(int i) {
        return DoubleVector.wrapArray(mValues[i]);
    }

    @Override
    public String toString() {
        return "DoubleMatrix\n" + MatrixIO.toString(this);
    }

    static public double[][] cloneArray(double[][] orig) {
        final double[][] result = new double[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            result[i] = orig[i].clone();
        }
        return result;
    }

}
