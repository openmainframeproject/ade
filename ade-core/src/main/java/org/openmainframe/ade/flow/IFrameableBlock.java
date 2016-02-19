/*
 
    Copyright IBM Corp. 2012, 2016
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
 * Extends {@link IFrameableTarget}&lt;OBJ_IN, SEP_IN&gt;
 * and {@link IStreamSource}&lt;OBJ_IN&gt;
 * @see {@link IFrameableTarget}&lt;OBJ_IN, SEP_IN&gt;
 * @see {@link IStreamSource}&lt;OBJ_IN&gt;
 * @param <T> see {@link IFrameableTarget}
 * @param <U> see {@link IFrameableTarget}
 * @param <V> see {@link IStreamSource}
 */
public interface IFrameableBlock<T, U, V> extends
        IFrameableTarget<T, U>, IStreamSource<V> {
}
