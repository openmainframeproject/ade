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
package org.openmainframe.ade.impl.flow.split;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.flow.IFrameableFramingBlock;

/**
 * This class extends {@link AbstractSplittingFramingBlock} for a convenient way to add separators.
 *
 * @param <T> The Frameable objects that will be stored into sets
 * @param <U> Separator between those Frameable objects
 * @param <V> Key to represent the set of the Frameable objects
 */
public abstract class AbstractSplittingFrameableFramingBlock<T, U, V extends Comparable<?>> extends
        AbstractSplittingFramingBlock<T, U, V> implements
        IFrameableFramingBlock<T, U, T, U> {

    @Override
    public final void incomingSeparator(U sep) throws AdeException {
        sendToFrameables(sep);
    }

}
