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

import org.openmainframe.ade.core.exceptions.AdeCoreIllegalArgumentException;

public final class DimAndSizesAsserts {
    
    private DimAndSizesAsserts() {
        // Private constructor to hide the implicit public one.
    }
    
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    static void assertMatchingDimensionSize(int expectedDims, int actualDims) {
        if (expectedDims != actualDims) {
            throw new AdeCoreIllegalArgumentException(String.format("Expecting %d dimensions. Recieved %d dimensions", expectedDims, actualDims));
        }
    }

    static void assertLegalDimension(int dim, int numDims) {
        if (dim < 0 || dim >= numDims) {
            throw new AdeCoreIllegalArgumentException(String.format("Illegal dimension %d for md-array with %d dimensions", dim, numDims));
        }
    }

    public static void assertEqualLength(IDoubleVector vec1, IDoubleVector vec2) {
        assertEqualLength(vec1.getLength(), vec2.getLength());
    }

    public static void assertEqualLength(int len1, int len2) {
        if (len1 != len2) {
            throw new AdeCoreIllegalArgumentException(String.format("Expecting equal lengths. Actual lengths are %d and %d", len1, len2));
        }
    }

    static void assertLegalColIndex(int index, int numCols) {
        if (index < 0 || index >= numCols) {
            throw new AdeCoreIllegalArgumentException(String.format("Illegal column %d for matrix with %d columns", index, numCols));
        }
    }

    static void assertLegalRowIndex(int index, int numRows) {
        if (index < 0 || index >= numRows) {
            throw new AdeCoreIllegalArgumentException(String.format("Illegal row %d for matrix with %d rows", index, numRows));
        }
    }

    static void assertLegalColIndex(int index, IDoubleMatrix mat) {
        assertLegalColIndex(index, mat.getColNum());
    }

    static void assertLegalRowIndex(int index, IDoubleMatrix mat) {
        assertLegalRowIndex(index, mat.getRowNum());
    }

    /** Verifies the specified indices are legal for a matrix of given sizes*/
    public static void assertLegalMatrixIndex(int i, int j, int rowNum, int colNum) {
        assertLegalRowIndex(i, rowNum);
        assertLegalColIndex(j, colNum);
    }

    public static void assertLegalMatrixIndex(int i, int j, IDoubleMatrix mat) {
        assertLegalRowIndex(i, mat);
        assertLegalColIndex(j, mat);
    }

    public static void assertLegalMDIndex(int dim, int index, int dimSize) {
        if (index < 0 || index >= dimSize) {
            throw new AdeCoreIllegalArgumentException(String.format("Illegal index %d of dimension %d of size %d", index, dim, dimSize));
        }
    }

    static void assertLegalMDArrayIndex(int[] pos, int[] dimSizes) {
        assertMatchingDimensionSize(dimSizes.length, pos.length);
        for (int i = 0; i < pos.length; i++) {
            assertLegalMDIndex(i, pos[i], dimSizes[i]);
        }

    }
}
