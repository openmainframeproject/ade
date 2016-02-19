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

/**
 * Extends the {@link IStreamTarget}&lt;OBJ&gt; interface but also allows receiving objects of type
 * &lt;SEP&gt; by extending the {@link IFrameable} interface. The expected sequence is similar to the
 * sequence in {@link IStreamTarget}, with added calls to {@link #incomingSeparator(Object)} between
 * {@link #beginOfStream()} and {@link #endOfStream()}. It is left open for the programmer to decide
 * the exact 'contract' regarding separators. A common 'contract' is that a separator is related to
 * all succeeding &lt;OBJ&gt; objects, up to the next separator.
 *
 * @param <T> see {@link IStreamTarget}
 * @param <U> type of separator signal received in the {@link #incomingSeparator(Object)} method
 */
public interface IFrameableTarget<T, U> extends IStreamTarget<T>, IFrameable<U> {

}
