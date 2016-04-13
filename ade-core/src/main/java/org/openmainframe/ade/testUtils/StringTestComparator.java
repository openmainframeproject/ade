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
package org.openmainframe.ade.testUtils;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;

import org.openmainframe.ade.exceptions.AdeInternalException;

/** An object for unit tests.
 * Performs a comparison between two multi-line strings, and prints
 * very detailed error message in case of a mismatch.
 *
 * Used for comparing objects by comparing their toString() results.
 */
public class StringTestComparator {

    String m_title1;
    String m_val1;
    String m_title2;
    String m_val2;
    private boolean m_printWithLineNumbers;

    /**
     * Compare multi-line strings
     * Note: constructor already runs the comparison
     * @param title1 Title of first string (printed in error messages)
     * @param val1 First string
     * @param title2 Title of second string (printed in error messages)
     * @param val2 second string
     * @throws AdeInternalException
     */
    public StringTestComparator(String title1, String val1, String title2, String val2) throws AdeInternalException {
        this(title1, val1, title2, val2, true);
    }

    /**
     * Compares multi-line strings
     * Note: constructor already runs the comparison
     * @param title1 Title of first string (printed in error messages)
     * @param val1 First string
     * @param title2 Title of second string (printed in error messages)
     * @param val2 second string
     * @param printWithLineNumbers Indicates whether line numbers should be included in error messages
     * @throws AdeInternalException
     */
    public StringTestComparator(String title1, String val1, String title2, String val2, boolean printWithLineNumbers) throws AdeInternalException {
        m_title1 = title1;
        m_val1 = val1;
        m_title2 = title2;
        m_val2 = val2;
        m_printWithLineNumbers = printWithLineNumbers;
        runCompare();

    }

    private void runCompare() throws AdeInternalException {
        int lines = 0;
        //This try-with-resources block will ensure that both reader1 and reader2 are closed.
        try (final LineNumberReader reader1 = new LineNumberReader(new StringReader(m_val1));
                final LineNumberReader reader2 = new LineNumberReader(new StringReader(m_val2))) {
            while (true) {
                String line1 = reader1.readLine();
                String line2 = reader2.readLine();
                if (line1 == null && line2 == null) {
                    break;
                }
                if (line1 == null) {
                    fail(String.format("%s has additional line(s):\n%3d: %s", m_title2, reader2.getLineNumber(), line2));
                }
                if (line2 == null) {
                    fail(String.format("%s has additional line(s):\n%3d: %s", m_title1, reader1.getLineNumber(), line1));
                }
                ++lines;
                line1 = line1.trim();
                line2 = line2.trim();
                if (line1.equals(line2)) {
                    continue;
                }
                int i = 0;
                while (i < line1.length() && i < line2.length() && line1.charAt(i) == line2.charAt(i)) {
                    ++i;
                }
                StringBuilder errMsg = new StringBuilder();
                errMsg.append(String.format("Mismatch at line %d (A:%s B:%s)\n", reader1.getLineNumber(), m_title1, m_title2));
                errMsg.append(String.format("A:%s\n", line1));
                errMsg.append(String.format("B:%s\n", line2));
                errMsg.append("--");
                for (int j = 0; j < i; ++j) {
                    errMsg.append("-");
                }
                errMsg.append("^");
                fail(errMsg.toString());
    
            }
        } catch (IOException e) {
            throw new AdeInternalException("comparison internal bug", e);
        }
        System.out.printf("OK: Comparison of %s and %s passed (%d lines, %d chars)\n",
                m_title1, m_title2, lines, m_val1.length());
    }

    private void fail(String msg) throws IOException, AdeInternalException {
        printStringWithLineNumbers(m_title1, m_val1);
        printStringWithLineNumbers(m_title2, m_val2);
        System.out.println(msg);
        throw new AdeInternalException(String.format("Comparison of %s and %s failed", m_title1, m_title2));
    }

    private void printStringWithLineNumbers(String title, String val) throws IOException {
        System.out.printf("Content of %s\n", title);
        LineNumberReader reader = new LineNumberReader(new StringReader(val));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (m_printWithLineNumbers) {
                System.out.printf("%3d: %s\n", reader.getLineNumber(), line);
            } else {
                System.out.printf("%s\n", line);
            }
        }
    }

    static public void main(String[] args) throws Exception {
        String v1 = "hello\nworld\nfoo";
        String v2 = "hello\nworld\nfoo ";
        new StringTestComparator("obj1", v1, "obj2", v2);
        System.out.println("done");
    }
}
