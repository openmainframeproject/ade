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
 
*/package org.openmainframe.ade.scores;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.scoringApi.MessageScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.IStructuredOutputWriter;

public class RarityScore extends MessageScorer {

    public static final String FULL_PROB = "fullProb";
    public static final String BERNOULLI_COUNT = "bernoulliCount";
    public static final String BERNOULLI_PROB = "bernoulliProb";
    public static final String NORMRLIZED_PROBABILITY = "normalized";
    public static final String PROBABILITY = "probability";
    public static final String LOG_PROBABILITY = "logProbability";
    public static final int MAX_SCORE = 100;
    public static final int MAXDB = 16;

    private static final long serialVersionUID = 1L;

    static private class MsgData implements Serializable {
        public MsgData() {
            super();
            m_counts = new int[MAXDB + 1];
            for (int i = 0; i <= MAXDB; ++i) {
                m_counts[i] = 0;
            }
        }

        private static final long serialVersionUID = 1L;
        int[] m_counts;
        int m_count = 0;
        double[] m_probs;
        public double m_bernoulliProb;
        public Double m_maxProb;
    }

    private SortedMap<String, MsgData> m_msgData = null;

    private boolean m_trained = false;
    private int m_totalIntervalCount = 0;
    private double m_neverSeenProb;
    private double m_logProbEmpty;

    @Override
    public boolean needsAnotherIteration() throws AdeException {
        return !m_trained;
    }

    @Override
    public void reset() throws AdeException {
        super.reset();
        m_trained = false;
        m_msgData = null;
        m_totalIntervalCount = 0;
        m_logProbEmpty = 0.0;
        m_neverSeenProb = 1.0;

    }

    @Override
    public void startIteration() throws AdeException {
        if (m_trained) {
            throw new AdeInternalException("Already trained");
        }
        m_msgData = new TreeMap<String, MsgData>();
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
            final int dbNum = descritizeCount(ms);
            ++data.m_counts[dbNum];
            ++data.m_count;
        }
        ++m_totalIntervalCount;
    }

    @Override
    public void endOfStream() throws AdeException {
        m_logProbEmpty = 0.0;
        for (MsgData data : m_msgData.values()) {
            data.m_probs = new double[MAXDB + 1];
            data.m_bernoulliProb = (data.m_count + 0.5) / (m_totalIntervalCount + 1);
            final double adjustedSum = data.m_count + (MAXDB + 1.0) / 2.0;
            data.m_maxProb = 0.0;
            for (int i = 0; i <= MAXDB; ++i) {
                data.m_probs[i] = ((double) data.m_counts[i] + 0.5) / adjustedSum;
                if (data.m_probs[i] > data.m_maxProb) {
                    data.m_maxProb = data.m_probs[i];
                }
            }
            data.m_counts = null;//we can now clear this.
            m_logProbEmpty += Math.log(1 - data.m_bernoulliProb);
        }
        m_neverSeenProb = (0.5) / (m_totalIntervalCount + 1);
        m_trained = true;
    }

    private int descritizeCount(IMessageSummary msg) {
        return Math.min(MAXDB, (int) (Math.log((double) msg.getNumMessageInstances()) / Math.log(2)));
    }

    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary ams, IAnalyzedInterval interval) throws AdeException {
        final StatisticsChart sc = new StatisticsChart();
        final MsgData data = m_msgData.get(ams.getMessageId());
        if (data == null) {
            sc.setStat(MAIN, 1.0 - m_neverSeenProb);
        } else {
            final Double prob = data.m_probs[descritizeCount(ams.getMessageSummary())];
            sc.setStat("MessageCount", ams.getMessageSummary().getNumMessageInstances());
            sc.setStat(PROBABILITY, prob);
            sc.setStat(LOG_PROBABILITY, Math.log(prob));
            final double normalizedProb = prob / data.m_maxProb;
            sc.setStat(NORMRLIZED_PROBABILITY, normalizedProb);
            sc.setStat(BERNOULLI_PROB, data.m_bernoulliProb);
            sc.setStat(BERNOULLI_COUNT, data.m_count);
            sc.setStat(FULL_PROB, 1 - prob * data.m_bernoulliProb);
            sc.setStat(MAIN, 1 - normalizedProb * data.m_bernoulliProb);
        }
        return sc;
    }

    @Override
    public void debugPrint(PrintStream out) throws AdeException {
        out.println("Trained=" + m_trained);
        out.println("Total interval count=" + m_totalIntervalCount);
        out.println("Data=\n" + m_msgData);
    }

    @Override
    public void printMessageUserData(IStructuredOutputWriter out, String msgId) {
        final MsgData data = m_msgData.get(msgId);
        if (data == null) {
            return;
        }
    }

    @Override
    public void printGeneralUserData(IStructuredOutputWriter out) throws Exception {
        out.simpleChild("intervalCount", m_totalIntervalCount);
        out.simpleChild("uniqueMsgIds", m_msgData.size());
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        // TODO Auto-generated method stub

    }

}
