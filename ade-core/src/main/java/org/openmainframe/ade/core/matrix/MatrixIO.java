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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.openmainframe.ade.core.exceptions.AdeCoreInvalidFileException;
import org.openmainframe.ade.core.io.NumberFileParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MatrixIO {
    
    private static final Logger logger = LoggerFactory.getLogger(MatrixIO.class);
    
    private MatrixIO() {
        // Private constructor to hide the implicit public one.
    }
    
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    public static RealMatrix readRealMatrixFromFile(String fileName) throws IOException {
        DoubleMatrix mat;

        mat = (DoubleMatrix) MatrixIO.readMatrixFromFile(fileName);

        return MatrixUtils.createRealMatrix(mat.get2dArray());
    }

    public static void writeMatrixToFile(IDoubleMatrix mat, final String fileName) throws IOException {
        PrintWriter fw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(fileName), StandardCharsets.UTF_8));
        try {
            for (int i = 0; i < mat.getRowNum(); ++i) {
                for (int j = 0; j < mat.getColNum(); ++j) {
                    if (j > 0) {
                        fw.print(" ");
                    }
                    fw.print(Double.toString(mat.get(i, j)));
                }
                fw.println();
            }
            fw.close();
            fw = null;
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (Throwable t) {
                    logger.error("Error trying to close the PrintWriter.", t);
                }
            }
        }
    }

    public static IDoubleMatrix readMatrixFromFile(final String fileName) throws IOException {
        final NumberFileParser fp = new NumberFileParser(fileName);
        final ArrayList<Double> res = new ArrayList<>();
        int firstRowNumColumns = -1;
        int tmpNumColumns = -1;
        int prevResSize = 0;
        boolean firstRow = true;
        int numRows = 0;
        while (true) {
            while (fp.isDouble()) {
                res.add(fp.getDouble());
            }

            if (firstRow) {
                firstRowNumColumns = res.size();
                firstRow = false;
                ++numRows;
            } else {
                tmpNumColumns = res.size() - prevResSize;
                if ((tmpNumColumns > 0) && (tmpNumColumns != firstRowNumColumns)) {
                    throw new AdeCoreInvalidFileException("line " + (numRows + 1) + ": numColumns:" + tmpNumColumns + "; expectedNumColumns:" + firstRowNumColumns);
                }
            }
            prevResSize = res.size();
            if (tmpNumColumns > 0) {
                ++numRows;
            }
            if (fp.isEol()) {
                fp.getEol();
            } else if (fp.isEof()) {
                fp.close();
                break;
            } else {
                fp.parseError("File contains something other than double or nan");
            }
        }
        final int numCols = firstRowNumColumns;
        final DoubleMatrix doubleMat = new DoubleMatrix(numRows, numCols);
        int counter = 0;
        for (int i = 0; i < numRows; ++i) {
            for (int j = 0; j < numCols; ++j) {
                doubleMat.set(i, j, res.get(counter++));
            }
        }
        return doubleMat;
    }

    public static void printMatrixRow(IDoubleMatrix mat, int row) {
        for (int j = 0; j < mat.getRowNum(); j++) {
            logger.info("%6.2f ", mat.get(row, j));
        }
    }

    public static void print(IDoubleMatrix mat) {
        logger.info("Matrix %d x %d\n", mat.getRowNum(), mat.getColNum());
        for (int i = 0; i < mat.getRowNum(); i++) {
            printMatrixRow(mat, i);
        }
    }

    public static String toString(IDoubleMatrix mat) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mat.getRowNum(); ++i) {
            if (i > 0) {
                builder.append("\n");
            }
            for (int j = 0; j < mat.getColNum(); ++j) {
                if (j > 0) {
                    builder.append(" ");
                }
                builder.append(mat.get(i, j));
            }

        }
        return builder.toString();
    }
}
