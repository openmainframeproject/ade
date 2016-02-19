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
package org.openmainframe.ade.data;

import java.util.Collection;

import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.patches.Version;

/** An AnalyezdInterval object contains summary results of an Interval */
public interface IAnalyzedInterval extends IBasicInterval {

    /** @return The interval for which these results apply */
    public IInterval getInterval();

    /**Returns the position of the interval in the period based on its start time
     * @return the serial on the interval in the period
     */
    public int getSerialNum();

    /** Returns number of unique message ids in interval */
    public int getNumUniqueMessageIds();

    /**
     *  Returns the resulting anomaly score of the anlyzed interval.
     *  @return the anomaly score in the range 0..1010 
     * @throws AdeInternalException */
    public double getScore() throws AdeInternalException;

    /** Returns analysis results for msg-ids contained in this interval */
    public Collection<IAnalyzedMessageSummary> getAnalyzedMessages();

    /**
     * @return ade version at the time this analyzed interval was produced
     */
    public Version getAdeVersion();

    /**
     * @return the model internal id by which this analyzed interval was produced
     */
    public Integer getModelInternalId();

    /** @return The analysis results: statistics added by the scorers for this interval */
    public StatisticsChart getStatistics();
}
