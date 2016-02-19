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
package org.openmainframe.ade.impl.flow.modules;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.actions.Action;
import org.openmainframe.ade.impl.data.IntervalClassificationEnum;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.factory.jaxb.OutputerType;
import org.openmainframe.ade.impl.flow.hub.HubFrameableFramingBlock;
import org.openmainframe.ade.impl.stats.StatsCollectorFactory;
import org.openmainframe.ade.output.OutputerFactory;
import org.openmainframe.ade.summary.SummarizationProperties;

public class HubToIntervalFramers extends
        HubFrameableFramingBlock<IMessageInstance, TimeSeparator, IMessageInstance, TimeSeparator> {

    private String m_sourceGroup;
    private ISource m_source;
    private Action m_action;
    private SummarizationProperties uploadSumProps = new SummarizationProperties(false, true, true);
    private SummarizationProperties analyzeSumProps = new SummarizationProperties(true, true, true);

    /**
     * Construct a new HubToIntervalFramers.
     * 
     * @param sourceGroup the group of the source.
     * @param source the source.
     * @param action the action.
     */
    public HubToIntervalFramers(String sourceGroup, ISource source, Action action) throws AdeException {
        m_sourceGroup = sourceGroup;
        m_source = source;
        m_action = action;
        initIntervalFramers();
    }

    private void initIntervalFramers() throws AdeException {

        boolean foundAction = false;

        if (m_action == Action.UPLOAD_AND_ANALYZE_LOG || m_action == Action.UPLOAD_LOG) {
            addUploadFlow();
            foundAction = true;
        }

        if (m_action == Action.UPLOAD_AND_ANALYZE_LOG || m_action == Action.ANALYZE_LOG) {
            addAnalysisFlow();
            foundAction = true;
        }

        if (m_action == Action.INTERVAL_STATISTICS) {
            addStatisticsFlow();
            foundAction = true;
        }

        if (!foundAction) {
            throw new AdeInternalException("Cant handle action value: " + m_action);
        }

    }

    public final IntervalBuilder addUploadFlow() throws AdeException {
        final IntervalFramer intervalFramer = Ade.getAde().getFlowFactory().getFlowBySourceId(m_source.getSourceId()).getUploadIntervalFramer();
        final FramingFlowType framingFlowType = intervalFramer.getFramingFlowType();
        final IntervalBuilder intervalBuilder = intervalFramer.getIntervalBuilder(m_source,
                uploadSumProps, framingFlowType, IntervalClassificationEnum.REGULAR, m_action);
        intervalBuilder.addTarget(new IntervalDbUploader(m_source, framingFlowType));
        // connect the source flow blocks
        intervalFramer.addTarget(intervalBuilder);
        // add to result

        addTarget(intervalFramer);
        return intervalBuilder;
    }

    public final void addAnalysisFlow() throws AdeException {
        final IntervalFramer intervalFramer = Ade.getAde().getFlowFactory().getFlowBySourceId(m_source.getSourceId()).getAnalysisIntervalFramer();
        final FramingFlowType framingFlowType = intervalFramer.getFramingFlowType();
        final IntervalBuilder intervalBuilder = intervalFramer.getIntervalBuilder(m_source,
                analyzeSumProps, framingFlowType, IntervalClassificationEnum.REGULAR, m_action);
        createAnalysisFlow(intervalBuilder);
        // connect the source flow blocks
        intervalFramer.addTarget(intervalBuilder);

        // add to result
        addTarget(intervalFramer);
    }

    private void createAnalysisFlow(IntervalBuilder intervalBuilder) throws AdeException {
        final IntervalAnalyzer intervalAnalyzer = new IntervalAnalyzer(m_source);
        intervalBuilder.addTarget(intervalAnalyzer);
        for (OutputerType outputer : Ade.getAde().getFlowFactory().getFlowBySourceId(m_source.getSourceId()).getOutputers()) {
            intervalAnalyzer.addTarget(
                    OutputerFactory.createEmptyOutputer(outputer, m_source, intervalBuilder.getFramingFlowType()));
        }
    }

    public final void addStatisticsFlow() throws AdeException {
        for (IntervalFramer intervalFramer : Ade.getAde().getFlowFactory().getFlowBySourceId(m_source.getSourceId()).getAllIntervalFramers()) {
            final FramingFlowType framingFlowType = intervalFramer.getFramingFlowType();
            IntervalBuilder intervalBuilder = null;
            intervalBuilder = intervalFramer.getIntervalBuilder(
                    m_source, uploadSumProps, framingFlowType, IntervalClassificationEnum.REGULAR, m_action);
            intervalBuilder.addTarget(StatsCollectorFactory.newIntervalStatsCollector(m_sourceGroup, framingFlowType));
            // connect the source flow blocks
            intervalFramer.addTarget(intervalBuilder);

            // add to result
            addTarget(intervalFramer);
        }
    }

    @Override
    public final void beginOfStream() throws AdeException {
        sendBeginOfStream();
    }

    @Override
    public final void incomingObject(IMessageInstance obj) throws AdeException {
        sendObject(obj);
    }

    @Override
    public final void endOfStream() throws AdeException {
        sendEndOfStream();
    }

    @Override
    public final void incomingSeparator(TimeSeparator sep) throws AdeException {
        sendSeparator(sep);
    }

}
