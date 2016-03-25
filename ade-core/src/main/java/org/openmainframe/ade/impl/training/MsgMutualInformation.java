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

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.core.ListSortedByKey;
import org.openmainframe.ade.core.matrix.IDoubleMatrix;
import org.openmainframe.ade.core.matrix.SymmetricDoubleMatrix;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.dbUtils.DbDictionary;
import org.openmainframe.ade.impl.utils.FileUtils;
import org.openmainframe.ade.impl.utils.GeneralUtils;
import org.openmainframe.ade.utils.IndexedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A streaming based computation of signed mutual information between occurrences of message IDs. </p>
 * 
 * This class calculate the mutual information matrix between occurrences of legal message IDs
 * in intervals. All illegal messages IDs are ignores </p>
 * The calculation is divided into two stages: </p>
 * 1) Training: Intervals are added using the {@link #incomingObject(Object)} method.
 * A matrix holding the number of occurrences and co-occurrences (in interval) is kept and 
 * updated at each interval addition. Note that the matrix is indexed using arbitrary 
 * consecutive indices that differ from the original internal message IDs</p>
 * 2) Training completion: When the {@link #endOfStream()} method is called, an in place calculation 
 * of signed mutual information is performed on the above matrix. Each entry, originally containing the 
 * co-occurrences value corresponding to a pair of message IDs is replaced by the mutual 
 * information value corresponding to the same pair. The calculation is performed in place in 
 * order to conserve memory. 
 */
public class MsgMutualInformation implements IFrameableTarget<IInterval, TimeSeparator>, IMutualInformationHolder {

    private static Logger logger = LoggerFactory.getLogger(MsgMutualInformation.class);
    private static final String DELIM = "\t";
    private static final double LOG2 = Math.log(2);

    protected boolean m_streamClosed;

    /**
     * A counter that counts the number of processed intervals.
     */
    protected int m_totalNumIntervals = 0;
    /**
     * A counter of encountered message IDs.
     */
    private int m_totalMsgIdCount = 0;

    /**
     * For memory optimization reasons: 
     * This matrix will hold the co-occurrences values during the training.
     * When training is completed (eof() is called) the values will be replaced by 
     * the mutual information values.
     */
    private IDoubleMatrix mCoOccurrencesAndMiMatrix;

    /**
     * Data structures used for two ways translating of mutual information matrix indices 
     * to message IDs and vice versa.
     */
    private int[] m_msgIndices2msgIdMap;
    private IndexedSet<Integer> m_msgId2msgIndicesMap = new IndexedSet<Integer>();

    /**
     * A set holding all the legal message id's, all other 
     * message id's will be ignored.
     */
    private Set<Integer> mLegalMsgIds;
    protected int m_countFactor = 1;
    protected final int m_intervalFactor;

    /**
     * Constructor, receives the set of legal message IDs, all other id's will be ignored.
     * @param legalMsgIds
     */
    public MsgMutualInformation(Set<Integer> legalMsgIds) {
        this(legalMsgIds, 1);
    }

    protected MsgMutualInformation(Set<Integer> legalMsgIds, int intervalFactor) {
        mLegalMsgIds = legalMsgIds;
        mCoOccurrencesAndMiMatrix = new SymmetricDoubleMatrix(legalMsgIds.size());
        m_intervalFactor = intervalFactor;
    }

    @Override
    public void incomingObject(IInterval interval) throws AdeInternalException {
        if (m_streamClosed) {
            throw new AdeInternalException("Cannot add interval to a closed stream.");
        }

        m_totalNumIntervals += m_intervalFactor;

        final ArrayList<Integer> msgIds = new ArrayList<Integer>(interval.getMessageSummaries().size());
        for (IMessageSummary ms : interval.getMessageSummaries()) {
            msgIds.add(ms.getMessageInternalId());
        }
        calculateJoinOccurence(msgIds);
    }

    protected void calculateJoinOccurence(ArrayList<Integer> msgIds)
            throws AdeInternalException {
        // used ArrayList in order to doubly iterate on the message summaries without repeats in the
        // internal loop
        for (int i = 0; i < msgIds.size(); i++) {
            final int msg1Id = msgIds.get(i);
            // The diagonal corresponds to each message occurrences.
            increaseJointOccurences(msg1Id, msg1Id);
            for (int j = i + 1; j < msgIds.size(); j++) {
                final int msg2Id = msgIds.get(j);
                increaseJointOccurences(msg1Id, msg2Id);
            }
        }
    }

    /**
     * Translates message IDs to message indices and updates the co-occurrences matrix.
     * @param msg1Id
     * @param msg2Id
     * @throws AdeInternalException
     */
    protected void increaseJointOccurences(int msg1Id, int msg2Id) throws AdeInternalException {
        increaseJointOccurences(msg1Id, msg2Id, m_countFactor);
    }

    /**
     * Translates message IDs to message indices and updates the co-occurrences matrix.
     * @param msg1Id
     * @param msg2Id
     * @param count - count to increase
     * @throws AdeInternalException
     */
    protected void increaseJointOccurences(int msg1Id, int msg2Id, int count) throws AdeInternalException {
        if (!mLegalMsgIds.contains(msg1Id) || !mLegalMsgIds.contains(msg2Id)) {
            return;
        }
        m_msgId2msgIndicesMap.add(msg1Id);
        m_msgId2msgIndicesMap.add(msg2Id);
        final int globalMsgIndex1 = m_msgId2msgIndicesMap.indexOf(msg1Id);
        final int globalMsgIndex2 = m_msgId2msgIndicesMap.indexOf(msg2Id);

        mCoOccurrencesAndMiMatrix.set(globalMsgIndex1, globalMsgIndex2,
                mCoOccurrencesAndMiMatrix.get(globalMsgIndex1, globalMsgIndex2) + count);
    }

    @Override
    public void endOfStream() throws AdeException {
        m_streamClosed = true;
        m_totalMsgIdCount = mCoOccurrencesAndMiMatrix.getRowNum();
        logger.info("Create MI model");
        calculateModel();
    }

    /**
     * Returns the symmetric mutual information matrix.
     * @return
     * @throws AdeInternalException
     */
    public IDoubleMatrix getMutualInformationMatrix() throws AdeInternalException {
        if (mCoOccurrencesAndMiMatrix == null) {
            throw new AdeInternalException("Mutual information matrix not calculated yet. eof() must be called to close stream.");
        }
        return mCoOccurrencesAndMiMatrix;
    }

    /**
     * Calculates the mutual information matrix in place from the co-occurrences matrix.
     * @throws AdeInternalException
     */
    private void calculateModel() throws AdeInternalException {
        calculateMsgIndices2msgIdMap();
        final int numValidMessages = m_msgIndices2msgIdMap.length;
        logger.info("MI matrix size (num of valid messege IDs) is " + numValidMessages + " X " + numValidMessages);

        int numOfNonZeroPairs = 0;

        // A matrix corresponding to the joint probability distribution of two message IDs.
        final double[][] jointProbabilities = new double[2][2];
        // Calculate in line the signed mutual information.
        for (int i = 0; i < mCoOccurrencesAndMiMatrix.getRowNum(); i++) {
            for (int j = 0; j < i; j++) {
                if ((int) mCoOccurrencesAndMiMatrix.get(i, j) != 0) {
                    numOfNonZeroPairs++;
                }
                calculateAndSetSignedMutualInformation(i, j, jointProbabilities);
            }
        }

        // Diagonal is calculated last since it's entries are needed for previous calculations.
        for (int i = 0; i < mCoOccurrencesAndMiMatrix.getRowNum(); i++) {
            calculateAndSetSignedMutualInformation(i, i, jointProbabilities);
        }

        GeneralUtils.logMemStatus("End of calculateModel()");
        logger.info("number of non-zero messege ID pairs is " + numOfNonZeroPairs);
    }

    /**
     * calculate message indices to message IDs mapping from the reverse mapping.
     */
    private void calculateMsgIndices2msgIdMap() {
        m_msgIndices2msgIdMap = new int[m_msgId2msgIndicesMap.size()];
        for (Entry<Integer, Integer> entry : m_msgId2msgIndicesMap.getReverseTranslationMap().entrySet()) {
            m_msgIndices2msgIdMap[entry.getKey()] = entry.getValue();
        }
    }

    /**
     * Calculates the signed mutual information for the pair i, j and sets it
     * in the corresponding place of the m_miMatrix.
     * @param i
     * @param j
     * @param jointProbabilities is inputed in order to decrease the memory allocation operations.
     * @throws MlCoreException
     */
    private void calculateAndSetSignedMutualInformation(int i, int j,
            double[][] jointProbabilities) {
        // Calculate the joint probability distribution matrix.
        // P(both msgs appear in an interval)
        jointProbabilities[1][1] = (double) Math.round(mCoOccurrencesAndMiMatrix.get(i, j)) / m_totalNumIntervals;
        // P(msg1 appears in an interval but msg2 does not)
        jointProbabilities[1][0] = (double) Math.round(mCoOccurrencesAndMiMatrix.get(i, i) - mCoOccurrencesAndMiMatrix.get(i, j))
                / m_totalNumIntervals;
        // P(msg2 appears in an interval but msg1 does not)
        jointProbabilities[0][1] = (double) Math.round(mCoOccurrencesAndMiMatrix.get(j, j) - mCoOccurrencesAndMiMatrix.get(i, j)) 
                / m_totalNumIntervals;
        // P(both msgs don't appear in an interval)
        jointProbabilities[0][0] = (double) Math.round(m_totalNumIntervals - mCoOccurrencesAndMiMatrix.get(i, i)
                - mCoOccurrencesAndMiMatrix.get(j, j) + mCoOccurrencesAndMiMatrix.get(i, j)) / m_totalNumIntervals;

        // Replace the i,j'th MI entry (currently holding the co-occurrences count) 
        // with the result of the MI(i, j) calculation times the sign.
        final double mi = calculateSign(jointProbabilities) * calculateMutualInformation(jointProbabilities);
        mCoOccurrencesAndMiMatrix.set(i, j, mi);
    }

    /**
     * Returns the sign of the 
     * <a href="http://en.wikipedia.org/wiki/Phi_coefficient">
     * Phi coefficient (Pearson Correlation)</a>.
     * @param jointProbabilities
     * @return 1 if the correlation coefficient if non negative, -1 otherwise.
     */
    private int calculateSign(double[][] jointProbabilities) {
        final double correlation = jointProbabilities[1][1] * jointProbabilities[0][0]
                - jointProbabilities[0][1] * jointProbabilities[1][0];
        return (correlation >= 0) ? 1 : -1;
    }

    /**
     * Calculate the mutual information using: MI(X, Y) = H(X) + H(Y) - H(X, Y)
     * @param jointProbabilities
     * @return
     */
    private double calculateMutualInformation(double[][] jointProbabilities) {
        double entropy1, entropy2, jointEntropy;
        entropy1 = calculateEntropy(jointProbabilities[1][0] + jointProbabilities[1][1],
                jointProbabilities[0][0] + jointProbabilities[0][1]);
        entropy2 = calculateEntropy(jointProbabilities[0][1] + jointProbabilities[1][1],
                jointProbabilities[0][0] + jointProbabilities[1][0]);
        jointEntropy = calculateEntropy(jointProbabilities[0][0], jointProbabilities[0][1],
                jointProbabilities[1][0], jointProbabilities[1][1]);

        return entropy1 + entropy2 - jointEntropy;
    }

    /**
     * Calculates the entropy corresponding to the input masses.
     * @param masses
     * @return
     */
    private double calculateEntropy(double... masses) {
        double res = 0;
        for (double d : masses) {
            res -= (d == 0) ? 0 : d * Math.log(d);
        }
        return res / LOG2;
    }

    /**
     * Returns the map translation from mutual information matrix indices to message IDs. 
     * @return
     */
    public int[] getMatIndexToMsgInternalId() {
        return m_msgIndices2msgIdMap;
    }

    /** Print the matrix report to given file.
     * @see printReport(PrintWriter)
     * @param file
     * @throws AdeException
     */
    public void printReport(File file) throws AdeException {
        final PrintWriter out = FileUtils.openPrintWriterToFile(file, true);
        printReport(out);
        out.close();
    }

    /**
     * Prints the MI matrix to a given output stream.
     * Report contains only pair that (both) passed the threshold.
     * This method may only be called after eof() and before compact()
     * Note: this method outputs a different output than it's previous versions. 
     * @param out
     * @throws AdeException
     */
    public void printReport(PrintWriter out) throws AdeException {
        out.println("% msg-id1" + DELIM + "msg-internal-id" + DELIM + "msg-id2"
                + DELIM + "msg-internal-id2" + DELIM + "information");
        final DbDictionary dictionary = AdeInternal.getAdeImpl().getDataStore().getAdeDictionaries().getMessageIdDictionary();

        int msgId1, msgId2;
        String msgStr1, msgStr2;
        for (int i = 0; i < mCoOccurrencesAndMiMatrix.getRowNum(); i++) {
            for (int j = 0; j <= i; j++) {
                msgId1 = m_msgIndices2msgIdMap[i];
                msgId2 = m_msgIndices2msgIdMap[j];
                msgStr1 = dictionary.getWordById(msgId1);
                msgStr2 = dictionary.getWordById(msgId2);

                out.println(msgStr1 + DELIM + msgId1 + DELIM + msgStr2 + DELIM + msgId2
                        + DELIM + mCoOccurrencesAndMiMatrix.get(i, j));
            }
        }
    }

    /** Print details of a specific member of a cluster
     * 
     * @param out Destination stream
     * @param member index to the mutual information matrix of the target member
     * @param cluster the cluster it belongs to
     * @throws AdeException
     */
    public void printMemberReport(PrintWriter out, int member,
            Set<Integer> cluster) throws AdeException {
        out.printf("%-15s:\n", memberStr(member));

        final ListSortedByKey<Double, String> list = new ListSortedByKey<Double, String>();
        list.setValueNaturalOrder();
        for (int i : cluster) {
            double info;
            info = mCoOccurrencesAndMiMatrix.get(member, i);
            list.add(info, memberStr(i));
        }
        list.invertKeyOrdering();
        out.printf("\t\tCluster relations:");
        for (int i = 0; i < list.size(); ++i) {
            out.printf(" %f:%s", list.getKey(i), list.getValue(i));
        }
        out.println();

        list.clear();
        for (int i = 0; i < mCoOccurrencesAndMiMatrix.getRowNum(); ++i) {
            if (cluster.contains(i)) {
                continue;
            }
            double info;
            info = mCoOccurrencesAndMiMatrix.get(member, i);
            list.add(info, memberStr(i));
        }
        out.printf("\t\tTop 5 out of cluster relations:");
        for (int i = 0; i < Math.min(5, list.size()); ++i) {
            out.printf(" %f:%s", list.getKey(i), list.getValue(i));
        }
        out.println();
    }

    /** Return a string representation of a msg-id corresponding to given matrix index */
    private String memberStr(int member) throws AdeException {
        if (member < 0) {
            return "NONE";
        }
        final int msgIid = m_msgIndices2msgIdMap[member];
        final String msgId = AdeInternal.getAdeImpl().getDictionaries().getMessageIdDictionary().getWordById(msgIid);
        return String.format("%s(%d)", msgId, msgIid);
    }

    /** Returns total number of msg-ids found. 
     * May be called only after eof().
     * This value is larger or equal to getMutualInformationMatrix().getRowNum()
     */
    public int getTotalMsgIdCount() {
        return m_totalMsgIdCount;
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        // TODO Auto-generated method stub

    }

    /**
     * Returns the index of the specified msgid in this set, or -1 if this set does not contain the msgid
     */
    public int getIndexOfMsgId(int msgId) {
        return m_msgId2msgIndicesMap.indexOf(msgId);
    }

    public double calcSimilaritySum(Set<Integer> cluster) {
        double similaritySum = 0;
        int similaritySumN = 0;
        for (int i : cluster) {
            for (int j : cluster) {
                final double v = mCoOccurrencesAndMiMatrix.get(i, j);
                if (!Double.isNaN(v)) {
                    similaritySum += v;
                    similaritySumN++;
                }
            }
        }
        return similaritySum / similaritySumN;
    }

    @Override
    public void incomingSeparator(TimeSeparator sep) throws AdeException,
            AdeFlowException {
        // nothing to do.  This version holds no state.
    }

}
