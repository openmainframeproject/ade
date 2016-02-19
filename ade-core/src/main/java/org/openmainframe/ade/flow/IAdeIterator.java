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
package org.openmainframe.ade.flow;

import org.openmainframe.ade.exceptions.AdeException;

/**
 * 
 * Here's a demo code for using a ade iterator.
 * The code guarantees exceptions will be handled properly and the iterator
 * will be closed.
 * 
 * AdeIterator<T> myIt=new ....
 * try {
 *    myIt.open();
 *    T val;
 *    while ( (val=myIt.next())!=null) { ... }
 *    myIt.close();
 * } finally {
 *    myIt.quietCleanup();
 * }
 *
 * @param <T>
 */
public interface IAdeIterator<T> {

    /** Opens the iterator. When desigining an iterator aim to have
     * the exception throwing code here while keeping the constructor
     *  exception free if possible 
     * @throws AdeException
     */
    void open() throws AdeException;

    /** Returns the next element or null if there eof is reached */
    T getNext() throws AdeException;

    /** Closes the iterator */
    void close() throws AdeException;

    /** Closes the iterator without throwing exceptions. Used for closing
     *  it when handling another exception 
     */
    void quietCleanup();
}
