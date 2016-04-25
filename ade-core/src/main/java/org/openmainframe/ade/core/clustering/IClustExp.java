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
package org.openmainframe.ade.core.clustering;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.openmainframe.ade.core.exceptions.AdeCoreIllegalArgumentException;
import org.openmainframe.ade.core.exceptions.AdeCoreIllegalStateException;
import org.openmainframe.ade.core.exceptions.AdeCoreInternalException;
import org.openmainframe.ade.core.exceptions.AdeCoreInvalidInitialStateException;
import org.openmainframe.ade.core.matrix.IDoubleMatrix;
import org.openmainframe.ade.core.matrix.DoubleMatrixOps;

public class IClustExp implements IClusteringAlgorithm {
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    private IDoubleMatrix mSimilarity;
    private int mMaxTrialNum = 10000;
    private int mClusterNum = -1;
    private int mMaxIdleTrialNum = 100;
    private double mAlpha = 0;
    private int mNumElements;
    private Random mRandom;
    private int mGlobalSeed = 0;
    private int mMinClusterSize = 1;

    private int mTrials;
    private int mIdleTrials;
    private int mVerbosity = 0;
    private PrintStream mVerbosityOut = null;
    private List<Double> mScoreArchive = null;
    private int[] mCandidateClusters = null;
    private int mCandidateClustersNum = 0;
    private boolean mConverged;
    private int[] mInitialPartition = null;
    private int mRunNum = 10;
    /**
     *  Allow emptying of clusters
     */
    private boolean mEnableEmptyClusters = false;
    /**
     *  Value for iClust for a cluster with just a single element.  
     *  Setting this to zero or less should not allow clusters to empty
     */
    private double mSingleElementScore = .2;

    private int[] mElementsPermutation = null;
    private int mElementsPermutationIndex = 0;

    private ArrayList<IClustRunSummary> mRunsSummary = null;

    public class IClustRunSummary extends IClusteringAlgorithm.RunSummary {
        private static final long serialVersionUID = 1L;

        public int mIdleTrials;

        public IClustRunSummary(double score, int iterations, long time, long seed, int idleTrials) {
            super(score, iterations, time, seed);
            mIdleTrials = idleTrials;
        }

        public String toString() {
            return String.format("[IClust run: score=%f trials=%d idleTrials=%d time(seconds)=%5.2f seed=%d]", mScore, mTrials, mIdleTrials, (double) mTime / 1000, mSeed);
        }
    }

    public class Partition implements IClusteringAlgorithm.IPartition {

        private double mTotalScore = 0;

        protected int[] mClusterIndices = null;

        private int mSeed;

        public class Cluster {

            public int mIndex;
            public Set<Integer> mMembers = new TreeSet<Integer>();
            public int mSize = 0;

            public double mSimilaritySum = 0;
            public int mSimilaritySumN = 0;

            public int mCandidate = -1;
            public Double mCandidateExtraSum = 0.0;
            public Integer mCandidateExtraSumN = 0;

            public Map<Integer, Double> mCandidateCalculatedExtraTermSum;
            public Map<Integer, Integer> mCandidateCalculatedExtraTermSumN;

            public Cluster(int index, List<Integer> members) {

                mIndex = index;
                for (int i : members) {
                    mMembers.add(i);
                    mClusterIndices[i] = mIndex;
                }
                mSize = mMembers.size();
                calcSimilaritySum();
                mTotalScore += scoreContribution();

                // Stores intermediate results of the function calcCandidateExtraTerm()
                mCandidateCalculatedExtraTermSum = new HashMap<Integer, Double>();
                mCandidateCalculatedExtraTermSumN = new HashMap<Integer, Integer>();
            }

            public double checkCandidate(int candidate) {

                mCandidate = candidate;
                calcCandidateExtraTerms();
                if (mSize == 0) {
                    return mSingleElementScore;
                } else {
                    return candidateContribution();
                }
            }

