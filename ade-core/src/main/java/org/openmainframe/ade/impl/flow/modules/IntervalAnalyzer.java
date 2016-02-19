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

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.hub.HubFrameableBlock;
import org.openmainframe.ade.scoringApi.IMainScorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A block that analyzes an interval and outputs an analyzed interval. All intervals are from the same source.
 */
public class IntervalAnalyzer extends HubFrameableBlock<IInterval, TimeSeparator, IAnalyzedInterval> {
    private ISource m_source;
    @SuppressWarnings("unused")
    private static Logger s_logger = LoggerFactory.getLogger(IntervalAnalyzer.class);

    /**
     * Creates a new {@link IntervalAnalyzer} for the given source.
     * @param source source for this {@link IntervalAnalyzer}. The model is set according to the source.
     * @throws AdeException if model fails to be loaded.
     */
    public IntervalAnalyzer(ISource source) throws AdeException {
        m_source = source;
    }

    private IMainScorer getModel() throws AdeException {
        return Ade.getAde().getDataStore().models().loadDefaultModel(Ade.getAde().getDataStore().sources().getAnalysisGroup(m_source.getSourceId()));
    }

    /**
     * check to see if we need to load a new model (either we have no model but the DB holds a default one, or we have one but there is a newer one)
     * and if this is the case, load that model.  
     * If we hold a model at the end of this process (either a new one of an old one),
     * we check to see if the user rules need updating, and if so we update them.
     * @throws AdeException
     */

    @Override
    public void incomingObject(IInterval interval) throws AdeException,
            AdeFlowException {
        final IMainScorer ms = getModel();
        if (ms != null) {
            final IAnalyzedInterval analyzedInterval = ms.analyze(interval);
            sendObject(analyzedInterval);
        }
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        sendBeginOfStream();
    }

    @Override
    public void endOfStream() throws AdeException, AdeFlowException {
        sendEndOfStream();
    }

    @Override
    public void incomingSeparator(TimeSeparator sep) throws AdeException,
            AdeFlowException {
        final IMainScorer ms = getModel();
        if (ms != null) {
            ms.incomingSeparator(sep);
        }

    }

}
