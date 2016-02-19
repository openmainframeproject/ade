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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.openmainframe.ade.core.exceptions.AdeCoreUnsupportedOperationException;
import org.openmainframe.ade.core.statistics.BasicStatistics;

public final class DoubleVectorOps implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private DoubleVectorOps() {
        // Private constructor to hide the implicit public one.
    }

    static public double getDistanceL2(IDoubleVector vec1, IDoubleVector vec2) {
        DimAndSizesAsserts.assertEqualLength(vec1, vec2);
        double ss = 0;
        for (int i = 0; i < vec1.getLength(); ++i) {
            final double v = vec1.get(i) - vec2.get(i);
            ss += v * v;
        }
        return Math.sqrt(ss);
    }

    static public double getDistanceL1(IDoubleVector vec1, IDoubleVector vec2) {
        DimAndSizesAsserts.assertEqualLength(vec1, vec2);
        double ss = 0;
        for (int i = 0; i < vec1.getLength(); ++i) {
            final double v = Math.abs(vec1.get(i) - vec2.get(i));
            ss += v;
        }
        return ss;
    }

    static public double getDistanceL2NoNans(IDoubleVector vec1, IDoubleVector vec2) {
        DimAndSizesAsserts.assertEqualLength(vec1, vec2);
        boolean allNaN = true;
        double ss = 0;
        for (int i = 0; i < vec1.getLength(); ++i) {
            final double xi = vec1.get(i);
            final double yi = vec2.get(i);
            if (Double.isNaN(xi) || Double.isNaN(yi)) {
                continue;
            }
            final double v = xi - yi;
            ss += v * v;
            allNaN = false;
        }
        if (!allNaN) {
            return Math.sqrt(ss);
        } else {
            return Double.NaN;
        }
    }

    static public double getDistanceL1NoNans(IDoubleVector vec1, IDoubleVector vec2) {
        DimAndSizesAsserts.assertEqualLength(vec1, vec2);
        double ss = 0;
        boolean allNaN = true;
        for (int i = 0; i < vec1.getLength(); ++i) {
            final double xi = vec1.get(i);
            final double yi = vec2.get(i);
            if (Double.isNaN(xi) || Double.isNaN(yi)) {
                continue;
            }
            final double v = Math.abs(xi - yi);
            ss += v;
            allNaN = false;
        }
        if (!allNaN) {
            return ss;
        } else {
            return Double.NaN;
        }
    }

    /** Fills vector with random double values in the range [min,max) */
    static public void fillRandomUniform(IDoubleVector vec, Random rand, double min, double max) {
        final double delta = max - min;
        for (int i = 0; i < vec.getLength(); ++i) {
            vec.set(i, min + rand.nextDouble() * delta);
        }
    }

    /** Fills vector with random integer values in the range [min,max) */
    static public void fillRandomIntegerUniform(IDoubleVector vec, Random rand, int min, int max) {
        final int delta = max - min;
        for (int i = 0; i < vec.getLength(); ++i) {
            vec.set(i, min + rand.nextInt(delta));
        }
    }

    static public void setAll(IDoubleVector vec, double[] values) {
        DimAndSizesAsserts.assertEqualLength(vec.getLength(), values.length);
        for (int j = 0; j < values.length; j++) {
            vec.set(j, values[j]);
        }
    }

    public static double dotProduct(IDoubleVector vec1, IDoubleVector vec2) {
        DimAndSizesAsserts.assertEqualLength(vec1, vec2);
        double res = 0;
        for (int i = 0; i < vec1.getLength(); ++i) {
            res += vec1.get(i) * vec2.get(i);
        }
        return res;
    }

    public static IDoubleVector elementwiseMultiplication(IDoubleVector vec1, IDoubleVector vec2) {
        return elementwiseMultiplication(vec1, vec2, null);
    }

    public static IDoubleVector elementwiseMultiplication(IDoubleVector vec1, IDoubleVector vec2, IDoubleVector res) {
        DimAndSizesAsserts.assertEqualLength(vec1, vec2);
        final int n = vec1.getLength();
        if (res == null) {
            res = new DoubleVector(n);
        }
        for (int i = 0; i < n; ++i) {
            res.set(i, vec1.get(i) * vec2.get(i));
        }
        return res;
    }

    public static IDoubleVector scalarMultiplication(IDoubleVector vec1, double scalar) {
        final int n = vec1.getLength();
        final IDoubleVector res = new DoubleVector(n);
        for (int i = 0; i < n; ++i) {
            res.set(i, vec1.get(i) * scalar);
        }
        return res;

    }

    public static IDoubleVector sum(IDoubleVector vec1, IDoubleVector vec2) {
        DimAndSizesAsserts.assertEqualLength(vec1, vec2);
        final int n = vec1.getLength();
        final IDoubleVector res = new DoubleVector(n);
        for (int i = 0; i < n; ++i) {
            res.set(i, vec1.get(i) + vec2.get(i));
        }
        return res;
    }

    public static IDoubleVector difference(IDoubleVector vec1, IDoubleVector vec2) {
        DimAndSizesAsserts.assertEqualLength(vec1, vec2);
        final int n = vec1.getLength();
        final IDoubleVector res = new DoubleVector(n);
        for (int i = 0; i < n; ++i) {
            res.set(i, vec2.get(i) - vec1.get(i));
        }
        return res;
    }

    public static void swap(IDoubleVector x, int i, int j) {
        final double tmp = x.get(i);
        x.set(i, x.get(j));
        x.set(j, tmp);
    }

    public static double[] toArray(IDoubleVector vec) {
        return copyToArray(vec, new double[vec.getLength()]);
    }

    public static double[] copyToArray(IDoubleVector vec, double[] arr) {
        for (int j = 0; j < vec.getLength(); ++j) {
            arr[j] = vec.get(j);
        }
        return arr;
    }

    public static class BinaryDoubleVectorL1 implements IBinaryDoubleVectorFunc {
        static String copyright() {
            return Copyright.IBM_COPYRIGHT;
        }

        /**
         * @see org.openmainframe.ade.core.matrix.IBinaryDoubleVectorFunc#getFuncValue(org.openmainframe.ade.core.matrix.IDoubleVector, org.openmainframe.ade.core.matrix.IDoubleVector)
         */
        @Override
        public double getFuncValue(IDoubleVector vec1, IDoubleVector vec2) {
            return DoubleVectorOps.getDistanceL1NoNans(vec1, vec2);
        }

    }

    public static class BinaryDoubleVectorL2 implements IBinaryDoubleVectorFunc {
        @Override
        public double getFuncValue(IDoubleVector vec1, IDoubleVector vec2) {
            return DoubleVectorOps.getDistanceL2NoNans(vec1, vec2);
        }

    }

    /** Computes squared value of L2 norm (sum of squares of differences) */
    public static class BinaryDoubleVectorL2Squared implements IBinaryDoubleVectorFunc, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public double getFuncValue(IDoubleVector vec1, IDoubleVector vec2) {
            DimAndSizesAsserts.assertEqualLength(vec1, vec2);
            boolean allNaN = true;
            double ss = 0;
            for (int i = 0; i < vec1.getLength(); ++i) {
                final double xi = vec1.get(i);
                final double yi = vec2.get(i);
                if (Double.isNaN(xi) || Double.isNaN(yi)) {
                    continue;
                }
                final double v = xi - yi;
                ss += v * v;
                allNaN = false;
            }
            if (allNaN) {
                return Double.NaN;
            }
            return ss;
        }

    }

    public static class BinaryDoubleVectorPearsonCorrelation implements IBinaryDoubleVectorFunc {
        static String copyright() {
            return Copyright.IBM_COPYRIGHT;
        }

        /**
         * @see org.openmainframe.ade.core.matrix.IBinaryDoubleVectorFunc#getFuncValue(org.openmainframe.ade.core.matrix.IDoubleVector, org.openmainframe.ade.core.matrix.IDoubleVector)
         */
        @Override
        public double getFuncValue(IDoubleVector vec1, IDoubleVector vec2) {
            return BasicStatistics.correlationNoNans(vec1, vec2);
        }

    }

    public static class BinaryDoublnoeVectorHamming implements IBinaryDoubleVectorFunc {
        static String copyright() {
            return Copyright.IBM_COPYRIGHT;
        }

        /**
         * @see org.openmainframe.ade.core.matrix.IBinaryDoubleVectorFunc#getFuncValue(org.openmainframe.ade.core.matrix.IDoubleVector, org.openmainframe.ade.core.matrix.IDoubleVector)
         */
        @Override
        public double getFuncValue(IDoubleVector vec1, IDoubleVector vec2) {
            //The function skips NaN values
            DimAndSizesAsserts.assertEqualLength(vec1, vec2);
            boolean allNaN = true;
            double ss = 0;
            for (int i = 0; i < vec1.getLength(); ++i) {
                final double xi = vec1.get(i);
                final double yi = vec2.get(i);
                if (Double.isNaN(xi) || Double.isNaN(yi)) {
                    continue;
                }
                if (xi != yi) {
                    ss++;
                }
                allNaN = false;
            }
            if (allNaN) {
                return Double.NaN;
            }
            return ss;
        }

    }

    public static class BinaryDoubleVectorScaledPearsonCorrelation implements IBinaryDoubleVectorFunc {
        static String copyright() {
            return Copyright.IBM_COPYRIGHT;
        }

        /**
         * @see org.openmainframe.ade.core.matrix.IBinaryDoubleVectorFunc#getFuncValue(org.openmainframe.ade.core.matrix.IDoubleVector, org.openmainframe.ade.core.matrix.IDoubleVector)
         */
        @Override
        public double getFuncValue(IDoubleVector vec1, IDoubleVector vec2) {
            double corr = BasicStatistics.correlationNoNans(vec1, vec2);
            corr = 0.5 - corr / 2;
            return corr;
        }

    }

    public static int[] getSortingIx(final IDoubleVector vec, final boolean ascend) {
        final int n = vec.getLength();
        final Integer[] ix = new Integer[n];
        for (int i = 0; i < n; i++) {
            ix[i] = i;
        }
        final Comparator<Integer> comparator = new Comparator<Integer>() {

            @Override
            public int compare(Integer i1, Integer i2) {
                if (ascend) {
                    return Double.compare(vec.get(i1), vec.get(i2));
                } else {
                    return Double.compare(vec.get(i2), vec.get(i1));
                }
            }
        };

        Arrays.sort(ix, comparator);

        return IntVectorOps.toArray(IntVectorViews.wrapBigIntArray(ix));
    }

    public static int[] getNonNanSortingIx(final IDoubleVector vec, final boolean ascend) {

        final IBooleanVector nonNans = isNonNan(vec);
        final Integer[] ix = new Integer[BooleanVectorOps.count(nonNans)];
        int j = 0;
        for (int i = 0; i < vec.getLength(); i++) {
            if (nonNans.get(i)) {
                ix[j++] = i;
            }
        }
        final Comparator<Integer> comparator = new Comparator<Integer>() {


            @Override
            public int compare(Integer i1, Integer i2) {

                double diff = vec.get(i1) - vec.get(i2);

                if (!ascend) {
                    diff *= -1;
                }
                if (diff > 0) {
                    return 1;
                }
                if (diff < 0) {
                    return -1;
                }
                return 0;

            }
        };

        Arrays.sort(ix, comparator);

        return IntVectorOps.toArray(IntVectorViews.wrapBigIntArray(ix));
    }

    public static void setAll(IDoubleVector target, IDoubleVector source, IBooleanVector p) {
        int len = source.getLength();
        if (len > target.getLength()) {
            len = target.getLength();
        }
        for (int i = 0; i < len; i++) {
            if (p.get(i)) {
                target.set(i, source.get(i));
            }
        }
    }

    public static void setAll(IDoubleVector target, IDoubleVector source) {
        int len = source.getLength();
        if (len > target.getLength()) {
            len = target.getLength();
        }
        for (int i = 0; i < len; i++) {
            target.set(i, source.get(i));
        }
    }

    static public IBooleanVector isNonNan(final IDoubleVector arg) {
        return new DoubleVectorUnaryPredicate(arg) {
            @Override
            public boolean get(int pos) {
                return !Double.isNaN(mArg.get(pos));
            }
        };
    }

    static private abstract class DoubleVectorUnaryPredicate implements IBooleanVector {

        protected IDoubleVector mArg;

        DoubleVectorUnaryPredicate(IDoubleVector arg) {
            mArg = arg;
        }

        @Override
        public int getLength() {
            return mArg.getLength();
        }

        @Override
        public void set(int pos, boolean val) {
            throw new AdeCoreUnsupportedOperationException("read only vector");
        }

    }
}
