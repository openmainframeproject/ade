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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.scoringApi.MessageScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.utils.IStructuredOutputWriter;

public class MaxInformation1ContextScore extends MessageScorer {

    private class MsgData implements Serializable {

        private String m_key;
        private static final long serialVersionUID = 1L;
        private HashMap<String, Integer> m_coOccurance;
        private int m_totalCount;
        private String m_bestPair;
        private Double[] m_probsWithoutPair;
        private Double[] m_probsWithPair;
        private double m_entropy;
        private Double m_infoGain;

        public MsgData(String key) {
            super();
            m_key = key;
            m_coOccurance = new HashMap<String, Integer>();
            m_probsWithPair = new Double[2];
            m_probsWithoutPair = new Double[2];
            m_totalCount = 0;
        }

        public String getBestPair() {
            return m_bestPair;
        }

        public void endOfStream() {
            m_totalCount = m_coOccurance.get(getKey());
            m_entropy = calcEntropy(((double) m_totalCount) / m_totalIntervals);
        }

        public String locateBestPair() throws AdeInternalException {
            // go over all the matrix, and calculate the infoGain for each pair.  remember the top infoGain. 
            Double minCondEntropy = Double.MAX_VALUE;
            String bestKey = null;
            Double conditionalEntropy = null;
            for (String otherKey : keySet()) {
                if (otherKey.equals(getKey())) {
                    continue;
                }
                conditionalEntropy = calcConditionalEntropy(otherKey);

                if (conditionalEntropy < minCondEntropy) {
                    bestKey = otherKey;
                    minCondEntropy = conditionalEntropy;
                }
            }

            setBestPair(bestKey, conditionalEntropy);
            return bestKey;
        }

        // sets the best pair and the relevant probabilities.
        @SuppressWarnings("unused")
        public void setBestPair(String key) {
            double pairEntropy = calcConditionalEntropy(key);
            setBestPair(key, pairEntropy);
        }

        // sets the best pair and the relevant probabilities.
        public void setBestPair(String key, Double pairEntropy) {
            this.m_bestPair = key;

            if (key == null) {
                setProbsWithMate(getTotalCount(), m_totalIntervals);
                setProbsWithoutMate(getTotalCount(), m_totalIntervals);
                m_infoGain = 0.0;
            } else {
                Integer coOccor = m_coOccurance.get(key);
                Integer totalCountBestPair = m_data.get(key).getTotalCount();
                setProbsWithMate(coOccor, totalCountBestPair);
                setProbsWithoutMate(getTotalCount() - coOccor, m_totalIntervals - totalCountBestPair);
                m_infoGain = (getEntropy() - pairEntropy) / getEntropy() * 100.0;
            }
        }

        /*
         * calc the joined entropy of 
         */
        private double calcJoinedEntropy(String otherKey) {
            //      data.
            //      ,
            //      

            double p1 = getCount(otherKey).doubleValue() / m_totalIntervals;
            double p2 = (((double) getTotalCount()) - m_coOccurance.get(otherKey)) / m_totalIntervals;
            double p3 = (((double) m_data.get(otherKey).getTotalCount()) - m_coOccurance.get(otherKey)) / m_totalIntervals;
            double p4 = 1 - p1 - p2 - p3;
            return -(p1 * Math.log(p1 + Double.MIN_NORMAL) + p2 * Math.log(p2 + Double.MIN_NORMAL) + p3 * Math.log(p3 + Double.MIN_NORMAL) + p4 * Math.log(p4 + Double.MIN_NORMAL));
        }

        public double calcConditionalEntropy(String otherKey) {

            double entropy = m_data.get(otherKey).getEntropy();
            double h = calcJoinedEntropy(otherKey) - entropy;

            return h;
        }

        private double getEntropy() {
            return m_entropy;
        }

        public Double getProb(boolean thisMsgState, boolean otherMsgState) {
            return otherMsgState ? getProbsWithMate(thisMsgState) : getProbsWithoutMate(thisMsgState);
        }

        private Double getProbsWithMate(boolean see) {
            return m_probsWithPair[see ? 1 : 0];
        }

        public void setProbsWithMate(Integer countSeeGivenSee, Integer totalCountBestPair) {
            Double probSeeGivenSee = (countSeeGivenSee.doubleValue() + 0.5) / (totalCountBestPair.doubleValue() + 0.5);
            this.m_probsWithPair[0] = 1 - probSeeGivenSee;
            this.m_probsWithPair[1] = probSeeGivenSee;
        }

        private Double getProbsWithoutMate(Boolean see) {
            return m_probsWithoutPair[see ? 1 : 0];
        }

