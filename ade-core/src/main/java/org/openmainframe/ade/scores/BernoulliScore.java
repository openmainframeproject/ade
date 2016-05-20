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

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.scoringApi.MessageScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.IStructuredOutputWriter;

public class BernoulliScore extends MessageScorer {

    public static final String LOG_PROB = "logProb";
    public static final String PROBABILITY = "probability";

    @Property(key = "OriginalMaxScore", required = false, help = "NOT CLEAR")
    public int ORG_MAX_SCORE = 101;

    private static final long serialVersionUID = 1L;

    static public class MsgData implements Serializable {
        private static final long serialVersionUID = 1L;
        public int m_count = 0;
        public double m_prob = 0;
        public double m_score = 0;

        public String toString() {
            return String.format("count=%d prob=%f score=%f", m_count, m_prob, m_score);
        }
    }

    protected SortedMap<String, MsgData> m_msgData = null;

    protected boolean m_trained = false;
    protected int m_totalIntervalCount = 0;

    @Override
    protected void reset() {
        m_trained = false;
        m_totalIntervalCount = 0;
        m_msgData = null;
    }

    @Override
    public boolean needsAnotherIteration() throws AdeException {
        return !m_trained;
    }

    @Override
    public void startIteration() throws AdeException {
        if (m_trained) {
            throw new AdeInternalException("Already trained");
        }
        m_msgData = new TreeMap<>();
        m_totalIntervalCount = 0;
    }

    @Override
    public void incomingObject(IAnalyzedInterval analyzedInterval) throws AdeException {
        for (IMessageSummary ms : analyzedInterval.getInterval().getMessageSummaries()) {
            final String id = ms.getMessageId();
            MsgData data = m_msgData.get(id);
            if (data == null) {
                data = new MsgData();
                m_msgData.put(id, data);
            }
            ++data.m_count;
        }
        ++m_totalIntervalCount;
    }

    @Override
    public void endOfStream() throws AdeException {
        double minProb = 1;
        // to avoid dividing by zero if no intervals were trained on
        if (m_totalIntervalCount == 0) {
            m_totalIntervalCount = 1;
        }
        for (MsgData data : m_msgData.values()) {
            data.m_prob = (double) data.m_count / m_totalIntervalCount;
            if (data.m_prob < minProb) {
                minProb = data.m_prob;
            }

        }
        
        for (MsgData data : m_msgData.values()) {
            final double probability = data.m_prob;
            data.m_score = -Math.log(probability);
            if (data.m_score < 1) {
                data.m_score = 1;
            }
        }
        m_trained = true;
    }

    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary ams, IAnalyzedInterval interval) throws AdeException {
        final StatisticsChart sc = new StatisticsChart();
        final MsgData data = m_msgData.get(ams.getMessageId());
        if (data == null) {
            final double prob = 0.5 / m_totalIntervalCount;
            sc.setStat(MAIN, 1.0);
            sc.setStat(PROBABILITY, prob);
            sc.setStat(LOG_PROB, -Math.log(prob));
        } else {
            sc.setStat(PROBABILITY, data.m_prob);
            sc.setStat(MAIN, data.m_score / (double) ORG_MAX_SCORE);
            sc.setStat(LOG_PROB, -Math.log(data.m_prob));
        }
        return sc;
    }

    @Override
    public void debugPrint(PrintStream out) throws AdeException {
        super.debugPrint(out);
        out.println("Trained=" + m_trained);
        out.println("Total interval count=" + m_totalIntervalCount);
        for (Entry<String, MsgData> msg : m_msgData.entrySet()) {
            final MsgData value = msg.getValue();
            out.println(msg.getKey() + " : " + value);
        }
    }

    @Override
    public void printMessageUserData(IStructuredOutputWriter out, String msgId) throws Exception {
        final MsgData data = m_msgData.get(msgId);
        if (data == null) {
            out.simpleChild("intervalsSeenIn", 0);
            return;
        }
        out.simpleChild("intervalsSeenIn", data.m_count);
        out.simpleChild("probabiltyToAppearInInterval", data.m_prob);
        out.simpleChild("score", data.m_score);
    }

    @Override
    public void printGeneralUserData(IStructuredOutputWriter out) throws Exception {
        out.simpleChild("intervalCount", m_totalIntervalCount);
        out.simpleChild("uniqueMsgIds", m_msgData.size());
    }

    public void addRec(String msgId, int count, double prob, double score) {
        if (m_msgData == null) {
            m_msgData = new TreeMap<>();
        }
        m_trained = true;
        final MsgData data = new MsgData();
        data.m_count = count;
        data.m_prob = prob;
        data.m_score = score;
        m_msgData.put(msgId, data);
    }

    public double getScoreByMsgId(String msgId) {
        final MsgData data = m_msgData.get(msgId);
        if (data == null) {
            return 1;
        }
        return data.m_score / 101.0;
    }

    public void setTotIntervals(int i) {
        m_totalIntervalCount = i;

    }

    public double getProbByMsgId(String msgId) {
        final MsgData data = m_msgData.get(msgId);
        if (data == null) {
            return 0;
        }
        return data.m_prob;
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        // TODO: Auto-generated method stub

    }

}
