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

import org.openmainframe.ade.core.exceptions.AdeCoreIllegalArgumentException;

/**
 * A collection of various matrix operators.
 */
public final class DoubleMatrixOps implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private DoubleMatrixOps() {
        // Private constructor to hide the implicit public one.
    }

    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    /** Sum over the first dimension */
    static public IDoubleVector colSums(IDoubleMatrix matrix) {
        final double[] vals = new double[matrix.getColNum()];
        for (int i = 0; i < matrix.getRowNum(); ++i) {
            for (int j = 0; j < matrix.getColNum(); ++j) {
                vals[j] += matrix.get(i, j);
            }
        }
        return DoubleVector.wrapArray(vals);
    }

    /* Sum over the second dimension */
    static public IDoubleVector rowSums(IDoubleMatrix matrix) {
        final double[] vals = new double[matrix.getRowNum()];
        for (int i = 0; i < matrix.getRowNum(); ++i) {
            for (int j = 0; j < matrix.getColNum(); ++j) {
                vals[i] += matrix.get(i, j);
            }
        }
        return DoubleVector.wrapArray(vals);
    }

    /** Throws an exception if matrix is not square */
    static public void assertSquare(IDoubleMatrix mat) {
        if (mat.getRowNum() != mat.getColNum()) {
            throw new AdeCoreIllegalArgumentException(String.format("Expected a square matrix. Matrix size is %d x %d", mat.getRowNum(), mat.getColNum()));
        }
    }

    /** Throws an exception if matrix is not symmetric */
    public static void assertSymmetric(IDoubleMatrix mat, double epsilon) {
        assertSquare(mat);
        for (int i = 0; i < mat.getRowNum(); ++i) {
            for (int j = i + 1; j < mat.getRowNum(); ++j) {
                final double v1 = mat.get(i, j);
                final double v2 = mat.get(j, i);
                if (Double.isNaN(v1) || Double.isNaN(v2)) {
                    if (!Double.isNaN(v1) || !Double.isNaN(v2)) {
                        final String msg = String.format("Matrix is not symmetric with respect to Nans: (%d,%d)=%f (%d,%d)=%f",
                                i, j, v1, j, i, v2);
                        throw new AdeCoreIllegalArgumentException(msg);
                    }
                } else if (Math.abs(v1 - v2) > epsilon) {
                    final String errorMsg = String.format("Matrix not symmetric: (%d,%d)=%f (%d,%d)=%f  (epsilon=%g)",
                            i, j, v1, j, i, v2, epsilon);
                    throw new AdeCoreIllegalArgumentException(errorMsg);
                }
            }
        }

    }

    /** Throws an exception if matrix is not diagonally dominant */
    public static void assertDiagonallyDominant(IDoubleMatrix mat) {
        assertSquare(mat);
        for (int i = 0; i < mat.getRowNum(); ++i) {
            final double d = mat.get(i, i);
            if (Double.isNaN(d)) {
                continue;
            }
            for (int j = 0; j < mat.getRowNum(); ++j) {
                double v;
                v = mat.get(i, j);
                if (!Double.isNaN(v) && v > d) {
                    final String msg = String.format("Matrix is not diagonally dominant: (%d,%d)=%f (%d,%d)=%f diff=%f",
                            i, j, v, i, i, d, d - v);
                    throw new AdeCoreIllegalArgumentException(msg);
                }
                v = mat.get(j, i);
                if (!Double.isNaN(v) && v > d) {
                    final String msg = String.format("Matrix is nSot diagonally dominant: (%d,%d)=%f (%d,%d)=%f",
                            j, i, v, i, i, d);
                    throw new AdeCoreIllegalArgumentException(msg);
                }
            }
        }
    }

    public static IDoubleMatrix multiply(IDoubleMatrix mat1, IDoubleMatrix mat2) {
        return multiply(mat1, mat2, null);
    }

    public static IDoubleMatrix multiply(IDoubleMatrix mat1, IDoubleMatrix mat2, IDoubleMatrix res) {
        if (mat1.getColNum() != mat2.getRowNum()) {
            throw new AdeCoreIllegalArgumentException(String.format("mat1 column count %d != mat2 row count %d", mat1.getColNum(), mat2.getRowNum()));
        }
        if (res == null) {
            res = new DoubleMatrix(mat1.getRowNum(), mat2.getColNum());
        }
        for (int i = 0; i < res.getRowNum(); ++i) {
            for (int j = 0; j < res.getColNum(); ++j) {
                res.set(i, j, DoubleVectorOps.dotProduct(mat1.getRow(i), mat2.getCol(j)));
            }
        }
        return res;
    }

    public static IDoubleMatrix multiply2(IDoubleMatrix mat1, IDoubleMatrix mat2, IDoubleMatrix res) {
        if (mat1.getColNum() != mat2.getRowNum()) {
            throw new AdeCoreIllegalArgumentException(String.format("mat1 column count %d != mat2 row count %d", mat1.getColNum(), mat2.getRowNum()));
        }
        final int N = mat1.getRowNum();
        final int K = mat1.getColNum();
        final int M = mat2.getColNum();
        if (res == null) {
            res = new DoubleMatrix(N, M);
        }

        final double[][] r = res.get2dArray();
        final double[][] m1 = mat1.get2dArray();
        final double[][] m2 = mat2.get2dArray();
        for (int i = 0; i < N; ++i) {
            final double[] v1 = m1[i];
            final double[] ri = r[i];
            for (int j = 0; j < M; ++j) {
                double sum = 0;
                for (int k = 0; k < K; k++) {
                    sum += v1[k] * m2[k][j];
                }
                ri[j] = sum;
            }
        }
        return res;
    }

    public static IDoubleVector multiply(IDoubleMatrix mat, IDoubleVector v) {
        return multiply(mat, v, null);
    }

    public static IDoubleVector multiply(IDoubleMatrix mat, IDoubleVector v, IDoubleVector res) {
        final int m = mat.getColNum();
        final int n = mat.getRowNum();
        if (v.getLength() != m) {
            throw new AdeCoreIllegalArgumentException(String.format("mat column count %d != v length count %d", m, v.getLength()));
        }
        if (res == null) {
            res = new DoubleVector(n);
        }
        for (int i = 0; i < n; ++i) {
            res.set(i, DoubleVectorOps.dotProduct(mat.getRow(i), v));
        }
        return res;
    }

    public static DoubleVector multiply2(IDoubleMatrix mat, DoubleVector v, DoubleVector res) {
        final int M = mat.getColNum();
        final int N = mat.getRowNum();
        if (v.getLength() != M) {
            throw new AdeCoreIllegalArgumentException(String.format("mat column count %d != v length count %d", M, v.getLength()));
        }
        if (res == null) {
            res = new DoubleVector(N);
        }

        final double[] v2 = v.getArray();
        final double[][] m = mat.get2dArray();
        final double[] r = res.getArray();

        for (int i = 0; i < N; ++i) {
            final double[] vec = m[i];
            double sum = 0;
            for (int j = 0; j < M; j++) {
                sum += vec[j] * v2[j];
            }
            r[i] = sum;
        }
        return res;
    }

    public static IDoubleMatrix transpose(IDoubleMatrix mat) {
        final int n = mat.getRowNum();
        final int m = mat.getColNum();
        final IDoubleMatrix res = new DoubleMatrix(m, n);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < m; ++j) {
                res.set(j, i, mat.get(i, j));
            }
        }
        return res;
    }
}
