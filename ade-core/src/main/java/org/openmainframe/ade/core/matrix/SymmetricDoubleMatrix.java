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

import java.util.Arrays;

/**
 * An implementation of a symmetric matrix.
 */
public class SymmetricDoubleMatrix extends AbstractDoubleMatrix {
    private double[][] mValues;
    private int mNumRows;

    /**
     * Creates a symmetric matrix with n rows and columns.
     * @param n
     */
    public SymmetricDoubleMatrix(int n) {
        mNumRows = n;
        mValues = new double[n][];
        for (int i = 0; i < n; i++) {
            mValues[i] = new double[i + 1];
        }
    }

    @Override
    public final int getLength() {
        return mNumRows * mNumRows;
    }

    @Override
    public final void setAll(double val) {
        for (double[] row : mValues) {
            Arrays.fill(row, val);
        }
    }

    @Override
    public final double get(int i, int j) {
        if (j > i) {
            return get(j, i);
        }
        return mValues[i][j];
    }

    @Override
    public final void set(int i, int j, double val) {
        if (j > i) {
            set(j, i, val);
        } else {
            mValues[i][j] = val;
        }
    }

    @Override
    public final int getRowNum() {
        return mNumRows;
    }

    @Override
    public final int getColNum() {
        return mNumRows;
    }
}
