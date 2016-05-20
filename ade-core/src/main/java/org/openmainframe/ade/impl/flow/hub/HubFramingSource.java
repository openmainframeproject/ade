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

import java.util.HashSet;
import java.util.Set;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.flow.FlowUtils;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.flow.IFramingSource;
import org.openmainframe.ade.flow.IStreamSource;

/**
 * This class adds the {@link #sendSeparator(Object)} functionality.
 *
 * @param <T> - see {@link iStreamSource}
 * @param <U> - see {@link Framing}
 * 
 * @see HubStreamSource
 */
public abstract class HubFramingSource<T, U> extends HubStreamSource<T> implements IFramingSource<T, U> {
    /**
     * This set holds the {@link IFrameableTarget} targets that can receive separators.
     */
    protected Set<IFrameableTarget<T, U>> m_frameableTargets = new HashSet<>();

    /**
    * Add target for the framing operators. Note that the {@link IFrameableTarget} does not have to be the same as
    * the "regular" targets: {@link StreamTarget}.
    * @param frameableTarget the target to receive T objects and U separators
    * @throws AdeFlowException see {@link HubStreamSource#addTarget(StreamTarget)} 
    */
    public void addTarget(IFrameableTarget<T, U> frameableTarget) throws AdeFlowException {
        super.addTarget(frameableTarget);
        m_frameableTargets.add(frameableTarget);
    }

    /**
     * Invoke {@link IFrameableTarget#incomingSeparator(Object)} in all {@link #m_frameableTargets}
     * with input SEP separator.
     * @param sep - the separator to be forwarded
     * @throws AdeException - see {@link Frameable#incomingSeparator(Object)}
     * @throws AdeFlowException - see {@link Frameable#incomingSeparator(Object)}
     */
    protected final void sendSeparator(U sep) throws AdeException {
        FlowUtils.sendSeparator(sep, m_frameableTargets);
    }

}
