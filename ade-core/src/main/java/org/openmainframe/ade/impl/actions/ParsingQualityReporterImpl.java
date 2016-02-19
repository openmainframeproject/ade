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
package org.openmainframe.ade.impl.actions;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.openmainframe.ade.actions.IParsingQualityReporter;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Reporter to indicate how well the input was parsed in terms of errors vs events.
 */
public class ParsingQualityReporterImpl implements IParsingQualityReporter {
    
    private static final Logger logger = LoggerFactory.getLogger(ParsingQualityReporterImpl.class);
    private static final int MAX_ERROR_PER_TYPE = 3;

    private WordCounts m_errors = new WordCounts();
    private WordCounts m_events = new WordCounts();
    private PrintStream m_out;
    private String m_outputName;

    /** 
     * Constructor to get the reporter ready to print.
     */
    public ParsingQualityReporterImpl() {
        m_out = System.out;
        m_outputName = null;
    }
    /** 
     * Constructor for testing.
     * @param out The PrintStream to print to.
     */
    public ParsingQualityReporterImpl(PrintStream out) {
        m_out = out;
        m_outputName = null;
    }

    @Override
    public final void open(String fileName) throws AdeInternalException {
        try {
            m_out = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName)), false, "UTF-8");
        } catch (IOException e) {
            throw new AdeInternalException("Cannot open file " + fileName, e);
        }
        m_outputName = fileName;
    }

    @Override
    public final void addEvent(String name) {
        m_events.add(name);
    }

    @Override
    public final void lineError(String error, String details, int lineNum, String offendingLine) {
        String errorMessage = error;
        if (error == null) {
            errorMessage = "unknown error";
        }
        final int val = m_errors.add(errorMessage);
        if (val <= MAX_ERROR_PER_TYPE) {
            m_out.printf("Error at line %d:%n%s%n%s",
                    lineNum, offendingLine, errorMessage);
            if (details != null && details.length() > 0) {
                m_out.printf(" (%s)", details);
            }
            m_out.printf("%n%n");
        } else if (val == MAX_ERROR_PER_TYPE + 1) {
            m_out.printf("'%s' repeated %d times. It will no longer be reported.%n%n", errorMessage, val);
        }
    }
    
    /** 
     * Prints number of events and errors to indicate parsing quality.
     */
    public final void printSummary() {
        m_out.println("Parsing summary:");
        m_out.println("  Events:");
        m_events.print(m_out);
        m_out.println("  Errors:");
        m_errors.print(m_out);
    }
    
    @Override
    public final void close() {
        printSummary();
        if (m_outputName != null) {
            logger.info("Wrote parsing report in " + m_outputName);
            m_out.close();
        }
    }

    static class WordCounts {
        private static final Logger logger = LoggerFactory.getLogger(WordCounts.class);
        private Map<String, Integer> m_wordToCount = new TreeMap<String, Integer>();
        
        public WordCounts() { 
        }

        int add(String word) {
            Integer val = m_wordToCount.get(word);
            if (val == null) {
                val = 1;
            } else {
                val++;
            }
            m_wordToCount.put(word, val);
            return val;
        }

        Collection<String> getWords() {
            return m_wordToCount.keySet();
        }

        int getCount(String word) {
            return m_wordToCount.get(word);
        }

        void print(PrintStream out) {
            int total = 0;
            for (Map.Entry<String, Integer> entry : m_wordToCount.entrySet()) {
                final int num = entry.getValue();
                out.printf("    %5d %s%n", num, entry.getKey());
                total += num;
            }
            logger.info(String.format("Total: %d%n", total));
        }
    }
}
