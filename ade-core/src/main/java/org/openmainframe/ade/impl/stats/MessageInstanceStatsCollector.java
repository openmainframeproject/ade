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
import java.util.Date;
import java.util.Map;
import java.util.Map.*;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.hub.HubFrameableFramingBlock;
import org.openmainframe.ade.impl.utils.DateTimeUtils;

class MessageInstanceStatsCollector extends HubFrameableFramingBlock<IMessageInstance, TimeSeparator, IMessageInstance, TimeSeparator> {

    MessageInstanceStatsCollector(String analysisGroup) throws AdeException {
        final File dir = StatsUtils.getStatsDir(analysisGroup);

        addTarget(new TimeDiffBetweenConsecMsgs(dir));
        addTarget(new NumMsgsBetweenSepStats(dir));
        addTarget(new TimeSpanBetweenSepStats(dir));
        addTarget(new NumUniqueMsgIdsBetweenSepStats(dir));
        addTarget(new TotalNumMsgIdsStats(dir));
        addTarget(new TotalNumSepsStats(dir));
        addTarget(new MessageIdGenerationStats(dir));
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        sendBeginOfStream();
    }

    @Override
    public void incomingSeparator(TimeSeparator sep) throws AdeException, AdeFlowException {
        sendSeparator(sep);
    }

    @Override
    public void incomingObject(IMessageInstance obj) throws AdeException, AdeFlowException {
        sendObject(obj);
    }

    @Override
    public void endOfStream() throws AdeException, AdeFlowException {
        sendEndOfStream();
    }

    private static class TimeDiffBetweenConsecMsgs extends MessageInstanceStatManager {

        private Date m_prevMsgTime = null;

        TimeDiffBetweenConsecMsgs(File dir) throws AdeInternalException {
            super(new File(dir, "timeDiffBetweenConsecMsgs.txt"), "The time in milliseconds between each two consecutive messages (not separated by a time separator)");
        }

        @Override
        public void incomingSeparator(TimeSeparator sep) throws AdeException, AdeFlowException {
            m_prevMsgTime = null;
        }

        @Override
        public void incomingObject(IMessageInstance obj) throws AdeException, AdeFlowException {
            if (m_prevMsgTime == null) {
                m_prevMsgTime = obj.getDateTime();
            } else {
                final long timeDiff = obj.getDateTime().getTime() - m_prevMsgTime.getTime();
                addStat(timeDiff);
                m_prevMsgTime = obj.getDateTime();
            }
        }

        @Override
        protected void beforeEndOfStream() throws AdeException, AdeFlowException {
            // do nothing		
        }
    }

    private static class NumMsgsBetweenSepStats extends MessageInstanceStatManager {

        private Integer m_count = null;

        NumMsgsBetweenSepStats(File dir) throws AdeInternalException {
            super(new File(dir, "numMsgsBetweenSep.txt"), "The number of messages between each two consecutive time separators");
        }

        @Override
        public void beforeEndOfStream() throws AdeException, AdeFlowException {
            addStatAndResetCount();
        }

        @Override
        public void incomingSeparator(TimeSeparator sep) throws AdeException, AdeFlowException {
            addStatAndResetCount();
        }

        @Override
        public void incomingObject(IMessageInstance obj) throws AdeException, AdeFlowException {
            m_count++;
        }

        private void addStatAndResetCount() throws AdeInternalException {
            if (m_count != null) {
                addStat(m_count);
            }
            m_count = 0;
        }
    }

    private static class TimeSpanBetweenSepStats extends MessageInstanceStatManager {

        private Date m_firstMsgTime = null;
        private Date m_lastMsgTime = null;

        TimeSpanBetweenSepStats(File dir) throws AdeInternalException {
            super(new File(dir, "timeSpanBetweenSep.txt"), "The time in seconds between the first and last message between each two consecutive time separators");
        }

        @Override
        public void incomingSeparator(TimeSeparator sep) throws AdeException, AdeFlowException {
            addStatAndResetTime();
        }

        @Override
        public void incomingObject(IMessageInstance obj) throws AdeException, AdeFlowException {
            if (m_firstMsgTime == null) {
                m_firstMsgTime = obj.getDateTime();
            }
            m_lastMsgTime = obj.getDateTime();
        }

        @Override
        protected void beforeEndOfStream() throws AdeException, AdeFlowException {
            addStatAndResetTime();
        }

        private void addStatAndResetTime() throws AdeInternalException {
            if (m_firstMsgTime != null) {
                addStat((m_lastMsgTime.getTime() - m_firstMsgTime.getTime()) / DateTimeUtils.MILLIS_IN_SECOND);
            }
            m_firstMsgTime = null;
            m_firstMsgTime = null;
        }
    }

    private static class NumUniqueMsgIdsBetweenSepStats extends MessageInstanceStatManager {

        Set<String> m_uniqueMsgIds = null;

        NumUniqueMsgIdsBetweenSepStats(File dir) throws AdeInternalException {
            super(new File(dir, "numUniqueMsgIdsBetweenSep.txt"), "The number of unique message IDs between each two consecutive time separators");
        }

        @Override
        public void incomingSeparator(TimeSeparator sep) throws AdeException, AdeFlowException {
            addStatAndResetSet();
        }

