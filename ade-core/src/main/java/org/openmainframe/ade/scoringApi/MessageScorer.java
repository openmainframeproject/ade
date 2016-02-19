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

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.utils.IStructuredOutputWriter;

/** An object for scoring messages. 
 * It receives AnalyzedMessageSummary for scoring, so it can access statistics from previous scorers.
 * It receives AnalyzedInterval in training and as context in scoring,
 * so it can see the full picture of relations between messages in the interval. */
public abstract class MessageScorer extends AbstractScorer<IAnalyzedMessageSummary, IAnalyzedInterval> {

    private static final long serialVersionUID = 1L;

    /**
     * Produces a section of the model that is related to given message id.
     * The information printed here should not be repeated by printGeneralUserData().
     * 
     * @param out The output stream receiving the output
     * @param msgId The msg-id for each the section of the model should be written.
     * @throws Exception
     */
    public void printMessageUserData(IStructuredOutputWriter out, String msgId) throws Exception {

    }
}
