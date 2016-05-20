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

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang3.ArrayUtils;
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.dbUtils.DbDictionary;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.summary.SummarizationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class retrieves a message's timeline and keeps track of when the message was last seen. 
 */
public class LastSeenLoggingScorerContinuous extends FixedMessageScorer {

    /**
     * Default logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(LastSeenLoggingScorerContinuous.class);
    /**
     * The universal version identifier for serialization. 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Factor to split the timeline resolution.
     */
    private static final long SPLIT_TIMELINE_FACTOR = 2;

    /**
     * Data Factory for GregorianCalendar creation.
     */
    protected transient DatatypeFactory m_dataTypeFactory = null;
    
    /**
     * GregorianCalendar to store time.
     */
    protected transient GregorianCalendar m_gc = null;

    /**
     * Maps message ID to the end time of the last interval it appeared in and the last tick in that interval. 
     */
    private transient Map<String, TreeSet<Long>> m_prevIntervalTimelineMap = new TreeMap<>();

    @Property(key = "verbose", help = "print diffs to stdout", required = false)
    private boolean m_verbose = false;
    
    @Property(key = "flushMemoryOnGap", help = "triggers clearing of the previous timeline map"
            + "Should never calculate delta between two messages on"
            + "diffrent sides of a gap", required = false)
    protected boolean m_flushMemoryOnGap = false;

    /**
     * Keep track of all the messages that have been seen already. 
     */
    private transient Set<String> m_alreadySeen; 
    
    /**
     * Time the first message was sent. 
     */
    private long m_firstMsgTime;
    
    /**
     * Enum value that represents the timeline status of message. 
     */
    private MainStatVal m_mainStat;
    
    /**
     * List that gives the number of seconds between message occurrences. 
     */
    private List<Long> m_deltasInSeconds;
    /**
     * Class constructor that initializes all variables. 
     * @throws AdeException
     */
    public LastSeenLoggingScorerContinuous() throws AdeException {
        super();
        createUsageVariables();
    }

    /**
     * Creates variables used by this class for tracking last seen messages. 
     * m_prevIntervalTimelineMap contains the previous timeline (milliseconds from epoch time)
     * for each message ID. m_alreadySeen contains the message IDs of those messages that were seen
     * previously. 
     * @throws AdeException
     */
    private final void createUsageVariables() throws AdeException {
        String dataObjectName = getAnalysisGroup() + "." + getName() + ".m_prevIntervalTimelineMap";
        Object tmp = Ade.getAde().getDataStore().models().getModelDataObject(dataObjectName);
        instantiateTimelineAndAlreadySeen(dataObjectName,tmp);

        dataObjectName = getAnalysisGroup() + "." + getName() + ".m_alreadySeen";
        tmp = Ade.getAde().getDataStore().models().getModelDataObject(dataObjectName);
        instantiateTimelineAndAlreadySeen(dataObjectName,tmp);

        if (m_dataTypeFactory == null) {
            try {
                m_dataTypeFactory = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException e) {
                throw new AdeInternalException("Failed to instantiate data factory for calendar", e);
            }
        }

        if (m_gc == null) {
            final TimeZone outputTimeZone = Ade.getAde().getConfigProperties().getOutputTimeZone();
            m_gc = new GregorianCalendar(outputTimeZone);
        }
    }

