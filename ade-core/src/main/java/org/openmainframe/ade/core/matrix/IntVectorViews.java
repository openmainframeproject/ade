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

import org.openmainframe.ade.core.exceptions.AdeCoreIllegalArgumentException;

public final class IntVectorViews {
    
    private IntVectorViews() {
        // Private constructor to hide the implicit public one.
    }
    
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    public static IIntVector select(IIntVector vec, int... indices) {
        return new Select(vec, indices);
    }

    public static IIntVector select(IIntVector vec, IBooleanVector p) {
        return new Select(vec, p);
    }

    public static IIntVector[] partition(IIntVector vec, IBooleanVector... allP) {
        final IIntVector[] result = new IIntVector[allP.length];
        int i = 0;
        for (IBooleanVector p : allP) {
            result[i++] = select(vec, p);
        }
        return result;
    }

    public static IIntVector subVector(IIntVector vec, int from, int to) {
        return new Subvector(vec, from, to);
    }

    public static IIntVector wrapBigIntArray(final Integer[] arr) {
        return new IIntVector() {
            @Override
            public void setAll(int val) {
                for (int i = 0; i < getLength(); i++) {
                    set(i, val);
                }
            }

            @Override
            public void set(int i, int val) {
                arr[i] = val;
            }

            @Override
            public int getLength() {
                return arr.length;
            }

            @Override
            public int get(int i) {
                return arr[i];
            }
        };
    }

    private static class Select implements IIntVector {
        private IIntVector mVector;

        private IIntVector mSelectedIndices;

        Select(IIntVector vec, int[] selected) {
            mVector = vec;
            mSelectedIndices = IntVector.wrapArray(selected);
        }

        Select(IIntVector vec, IBooleanVector p) {
            // does not check if length of p exceeds length of vec
            // get will throw exception if this is the case
            mVector = vec;
            final int[] selection = new int[p.getLength()];
            int count = 0;
            for (int i = 0; i < p.getLength(); i++) {
                if (p.get(i)) {
                    selection[count++] = i;
                }
            }
            mSelectedIndices = IntVector.wrapArray(Arrays.copyOf(selection, count));
        }

        @Override
        public int get(int i) {
            return mVector.get(mSelectedIndices.get(i));
        }

        @Override
        public int getLength() {
            return mSelectedIndices.getLength();
        }

        @Override
        public void set(int i, int val) {
            mVector.set(mSelectedIndices.get(i), val);
        }

        @Override
        public void setAll(int val) {
            for (int i = 0; i < getLength(); ++i) {
                set(i, val);
            }
        }

        @Override
        public String toString() {
            return "IntVectorViews.Select " + VectorIO.toString(this);
        }
    }

    private static class Subvector implements IIntVector {
        private IIntVector mVec;
        private int mFrom, mTo;

        private Subvector(IIntVector vec, int from, int to) {
            this.mVec = vec;
            this.mFrom = from;
            this.mTo = to;
            if (mTo > vec.getLength()) {
                mTo = vec.getLength();
            }
            if (mFrom > mTo) {
                throw new AdeCoreIllegalArgumentException("subVector index out of range");
            }
        }

        @Override
        public int get(int i) {
            return mVec.get(i + mFrom);
        }

        @Override
        public int getLength() {
            return mTo - mFrom;
        }

        @Override
        public void set(int i, int val) {
            mVec.set(i + mFrom, val);
        }

        @Override
        public void setAll(int val) {
            for (int i = mFrom; i < mTo; i++) {
                mVec.set(i, val);
            }
        }

        @Override
        public String toString() {
            return "IntVectorViews.Select " + VectorIO.toString(this);
        }
    }

}
