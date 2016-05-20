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
package org.openmainframe.ade.impl.training;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.summary.SummarizationProperties;

public class MsgMutualInformationSmoothed extends MsgMutualInformation {

    private IInterval m_lastInterval = null;

    public MsgMutualInformationSmoothed(Set<Integer> legalMsgIds) {
        super(legalMsgIds);
        m_countFactor = SummarizationProperties.TIMELINE_RESOLUTION;
        //if we only have this interval, we need to compute the MI based on it, but we need it to be scaled to the num splits
        // using the full count will over-emphasize the first intervals in each continues set of intervals, but only these are rare so it's not too bad.
    }

    @Override
    public final void incomingObject(IInterval interval) throws AdeInternalException {
        if (m_streamClosed) {
            throw new AdeInternalException("Cannot add interval to a closed stream.");
        }

        if (m_lastInterval == null) {
            m_lastInterval = interval;
            super.incomingObject(interval);
            return;
        }

        final Map<Integer, Short> msgIdsLast = new TreeMap<>();
        final Map<Integer, Short> msgIdsFirst = new TreeMap<>();

        fillMissingValues(msgIdsFirst, m_lastInterval, (short) SummarizationProperties.TIMELINE_RESOLUTION);
        fillMissingValues(msgIdsLast, interval, (short) 0);

        fillFirstLastMap(msgIdsLast, m_lastInterval, false);
        fillFirstLastMap(msgIdsFirst, interval, true);

        // at this point msgIdsFirst and msgIdsLast have equal key sets which is
        // equal to the union sets of both intervals
        calculateJoinOccurence(msgIdsLast, msgIdsFirst);

        m_totalNumIntervals += SummarizationProperties.TIMELINE_RESOLUTION;

        m_lastInterval = interval;
    }

    /**
     * Use negative scale 
     */
    static private void fillMissingValues(Map<Integer, Short> map, IInterval interval, short defaultVal) {
        for (IMessageSummary ms : interval.getMessageSummaries()) {
            map.put(ms.getMessageInternalId(), defaultVal);
        }
    }

    /**
     * 
     * @throws AdeInternalException 
     */

    static private void fillFirstLastMap(Map<Integer, Short> map, IInterval interval, boolean isFirst) throws AdeInternalException {
        for (IMessageSummary ms : interval.getMessageSummaries()) {
            short val = (short) (SummarizationProperties.TIMELINE_RESOLUTION / 2);
            final short[] timeline = ms.getTimeLine();
            if (timeline != null && timeline.length > 0) {
                val = isFirst ? timeline[0] : timeline[timeline.length - 1];
            } else {
                final Integer n = ms.getNumMessageInstances();
                if (n != null) {
                    if (isFirst) {
                        val = (short) (SummarizationProperties.TIMELINE_RESOLUTION * (0.5 / Math.sqrt(n)));
                    } else {
                        val = (short) (SummarizationProperties.TIMELINE_RESOLUTION * (1.0 - 0.5 / Math.sqrt(n)));
                    }
                }
            }
            final int messageInternalId = ms.getMessageInternalId();
            map.put(messageInternalId, val);
        }
    }

    private void calculateJoinOccurence(Map<Integer, Short> msgIdsLast,
            Map<Integer, Short> msgIdsFirst) throws AdeInternalException {
        // used ArrayList in order to doubly iterate on the message summaries without repeats in the
        // internal loop
        final ArrayList<Integer> msgIds = new ArrayList<>(msgIdsLast.keySet());

        for (int i = 0; i < msgIds.size(); i++) {
            final int msg1Id = msgIds.get(i);
            final int from1 = msgIdsLast.get(msg1Id);
            final int to1 = msgIdsFirst.get(msg1Id);
            final int length1 = Math.max(0, to1 - from1);
            // The diagonal corresponds to each message occurrences.
            increaseJointOccurences(msg1Id, msg1Id, SummarizationProperties.TIMELINE_RESOLUTION - length1);

            for (int j = i + 1; j < msgIds.size(); j++) {
                final int msg2Id = msgIds.get(j);
                final Short to2 = msgIdsFirst.get(msg2Id);
                final Short from2 = msgIdsLast.get(msg2Id);
                final int length2 = Math.max(0, to2 - from2);
                final int overlap = Math.max(0, Math.min(to1, to2) - Math.max(from1, from2));
                increaseJointOccurences(msg1Id, msg2Id,
                        SummarizationProperties.TIMELINE_RESOLUTION - length1 - length2 + overlap);
            }
        }
    }

    @Override
    public final void incomingSeparator(TimeSeparator sep) throws AdeException,
            AdeFlowException {
        flushState();
    }

    @Override
    public final void endOfStream() throws AdeException {
        super.endOfStream();
        flushState();
    }

    protected final void flushState() {
        // flash state
        m_lastInterval = null;
    }

}
