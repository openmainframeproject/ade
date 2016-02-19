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
package org.openmainframe.ade.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.openmainframe.ade.exceptions.AdeParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** General I/O utilities */
public final class AdeIoUtils {

    private static final Logger logger = LoggerFactory.getLogger(AdeIoUtils.class);
    
    private AdeIoUtils() {
        // Private constructor to hide the implicit public one.
    }
    
    /** print a sequence of spaces 
     * 
     * @param out Outputstream to print to
     * @param padding number of spaces
     */
    public static void printPadding(PrintWriter out, int padding) {
        for (int i = 0; i < padding; ++i) {
            out.print(" ");
        }
    }

    public static boolean promptUser(String yesNoQuestion) {
        System.out.print(yesNoQuestion);
        YesNoOperator yesNoOperator = null;
        try {
            do {
                System.out.println(Arrays.toString(YesNoOperator.values()));
                final BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                String in = br.readLine();
                try {
                    yesNoOperator = YesNoOperator.parse(in);
                } catch (AdeParsingException e) {
                    logger.error("Error encountered while parsing line: " + in, e);
                    continue;
                }
            } while (yesNoOperator == null);
        } catch (IOException ioe) {
            logger.error("IO error trying to read input!", ioe);
            System.exit(1);
        }

        switch (yesNoOperator) {
            case Yes:
                return true;
            case No:
                return false;
            default:
                throw new IllegalArgumentException("Unknown");
        }
    }

    public static enum YesNoOperator {
        Yes("yes"), No("no");

        private String m_op;

        YesNoOperator(String operatorValue) {
            m_op = operatorValue;
        }

        private static YesNoOperator parse(String op) throws AdeParsingException {
            for (YesNoOperator val : values()) {
                if (val.m_op.equalsIgnoreCase(op)) {
                    return val;
                }
            }
            throw new AdeParsingException("Could not parse expressions: " + op);
        }
    }
}
