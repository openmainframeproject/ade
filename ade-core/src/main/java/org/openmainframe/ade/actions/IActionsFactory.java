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
package org.openmainframe.ade.actions;

import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.flow.IMessageInstanceTarget;
import org.openmainframe.ade.impl.data.TextClusteringComponentModel;
import org.openmainframe.ade.impl.data.TimeSeparator;

/**
 * Factory for action classes.
 */
public interface IActionsFactory {

    /**
     * Create a new log uploader.
     * @return A {@link IMessageInstanceTarget} that builds
     *     {@link IInterval} objects out of incoming {@link IMessageInstance}
     *     and {@link TimeSeparator} objects, and uploads them to
     *     the DB, so we can later use them for training
     */
    IMessageInstanceTarget newLogUploader();

    /**
     * Create a new log analyzer.
     * @return A {@link IMessageInstanceTarget} that builds
     *     {@link IInterval} objects out of incoming {@link IMessageInstance}
     *     and {@link TimeSeparator} objects, analyzes
     *     them, and outputs an XML file for each interval.
     */
    IMessageInstanceTarget newLogAnalyzer();

    /**
     * Create a new log uploader/analyzer.
     * @return A {@link IMessageInstanceTarget} that builds
     *     {@link IInterval} objects out of incoming {@link IMessageInstance}
     *     and {@link TimeSeparator} objects, uploads and then analyzes
     *     them, and outputs an XML file for each interval.
     */
    IMessageInstanceTarget newLogUploaderAnalyzer();

    /**
     * Create a new interval statistics gatherer.
     * @return A {@link IMessageInstanceTarget} that groups
     *     incoming {@link IMessageInstance} and {@link TimeSeparator}
     *     into {@link IInterval} objects, gathers statistics on them,
     *     and outputs those statistics to files.
     */
    IMessageInstanceTarget newIntervalStatistics();

    /**
     * Create a new message instance statistics gatherer.
     * @return A {@link IMessageInstanceTarget} that gathers
     *     statistics out of incoming {@link IMessageInstance}
     *     and {@link TimeSeparator} objects, and outputs them
     *     to files.
     */
    IMessageInstanceTarget newMessageInstanceStatistics();

    /**
     * Creates a ParsingQualityReporter object.
     * This object can be plugged in to a AdeReader to produce a file summarizing parsing quality
     * @return the ParsingQualityReporter object
     */
    IParsingQualityReporter createParsingQualityReporter();

    /**
     * Creates a {@link TextClusteringComponentModel} object.
     * 
     * @param updateDataStore if true, newly created clusters are stored in the datastore.
     * @return a new {@link OnlineTextClustering} object 
     */
    TextClusteringComponentModel getTextClusteringModel(boolean updateDataStore);

}
