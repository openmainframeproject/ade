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

import java.io.File;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.core.statistics.TimingStatistics;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.flow.IMultipleIterationsTarget;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.dataStore.GroupRead;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.GeneralUtils;
import org.openmainframe.ade.scoringApi.IMainScorer;
import org.openmainframe.ade.scoringApi.ScorerEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that handles the event log training.
 * 
 */
public class EventLogTrainer implements IFrameableTarget<IInterval, TimeSeparator>,
        IMultipleIterationsTarget<FramingFlowType> {
    private static final Logger logger = LoggerFactory.getLogger(EventLogTrainer.class);
    private IMainScorer m_mainScorer;

    /**
     * Construct a new EventLogTrianer.
     *  
     */
    public EventLogTrainer(int sourceGroup) throws AdeException {
        final String groupName = GroupRead.getAnalysisGroupName(sourceGroup);
        final Ade ade = Ade.getAde();
        m_mainScorer = ade.getFlowFactory().getEmptyMainScorer(sourceGroup);
        final ScorerEnvironment env = new ScorerEnvironment(groupName);
        env.m_traceOutputPath = new File(ade.getDirectoryManager().getTracePath(), groupName);
        m_mainScorer.initTraining(env);
    }

    @Override
    public final void beginOfStream() throws AdeException {
        GeneralUtils.logMemStatus("EventLogTrainer.beginOfStream");
        m_mainScorer.startIteration();
    }

    @Override
    public final void incomingObject(IInterval interval) throws AdeException {
        m_mainScorer.incomingObject(interval);
    }

    @Override
    public final void incomingSeparator(TimeSeparator sep) throws AdeException {
        m_mainScorer.incomingSeparator(sep);
    }

    @Override
    public final void endOfStream() throws AdeException {
        logger.info("Time for iterating on intervals: "
                + TimingStatistics.getSummary("EventLogTrainer.incomingObject(Interval)"));
        m_mainScorer.endOfStream();
    }

    @Override
    public final boolean requiresAnotherIteration() throws AdeException {
        return m_mainScorer.needsAnotherIteration();
    }

    @Override
    public final FramingFlowType getRequiredIterationType() throws AdeException {
        return m_mainScorer.getRequiredIntervalFramer();
    }

    /**
     * Get the {@link IMainScorer} which is used for training.
     * 
     * @return the {@link IMainScorer}
     * @throws {@link AdeException} - if the {@link IMainScorer} requires another iteration.
     */
    public final IMainScorer getFinalMainScorer() throws AdeException {
        if (m_mainScorer.needsAnotherIteration()) {
            throw new AdeFlowException("MainScorer not ready yet");
        }
        return m_mainScorer;
    }

}
