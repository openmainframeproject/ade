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
package org.openmainframe.ade.impl.flow.hub;

import org.openmainframe.ade.flow.IFrameableFramingBlock;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.flow.IStreamSource;
import org.openmainframe.ade.flow.IStreamTarget;

/**
 * This class extends {@link HubFramingBlock} and implements {@link IFrameableFramingBlock}
 * so it will have {@link IStreamTarget}, {@link Frameable}, {@link IStreamSource}, and {@link Framing}.
 * 
 * @param <T> - see {@link IStreamTarget}
 * @param <U> - see {@link IFrameable}
 * @param <V> - see {@link IStreamSource}
 * @param <W> - see {@link Framing}
 * 
 * @see HubFramingSource
 * @see IFrameableTarget
 */
public abstract class HubFrameableFramingBlock<T, U, V, W>
        extends HubFramingBlock<T, V, W> implements
        IFrameableFramingBlock<T, U, V, W> {

}