            public void acceptLastCandidate() {
                if (mCandidate == -1) {
                    throw new AdeCoreInternalException("no candidate");
                }

                mTotalScore += candidateContribution();
                mMembers.add(mCandidate);
                mClusterIndices[mCandidate] = mIndex;
                mSize++;
                mSimilaritySum += mCandidateExtraSum;
                mSimilaritySumN += mCandidateExtraSumN;
                mCandidate = -1;
                mCandidateCalculatedExtraTermSum.clear();
                mCandidateCalculatedExtraTermSumN.clear();
            }

            public double demoteToCandidate(int i) {
                mMembers.remove(i);
                mSize--;
                mCandidate = i;
                mClusterIndices[mCandidate] = -1;
                calcCandidateExtraTerms();
                mSimilaritySum -= mCandidateExtraSum;
                mSimilaritySumN -= mCandidateExtraSumN;
                double contr = candidateContribution();
                mTotalScore -= contr;
                mCandidateCalculatedExtraTermSum.clear();
                mCandidateCalculatedExtraTermSumN.clear();
                if (mSize == 0) {
                    return contr + mSingleElementScore;
                } else {
                    return contr;
                }
            }

            public String toString() {
                String res = "  [ Cluster: " + mIndex + " members=" + mMembers + "\n";
                res += "    size=" + mSize + " similaritySum=" + mSimilaritySum + "\n";
                res += "    candidate=" + mCandidate + " candidateExtraTerms=" + mCandidateExtraSum + " ]";
                return res;
            }

            public String toStringSimple() {
                return "  " + mMembers + " score=" + scoreContribution();
            }

            public double calculateSimilarityGainForMember(int element) {
                mCandidate = element;
                calcMemberExtraTerms();
                double contr = memberContribution();
                if (mSize <= 1) {
                    return contr + mSingleElementScore;
                }
                return contr;
            }

            private void calcMemberExtraTerms() {
                mCandidateExtraSum = mCandidateCalculatedExtraTermSum.get(mCandidate);
                mCandidateExtraSumN = mCandidateCalculatedExtraTermSumN.get(mCandidate);
                if (mCandidateExtraSum != null && mCandidateExtraSumN != null) {
                    return;
                }

                mCandidateExtraSum = 0.0;
                mCandidateExtraSumN = 0;
                for (int i : mMembers) {
                    double v = mSimilarity.get(i, mCandidate);
                    if (!Double.isNaN(v) && i != mCandidate) {
                        mCandidateExtraSum += 2 * v;
                        mCandidateExtraSumN += 2;
                    }
                }
                double v = mSimilarity.get(mCandidate, mCandidate);
                if (!Double.isNaN(v)) {
                    mCandidateExtraSum += v;
                    mCandidateExtraSumN++;
                }

                mCandidateCalculatedExtraTermSum.put(mCandidate, mCandidateExtraSum);
                mCandidateCalculatedExtraTermSumN.put(mCandidate, mCandidateExtraSumN);
            }

            private void calcCandidateExtraTerms() {
                mCandidateExtraSum = mCandidateCalculatedExtraTermSum.get(mCandidate);
                mCandidateExtraSumN = mCandidateCalculatedExtraTermSumN.get(mCandidate);
                if (mCandidateExtraSum != null && mCandidateExtraSumN != null) {
                    return;
                }

                mCandidateExtraSum = 0.0;
                mCandidateExtraSumN = 0;

                double temp = 0;
                for (int i : mMembers) {
                    double v = mSimilarity.get(i, mCandidate);
                    if (!Double.isNaN(v)) {
                        temp += v;
                        mCandidateExtraSumN += 2;
                    }
                }
                mCandidateExtraSum += 2 * temp;

                double v = mSimilarity.get(mCandidate, mCandidate);
                if (!Double.isNaN(v)) {
                    mCandidateExtraSum += v;
                    mCandidateExtraSumN++;
                }

                mCandidateCalculatedExtraTermSum.put(mCandidate, mCandidateExtraSum);
                mCandidateCalculatedExtraTermSumN.put(mCandidate, mCandidateExtraSumN);
            }

            private void calcSimilaritySum() {
                mSimilaritySum = 0;
                mSimilaritySumN = 0;
                for (int i : mMembers) {
                    for (int j : mMembers) {
                        double v = mSimilarity.get(i, j);
                        if (!Double.isNaN(v)) {
                            mSimilaritySum += v;
                            mSimilaritySumN++;
                        }
                    }
                }

            }

