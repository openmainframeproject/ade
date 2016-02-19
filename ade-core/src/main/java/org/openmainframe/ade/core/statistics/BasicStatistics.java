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
package org.openmainframe.ade.core.statistics;

import java.util.Random;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.openmainframe.ade.core.exceptions.AdeCoreIllegalArgumentException;
import org.openmainframe.ade.core.matrix.DoubleVector;
import org.openmainframe.ade.core.matrix.IDoubleVector;
import org.openmainframe.ade.core.matrix.DoubleVectorOps;
import org.openmainframe.ade.core.matrix.IntVector;

/**
 * Basic operations on vectors
 */
public final class BasicStatistics {
    
    private BasicStatistics() {
        // Private constructor to hide the implicit public one.
    }

    public static double covarianceNoNans(IDoubleVector x, IDoubleVector y) {
        if (x.getLength() != y.getLength()) {
            throw new AdeCoreIllegalArgumentException("Mistmatching lengths");
        }
        double xn = 0;
        double yn1 = 0;
        double Cn = 0;
        final int n = x.getLength();
        int counter = 0;
        for (int i = 0; i < n; ++i) {
            final double xi = x.get(i);
            final double yi = y.get(i);
            if (Double.isNaN(xi) || Double.isNaN(yi)) {
                continue;
            }
            xn += (xi - xn) / (counter + 1);
            Cn += (xi - xn) * (yi - yn1);
            yn1 += (yi - yn1) / (counter + 1);
            ++counter;
        }
        if (counter == 0) {
            return Double.NaN;
        }
        if (counter == 1) {
            return 0;
        }
        return Cn / (counter - 1);
    }

    static public double correlationNoNans(IDoubleVector x, IDoubleVector y) {
        if (x.getLength() != y.getLength()) {
            throw new AdeCoreIllegalArgumentException("Mismatching lengths");
        }
        double xn1 = 0;
        double yn1 = 0;
        double cxyn = 0;
        double cxxn = 0;
        double cyyn = 0;
        final int n = x.getLength();
        double counter = 0;
        for (int i = 0; i < n; ++i) {
            final double xi = x.get(i);
            final double yi = y.get(i);
            if (Double.isNaN(xi) || Double.isNaN(yi)) {
                continue;
            }
            cxyn += (counter / (counter + 1)) * (xi - xn1) * (yi - yn1);
            cxxn += (counter / (counter + 1)) * (xi - xn1) * (xi - xn1);
            cyyn += (counter / (counter + 1)) * (yi - yn1) * (yi - yn1);
            xn1 += (xi - xn1) / (counter + 1);
            yn1 += (yi - yn1) / (counter + 1);
            counter++;
        }

        final double stdx = Math.sqrt(cxxn / counter);
        final double stdy = Math.sqrt(cyyn / counter);
        final double covxy = cxyn / counter;
        if (counter > 0) {
            return covxy / (stdx * stdy);
        } else {
            return Double.NaN;
        }
    }

    public static double correlation(IDoubleVector x, IDoubleVector y) {
        final int len = x.getLength();
        if (len != y.getLength()) {
            throw new AdeCoreIllegalArgumentException("Mismatching lengths");
        }
        if (len < 2) {
            throw new AdeCoreIllegalArgumentException("Vectors must have length >=2");
        }
        final SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < len; i++) {
            regression.addData(x.get(i), y.get(i));
        }
        return regression.getR();
    }

    public static IntVector generatePermutation(final int numSamples, Random rand) {
        final int[] perm = new int[numSamples];
        for (int i = 0; i < numSamples; i++) {
            perm[i] = i;
        }
        for (int i = 0; i < numSamples - 1; i++) {
            final int randPos = i + rand.nextInt(numSamples - i);
            final int tmp = perm[i];
            perm[i] = perm[randPos];
            perm[randPos] = tmp;
        }
        return IntVector.wrapArray(perm);
    }

    static public double median(IDoubleVector x) {
        final int size = x.getLength();
        if (size % 2 == 1) {
            return select(x, size / 2);
        } else {
            final double tmp1 = select(x, size / 2);
            final double tmp2 = select(x, size / 2 - 1);
            return (tmp1 + tmp2) / 2;
        }
    }

    static public double select(IDoubleVector x, int k) {
        final IDoubleVector copyX = new DoubleVector(x);
        return quickSelect(copyX, k, 0, copyX.getLength() - 1);
    }

    /**
     * quickSort like algorithm to select k'th member. 
     * Has expected linear performance but o(n^2) worst case.
     * Considered to have good performance in practice 
     * @param x
     * @param k
     * @param start
     * @param end  - index of vector end (i.e vector size -1)
     * @return	 
     */
    static private double quickSelect(IDoubleVector x, int k, int start, int end) {
        if (k < start || k > end) {
            throw new AdeCoreIllegalArgumentException("requested selection is outside of range");
        }
        final int pivotIndex = ((int) Math.random() * (end - start + 1)) + start;
        final int pivotPosition = partition(x, start, end, pivotIndex);
        if (pivotPosition == k) {
            return x.get(k);
        } else if (pivotPosition < k) {
            return quickSelect(x, k, pivotPosition + 1, end);
        } else {
            return quickSelect(x, k, start, pivotPosition - 1);
        }
    }

    /**
     * @param x
     * @param start
     * @param end
     * @param pivotIndex
     * @return
     */
    private static int partition(IDoubleVector x, int start, int end, int pivotIndex) {
        final double pivotVal = x.get(pivotIndex);
        DoubleVectorOps.swap(x, pivotIndex, end);
        int afterKnownToBeSmaller = start;
        for (int i = start; i <= end - 1; ++i) {
            if (x.get(i) < pivotVal) {
                DoubleVectorOps.swap(x, afterKnownToBeSmaller, i);
                afterKnownToBeSmaller++;
            }
        }
        DoubleVectorOps.swap(x, afterKnownToBeSmaller, end);
        return afterKnownToBeSmaller;
    }

}
