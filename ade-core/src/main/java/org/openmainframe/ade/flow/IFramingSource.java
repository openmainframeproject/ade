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
 * Extends the {@link IStreamSource}&lt;OBJ&gt; interface, but also intends the object to
 * produce objects of type &lt;SEP&gt; and typically send them to {@link IFrameableTarget}&lt;SEP&gt
 * objects using the {@link IFrameableTarget#incomingSeparator(Object)} method.
 *
 * @param <T> see {@link IStreamSource}&lt;OBJ&gt
 * @param <U> the type of the separating object
 */
public interface IFramingSource<T, U> extends IStreamSource<T> {

}
