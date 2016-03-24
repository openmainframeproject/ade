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
package org.openmainframe.ade.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.impl.dataStore.GroupRead;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.modules.EventLogTrainer;
import org.openmainframe.ade.impl.flow.modules.IntervalDbDownloader;
import org.openmainframe.ade.models.IModelMetaData;
import org.openmainframe.ade.scoringApi.IMainScorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

/**
 * Main class for training message logs only. 
 * <br/><br/>Run main with the 'h' flag for usage.
 */
public class TrainLogs extends Train {

    private static Logger s_logger = LoggerFactory.getLogger(TrainLogs.class);

    public static void trainLogs(int analysisGroup, DateTime startDate, DateTime endDate) throws AdeFlowException, AdeException {
        final List<IPeriod> analysisGroupPeriods = getAnalysisGroupPeriods(analysisGroup, startDate, endDate);

        final List<IPeriod> includedPeriods = new ArrayList<IPeriod>();
        final List<IPeriod> excludedPeriods = new ArrayList<IPeriod>();
        for (IPeriod period : analysisGroupPeriods) {
            if (period.getExcludeFromTraining()) {
                excludedPeriods.add(period);
            } else {
                includedPeriods.add(period);
            }
        }
        if (analysisGroupPeriods.isEmpty()) {
            String groupName = GroupRead.getAnalysisGroupName(analysisGroup);
            s_logger.warn("No data to train " + groupName + " - skipping");
            System.out.println("No data to train " + groupName + " - skipping");
            return;
        }
        // train on logs iteratively
        final EventLogTrainer eventLogTrainer = new EventLogTrainer(analysisGroup);
        while (eventLogTrainer.requiresAnotherIteration()) {
            final FramingFlowType requiredFlowType = eventLogTrainer.getRequiredIterationType();
            final IntervalDbDownloader intervalSource = new IntervalDbDownloader(includedPeriods, requiredFlowType);
            intervalSource.addTarget(eventLogTrainer);
            intervalSource.run();
        }
        final IMainScorer eventsModel = eventLogTrainer.getFinalMainScorer();

        final IModelMetaData modelMetaData = eventsModel.getModelMetaData();

        // exclude periods
        for (IPeriod period : excludedPeriods) {
            modelMetaData.excludePeriod(period);
        }

        boolean setAsDefaultModel = false;
        if (analysisGroupPeriods.size() >= Ade.getAde().getConfigProperties().getMinimalRequieredTrainPeriod()) {
            setAsDefaultModel = true;
        }
        if (eventsModel.getModelMetaData().getStartTime() == null) {
            String groupName = GroupRead.getAnalysisGroupName(analysisGroup);
            s_logger.warn("No intervals from the right flow to train " + groupName + " - skipping");
            System.out.println("No intervals from the right flow to train " + groupName + " - skipping");
        } else {
            Ade.getAde().getDataStore().models().storeModel(eventsModel, setAsDefaultModel);
        }
    }

    public static List<IPeriod> getAnalysisGroupPeriods(int analysisGroup, DateTime startDate, DateTime endDate) throws AdeException {
        final List<IPeriod> analysisGroupPeriods = new ArrayList<IPeriod>();

        // get all of the periods of the analysis group and fill analysisGroupPeriods
        for (ISource source : Ade.getAde().getDataStore().sources().getSourcesForAnalysisGroup(analysisGroup)) {

            final Collection<IPeriod> periods = getSourcePeriods(source, startDate, endDate);
            if (periods.isEmpty()) {
                String groupName = GroupRead.getAnalysisGroupName(analysisGroup);
                s_logger.warn("Source " + source + " of source group " + groupName + " has no data - skipping");
                continue;
            }
            analysisGroupPeriods.addAll(periods);
        }
        return analysisGroupPeriods;
    }

    public static Collection<IPeriod> getSourcePeriods(ISource source, DateTime startDate, DateTime endDate) throws AdeException {
        final Date start = startDate == null ? null : startDate.toDate();
        final Date end = endDate == null ? null : endDate.toDate();
        final Collection<IPeriod> periods = Ade.getAde().getDataStore().periods().getAllPeriods(source, start, end);

        final StringBuilder sb = new StringBuilder(String.format("Source %s "
                + "has %d periods ", source.getSourceId(), periods.size()));
        if (startDate != null && endDate != null) {
            sb.append("between " + startDate + " and " + endDate);
        } else if (startDate == null && endDate != null) {
            sb.append("before " + endDate);
        } else if (startDate != null && endDate == null) {
            sb.append("since" + startDate);
        } else if (startDate == null && endDate == null) {
            sb.append("in total");
        }
        s_logger.info(sb.toString());
        return periods;
    }

    @Override
    protected final void trainAnalysisGroup(int analysisGroup, DateTime startDate, DateTime endDate) throws AdeException {
        trainLogs(analysisGroup, startDate, endDate);
    }

}
