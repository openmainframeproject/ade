/*
 
    Copyright IBM Corp. 2016
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
package org.openmainframe.ade.actions;

import org.openmainframe.ade.exceptions.AdeInternalException;

/** A ParsingQualityReporter object is plugged in to a AdeReader
 * and sums up events that occur while parsing, and outputs them in a file.
 *
 */
public interface IParsingQualityReporter {

    /**
     * Open parsing quality summary result file.
     * @param fileName the name of the file to open
     * @throws AdeInternalException
     */
    void open(String fileName) throws AdeInternalException;

    /**
     * Report the occurrence of a named event, e.g., found message of a particular type.
     * @param name the name of event to report
     */
    void addEvent(String name);

    /** 
     * Report a line that produced an error.
     * @param error error produced
     * @param details details of the error (e.g., msg-id, timestamp and such) 
     * @param lineNum input source line number
     * @param offendingLine the entire line
     */
    void lineError(String error, String details, int lineNum, String offendingLine);

    /** 
     * Signal end of parsing. 
     * */
    void close();
}