    /**
     * For instantiating the previous timeline map and already seen variables. 
     * @param dataObjectName The name of the data object we are trying to retrieve from datastore.
     * @param tmp The object returned from retrieving the data object name from datastore.
     * @throws AdeException
     */
    private void instantiateTimelineAndAlreadySeen(String dataObjectName, Object tmp) throws AdeException{ 
        if (dataObjectName.contains("m_prevIntervalTimelineMap")){
            
            if (tmp instanceof Map<?, ?>) {
                m_prevIntervalTimelineMap = (Map<String, TreeSet<Long>>) tmp;
            } else {
                m_prevIntervalTimelineMap = new TreeMap<>();
                Ade.getAde().getDataStore().models().setModelDataObject(dataObjectName, m_prevIntervalTimelineMap);
            }
            
        } else if (dataObjectName.contains("m_alreadySeen")){
            
            if (tmp instanceof Set<?>) {
                m_alreadySeen = (HashSet<String>) tmp;
            } else {
                final DbDictionary dict = AdeInternal.getAdeImpl().getDictionaries().getMessageIdDictionary();
                m_alreadySeen = new HashSet<>(dict.getWords());
                Ade.getAde().getDataStore().models().setModelDataObject(dataObjectName, m_alreadySeen);
            }             
        }         
    }

    /**
     * Create variables for this class after deserialization. 
     * @throws AdeException
     */
    @Override
    public final void wakeUp() throws AdeException {
        super.wakeUp();
        createUsageVariables();
    }

    /**
     * Flushes previous timeline map on incoming time separator if m_flushMemoryOnGap is set to true.
     * @param sep The incoming separator object.
     * @throws AdeException
     */
    @Override
    public final void incomingSeparator(TimeSeparator sep) throws AdeException {
        super.incomingSeparator(sep);  
        if (m_flushMemoryOnGap) {
            flushMemory();
        }
    }
    /**
     * Removes all the mappings from the previous interval timeline map.
     */
    private void flushMemory() {
        m_prevIntervalTimelineMap.clear();
    }