            public double scoreContribution() {
                if (mSimilaritySumN == 0) {
                    return 0;
                }
                return mSize * mSimilaritySum / mSimilaritySumN;
            }

            /** Returns the average similarity over all pairs in the cluster.
             * NaN similarities are not taken into account
             */
            public double averageSimilarity() {
                if (mSimilaritySumN == 0) {
                    return 0;
                }
                return mSimilaritySum / mSimilaritySumN;
            }

            private double memberContribution() {
                int totalN = mSimilaritySumN - mCandidateExtraSumN;
                double newScore = 0;
                if (totalN > 0) {
                    newScore = (mSize - 1) * (mSimilaritySum - mCandidateExtraSum) / totalN;
                }
                return (scoreContribution() - newScore);
            }

            private double candidateContribution() {
                // // This expression is equivalent to:
                // // mSize/(mSize+1) * [ mCandidateExtra/mSize + mSum/mSize^2]				
                int totalN = mSimilaritySumN + mCandidateExtraSumN;
                double newScore = 0;
                if (totalN > 0) {
                    newScore = (mSize + 1) * (mCandidateExtraSum + mSimilaritySum) / totalN;
                }
                return (newScore - scoreContribution());

            }

            public double refreshAndVerify(double warnEpsilon) {
                for (int m : mMembers) {
                    if (mClusterIndices[m] != mIndex) {
                        throw new AdeCoreIllegalStateException("Mismatching index");
                    }
                }
                double oldSimilaritySum = mSimilaritySum;
                int oldSimilaritySumN = mSimilaritySumN;
                refresh();
                if (oldSimilaritySumN != mSimilaritySumN) {
                    throw new AdeCoreIllegalStateException("sumN changed");
                }
                double diff = Math.abs(oldSimilaritySum - mSimilaritySum);
                if (diff > warnEpsilon) {
                    myPrintln(1, "Warning: cluster " + mMembers + " similiarySum drifted from " + oldSimilaritySum + " to " + mSimilaritySum);
                }

                return diff;
            }

            public void refresh() {
                calcSimilaritySum();
                mTotalScore += scoreContribution();
                mCandidateCalculatedExtraTermSum.clear();
                mCandidateCalculatedExtraTermSumN.clear();
            }

        }

        private ArrayList<Cluster> mClusters = new ArrayList<Cluster>();

        public Partition() {
            ArrayList<Integer> indices = new ArrayList<Integer>();
            for (int i = 0; i < mNumElements; ++i) {
                indices.add(i);
            }
            Collections.shuffle(indices, mRandom);

            mClusterIndices = new int[mNumElements];

            ArrayList<Integer> members = new ArrayList<Integer>();
            members.ensureCapacity(mNumElements / mClusterNum + 1);
            for (int i = 0; i < mClusterNum; ++i) {
                members.clear();
                for (int j = i; j < mNumElements; j += mClusterNum) {
                    members.add(indices.get(j));
                }
                mClusters.add(new Cluster(i, members));
            }

        }

        public Partition(int[] initialPartition) {
            HashMap<Integer, Integer> clusterIdx = new HashMap<Integer, Integer>();
            HashMap<Integer, ArrayList<Integer>> clusters = new HashMap<Integer, ArrayList<Integer>>();
            mClusterIndices = initialPartition.clone();
            int k = 0;
            for (int i = 0; i < initialPartition.length; ++i) {
                int currClust = initialPartition[i];
                if (!clusterIdx.containsKey(currClust)) {
                    clusterIdx.put(currClust, k);
                    clusters.put(k, new ArrayList<Integer>());
                    k++;
                }
                clusters.get(clusterIdx.get(currClust)).add(i);
            }
            for (int i = 0; i < k; i++) {
                mClusters.add(new Cluster(i, clusters.get(i)));
            }
        }

        public String toString() {
            StringBuilder bldres = new StringBuilder("[ Partition: totalScore=" + mTotalScore + "\n");
            for (Cluster cluster : mClusters) {
                bldres.append(cluster.toString() + "\n");
            }
            bldres.append("]");
            return bldres.toString();
        }

