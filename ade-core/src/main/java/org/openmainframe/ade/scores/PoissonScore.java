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
import java.util.SortedMap;
import java.util.TreeMap;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.impl.utils.MathUtils;
import org.openmainframe.ade.scoringApi.MessageScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.IStructuredOutputWriter;

public class PoissonScore extends MessageScorer {

    public static final String LOG_PROB = "logProb";
    public static final String LOG_PROB_T = "logProbT";
    public static final String MAX_PROB = "maxProb";
    public static final String IS_NEW = "isNew";

    private static final long serialVersionUID = 1L;

    @Property(key = "baseScorer", required = true, help = "Clustering Scorer used to detirmane if message is in context")
    private String m_baseScorerName;

    // msgIDs that appear more than this number of times in an interval
    // are considered as if they appeared this number of times
    public static final int MAX_NUM_APPEAR = 1000;

    static public class MsgData implements Serializable {
        private static final long serialVersionUID = 1L;
        public int m_count = 0;
        public int m_intervalCount = 0;
        public double m_lambda = 0;
        public int m_intervalAllCount = 0;
        public int m_allCount;

        public String toString() {
            return String.format("count=%d intervalCount=%d lambda=%f intervalAllCount=%d allCount=%d",
                    m_count, m_intervalCount, m_lambda, m_intervalAllCount, m_allCount);
        }
    }

    private SortedMap<String, MsgData> m_msgData = null;

    private boolean m_trained = false;
    private int m_totalIntervalCount = 0;
    private double m_minLambda;
    static private MathUtils m_logFactorials = new MathUtils();

    @Override
    protected void reset() throws AdeException {
        super.reset();
        m_trained = false;
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
        m_msgData = new TreeMap<String, MsgData>();
        m_totalIntervalCount = 0;
        m_minLambda = 1;
    }

    @Override
    public void incomingObject(IAnalyzedInterval analyzedInterval) throws AdeException {
        for (IAnalyzedMessageSummary ams : analyzedInterval.getAnalyzedMessages()) {
            final String id = ams.getMessageId();

            MsgData data = m_msgData.get(id);
            if (data == null) {
                data = new MsgData();
                m_msgData.put(id, data);
            }
            if (!isClustered(ams)) {
                data.m_count += ams.getNumberOfAppearances();
                data.m_intervalCount++;
            }
            data.m_allCount += ams.getNumberOfAppearances();
            data.m_intervalAllCount++;
        }
        ++m_totalIntervalCount;
    }

