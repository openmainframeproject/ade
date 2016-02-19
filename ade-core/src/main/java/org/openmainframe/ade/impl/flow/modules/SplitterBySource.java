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
package org.openmainframe.ade.impl.flow.modules;

import java.util.HashSet;
import java.util.Set;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.impl.actions.Action;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.modules.HubToIntervalFramers;

/**
 * This class organizes ade data for a given Source Group and Action.
 *
 */
public class SplitterBySource extends SplitterBySourceGroup {

    private String m_sourceGroup;

    /**
     * Split the ade data using the given source group and action.
     * @param sourceGroup name of source group
     * @param action type of action
     */
    public SplitterBySource(String sourceGroup, Action action) {
        super(action);
        m_sourceGroup = sourceGroup;
    }

    @Override
    protected final String genSepKey(TimeSeparator sep) throws AdeException {
        return sep.getSource();
    }

    @Override
    protected final String genKey(IMessageInstance mi) throws AdeException {
        return mi.getSourceId();
    }

    @Override
    protected final Set<IFrameableTarget<IMessageInstance, TimeSeparator>> getFrameableTargets(String key)
            throws AdeException {
        final ISource src = Ade.getAde().getDataStore().sources().getSource(key);
        final Set<IFrameableTarget<IMessageInstance, TimeSeparator>> frameableTargets = new HashSet<IFrameableTarget<IMessageInstance, TimeSeparator>>();
        frameableTargets.add(new HubToIntervalFramers(m_sourceGroup, src, m_action));
        return frameableTargets;
    }

}