    /**
     * Main logic for creating the previous timeline of a particular message. First, it processes
     * the first message. Then, it gets the previous timeline from the map; if the previous time line is not null, 
     * we take the LATEST time this message was sent from the previous time line (right before the first message
     * time in this interval). Then, we take this latest time, the first message time in this interval, 
     * and the times this message occurred after the first time in the same interval is used to create
     * the "new" previous time line. 
     * @param scoredElement The analysis results of a MessageSummary object. Message summaries contain 
     * statistics and information on message instances. i.e. text body message, message id, severity, etc.
     * @param contextElement contains a summary of the interval i.e. information such as time, number of 
     * message ids, etc.
     * @return The StatisticsChart for collecting double and string statistics.
     */
    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary scoredElement,
            IAnalyzedInterval contextElement) throws AdeException {
        final String messageID = scoredElement.getMessageId();
        final short[] timeLine = scoredElement.getTimeLine();         
        final StatisticsChart sc = new StatisticsChart();
        
        sc.setStat(LOG_PROB, 0);
        sc.setStat(ANOMALY, 0);
       
        processFirstMessage(contextElement,timeLine);

        final TreeSet<Long> timeLineSet = new TreeSet<>();
        
        processPrevTimeLine(messageID, contextElement, sc, timeLineSet);

        if (!m_alreadySeen.contains(messageID)) {
            m_mainStat = MainStatVal.NEVER_SEEN_BEFORE;
            m_alreadySeen.add(messageID);
        }

        timeLineSet.add(m_firstMsgTime);
        
        processCurrentTimeLine(timeLine, timeLineSet, contextElement);
        printLastSeenInfo(messageID);
        
        m_prevIntervalTimelineMap.put(messageID, timeLineSet);
        sc.setStat("res", m_deltasInSeconds.toString());
        sc.setStat(MAIN, m_mainStat.toString());
        return sc;
    }
    /**
     * Prints out last seen information if verbosity is turned on (ie. set to true)
     * @param messageID String value that gives message id. 
     */
    public void printLastSeenInfo(String messageID){
        if (m_verbose) {
            logger.info(messageID + "\t" + m_deltasInSeconds.toString());
        }
    }
    
    /**
     * Process to handle the first message.
     * @param contextElement AnalyzedInterval object that contains summary results of interval.
     * @param timeLine Array of Short values with the time line of the message.
     * @param millisPerTick Long value with number of milliseconds per tick. 
     */
    public void processFirstMessage(IAnalyzedInterval contextElement, short[] timeLine){
        final boolean hasTimeline = !ArrayUtils.isEmpty(timeLine);
        final long millisPerTick = contextElement.getInterval().getIntervalSize() / 
                SummarizationProperties.TIMELINE_RESOLUTION;
        if (hasTimeline) {
            m_firstMsgTime = contextElement.getIntervalStartTime() + timeLine[0] * millisPerTick;
            m_mainStat = MainStatVal.REGULAR;
            m_deltasInSeconds = new ArrayList<>(timeLine.length);
        } else {
            /**
             * If time line is not available, assume first message occurred in the middle of the
             * interval.
             */
            m_firstMsgTime = contextElement.getIntervalStartTime() + 
                    (SummarizationProperties.TIMELINE_RESOLUTION / SPLIT_TIMELINE_FACTOR) * millisPerTick;
            m_mainStat = MainStatVal.NO_TIMELINE;
            m_deltasInSeconds = new ArrayList<>(1);
        }
    }
    /**
     * Processes the previous time line of the current message ID. 
     * @param messageID String value that contains the message ID
     * @param contextElement AnalyzedInterval object that contains summary results of interval.
     * @param sc Contains statistics for message ID.
     * @param timeLineSet time line of current message ID. 
     * @throws AdeInternalException
     */
    public void processPrevTimeLine(String messageID, IAnalyzedInterval contextElement, StatisticsChart sc,
            TreeSet<Long> timeLineSet) throws AdeInternalException{ 
        final TreeSet<Long> prevTimeLine = m_prevIntervalTimelineMap.get(messageID);
        if (prevTimeLine == null) {
            m_mainStat = MainStatVal.NEW;
            m_deltasInSeconds.add((m_firstMsgTime-contextElement.getIntervalStartTime())/DateTimeUtils.MILLIS_IN_SECOND);
        } else {
            final Long prevLastTime = prevTimeLine.lower(m_firstMsgTime);
            if (prevLastTime != null) {
                m_gc.setTimeInMillis(prevLastTime);
                sc.setStat("LastTime", String.valueOf(m_dataTypeFactory.newXMLGregorianCalendar(m_gc)));
                final long delta = (m_firstMsgTime - prevLastTime) / DateTimeUtils.MILLIS_IN_SECOND;
                m_deltasInSeconds.add(delta);
                timeLineSet.add(prevLastTime);
            }
        }
    }
    
    /**
     * Processes the current time line. 
     * @param timeLine Array of Short values with the time line of the message.
     * @param timeLineSet time line of current message ID. 
     * @param contextElement AnalyzedInterval object that contains summary results of interval.
     * @param millisPerTick Long value with number of milliseconds per tick. 
     */
    public void processCurrentTimeLine(short[] timeLine,TreeSet<Long> timeLineSet,IAnalyzedInterval contextElement){
        final boolean hasTimeline = !ArrayUtils.isEmpty(timeLine);
        final long millisPerTick = contextElement.getInterval().getIntervalSize() / 
                SummarizationProperties.TIMELINE_RESOLUTION;
        if (hasTimeline) {
            Short prevPos = null;
            for (short pos : timeLine) {
                // ignore first message, as we dealt with it already
                if (prevPos == null) {
                    prevPos = pos;
                    continue;
                }
                Long delta = (pos - prevPos) * millisPerTick;
                m_deltasInSeconds.add(delta / DateTimeUtils.MILLIS_IN_SECOND);
                prevPos = pos;

                timeLineSet.add(contextElement.getIntervalStartTime() + pos * millisPerTick);
            }
        }
    }

    /**
     * Enum class to keep track of timeline status of messages. 
     */
    private enum MainStatVal {
        NEW("new"), REGULAR("regular"), NO_TIMELINE("noTimeline"), NEVER_SEEN_BEFORE("neverSeenBefore");

        private final String m_str;

        private MainStatVal(String str) {
            m_str = str;
        }

        @Override
        public String toString() {
            return m_str;
        }
    }

}
