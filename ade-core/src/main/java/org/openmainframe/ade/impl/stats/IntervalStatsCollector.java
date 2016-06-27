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
package org.openmainframe.ade.impl.stats;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dbUtils.DbDictionary;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.hub.HubStreamBlock;

class IntervalStatsCollector extends HubStreamBlock<IInterval, IInterval> {

    private static Map<String, Map<String, IntervalStatsCollector>> s_intervalStatCollectorsPerAnalysisGroup = new TreeMap<String, Map<String, IntervalStatsCollector>>();

    static IntervalStatsCollector getIntervalStatCollector(String analysisGroup, FramingFlowType framingFlowType) throws AdeException {
        IntervalStatsCollector intervalStatsCollector;
        Map<String, IntervalStatsCollector> innerMap = s_intervalStatCollectorsPerAnalysisGroup.get(analysisGroup);
        if (innerMap == null) {
            innerMap = new HashMap<String, IntervalStatsCollector>();
            intervalStatsCollector = new IntervalStatsCollector(analysisGroup, framingFlowType);

            innerMap.put(framingFlowType.getName(), intervalStatsCollector);
            s_intervalStatCollectorsPerAnalysisGroup.put(analysisGroup, innerMap);
        } else {
            intervalStatsCollector = innerMap.get(framingFlowType.getName());
            if (intervalStatsCollector == null) {
                intervalStatsCollector = new IntervalStatsCollector(analysisGroup, framingFlowType);
                innerMap.put(framingFlowType.getName(), intervalStatsCollector);
            }
        }
        return intervalStatsCollector;
    }

    static void closeAllIntervalStatCollectors() throws AdeFlowException, AdeException {
        for (Map<String, IntervalStatsCollector> innerMap : s_intervalStatCollectorsPerAnalysisGroup.values()) {
            for (IntervalStatsCollector intervalStatsCollector : innerMap.values()) {
                intervalStatsCollector.close();
            }
        }
    }

    private IntervalStatsCollector(String analysisGroup, FramingFlowType framingFlowType) throws AdeException {
        final File dir = StatsUtils.getStatsDir(analysisGroup, framingFlowType);

        addTarget(new MsgsCountPerMsgIdInIntervalStat(dir));
        addTarget(new NumUniqueMsgIdsInIntervalStat(dir));
        addTarget(new NumNewMsgIdsInIntervalStat(dir));
        addTarget(new MsgsNameAndInternalIdIntervalStat(dir));
        addTarget(new MsgsNameAndCountPerMsgIdInIntervalStat(dir));

        sendBeginOfStream();
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        /* do nothing */ }

    @Override
    public void incomingObject(IInterval obj) throws AdeException, AdeFlowException {
        sendObject(obj);
    }

    @Override
    public void endOfStream() throws AdeException, AdeFlowException {
        /* do nothing */ }

    private void close() throws AdeFlowException, AdeException {
        // send to all targets
        sendEndOfStream();
    }

    private static class MsgsCountPerMsgIdInIntervalStat extends IntervalStatManager {

        protected MsgsCountPerMsgIdInIntervalStat(File dir) throws AdeInternalException {
            super(new File(dir, "msgsCountPerMsgIdInInterval.txt"), "The number of appearances of a specific message ID in an interval (per message ID per interval)");
        }

        @Override
        public void incomingObject(IInterval obj) throws AdeException, AdeFlowException {
            for (IMessageSummary msgSummary : obj.getMessageSummaries()) {
                addStat(msgSummary.getNumMessageInstances());
            }
        }

        @Override
        protected void beforeEndOfStream() throws AdeException, AdeFlowException {
            // do nothing
        }
    }

    private static class MsgsNameAndCountPerMsgIdInIntervalStat extends IntervalStatManager {

        protected MsgsNameAndCountPerMsgIdInIntervalStat(File dir) throws AdeInternalException {
            super(new File(dir, "msgsNameAndCountPerMsgIdInInterval.txt"), "The number of appearances of each message ID in an interval (per message ID per interval)");
        }

        @Override
        public void incomingObject(IInterval obj) throws AdeException, AdeFlowException {
            final String intervalId = "" + obj.getIntervalStartTime();
            for (IMessageSummary msgSummary : obj.getMessageSummaries()) {
                writeln(intervalId + " " + msgSummary.getMessageInternalId() + " " + msgSummary.getNumMessageInstances());
            }
        }

        @Override
        protected void beforeEndOfStream() throws AdeException, AdeFlowException {
            // do nothing
        }
    }

    private static class MsgsNameAndInternalIdIntervalStat extends IntervalStatManager {

        Set<Integer> seenMessages;

        protected MsgsNameAndInternalIdIntervalStat(File dir) throws AdeInternalException {
            super(new File(dir, "msgsNameAndinternalIdInterval.txt"), "Message ID and internal message id");
            seenMessages = new TreeSet<Integer>();
        }

        @Override
        public void incomingObject(IInterval obj) throws AdeException, AdeFlowException {
            for (IMessageSummary msgSummary : obj.getMessageSummaries()) {
                seenMessages.add(msgSummary.getMessageInternalId());
            }
        }

        @Override
        protected void beforeEndOfStream() throws AdeException, AdeFlowException {
            final DbDictionary dict = AdeInternal.getAdeImpl().getDictionaries().getMessageIdDictionary();
            for (Integer id : seenMessages) {
                writeln(id + " " + dict.getWordById(id));
            }
        }
    }

    private static class NumUniqueMsgIdsInIntervalStat extends IntervalStatManager {

        protected NumUniqueMsgIdsInIntervalStat(File dir) throws AdeInternalException {
            super(new File(dir, "numUniqueMsgIdsInInterval.txt"), "The number of unique message IDs in an interval (per interval)");
        }

        @Override
        public void incomingObject(IInterval obj) throws AdeException, AdeFlowException {
            addStat(obj.getMessageSummaries().size());
        }

        @Override
        protected void beforeEndOfStream() throws AdeException, AdeFlowException {
            // do nothing
        }
    }

    private static class NumNewMsgIdsInIntervalStat extends IntervalStatManager {

        static Set<String> oldMessageIds = new HashSet<String>();

        protected NumNewMsgIdsInIntervalStat(File dir) throws AdeInternalException {
            super(new File(dir, "numNewMsgIdsInInterval.txt"), "The number of new message IDs in an interval (per interval)");
        }

        @Override
        public void incomingObject(IInterval obj) throws AdeException, AdeFlowException {
            final Iterator<IMessageSummary> it = obj.getMessageSummaries().iterator();
            String currMessage;
            int numOfNewMessages = 0;
            while (it.hasNext()) {
                currMessage = it.next().getMessageId();
                if (!oldMessageIds.contains(currMessage)) {
                    numOfNewMessages++;
                    oldMessageIds.add(currMessage);
                }
            }
            addStat(numOfNewMessages);
        }

        @Override
        protected void beforeEndOfStream() throws AdeException, AdeFlowException {
            // do nothing
        }
    }
}
