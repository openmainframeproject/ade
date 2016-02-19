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
package org.openmainframe.ade.scores;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.scoringApi.IntervalAnomalyScorer;

public abstract class FixedIntervalScorer extends IntervalAnomalyScorer {

    /**
     * 
     */
    private static final long serialVersionUID = 2506799109353564358L;

    public FixedIntervalScorer() {
        super();
    }

    @Override
    public boolean needsAnotherIteration() throws AdeException {
        return false;
    }

    @Override
    public void startIteration() throws AdeException {
        throw new AdeInternalException("not implemented");
    }

    @Override
    public void incomingObject(IAnalyzedInterval obj) throws AdeException {
        throw new AdeInternalException("not implemented");
    }

    @Override
    public void endOfStream() throws AdeException {
        throw new AdeInternalException("not implemented");
    }

    @Override
    protected void reset() throws AdeException {
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        // TODO Auto-generated method stub

    }

}