    @Override
    public void endOfStream() throws AdeException {
        for (MsgData data : m_msgData.values()) {
            if (data.m_count > 0) {
                data.m_lambda = (double) data.m_count / m_totalIntervalCount;
                m_minLambda = Math.min(data.m_lambda, m_minLambda);
            }
        }
        m_trained = true;
    }

    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary ams, IAnalyzedInterval interval) throws AdeException {
        final StatisticsChart sc = new StatisticsChart();

        final MsgData data = m_msgData.get(ams.getMessageId());
        final boolean noLambda = data == null || data.m_count == 0;

        double lambda = 0;
        if (noLambda) {
            lambda = m_minLambda;
        } else {
            lambda = data.m_lambda;
        }

        final int numInstances = ams.getMessageSummary().getNumMessageInstances();
        double logProb = 0;
        double logProbT = 0;
        double anomaly = 0;

        if (m_totalIntervalCount > 0 && !isClustered(ams)) {
            logProb = calcLogProb(numInstances, false, lambda);
            logProbT = calcLogProb(numInstances, true, lambda);
            anomaly = noLambda ? 1.0 : 0.999 * (1 - Math.exp(logProb));
        }

        sc.setStat(LOG_PROB, -logProb);
        sc.setStat(LOG_PROB_T, -logProbT);
        sc.setStat(MAX_PROB, -calcLogProb(numInstances, false, m_minLambda));
        sc.setStat(MAIN, anomaly);
        return sc;
    }

    private boolean isClustered(IAnalyzedMessageSummary ams) {
        final Double context = ams.getStatistics().getDoubleStat(m_baseScorerName + "." + MAIN);
        return context != null && context == 0;
    }

    static private double calcLogProb(int numAppear, boolean withThreshold, double lambda) {
        if (withThreshold) {
            numAppear = Math.min(numAppear, MAX_NUM_APPEAR);
        }
        // k *(log lambda) - lambda - (log k!)
        final double res = (numAppear * Math.log(lambda)) - lambda
                - m_logFactorials.computeLogFactorial(numAppear);

        assert (res <= 0);
        return res;
    }

    @Override
    public void debugPrint(PrintStream out) throws AdeException {
        out.println("Trained=" + m_trained);
        out.println("Total interval count=" + m_totalIntervalCount);
        out.println("min lambda=" + m_minLambda);
        for (String key : m_msgData.keySet()) {
            out.println(key + " : " + m_msgData.get(key));
        }
    }

    @Override
    public void printGeneralUserData(IStructuredOutputWriter out) throws Exception {
        out.simpleChild("intervalCount", m_totalIntervalCount);
        out.simpleChild("uniqueMsgIds", m_msgData.size());
    }

    @Override
    public void printMessageUserData(IStructuredOutputWriter out, String msgId) throws Exception {
        final MsgData data = m_msgData.get(msgId);
        if (data == null) {
            out.simpleChild("totalInstances", 0);
            return;
        }
        out.simpleChild("intervalSeenIn", data.m_intervalAllCount);
        out.simpleChild("totalInstances", data.m_allCount);
        out.simpleChild("intervalsSeenInOutOfContext", data.m_intervalCount);
        out.simpleChild("totalInstancesOutOfContext", data.m_count);
        if (data.m_count > 0) {
            out.simpleChild("estimatedLambda", data.m_lambda);
            out.simpleChild("instanceCountForAnomaly9", calcThreshold(0.9, data.m_lambda));
            out.simpleChild("instanceCountForAnomaly99", calcThreshold(0.99, data.m_lambda));
        }
    }

    /** Calculate the mininal number of instances required to achieve a score as high as the
     *  given scoreThreshold for the given lambda
     */
    static private String calcThreshold(double scoreThreshold, double lambda) {
        // Convert the score threshold to a threshold on log(P) by inverting the score formula
        // score=0.999*(1-P)
        // which gives:
        // 0.999*(1-P)>=scoreThreshold
        // P<=1-scoreThreshold/0.999
        // log(P)<=log(1-scoreThreshold/0.999)
        final double logProbThreshold = Math.log(1 - scoreThreshold / 0.999);
        int maxNum = 1000000;
        // If 1e6 is not enough to pass the score threshold, return ">1e6"
        if (calcLogProb(maxNum, false, lambda) > logProbThreshold) {
            return ">1e6";
        }
        // Use binary search to find the number
        int minNum = 1;
        while (minNum < maxNum) {
            final int midNum = (maxNum + minNum) / 2;
            // Invariant:
            // maxNum passes the threshold
            // minNum<=midNum<maxNum
            final boolean passThreshold = calcLogProb(midNum, false, lambda) <= logProbThreshold;
            if (passThreshold) {
                maxNum = midNum;
            } else {
                // midNum==minNum if maxNum=minNum+1, in which case maxNum is the lowest that passes the threshold
                if (midNum == minNum) {
                    break;
                }
                minNum = midNum;
            }
        }
        return Integer.toString(maxNum);
    }

    public int getNumRecords() {
        return m_msgData.size();
    }

    public MsgData getRecord(String msgId) {
        return m_msgData.get(msgId);
    }

    public void addRecord(String messageId, int i, double d) {
        if (m_msgData == null) {
            m_msgData = new TreeMap<String, PoissonScore.MsgData>();
            m_minLambda = 1;
        }
        final MsgData data = new MsgData();
        data.m_count = i;
        data.m_lambda = d;
        m_msgData.put(messageId, data);
        m_minLambda = Math.min(data.m_lambda, m_minLambda);
        // Artificially set number of intervals to 1.
        // If it remains 0, it will shut down calculation in getScore
        m_totalIntervalCount = 1;
    }

    public static void main(String args[]) {
        calcThreshold(0.99, 1.2569444444444444);

    }

    public int getNumOutOfContextRecords() {
        int res = 0;
        for (MsgData d : m_msgData.values()) {
            if (d.m_count > 0) {
                res++;
            }
        }
        return res;
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        // TODO Auto-generated method stub

    }

    public void setBaseScorerName(String name) {
        m_baseScorerName = name;
    }
}
