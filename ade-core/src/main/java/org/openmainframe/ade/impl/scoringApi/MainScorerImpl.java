/*
 
    Copyright IBM Corp. 2013, 2016
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
package org.openmainframe.ade.impl.scoringApi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.dataStore.IModelFileHandler;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.impl.actions.AnalyzedIntervalImpl;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.factory.FlowFactory.FlowTemplateFactory;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.factory.jaxb.LinkType;
import org.openmainframe.ade.impl.flow.factory.jaxb.ScoringSchemaType;
import org.openmainframe.ade.impl.flow.modules.IntervalShiftAccumulator;
import org.openmainframe.ade.impl.models.ModelMetaDataImpl;
import org.openmainframe.ade.models.IModelMetaData;
import org.openmainframe.ade.scoringApi.AbstractScorer;
import org.openmainframe.ade.scoringApi.AbstractTrainer;
import org.openmainframe.ade.scoringApi.IScorer;
import org.openmainframe.ade.scoringApi.IntervalAnomalyScorer;
import org.openmainframe.ade.scoringApi.IMainScorer;
import org.openmainframe.ade.scoringApi.MessageScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.IStructuredOutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainScorerImpl extends AbstractTrainer<IInterval> implements IMainScorer {

    /**
     * 
     */
    private static final long serialVersionUID = 4591463852834769217L;

    private static final Logger s_logger = LoggerFactory.getLogger(MainScorerImpl.class);

    public static final double HUGELOGPROB = 1001;

    private TreeMap<String, IScorer<?, IAnalyzedInterval>> m_trainedScorersMap;
    // trained scorers, sorted in a topological sort
    private Vector<String> m_scorersByOrder;
    private String m_finalMessageAnomalyScorer;
    private String m_finalIntervalAnomalyScorer;

    private final IModelMetaData m_modelMetaData;
    /**
     * maps a framing flow ID to a {@link FramingFlowType} object.
     */
    private transient FramingFlowType m_framingFlow;
    /**
     * maps a scorer id to a {@link ScoringSchemaType} object.
     */
    private transient Map<String, ScoringSchemaType> m_scorerSchemas;

    /**
     * maps a framing flow ID to all scorers that are being trained in the current iteration. Within
     * each flow ID, Scorers IDs are mapped to scorer objects.
     */
    private transient Map<String, Map<String, IScorer<?, IAnalyzedInterval>>> m_currentIterationScorers;

    private transient Set<String> m_currentIterationScorerIds;

    private transient SortedSet<String> m_allMsgIds;

    private transient Set<String> m_omitFromAnalysis;

    private int m_trainFrameFactor = 1;

    private String m_flowName;

    private transient IFrameableTarget<IInterval, TimeSeparator> m_target;

    private IntervalDispenser m_intervalDispenser;
 
    public MainScorerImpl(int analysisGroup, Map<String, ScoringSchemaType> scorerSchemas,
            FramingFlowType framingFlow, String finalMessageAnomalyScorer,
            String flowName,
            String finalIntervalAnomalyScorer) throws AdeException {
        this(analysisGroup, scorerSchemas, framingFlow
                , finalMessageAnomalyScorer, flowName, finalIntervalAnomalyScorer, 1);
    }

    /**
     * create a new, empty main scorer.
     * @param GroupKey type -- contains group name and group type. 
     * @param scorerSchemas a mapping between a scorer schema ID and a {@link ScoringSchemaType} object. 
     * @param framingFlows a mapping between a framing flow ID and a {@link FramingFlowType} object.
     * @throws AdeException
     */
    public MainScorerImpl(int analysisGroup, Map<String, ScoringSchemaType> scorerSchemas,
            FramingFlowType framingFlow, String finalMessageAnomalyScorer,
            String flowName,
            String finalIntervalAnomalyScorer, int trainFrameFactor) throws AdeException {
        m_scorerSchemas = scorerSchemas;
        m_framingFlow = framingFlow;
        m_finalIntervalAnomalyScorer = finalIntervalAnomalyScorer;
        m_finalMessageAnomalyScorer = finalMessageAnomalyScorer;

        m_trainedScorersMap = new TreeMap<>();
        m_scorersByOrder = new Vector<>();
        m_omitFromAnalysis = new TreeSet<>();
        m_currentIterationScorerIds = new TreeSet<>();
        m_modelMetaData = new ModelMetaDataImpl(analysisGroup);
        m_currentIterationScorers = new TreeMap<>();
        m_flowName = flowName;
        m_intervalDispenser = new IntervalDispenser();

        m_trainFrameFactor = trainFrameFactor;
        if (trainFrameFactor != 1) {
            final IntervalShiftAccumulator frameAccumulator = 
                    new IntervalShiftAccumulator(m_framingFlow, false, m_trainFrameFactor, false);
            frameAccumulator.addTarget(m_intervalDispenser);
            m_target = frameAccumulator;
        } else {
            m_target = m_intervalDispenser;
        }
    }

    /**
     *  Constructor for a trained main scorer
     * @param GroupKey type -- contains group name and group type.
     * @param scorers list of scorers 
     *                 NOTE: MUST BE TOPOLOGICALLY SORTED
     * @param flowName Name of flow
     * @param finalMessageAnomalyScorerId id of final message anomaly scorer
     * @param finalIntervalAnomalyScorerId id of final interval anomaly scorer
     * @throws AdeException
     */
    public MainScorerImpl(int analysisGroup, List<IScorer<?, IAnalyzedInterval>> scorers, String flowName,
            String finalMessageAnomalyScorerId,
            String finalIntervalAnomalyScorerId) throws AdeException {

        if (scorers.size() < 2) {
            throw new AdeInternalException("Expecting at least two scorers:"
                    + "final message anomaly + final interval anomaly");
        }

        m_modelMetaData = new ModelMetaDataImpl(analysisGroup);
        m_scorersByOrder = new Vector<>();
        m_trainedScorersMap = new TreeMap<>();
        m_omitFromAnalysis = new TreeSet<>();
        m_currentIterationScorerIds = new TreeSet<>();
        m_currentIterationScorers = new TreeMap<>();
        m_scorerSchemas = null;

        for (IScorer<?, IAnalyzedInterval> scorer : scorers) {
            if (scorer.getId() == null) {
                throw new AdeInternalException("Cannot set a scorer with no id");
            }
            if (m_trainedScorersMap.containsKey(scorer.getId())) {
                throw new AdeInternalException("Duplicate scorer");
            }
            m_trainedScorersMap.put(scorer.getId(), scorer);
            m_scorersByOrder.add(scorer.getId());
        }

        final IScorer<?, IAnalyzedInterval> finalMessageScorer = m_trainedScorersMap.get(finalMessageAnomalyScorerId);
        if (finalMessageScorer == null) {
            throw new AdeInternalException("Scorer " + finalMessageAnomalyScorerId + " does not exist");
        }
        if (!(finalMessageScorer instanceof MessageScorer)) {
            throw new AdeInternalException("Scorer " + finalMessageScorer.getId() + " should be a message scorer");
        }
        final IScorer<?, IAnalyzedInterval> finalIntScorer = m_trainedScorersMap.get(finalIntervalAnomalyScorerId);
        if (finalIntScorer == null) {
            throw new AdeInternalException("Scorer " + finalIntervalAnomalyScorerId + " does not exist");
        }
        if (!(finalIntScorer instanceof IntervalAnomalyScorer)) {
            throw new AdeInternalException("Scorer " + finalIntScorer.getId() + " should be an interval scorer");
        }

        m_omitFromAnalysis = new TreeSet<>();
        m_finalMessageAnomalyScorer = finalMessageScorer.getId();
        m_finalIntervalAnomalyScorer = finalIntScorer.getId();
        m_flowName = flowName;
    }

    public final String toString() {
        final StringBuilder line = new StringBuilder();
        line.append("main scorer\n");

        return line.toString();
    }
    
    /**
     * add into m_currentIterationScorers all the scores we can and need to train now.
     * @throws AdeException
     */
    private void setCurrentIterationScorers() throws AdeException {

        for (Entry<String, ScoringSchemaType> scorerSchemaEntry : m_scorerSchemas.entrySet()) {
            if (m_trainedScorersMap.containsKey(scorerSchemaEntry.getKey())
                    || m_currentIterationScorerIds.contains(scorerSchemaEntry.getKey())) {
                continue;
            }

            boolean cantAddScorer = false;
            final ScoringSchemaType scorerSchema = scorerSchemaEntry.getValue();
            // if scorer is dependent on a scorer that isn't trained yet, don't put it in the current scorers list

            int count = 0;
            for (String scorerId : scorerSchema.getDependsOn()) {
                final ScoringSchemaType dependsOnScorer = m_scorerSchemas.get(scorerId);
                count++;
                if (dependsOnScorer == null) {
                    throw new AdeUsageException("In The flow file, in the definition of the scorer with id=\"" 
                            + scorerSchema.getId() + "\", the depend on scorer number " + count
                            + " is refrencing the non-existant scorer id");
                }
                final String dependsOnScorerId = dependsOnScorer.getId();
                if (!m_trainedScorersMap.containsKey(dependsOnScorerId)) {
                    cantAddScorer = true;
                    break;
                }
            }
            for (LinkType dependsOnScorerLink : scorerSchema.getLinkedScorer()) {
                if (dependsOnScorerLink.getScorer() == null) {
                    // we the linked scorer does not exist in the flow file
                    throw new AdeUsageException("In The flow file, in the definition of the scorer with id=\"" 
                            + scorerSchema.getId() 
                            + "\", the linked scorer with Key=\"" + dependsOnScorerLink.getKey() 
                            + "\" is refrencing the non-existant scorer id");
                }
                final String scorer = dependsOnScorerLink.getScorer();
                final String dependsOnScorerId = scorer;
                if (!m_trainedScorersMap.containsKey(dependsOnScorerId)) {
                    cantAddScorer = true;
                    break;
                }
            }
            if (cantAddScorer) {
                continue;
            }
            // we now know that the scorer only requires other scorers that are already trained.

            final IScorer<?, IAnalyzedInterval> emptyScorer = ScorerFactory.createEmptyScorer(scorerSchema, this);
            if (emptyScorer instanceof AbstractScorer<?, ?>) {
                ((AbstractScorer<?, IAnalyzedInterval>) emptyScorer).setAnalysisGroup(getModelMetaData().getGroupName());
            }
            String currentFramingFlowId = scorerSchema.getTrainingFramingFlow();
            if (currentFramingFlowId == null) {
                currentFramingFlowId = m_framingFlow.getName();
            }

            // if the scorer has no training flow Id, it can be moved directly to the trained scorers list
            if (currentFramingFlowId == null) {
                // if it has no training flow id, it must not require an iteration on intervals to get trained
                if (emptyScorer.needsAnotherIteration()) {
                    throw new AdeUsageException("scorer " + scorerSchema.getId() 
                              + " requires training, but has no flow defined for training");
                } else {
                    m_omitFromAnalysis.add(scorerSchemaEntry.getKey());
                    m_trainedScorersMap.put(scorerSchema.getId(), emptyScorer);
                    m_scorersByOrder.add(scorerSchema.getId());
                    s_logger.info("adding " + emptyScorer.getName() + " to trained scorers");
                    continue;
                }

            }

            // if the scorer needs another iteration, put it in, else - put it in the trained scorers
            if (emptyScorer.needsAnotherIteration()) {
                Map<String, IScorer<?, IAnalyzedInterval>> currentIterationScorersForFlow = 
                        m_currentIterationScorers.get(currentFramingFlowId);

                if (currentIterationScorersForFlow == null) {
                    currentIterationScorersForFlow = new TreeMap<>();
                    m_currentIterationScorers.put(currentFramingFlowId, currentIterationScorersForFlow);
                }
                currentIterationScorersForFlow.put(scorerSchema.getId(), emptyScorer);
                m_currentIterationScorerIds.add(scorerSchema.getId());
                emptyScorer.initTraining(m_scorerEnvironment);
                s_logger.info("adding " + emptyScorer.getName() + " to current iteration scorers");
            } else {
                m_trainedScorersMap.put(scorerSchema.getId(), emptyScorer);
                m_scorersByOrder.add(scorerSchema.getId());
                s_logger.info("adding " + emptyScorer.getName() + " to trained scorers");

                // It mat happen that some scorer (denoted A) depends on another 
                // scorer denoted B), where B does not require an iteration. Combine
                // this scenario with A appearing before B in m_scorerSchemas.entrySet,
                // and you can be left with an empty m_currentIterationScorers, with
                // scorer A still missing. Thus, re-invoking the method recursively 
                // will solve the problem.
                setCurrentIterationScorers();
                return;
            }
        }
    }

    @Override
    public final boolean needsAnotherIteration() throws AdeException {
        return !m_currentIterationScorers.isEmpty();
    }

    @Override
    public final void startIteration() throws AdeException {
        s_logger.info("starting iteration main scorer");
        final String flowID = m_currentIterationScorers.keySet().iterator().next();
        for (Entry<String, IScorer<?, IAnalyzedInterval>> scorerEntry 
                : m_currentIterationScorers.get(flowID).entrySet()) {
            s_logger.info("starting iteration scorer " + scorerEntry.getKey() + " - "
                    + scorerEntry.getValue().getName());
            scorerEntry.getValue().startIteration();
        }
        m_allMsgIds = new TreeSet<>();

    }

    @Override
    public final void debugPrint(PrintStream out) throws AdeException {
        out.println("Meta data");
        out.println("" + m_modelMetaData);
        out.println("scorers:");
        for (IScorer<?, IAnalyzedInterval> scorer : m_trainedScorersMap.values()) {
            scorer.debugPrint(out);
        }
    }

    @Override
    public final void printGeneralUserData(IStructuredOutputWriter docWriter)
            throws Exception {

        final IStructuredOutputWriter modelWriter = docWriter.child("model");
        final IStructuredOutputWriter perMsgWriter = modelWriter.child("perMessageData");
        if (m_allMsgIds != null) {
            for (String id : m_allMsgIds) {
                final IStructuredOutputWriter msgWriter = perMsgWriter.child("message", "id", id);
                for (IScorer<?, IAnalyzedInterval> scorer : m_trainedScorersMap.values()) {
                    MessageScorer ms;
                    if (scorer instanceof MessageScorer) {
                        ms = (MessageScorer) scorer;
                    } else {
                        continue;
                    }
                    final IStructuredOutputWriter scoreWriter =
                            msgWriter.child("score", "name", ms.getClass().getName());
                    ms.printMessageUserData(scoreWriter, id);
                    scoreWriter.close();
                }
                msgWriter.close();
            }
        }
        perMsgWriter.close();
        final IStructuredOutputWriter generalData = modelWriter.child("generalData");
        for (IScorer<?, IAnalyzedInterval> scorer : m_trainedScorersMap.values()) {
            MessageScorer ms;
            if (scorer instanceof MessageScorer) {
                ms = (MessageScorer) scorer;
            } else {
                continue;
            }
            final IStructuredOutputWriter scoreWriter = generalData.child("score", "name", ms.getClass().getName());
            ms.printGeneralUserData(scoreWriter);
            scoreWriter.close();
        }
        for (IScorer<?, IAnalyzedInterval> scorer : m_trainedScorersMap.values()) {
            IntervalAnomalyScorer is;
            if (scorer instanceof IntervalAnomalyScorer) {
                is = (IntervalAnomalyScorer) scorer;
            } else {
                continue;
            }

            final IStructuredOutputWriter scoreWriter = generalData.child("score", "name", is.getClass().getName());
            is.printGeneralUserData(scoreWriter);
            scoreWriter.close();
        }
        generalData.close();
        modelWriter.close();
    }

    @Override
    public final void incomingObject(IInterval interval) throws AdeException {
        m_target.incomingObject(interval);
    }

    private void setIntervalLogProbScore(IAnalyzedInterval analyzedInterval
                                         , IntervalAnomalyScorer intervalScorer) throws AdeException {
        final StatisticsChart intStats = analyzedInterval.getStatistics();
        final double minIntScore = intStats.getDoubleStatOrThrow(IntervalAnomalyScorer.MIN_INTERVAL_SCORE);
        final double inputAnomaly = intStats.getDoubleStatOrThrow(intervalScorer.getName() + "." + IScorer.MAIN);
        double logProb = intStats.getDoubleStatOrThrow(intervalScorer.getName() + "." + IScorer.LOG_PROB);
        final double finalAnomaly = Math.max(inputAnomaly, minIntScore);
        intStats.setStat(IScorer.ANOMALY, finalAnomaly);
        if (finalAnomaly != inputAnomaly) {
            if (finalAnomaly >= 1.0) {
                logProb = HUGELOGPROB;
            } else {
                logProb = -Math.log(1 - finalAnomaly);
            }
        }
        intStats.setStat(IScorer.LOG_PROB, logProb);
    }

    private void setMessageLogProbAndMinIntervalScores(
            IAnalyzedInterval analyzedInterval, MessageScorer messageScorer) throws AdeInternalException {

        final String inAnomaly = messageScorer.getName() + "." + IScorer.MAIN;
        final double minIntervalScore = 0;
        for (IAnalyzedMessageSummary ams : analyzedInterval.getAnalyzedMessages()) {
            final StatisticsChart sc = ams.getStatistics();
            final double anomaly = sc.getDoubleStatOrThrow(inAnomaly);

            sc.setStat(IScorer.ANOMALY, anomaly);
            final double logProb = sc.getDoubleStatOrThrow(messageScorer.getName() + "." + IScorer.LOG_PROB);
            sc.setStat(IScorer.LOG_PROB, logProb);
        }
        analyzedInterval.getStatistics().setStat(IntervalAnomalyScorer.MIN_INTERVAL_SCORE, minIntervalScore);
    }

    @Override
    public final void endOfStream() throws AdeException {
        s_logger.info("eof main scorer");
        if (m_target != null) {
            //this will flush and held intervals.
            m_target.endOfStream();
        }
        final String firstFlow = m_currentIterationScorers.keySet().iterator().next();
        final Map<String, IScorer<?, IAnalyzedInterval>> scorers = m_currentIterationScorers.get(firstFlow);
        final Iterator<Entry<String, IScorer<?, IAnalyzedInterval>>> it = scorers.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, IScorer<?, IAnalyzedInterval>> scorerEntry = it.next();
            final IScorer<?, IAnalyzedInterval> scorer = scorerEntry.getValue();
            final String scorerId = scorerEntry.getKey();
            s_logger.info("eof scorer " + scorer.getName());
            scorer.endOfStream();
            if (!scorer.needsAnotherIteration()) {
                it.remove();
                m_currentIterationScorerIds.remove(scorerId);
                if (scorers.isEmpty()) {
                    m_currentIterationScorers.remove(firstFlow);
                }
                m_trainedScorersMap.put(scorerId, scorer);
                m_scorersByOrder.add(scorerId);
                s_logger.info("adding " + scorer.getName() + " to trained scorers");
            }
        }
        reset();
    }

    @Override
    public final FramingFlowType getRequiredIntervalFramer() throws AdeException {
        return m_framingFlow;
    }

    @Override
    public final IAnalyzedInterval analyze(IInterval interval) throws AdeException {
        final IAnalyzedInterval analyzedInterval = 
                new AnalyzedIntervalImpl(interval, m_modelMetaData.getModelInternalId());
        for (String key : m_scorersByOrder) {
            final String scorerId = key;
            final IScorer<?, IAnalyzedInterval> scorer = m_trainedScorersMap.get(key);

            if (m_omitFromAnalysis != null && m_omitFromAnalysis.contains(scorerId)) {
                continue;
            }
            if (scorer instanceof MessageScorer) {

                final MessageScorer messageScorer = (MessageScorer) scorer;
                final String name = scorer.getName();
                for (IAnalyzedMessageSummary ams : analyzedInterval.getAnalyzedMessages()) {
                    ams.getStatistics().add(name, messageScorer.getScore(ams, analyzedInterval));
                }
                if (m_finalMessageAnomalyScorer.equals(scorerId)) {
                    setMessageLogProbAndMinIntervalScores(analyzedInterval, messageScorer);
                }
            } else if (scorer instanceof IntervalAnomalyScorer) {
                final IntervalAnomalyScorer intervalScorer = (IntervalAnomalyScorer) scorer;
                analyzedInterval.getStatistics().add(scorer.getName(),
                        intervalScorer.getScore(analyzedInterval, analyzedInterval));
                if (m_finalIntervalAnomalyScorer.equals(scorerId)) {
                    setIntervalLogProbScore(analyzedInterval, intervalScorer);
                }
            }
        }
        return analyzedInterval;
    }

    @Override
    public final IModelMetaData getModelMetaData() {
        return m_modelMetaData;
    }

    @Override
    protected final void reset() throws AdeException {
        m_allMsgIds = new TreeSet<>();
        setCurrentIterationScorers();
        if (!needsAnotherIteration()) {
            m_target = m_intervalDispenser;
        }
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
    }

    public final Collection<IScorer<?, IAnalyzedInterval>> getTrainedScorers() {
        return m_trainedScorersMap.values();
    }

    @Override
    public final IScorer<?, IAnalyzedInterval> getTrainedScorer(String key) {
        if (m_trainedScorersMap.containsKey(key)) {
            return m_trainedScorersMap.get(key);
        }
        return null;
    }

    @Override
    public final List<MessageScorer> getMessageScorers() throws AdeException {
        final List<MessageScorer> messageScorerList = new ArrayList<>();
        for (Entry<String, IScorer<?, IAnalyzedInterval>> pair : m_trainedScorersMap.entrySet()) {
            final IScorer<?, IAnalyzedInterval> scorer = pair.getValue();
            final String scorerId = pair.getKey();

            if (m_omitFromAnalysis != null && m_omitFromAnalysis.contains(scorerId)) {
                continue;
            }
            if (scorer instanceof MessageScorer) {
                final MessageScorer messageScorer = (MessageScorer) scorer;
                messageScorerList.add(messageScorer);
            }
        }
        return messageScorerList;
    }
    
    @Override
    public final void incomingSeparator(TimeSeparator sep) throws AdeException,
            AdeFlowException {
        m_target.incomingSeparator(sep);
    }

    @Override
    public final void wakeUp() throws AdeException {
        m_target = m_intervalDispenser;
        for (IScorer<?, IAnalyzedInterval> scorer : m_trainedScorersMap.values()) {
            scorer.wakeUp();
        }
    }
    
    static public class FileHandler implements IModelFileHandler<IMainScorer> {

        @Override
        public final File store(IMainScorer model) throws IOException, AdeException {
            File modelFile = model.getModelMetaData().getModelFileName();
            if (modelFile == null) {
                final String filename = Ade.getAde().getConfigProperties().getOutputFilenameGenerator().generateLogModelFilename(model.getModelMetaData());
                modelFile = new File(Ade.getAde().getDirectoryManager().getModelHome(),
                        filename);
            }
            final ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(modelFile)));
            try {
                out.writeObject(model);
                return modelFile;
            } finally {
                out.close();
            }
        }

        @Override
        public final IMainScorer load(File modelFile) throws IOException, AdeException {
            final ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(modelFile)));
            try {
                final MainScorerImpl res = (MainScorerImpl) in.readObject();
                final FlowTemplateFactory flow = Ade.getAde().getFlowFactory().getFlowByName(res.m_flowName);
                final Map<String, FramingFlowType> myFramingFlows = flow.getMyFramingFlows();
                res.m_framingFlow = myFramingFlows.get(flow.getUploadFramer());
                res.m_scorerSchemas = flow.getScorerSchemas();
                res.wakeUp();
                return res;
            } catch (ClassNotFoundException e) {
                throw new AdeUsageException("Failed loading model from file: " + modelFile.getAbsolutePath(), e);
            } finally {
                in.close();
            }
        }

    }

    public class IntervalDispenser implements IFrameableTarget<IInterval, TimeSeparator>, Serializable {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        @Override
        public void beginOfStream() throws AdeException, AdeFlowException {
        }

        @Override
        public final void incomingObject(IInterval interval) throws AdeException,
                AdeFlowException {
            // we are going to train on this interval. We might need scores for this interval 
            // that are based on models we already have
            final IAnalyzedInterval analyzedInterval = analyze(interval);
            final String firstFlow = m_currentIterationScorers.keySet().iterator().next();
            final Map<String, IScorer<?, IAnalyzedInterval>> scorers = m_currentIterationScorers.get(firstFlow);

            for (IScorer<?, IAnalyzedInterval> scorer : scorers.values()) {
                scorer.incomingObject(analyzedInterval);
            }
            for (IMessageSummary ms : interval.getMessageSummaries()) {
                m_allMsgIds.add(ms.getMessageId());
            }

            // also update the start and end time of the model
            ModelMetaDataImpl.addTimeSpan((ModelMetaDataImpl) m_modelMetaData,
                    new Date(interval.getIntervalStartTime()),
                    new Date(interval.getIntervalEndTime()));
        }

        @Override
        public void endOfStream() throws AdeException, AdeFlowException {
        }

        @Override
        public final void incomingSeparator(TimeSeparator sep) throws AdeException,
                AdeFlowException {
            // We are in training
            if (m_currentIterationScorers != null) {
                final String firstFlow = m_currentIterationScorers.keySet().iterator().next();
                final Map<String, IScorer<?, IAnalyzedInterval>> scorers = m_currentIterationScorers.get(firstFlow);
                for (IScorer<?, IAnalyzedInterval> scorer : scorers.values()) {
                    scorer.incomingSeparator(sep);
                }
            // No longer in training
            } else {
                for (IScorer<?, IAnalyzedInterval> scorer : m_trainedScorersMap.values()) {
                    scorer.incomingSeparator(sep);
                }

            }
        }

    }
}