        public String toStringSimple() {
            StringBuilder bldres = new StringBuilder("[ Partition: totalScore=" + mTotalScore + "\n");
            for (Cluster cluster : mClusters) {
                bldres.append(cluster.toStringSimple() + "\n");
            }
            bldres.append("]");
            return bldres.toString();
        }

        public void refreshAndVerify(double warnEpsilon) {
            for (int i = 0; i < mNumElements; ++i) {
                int clusterIndex = mClusterIndices[i];
                if (!(clusterIndex >= 0 && clusterIndex < mClusterNum)) {
                    throw new AdeCoreIllegalStateException("Invalid cluster index");
                }
                if (!mClusters.get(clusterIndex).mMembers.contains(i)) {
                    throw new AdeCoreIllegalStateException("Inconsistency in cluster bookkeeping");
                }
            }

            double oldTotalScore = mTotalScore;
            double diff = 0;
            mTotalScore = 0;
            for (Cluster cluster : mClusters) {
                diff = Math.max(diff, cluster.refreshAndVerify(warnEpsilon));
            }
            double myDiff = Math.abs(mTotalScore - oldTotalScore);
            if (myDiff > warnEpsilon) {
                myPrintln(1, "Warning: total score drifted by " + myDiff);
            }
            diff = Math.max(myDiff, diff);
            myPrintln(1, "Refresh (diff=" + diff + ")");
        }

        public void refreshWithNewSimiliary(IDoubleMatrix sim) {
            mSimilarity = sim;
            mTotalScore = 0;
            for (Cluster cluster : mClusters) {
                cluster.refresh();
            }
        }

        public String toStringSummary() {
            StringBuilder bldres = new StringBuilder(String.format("%d %f", mSeed, getScore()));
            for (int i = 0; i < mClusterIndices.length; ++i) {
                bldres.append(" " + mClusterIndices[i]);
            }
            return bldres.toString();
        }

        public List<Cluster> getClusters() {
            return mClusters;
        }

        public int[] getClusterIndices() {
            return mClusterIndices;
        }

        public Double getScore() {
            return mTotalScore / mNumElements + +mAlpha * computeEntropy();
        }

        private double computeEntropy() {
            double entropy = 0;

            for (int i = 0; i < mClusterNum; ++i) {
                Partition.Cluster cluster = mClusters.get(i);
                if (cluster.mSize > 0) {
                    double prob = (double) cluster.mSize / mNumElements;
                    entropy -= (prob) * log2(prob);
                }
            }

            return entropy;
        }

        @Override
        public int getClusterOfElement(int index) {
            return mClusterIndices[index];
        }

        @Override
        public int getNumElements() {
            return mClusterIndices.length;
        }

        @Override
        public int getNumClusters() {
            return mClusters.size();
        }

