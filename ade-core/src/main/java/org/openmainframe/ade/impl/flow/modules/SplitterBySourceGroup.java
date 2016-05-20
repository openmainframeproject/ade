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
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.flow.FlowUtils;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.impl.actions.Action;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.split.AbstractSplittingFrameableFramingBlock;
import org.openmainframe.ade.impl.stats.StatsCollectorFactory;

/**
 * Use this class to organize the Ade data by {@link Action}.
 *
 */
public class SplitterBySourceGroup extends
        AbstractSplittingFrameableFramingBlock<IMessageInstance, TimeSeparator, String> {

    protected TimeSeparator m_prevGlobalSep;
    protected Action m_action;

    /**
     * Provide an action to organize the data.
     * 
     * @param action the action to split the data
     */
    public SplitterBySourceGroup(Action action) {
        super();
        m_action = action;
    }

    public final Action getAction() {
        return m_action;
    }

    @Override
    protected String genKey(IMessageInstance mi) throws AdeException {
        final String source = mi.getSourceId();
        return Ade.getAde().getDataStore().sources().getAnalysisGroup(source);
    }

    @Override
    protected String genSepKey(TimeSeparator sep) throws AdeException {
        final String source = sep.getSource();
        if (source == null) {
            return null;
        }
        return Ade.getAde().getDataStore().sources().getAnalysisGroup(source);
    }

    @Override
    protected final void addMissingTargets(String key) throws AdeException {
        // Notice that this code is not complete as the encapsulating class can only deal with
        // target sets that are non-intersecting. Otherwise, error WILL occur!!!
        final Set<IFrameableTarget<IMessageInstance, TimeSeparator>> frameableTargets = getFrameableTargets(key);
        addFrameableTargets(frameableTargets, key);
        // send begin of stream
        FlowUtils.sendBeginOfStream(frameableTargets);
        // If a preceding global separator exists, send it
        if (m_prevGlobalSep != null) {
            FlowUtils.sendSeparator(m_prevGlobalSep, frameableTargets);
        }
    }

    protected Set<IFrameableTarget<IMessageInstance, TimeSeparator>> getFrameableTargets(String key)
            throws AdeException {
        final Set<IFrameableTarget<IMessageInstance, TimeSeparator>> frameableTargets = new HashSet<>();

        switch (m_action) {
            case MESSAGE_INSTANCE_STATISTICS:
                frameableTargets.add(StatsCollectorFactory.newMessageInstanceStatsCollector(key));
                break;
            case INTERVAL_STATISTICS:
                // cascade into next
            case ANALYZE_LOG:
                // cascade into next
            case UPLOAD_AND_ANALYZE_LOG:
                // cascade into next
            case UPLOAD_LOG:
                frameableTargets.add(new SplitterBySource(key, m_action));
                break;
            default:
                throw new AdeInternalException("Unknown action: " + m_action);
        }

        return frameableTargets;
    }

    @Override
    protected final void handleNullSepKey(TimeSeparator sep) throws AdeException {
        // keep the sep for future frameable targets
        m_prevGlobalSep = sep;
        // send to all existing targets
        FlowUtils.sendSeparator(sep, getAllFrameables());
    }

    @Override
    protected final void handleNullKey(IMessageInstance mi) throws AdeException {
        throw new AdeInternalException("Should not have gotten to this exception! (should have failed before)");
    }

    @Override
    public void beginOfStream() throws AdeException {
        // do nothing. beginOfStream will be sent to each source when it is created
    }

    @Override
    public void endOfStream() throws AdeException {
        FlowUtils.sendEndOfStream(getAllTargets());
    }

}
