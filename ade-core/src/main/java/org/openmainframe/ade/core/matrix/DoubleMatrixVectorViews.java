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

class DoubleMatrixVectorViews extends AbstractDoubleVector implements IDoubleVector {
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    private IDoubleMatrix mMatrix;
    private int mRow0;
    private int mCol0;
    private int mDeltaRow;
    private int mDeltaCol;
    private int mLength;

    DoubleMatrixVectorViews(IDoubleMatrix mat, int row0, int col0, int deltaRow, int deltaCol, int length) {
        mMatrix = mat;
        mRow0 = row0;
        mCol0 = col0;
        mDeltaRow = deltaRow;
        mDeltaCol = deltaCol;
        mLength = length;
    }

    @Override
    public double get(int i) {
        return mMatrix.get(mRow0 + mDeltaRow * i, mCol0 + mDeltaCol * i);
    }

    @Override
    public int getLength() {
        return mLength;
    }

    @Override
    public void set(int i, double val) {
        mMatrix.set(mRow0 + mDeltaRow * i, mCol0 + mDeltaCol * i, val);
    }

    @Override
    public String toString() {
        return "DoubleMatrixVectorViews " + VectorIO.toString(this);
    }

}
