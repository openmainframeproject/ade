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

import java.util.Arrays;
import java.util.Comparator;

public final class IntVectorOps {
    
    private IntVectorOps() {
        // Private constructor to hide the implicit public one.
    }
    
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    static public int sum(IIntVector vec) {
        int res = 0;
        for (int i = 0; i < vec.getLength(); ++i) {
            res += vec.get(i);
        }
        return res;
    }

    static public void setAll(IIntVector vec, int[] values) {
        DimAndSizesAsserts.assertEqualLength(vec.getLength(), values.length);
        for (int j = 0; j < values.length; j++) {
            vec.set(j, values[j]);
        }
    }

    public static void swap(IIntVector x, int i, int j) {
        final int tmp = x.get(i);
        x.set(i, x.get(j));
        x.set(j, tmp);
    }

    public static int[] toArray(IIntVector vec) {
        return copyToArray(vec, new int[vec.getLength()]);
    }

    public static int[] copyToArray(IIntVector vec, int[] arr) {
        for (int j = 0; j < vec.getLength(); ++j) {
            arr[j] = vec.get(j);
        }
        return arr;
    }

    public static int[] getSortingIx(final IIntVector vec, final boolean ascend) {
        final int n = vec.getLength();
        final Integer[] ix = new Integer[n];
        for (int i = 0; i < n; i++) {
            ix[i] = i;
        }
        final Comparator<Integer> comparator = new Comparator<Integer>() {

            @Override
            public int compare(Integer i1, Integer i2) {

                int diff = vec.get(i1) - vec.get(i2);

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

        return toArray(IntVectorViews.wrapBigIntArray(ix));
    }

    public static void setAll(IIntVector target, IIntVector source, IBooleanVector p) {
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

    public static void setAll(IIntVector target, IIntVector source) {
        int len = source.getLength();
        if (len > target.getLength()) {
            len = target.getLength();
        }
        for (int i = 0; i < len; i++) {
            target.set(i, source.get(i));
        }
    }

}
