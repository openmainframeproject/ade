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
package org.openmainframe.ade.impl.summary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.openmainframe.ade.exceptions.AdeInternalException;

/**
 * Used for critical words feature in analysis.
 *
 */
public class CriticalWordsScorer {

    public static final int INIT_SIZE = 30;
    private Set<String> m_criticalWordsSet = null;
    private File m_criticalWordsFile = null;

    public CriticalWordsScorer(String criticalWordsFile) throws AdeInternalException {
        m_criticalWordsFile = new File(criticalWordsFile);
        m_criticalWordsSet = new HashSet<String>(INIT_SIZE);
        init();
    }

    private void init() throws AdeInternalException {
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(m_criticalWordsFile), StandardCharsets.UTF_8));
            while (null != (line = br.readLine())) {
                /* Line start with # is a comment */
                if (line.startsWith("#")) {
                    continue;
                }

                String[] tokens = line.split("\\s+");
                if (tokens.length != 1) {
                    throw new AdeInternalException("Multiple words in line not allowed: " + line);
                }
                m_criticalWordsSet.add(tokens[0].toLowerCase());
            }
        } catch (IOException e) {
            throw new AdeInternalException("Error: unable to initiliaze text score", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                throw new AdeInternalException("Error: closing text score file", e);
            }
        }
    }

    /**
     * Return the score of message, based on the critical keywords
     * found within the message body.
     * 
     * @param text - the message to be analyzed
     * @return the score of the provided message
     */
    public int calcScore(String text) {
        int score = 0;
        if (text == null || text.length() == 0) {
            return score;
        }
        String[] tokens = text.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i].toLowerCase();
            if (m_criticalWordsSet.contains(word)) {
                score++;
            }
        }
        return score;
    }

}