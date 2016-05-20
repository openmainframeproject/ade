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
import org.openmainframe.ade.flow.IStreamSource;
import org.openmainframe.ade.flow.IStreamTarget;

/**
 * This class implements the {@link IStreamSource} interface.
 * It should be used when you want to treat your target without differentiation, 
 * meaning that you want to call the {@link IStreamTarget} interface methods for all targets together.
 *
 * @param <T> see {@link IStreamSource}
 */
public abstract class HubStreamSource<T> implements IStreamSource<T> {

    /**
     * The list of added targets is kept in a Set as we do not allow target duplication.
     */
    protected Set<IStreamTarget<T>> m_targets = new HashSet<>();

    /**
     * Add a target for this object that will receive objects of type T once they are generated. 
     * @param target - a target to add. More than one target can be added to each {@link IStreamSource}
     * @throws AdeException when trying to add a target that has already been added (which is forbidden) 
     */
    public void addTarget(IStreamTarget<T> target) throws AdeFlowException {
        if (m_targets.contains(target)) {
            throw new AdeFlowException(target + " has already been added as a target!");
        }
        m_targets.add(target);
    }

    /**
     * Remove a target from this object and make it stop receiving objects of type T.
     * @param target - a target to be removed.
     * @throws AdeException when trying to remove a target has not been added 
     */
    public void removeTarget(IStreamTarget<T> target) throws AdeFlowException {
        if (!m_targets.contains(target)) {
            throw new AdeFlowException(target + " is not a target!");
        }
        m_targets.remove(target);
    }

    /**
     * Invoke {@link IStreamTarget#beginOfStream()} in all targets. 
     * @throws AdeException - see {@link IStreamTarget#beginOfStream()}
     */
    protected final void sendBeginOfStream() throws AdeException {
        FlowUtils.sendBeginOfStream(m_targets);
    }

    /**
     * Invoke {@link IStreamTarget#incomingObject(Object)} in all targets with the input T object.
     * @param obj - the object to be forwarded
     * @throws AdeException - see {@link IStreamTarget#incomingObject(Object)}
     */
    protected final void sendObject(T obj) throws AdeException {
        FlowUtils.sendObject(obj, m_targets);
    }

    /**
     * Invoke {@link IStreamTarget#endOfStream()} in all targets. 
     * @throws AdeException - see {@link IStreamTarget#endOfStream()}
     */
    protected final void sendEndOfStream() throws AdeException {
        FlowUtils.sendEndOfStream(m_targets);
    }

}
