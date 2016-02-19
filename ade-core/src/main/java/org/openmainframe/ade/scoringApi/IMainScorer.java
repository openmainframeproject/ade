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
package org.openmainframe.ade.scoringApi;

import java.io.Serializable;
import java.util.List;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.flow.IStreamTarget;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.models.IModel;

/**
 * A top-level scorer that represents a complete model used for analyzing log files.
 * The {@link IMainScorer} is used for training, by acting as a {@link IStreamTarget} of
 * {@link IInterval}s, and for analysis, by calling {@link #analyze(IInterval)}.
 * 
 */
public interface IMainScorer extends ILearner<IInterval>, Serializable, IModel {

    /**
     * The framer type required for the next iteration.
     * This method should be called only if {@link #needsAnotherIteration()}
     * returned true.
     */
    FramingFlowType getRequiredIntervalFramer() throws AdeException;

    /**
     * Perform analysis on given interval
     * 
     * @param interval Interval to be analyzed
     * @return The analysis results for that interval
     * @throws AdeException
     */
    IAnalyzedInterval analyze(IInterval interval) throws AdeException;

    List<MessageScorer> getMessageScorers() throws AdeException;

    /** 
     * to be called after de-serialization, for setting up whatever may need setting up.
    * @throws AdeException 
     */
    public void wakeUp() throws AdeException;

    IScorer<?, IAnalyzedInterval> getTrainedScorer(String key);

}
