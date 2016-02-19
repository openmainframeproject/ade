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

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.impl.data.TimeSeparator;

/**
 * An {@link IScorer} that also implements a streaming trainer {@link AbstractTrainer}
 *
 * @param <S> type of elements this scorer scores
 * @param <T> type of elements this scorer is being trained on
 */
public abstract class AbstractScorer<S, T> extends AbstractTrainer<T> implements IScorer<S, T> {

    private static final long serialVersionUID = 1L;

    private String mId = null;

    private String m_analysisGroup = null;

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public void setId(String id) {
        mId = id;
    }

    @Override
    public void incomingSeparator(TimeSeparator sep) throws AdeException,
            AdeFlowException {
        // Default is do nothing.  If the scorer keeps a history, it will need to override this.
    }

    @Override
    public void wakeUp() throws AdeException {
        // default is to do nothing.  Overwrite if needed
    }

    public void setAnalysisGroup(String analysisGroup) {
        m_analysisGroup = analysisGroup;
    }

    public String getAnalysisGroup() {
        return m_analysisGroup;
    }

}
