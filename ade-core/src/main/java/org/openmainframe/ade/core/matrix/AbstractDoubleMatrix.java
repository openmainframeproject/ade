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

public abstract class AbstractDoubleMatrix extends AbstractDoubleVector implements IDoubleMatrix {

    @Override
    public IDoubleVector getCol(int i) {
        return DoubleMatrixViews.column(this, i);
    }

    @Override
    public IDoubleVector getRow(int i) {
        return DoubleMatrixViews.row(this, i);
    }

    @Override
    public double get(int i) {
        return get(i % getRowNum(), i / getRowNum());
    }

    @Override
    public int getLength() {
        return getRowNum() * getColNum();
    }

    @Override
    public void set(int i, double val) {
        set(i % getRowNum(), i / getRowNum(), val);
    }

    @Override
    public String toString() {
        String className = getClass().getName();
        className = className.substring(className.lastIndexOf(".") + 1);
        return className + "\n" + MatrixIO.toString(this);
    }

    @Override
    public double[][] get2dArray() {
        final double[][] res = new double[getRowNum()][getColNum()];
        for (int i = 0; i < getRowNum(); ++i) {
            for (int j = 0; j < getColNum(); ++j) {
                res[i][j] = get(i, j);
            }
        }
        return res;
    }
}
