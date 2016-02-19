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
package org.openmainframe.ade.impl.data;

import org.openmainframe.ade.impl.summary.LevenshteinTextSummary;

/**
 * A functor object for setting a threshold for comparison of two given strings.
 * The threshold is based on the two strings (usually on their lengths), and can be used to threshold
 * a distance function between strings (e.g. edit distance). The use of the two strings might not be
 * symmetric.
 * @see LevenshteinTextSummary#calcDistance(String, String)
 */
public interface IThresholdSetter {
    /**
     * Calculates the appropriate threshold for deciding whether the first string is close to the second string
     * @param numWordsA the number of words in the first string
     * @param numWordsB the number of words in the second string
     * @return the highest distance allowed such that the strings won't be considered "close".
     */
    public int getThreshold(int numWordsA, int numWordsB);
}
