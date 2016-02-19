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

/**
   * inner class represents a Word: a pair of string and a boolean indicating if the word
   * in the sentence is preceded with whitespace.
   * This is used to recreate the summarized string with * in the correct places.
   */
public class Word {
    // Class representing a word

    public Word(String str, boolean ws) {
        super();
        this.str = str;
        this.ws = ws;
    }
    // The index of the word
    private String str; 
    // Was there white space before the word?
    boolean ws; 

    public final String toString() {
        return this.str;
    }

    public final String getStr() {
        return str;
    }
}