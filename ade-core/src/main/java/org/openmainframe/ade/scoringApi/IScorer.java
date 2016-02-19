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

/** A learner of T that also supports analyzing S objects based on the created
 * model
 */
public interface IScorer<S, T> extends ILearner<T> {

    /** The name of the main statistic scorer is expected to produce */
    public static final String MAIN = "main";
    /** Generic name for an anomaly statistic */
    public static final String ANOMALY = "anomaly";
    public static final String LOG_PROB = "logProb";

    /** Perform the scoring of an object
     * 
     * @param scoredElement The element for scoring
     * @param contextElement The scoredElement is scored in the context of this object
     * @return A statistics chart containing a list of statistics produced by this scorer. Most scoreres are
     * expected to produce on statistic named MAIN
     * @throws AdeException
     */
    StatisticsChart getScore(S scoredElement, T contextElement) throws AdeException;

    String getId();

    void setId(String id);

    /** 
     * to be called after de-serialization, for setting up whatever may need setting up.
    * @throws AdeException 
     */
    public void wakeUp() throws AdeException;

}