        public int getNumPopulatedClustrs() {
            int count = 0;
            for (Cluster cluster : mClusters) {
                if (cluster.mSize != 0) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public int getNumOfElementsInCluster(int clusterIndex) {
            if (clusterIndex < 0 || clusterIndex >= mClusters.size()) {
                return -1;
            }
            return mClusters.get(clusterIndex).mMembers.size();

        }

        @Override
        public Set<Integer> getClusterElements(int clusterIndex) {
            if (clusterIndex < 0 || clusterIndex >= mClusters.size()) {
                return new TreeSet<Integer>();
            }
            return mClusters.get(clusterIndex).mMembers;

        }

        @Override
        public double getClusterScore(int clusterIndex) {
            if (clusterIndex < 0 || clusterIndex >= mClusters.size()) {
                return Double.NaN;
            }
            return mClusters.get(clusterIndex).scoreContribution();
        }

    }

    public void setMaxIdleTrialNum(int maxIdleTrialNum) {
        mMaxIdleTrialNum = maxIdleTrialNum;
    }

    public void setMinClusterSize(int val) {
        if (val < 1) {
            throw new AdeCoreIllegalArgumentException("Cannot set min cluster size to value less than 1");
        }
        mMinClusterSize = val;
    }

    @Override
    public void setClusterNum(int clusterNum) {
        mClusterNum = clusterNum;
    }

    public void setAlpha(double alpha) {
        mAlpha = alpha;
    }

    public void setSeed(int seed) {
        mGlobalSeed = seed;
    }

    public void setVerbosity(PrintStream out, int level) {
        mVerbosityOut = out;
        mVerbosity = level;
        if (mVerbosity != 0 && mVerbosityOut == null) {
            throw new AdeCoreIllegalArgumentException("For positive verbosity a stream must be supplied");
        }

    }

    public void storeAllScores(List<Double> scoreArchive) {
        mScoreArchive = scoreArchive;
    }

    /** Performs IClust run on the given similarity matrix.
     * 
     * @param similarity Input similarity matrix to cluster	
     * @return The partition resulting from IClust run. If count>1, the best partition is returned.
     */
    public Partition run(IDoubleMatrix similarity) {
        DoubleMatrixOps.assertDiagonallyDominant(similarity);
        DoubleMatrixOps.assertSymmetric(similarity, 1e-16);
        if (similarity.getRowNum() <= 2) {
            throw new AdeCoreIllegalArgumentException("Minimal matrix size 3x3");
        }
        mSimilarity = similarity;
        if (mRunsSummary != null) {
            mRunsSummary.clear();
        }

        myPrintln(1, "Starting " + mRunNum + " runs");

        Partition bestPartition = null;
        mConverged = false;

        for (int i = 0; i < mRunNum; ++i) {
            Partition partition;

            long startTime = System.currentTimeMillis();
            partition = execute();
            long stopTime = System.currentTimeMillis();
            long duration = stopTime - startTime;

            if (mRunsSummary != null) {
                mRunsSummary.add(new IClustRunSummary(partition.mTotalScore, mTrials, duration,
                        partition.mSeed, mIdleTrials));
            }
            if (bestPartition == null || partition.mTotalScore > bestPartition.mTotalScore) {
                mConverged = (mTrials < mMaxTrialNum);
                bestPartition = partition;
            }
            if (partition.getScore() >= 1) {
                break;
            }
        }
        myPrintln(1, "Finished all runs with score=" + 
                (bestPartition != null ? bestPartition.mTotalScore : 0));
        return bestPartition;
    }

    /** Returns meta data summary of last call to run().
     * The number of elements in the returned array equals the number of runs specified in run().
     * @return meta data summary of last call to run()
     */
    @Override
    public List<IClustRunSummary> getRunsSummary() {
        return mRunsSummary;
    }

    public void collectRunsSummary() {
        mRunsSummary = new ArrayList<IClustExp.IClustRunSummary>();
    }

    /** Checks whether last call to run() gave a converged result.
     * Converged means that the max number of trials was not reached,
     * and that the algorithm stopped due to max number of idle trials.
     * @return true if the last call to run() converged.
     */
    public boolean isConverged() {
        return mConverged;
    }

    private Partition execute() {

        mRandom = new Random(mGlobalSeed);
        mNumElements = mSimilarity.getRowNum();

        if (mClusterNum < 0) {
            mClusterNum = (int) Math.round(Math.sqrt(mNumElements));
        }
        if (mClusterNum < 2) {
            throw new AdeCoreInvalidInitialStateException("Must have at least two clusters");
        }
        if (mClusterNum >= mNumElements) {
            throw new AdeCoreInvalidInitialStateException("Cluster number must be smaller than elements number");
        }

        Partition partition = null;
        if (mInitialPartition != null) {
            partition = new Partition(mInitialPartition);
        } else {
            partition = new Partition();
        }

        myPrintln(1, "Starting run with seed=" + mGlobalSeed);
        mTrials = 0;
        mIdleTrials = 0;

        mElementsPermutation = new int[mNumElements];
        for (int i = 0; i < mNumElements; i++) {
            mElementsPermutation[i] = i;
        }
        mElementsPermutationIndex = 0;

        partition.mSeed = mGlobalSeed;
        mGlobalSeed = mGlobalSeed + 1;
        if (mScoreArchive != null) {
            mScoreArchive.add(partition.mTotalScore);
        }
        mCandidateClusters = new int[mClusterNum];
        while (mTrials < mMaxTrialNum && mIdleTrials < mMaxIdleTrialNum && mElementsPermutationIndex < mNumElements) {
            trial(partition);
            if (mScoreArchive != null) {
                mScoreArchive.add(partition.getScore());
            }
        }

        myPrintln(1, "Finished run with score=" + partition.mTotalScore);
        return partition;
    }

    private void trial(Partition partition) {

        int element = nextIntFromPermutation();

        int clusterIndex = partition.mClusterIndices[element];

        if (clusterIndex < 0) {
            throw new AdeCoreInternalException("Invalid cluster index");
        }

        Partition.Cluster srcCluster = partition.mClusters.get(clusterIndex);

        if (!isEnableEmptyClusters() && srcCluster.mSize <= mMinClusterSize) {
            ++mTrials;
            return;
        }

        double bestGain = srcCluster.calculateSimilarityGainForMember(element) + mAlpha * computeEntropyDiffForMember(srcCluster);
        boolean srcGain = true;
        mCandidateClustersNum = 1;
        mCandidateClusters[0] = clusterIndex;

        for (int i = 0; i < mClusterNum; ++i) {
            if (i == clusterIndex) {
                continue;
            }
            Partition.Cluster checkedCluster = partition.mClusters.get(i);
            double gain = checkedCluster.checkCandidate(element) + mAlpha * computeEntropyDiff(checkedCluster);

            if (gain > bestGain) {
                mCandidateClustersNum = 1;
                mCandidateClusters[0] = i;
                bestGain = gain;
                srcGain = false;
            } else if (bestGain == gain && !srcGain) {
                mCandidateClusters[mCandidateClustersNum++] = i;
            }
        }
        int chosenIndex = 0;
        if (mCandidateClustersNum > 1) {
            chosenIndex = mRandom.nextInt(mCandidateClustersNum);

        }
        int chosenClusterIndex = mCandidateClusters[chosenIndex];
        if (srcGain) {
            ++mIdleTrials;
            ++mElementsPermutationIndex;
        } else {
            srcCluster.demoteToCandidate(element);
            partition.mClusters.get(chosenClusterIndex).acceptLastCandidate();
            mIdleTrials = 0;
            mElementsPermutationIndex = 0;
        }
        ++mTrials;
    }

    private double computeEntropyDiff(Partition.Cluster cluster) {
        if (cluster.mSize == 0) {
            return 0;
        }
        double prob = (double) cluster.mSize / mNumElements;
        double newProb = (double) (cluster.mSize + 1) / mNumElements;
        return (prob) * log2(prob) - (newProb) * log2(newProb);
    }

    private double computeEntropyDiffForMember(Partition.Cluster cluster) {
        if (cluster.mSize <= 1) {
            return 0;
        }
        double prob = (double) (cluster.mSize - 1) / mNumElements;
        double newProb = (double) (cluster.mSize) / mNumElements;
        return (prob) * log2(prob) - (newProb) * log2(newProb);
    }

    private double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    private void myPrintln(int level, String msg) {
        if (level <= mVerbosity) {
            mVerbosityOut.println(msg);
        }
    }

    private int nextIntFromPermutation() {
        int nextElement;

        int index = mRandom.nextInt(mNumElements - mElementsPermutationIndex) + mElementsPermutationIndex;
        nextElement = mElementsPermutation[index];
        mElementsPermutation[index] = mElementsPermutation[mElementsPermutationIndex];
        mElementsPermutation[mElementsPermutationIndex] = nextElement;

        return nextElement;
    }

    @Override
    public void setNumOfMaxIterationsPerRun(int maxIterations) {
        mMaxTrialNum = maxIterations;
    }

    @Override
    public void setRunNum(int runNum) {
        mRunNum = runNum;
    }

    @Override
    public void setInitialPartition(int[] partition) {
        mInitialPartition = partition;
    }

    public double getSingleElementScore() {
        return mSingleElementScore;
    }

    public void setSingleElementScore(double mSingleElementScore) {
        this.mSingleElementScore = mSingleElementScore;
    }

    public boolean isEnableEmptyClusters() {
        return mEnableEmptyClusters;
    }

    public void setEnableEmptyClusters(boolean mEnableEmptyClusters) {
        this.mEnableEmptyClusters = mEnableEmptyClusters;
    }
}
