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

import java.util.Date;

/** An output stream for structured, xml-like data
 * 
 * The output is composed of elements.
 * Each element has:
 * - a list of named attributes
 * - a list of sub-elements and texts (content)
 * 
 * A StructuredOutputWriter may represent either a single element,
 * in which case it can freely accept child elements and texts,
 * Or the top-level document, in which case it can only accept a single
 * top-level child element.
 * 
 * Note: depending on the underlying format further restrictions may be imposed.
 * E.g, adding multiple text contents to an element is forbidden in some formats. 
 */
public interface IStructuredOutputWriter {

    /** Add text to current element */
    void addContent(String val) throws Exception;

    /** Add double value to current element */
    void addContent(double val) throws Exception;

    /** Add int value to current element */
    void addContent(int val) throws Exception;

    /** Add a boolean value to current element */
    void addContent(boolean val) throws Exception;

    /** Add a date value to current element */
    void addContent(Date val) throws Exception;

    /** Add a child element to current element
     * Note: It is illegal to continue writing to current element before
     * the newly created child element is closed.
     * 
     * @param name Name of new child
     * @param attrs attributes of new child
     * @return Interface for writing to this child.
     * @throws Exception
     */
    IStructuredOutputWriter child(String name, String... attrs) throws Exception;

    /** Close current element.
     * After a call to close() no further data can be written to current element.
     * If current element has a parent element, then after calling close() it is possible
     * to continue to write to the parent.
     */
    void close() throws Exception;

    /** Adds a simple child element to current element:
     * 	Note: writing to current element may proceed immediatly after this call.
     * @param name name of new child.
     * @param val Single value of new child.
     * @param attrs Attributes of new child.
     * @throws Exception
     */
    void simpleChild(String name, String val, String... attrs) throws Exception;

    /** Adds a simple child element to current element:
    *  Note: writing to current element may proceed immediatly after this call.
    * @param name name of new child.
    * @param val Single value of new child.
    * @param attrs Attributes of new child.
    * @throws Exception
    */
    void simpleChild(String name, double val, String... attrs) throws Exception;

    /** Adds a simple child element to current element:
    *  Note: writing to current element may proceed immediatly after this call.
    * @param name name of new child.
    * @param val Single value of new child.
    * @param attrs Attributes of new child.
    * @throws Exception
    */
    void simpleChild(String name, int val, String... attrs) throws Exception;

    /** Adds a simple child element to current element:
    *  Note: writing to current element may proceed immediatly after this call.
    * @param name name of new child.
    * @param val Single value of new child.
    * @param attrs Attributes of new child.
    * @throws Exception
    */
    void simpleChild(String name, Date val, String... attrs) throws Exception;

    /** Adds a simple child element to current element:
    *  Note: writing to current element may proceed immediatly after this call.
    * @param name name of new child.
    * @param val Single value of new child.
    * @param attrs Attributes of new child.
    * @throws Exception
    */
    void simpleChild(String name, boolean val, String... attrs) throws Exception;

    /** @return whether current element was closed */
    boolean isDone() throws Exception;

    /** Report an error on the creation of this element.
    * This triggers an exception with details on the location of this element.
     */
    void elementError(String msg) throws Exception;

    /** Format a date in the internal representation of the underlying format */
    String formatDate(Date val);
}