        @Override
        public void incomingObject(IMessageInstance obj) throws AdeException, AdeFlowException {
            m_uniqueMsgIds.add(obj.getMessageId());
        }

        @Override
        protected void beforeEndOfStream() throws AdeException, AdeFlowException {
            addStatAndResetSet();
        }

        private void addStatAndResetSet() throws AdeInternalException {
            if (m_uniqueMsgIds != null) {
                addStat(m_uniqueMsgIds.size());
            }
            m_uniqueMsgIds = new TreeSet<String>();
        }
    }

    private static class TotalNumMsgIdsStats extends MessageInstanceStatManager {

        private Set<String> m_msgIds = new TreeSet<String>();

        TotalNumMsgIdsStats(File dir) throws AdeInternalException {
            super(new File(dir, "totalNumMsgIds.txt"), "The total number of unique message IDs encountered");
        }

        @Override
        public void incomingSeparator(TimeSeparator sep) throws AdeException, AdeFlowException {
            // do nothing
        }

        @Override
        public void incomingObject(IMessageInstance obj) throws AdeException, AdeFlowException {
            m_msgIds.add(obj.getMessageId());
        }

        @Override
        protected void beforeEndOfStream() throws AdeException, AdeFlowException {
            addStat(m_msgIds.size());
            m_msgIds.clear();
        }
    }

    private static class TotalNumSepsStats extends MessageInstanceStatManager {

        private int count = 0;

        TotalNumSepsStats(File dir) throws AdeInternalException {
            super(new File(dir, "totalNumSeps.txt"), "The total number of TimeSeparator encountered");
        }

        @Override
        public void incomingSeparator(TimeSeparator sep) throws AdeException, AdeFlowException {
            count++;
        }

        @Override
        public void incomingObject(IMessageInstance obj) throws AdeException, AdeFlowException {
            // do nothing
        }

        @Override
        protected void beforeEndOfStream() throws AdeException, AdeFlowException {
            addStat(count);
        }
    }

    private static class MessageIdGenerationStats extends MessageInstanceStatManager {

        protected MessageIdGenerationStats(File dir)
                throws AdeInternalException {
            super(new File(dir, "msgIdGeneration.txt"), "Generated message ID clusters collected per component. Each line is tab delimeted: <component>	<message ID> <message text>");
        }

        @Override
        public void incomingObject(IMessageInstance obj) throws AdeException,
                AdeFlowException {
            if (obj.getComponentId() != null) {
                write(obj.getComponentId() + "\t" + obj.getMessageId() + "\t" + obj.getText().replace("\n", "\\n").replace("\t", "\\t") + "\n");
            } else {
                write("N/A\t" + obj.getMessageId() + "\t" + obj.getText().replace("\n", "\\n").replace("\t", "\\t") + "\n");
            }

        }

        @Override
        public void incomingSeparator(TimeSeparator sep) throws AdeException, AdeFlowException {
            // do nothing
        }

        @Override
        protected void beforeEndOfStream() throws AdeException, AdeFlowException {
            // do nothing
        }

    }

    @SuppressWarnings("unused")
    private static class SortedMessageIdGenerationStats extends MessageInstanceStatManager {

        /**
         * Maps a component to [a map from message ID to messages text]
         */
        private Map<String, Map<String, Set<String>>> m_msgsPerComponentPerMsgId = new TreeMap<String, Map<String, Set<String>>>();

        protected SortedMessageIdGenerationStats(File dir) throws AdeInternalException {
            super(new File(dir, "sortedMsgIdGeneration.txt"), "Generated message ID clusters collected per component");
        }

        @Override
        public void incomingObject(IMessageInstance mi) throws AdeException,
                AdeFlowException {
            final String componentId = mi.getComponentId();
            if (componentId != null) {
                Map<String, Set<String>> msgsPerMsgId = m_msgsPerComponentPerMsgId.get(componentId);
                if (msgsPerMsgId == null) {
                    msgsPerMsgId = new TreeMap<String, Set<String>>();
                    m_msgsPerComponentPerMsgId.put(componentId, msgsPerMsgId);
                }
                final String msgId = mi.getMessageId();
                Set<String> msgs = msgsPerMsgId.get(msgId);
                if (msgs == null) {
                    msgs = new TreeSet<String>();
                    msgsPerMsgId.put(msgId, msgs);
                }
                msgs.add(mi.getText());
            }
        }

        @Override
        public void incomingSeparator(TimeSeparator sep) throws AdeException, AdeFlowException {
            // do nothing
        }

        @Override
        protected void beforeEndOfStream() throws AdeException,
                AdeFlowException {
            for (Entry<String, Map<String, Set<String>>> entry : m_msgsPerComponentPerMsgId.entrySet()) {
                write("component: " + entry.getKey() + "\n");
                final Map<String, Set<String>> msgsPerMsgId = m_msgsPerComponentPerMsgId.get(entry.getKey());
                for (Entry<String, Set<String>> msgSet : msgsPerMsgId.entrySet()) {
                    write("\tmessage ID: " + msgSet.getKey() + "\n");
                    for (String msg : msgSet.getValue()) {
                        write("\t\t" + msg);
                    }
                }
            }
        }

    }
}
