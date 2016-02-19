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

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Map;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.utils.IStructuredOutputWriter;

/** An interface for an object capable of learning from a stream of T objects */
public interface ILearner<T> extends Serializable, IFrameableTarget<T, TimeSeparator> {

    /** Returns the learner's name, as will be reflected in user output */
    String getName();

    /** Sets the learner's name, as will be reflected in the user output */
    void setName(String name);

    /** Determines if this learner needs another iteration. A learner may require 0 iterations */
    boolean needsAnotherIteration() throws AdeException;

    /** Start a training session. Needs to be called once before training starts.
     * 
     * @param globals Environment variables required for training, e.g., output directory for trace files
     * 
     */
    void initTraining(ScorerEnvironment globals) throws AdeException;

    /** Begin a learning iteration */
    void startIteration() throws AdeException;

    /** Print full object state for debug purposes */
    void debugPrint(PrintStream out) throws AdeException;

    /** Print general summary of model for user */
    void printGeneralUserData(IStructuredOutputWriter out) throws Exception;

    /** Set user supplied arguments */
    void setArguments(Map<String, Object> props) throws AdeException;

}