        public void setProbsWithoutMate(Integer countSeeGivenNoSee, Integer totalCountNoBestPair) {
            Double probSeeGivenNoSee = (countSeeGivenNoSee.doubleValue() + 0.5) / (totalCountNoBestPair.doubleValue() + 0.5);
            this.m_probsWithoutPair[0] = 1 - probSeeGivenNoSee;
            this.m_probsWithoutPair[1] = probSeeGivenNoSee;
        }

        public Double getInfoGain() {
            return m_infoGain;
        }

        public String getKey() {
            return m_key;
        }

        public int getTotalCount() {
            return m_totalCount;
        }

        public void addCount(String key) {
            Integer count = m_coOccurance.get(key);
            if (count == null) {
                count = Integer.valueOf(0);
            }
            ++count;
            m_coOccurance.put(key, count);

        }

        public Integer getCount(String key) {
            Integer count = m_coOccurance.get(key);
            if (count == null) {
                count = 0;
            }
            return count;
        }

        @SuppressWarnings("unused")
        public Set<Entry<String, Integer>> entrySet() {
            return m_coOccurance.entrySet();
        }

        public Collection<String> keySet() {
            return m_coOccurance.keySet();
        }

        /*
         * clear data needed only for train.
         */
        public void clearTrainData() {
            m_coOccurance = null;
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    Boolean m_doneTrain = false;
    private int m_totalIntervals;
    private HashMap<String, MsgData> m_data;

    // private 
    public MaxInformation1ContextScore() {
        super();
    }

    @Override
    public void reset() throws AdeException {
        super.reset();
        m_doneTrain = false;
        m_data = null;
        m_totalIntervals = 0;
    }

    @Override
    public void startIteration() throws AdeException {
        m_data = new HashMap<String, MsgData>();
    }

    @Override
    public StatisticsChart getScore(IAnalyzedMessageSummary scoredElement,
            IAnalyzedInterval contextElement) throws AdeException {

        StatisticsChart newSc = new StatisticsChart();
        MsgData data = m_data.get(scoredElement.getMessageId());
        if (data != null) {
            String pair = data.getBestPair();
            Boolean pairSeen = false;
            if (pair != null) {
                newSc.setStat("bestPair", pair);
                for (IAnalyzedMessageSummary ams : contextElement.getAnalyzedMessages()) {
                    if (pair.equals(ams.getMessageId())) {
                        pairSeen = true;
                        break;
                    }
                } // did not find the other key
            }
            Double prob = data.getProb(true, pairSeen);
            newSc.setStat("pairSeen", pairSeen.toString());
            newSc.setStat("prob", prob);
            newSc.setStat(MAIN, 1 - prob);
        } else {
            newSc.setStat(MAIN, 1);
        }
        return newSc;
    }

    @Override
    public boolean needsAnotherIteration() throws AdeException {
        return !m_doneTrain;
    }

    @Override
    public void incomingObject(IAnalyzedInterval obj) throws AdeException {
        for (IAnalyzedMessageSummary i : obj.getAnalyzedMessages()) {
            String messageId1 = i.getMessageId();
            for (IAnalyzedMessageSummary j : obj.getAnalyzedMessages()) {
                String messageId2 = j.getMessageId();
                addCoOccurance(messageId1, messageId2);
            }
        }
        ++m_totalIntervals;

        return;
    }

    private void addCoOccurance(String messageId1, String messageId2) {
        MsgData m = m_data.get(messageId1);
        if (m == null) {
            m = new MsgData(messageId1);
            m_data.put(messageId1, m);
        }
        m.addCount(messageId2);
    }

    private double calcEntropy(double prob) {
        return -(prob * Math.log(prob) + (1 - prob) * Math.log(1 - prob));
    }

    @Override
    public void endOfStream() throws AdeException {
        for (MsgData data : m_data.values()) {
            data.endOfStream();
        }
        for (MsgData data : m_data.values()) {
            data.locateBestPair();
            data.clearTrainData();
        }
        m_doneTrain = true;
    }

    @Override
    public void printMessageUserData(IStructuredOutputWriter out, String msgId) throws Exception {
        MsgData data = null;
        data = m_data.get(msgId);
        if (data == null) {
            return;
        }
        out.simpleChild("bestPredictor", data.getKey());
        out.simpleChild("bestPredictorInfoGain", data.getInfoGain());
        out.simpleChild("probWithPredictor", data.getProbsWithMate(true));
        out.simpleChild("probWithoutPredictor", data.getProbsWithoutMate(true));
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        // TODO Auto-generated method stub

    }

}
