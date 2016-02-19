/*
 
    Copyright IBM Corp. 2015, 2016
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
package org.openmainframe.ade.ext.service.notifications;

import org.joda.time.DateTime;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;
import org.openmainframe.ade.ext.service.AdeExtUsageException;

/**
 * Exception thrown when VerifyLinuxTraining detects that the training
 * will likely fail due to insufficient message data.
 */
public class TrainingPredconditionException extends AdeExtUsageException {
    private static final long serialVersionUID = 1L;

    /**
     * Identifies the model.
     */
    private final String sourceId;

    /**
     * The operation being performed
     */
    private final AdeExtRequestType requestType;

    private final DateTime trainingSetStartDate;

    private final DateTime trainingSetEndDate;

    private final int numUniqueMessages;

    private final int numAnalysisIntervals;

    private final int numUsableAnalysisIntervals;

    private final int analysisIntervalLengthInMinutes;

    public TrainingPredconditionException(String sourceId, AdeExtRequestType requestType,
            DateTime trainingSetStartDate, DateTime trainingSetEndDate,
            int numUniqueMessages, int numAnalysisIntervals, int numUsableAnalysisIntervals,
            int analysisIntervalLengthInMinutes) {

        super("Error: Insufficient messages. No training will occur"
                + "[sourceId=" + sourceId
                + ", requestType=" + requestType + ", trainingSetStartDate="
                + trainingSetStartDate + ", trainingSetEndDate="
                + trainingSetEndDate + ", numUniqueMessages="
                + numUniqueMessages + ", numAnalysisIntervals="
                + numAnalysisIntervals + ", numUsableAnalysisIntervals="
                + numUsableAnalysisIntervals
                + ", analysisIntervalLengthInMinutes="
                + analysisIntervalLengthInMinutes + "]");

        this.sourceId = sourceId;
        this.requestType = requestType;
        this.trainingSetStartDate = trainingSetStartDate;
        this.trainingSetEndDate = trainingSetEndDate;
        this.numUniqueMessages = numUniqueMessages;
        this.numAnalysisIntervals = numAnalysisIntervals;
        this.numUsableAnalysisIntervals = numUsableAnalysisIntervals;
        this.analysisIntervalLengthInMinutes = analysisIntervalLengthInMinutes;
    }

    public final String getSourceId() {
        return sourceId;
    }

    public final AdeExtRequestType getRequestType() {
        return requestType;
    }

    public final DateTime getTrainingSetStartDate() {
        return trainingSetStartDate;
    }

    public final DateTime getTrainingSetEndDate() {
        return trainingSetEndDate;
    }

    public final int getNumUniqueMessages() {
        return numUniqueMessages;
    }

    public final int getNumAnalysisIntervals() {
        return numAnalysisIntervals;
    }

    public final int getNumUsableAnalysisIntervals() {
        return numUsableAnalysisIntervals;
    }

    public final int getAnalysisIntervalLengthInMinutes() {
        return analysisIntervalLengthInMinutes;
    }
}
