/*
 
    Copyright IBM Corp. 2016
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import org.openmainframe.ade.core.exceptions.AdeCoreIllegalArgumentException;
import org.openmainframe.ade.core.exceptions.AdeCoreUnsupportedOperationException;
import org.openmainframe.ade.core.matrix.IBinaryDoubleVectorFunc;
import org.openmainframe.ade.core.matrix.DoubleMatrix;
import org.openmainframe.ade.core.matrix.IDoubleMatrix;
import org.openmainframe.ade.core.matrix.DoubleVector;
import org.openmainframe.ade.core.matrix.IDoubleVector;
import org.openmainframe.ade.core.matrix.DoubleVectorOps;
import org.openmainframe.ade.core.matrix.IntVector;
import org.openmainframe.ade.core.matrix.IIntVector;

/** A KMeans clustering algorithm.
 * 
 * Uses by default L2 squared as the distance function, but another function can be plugged in.
 * 
 *
 */
public class KMeans implements IClusteringAlgorithm, Serializable {
    protected static final long serialVersionUID = 1L;

    // Holds the input of the problem.
    // Each row of the matrix is a point needed to be clustered.
    // That is, each column corresponds to a dimension of the problem.
    protected IDoubleMatrix mProblemInput = null;

    // Number of runs (number of different initial random states).
    protected int mNumRuns = 10;
    protected int mMaxIterations = Integer.MAX_VALUE;

    // Number of clusters.
    // If not provided in constructor, will be set to sqrt(numOfRows(m_probleInput))
    protected int mNumClusters = -1;

    // Holds the partition from the last run.
    protected KMeansPartition mPartition;

    // This is a Global seed.
    protected long mSeed = 101;
    protected Random mGlobalRandom;
    protected Random mRandom = null;
    protected IBinaryDoubleVectorFunc mDistanceFunc = new DoubleVectorOps.BinaryDoubleVectorL2Squared();

    // Summary list - for performance evaluation.
    protected ArrayList<RunSummary> mRunSummary = null;

    public KMeans() {
        mGlobalRandom = new Random();
    }

    // Change the number of clusters.
    @Override
    public void setClusterNum(int numClusters) {
        mNumClusters = numClusters;
    }

    // Change the number of maximal iterations per run.
    @Override
    public void setNumOfMaxIterationsPerRun(int maxIterations) {
        mMaxIterations = maxIterations;
    }

    // Change the global seed
    @Override
    public void setSeed(int seed) {
        mSeed = seed;
        mGlobalRandom.setSeed(mSeed);
    }

    public class KMeansPartition implements IClusteringAlgorithm.IPartition, Serializable {
        protected static final long serialVersionUID = 1L;

        protected double mTotalScore = Double.MAX_VALUE;
        protected int[] mPreviousClusterIndices = null;
        protected int[] mClusterIndices = null;
        protected IDoubleMatrix mCentorids;
        protected IIntVector mNumOfElementsInCluster;

        public KMeansPartition() {
            mClusterIndices = new int[mProblemInput.getRowNum()];
            mNumOfElementsInCluster = new IntVector(mNumClusters);
            mPreviousClusterIndices = new int[mProblemInput.getRowNum()];
            mCentorids = new DoubleMatrix(mNumClusters, mProblemInput.getColNum());
            mNumOfElementsInCluster.setAll(0);
            java.util.Arrays.fill(mClusterIndices, -1);
            initialRandomCentroids();
        }

        @Override
        public int getClusterOfElement(int index) {
            if (mClusterIndices == null || index < 0 || index >= mClusterIndices.length) {
                return -1;
            }
            return mClusterIndices[index];
        }

        public void initialRandomCentroids() {
            final ArrayList<Integer> pickingCentroids = new ArrayList<>();
            for (int i = 0; i < mProblemInput.getRowNum(); i++) {
                pickingCentroids.add(i);
            }

            Collections.shuffle(pickingCentroids, mRandom);
            for (int i = 0; i < mNumClusters; i++) {
                for (int j = 0; j < mProblemInput.getColNum(); j++) {
                    mCentorids.set(i, j, mProblemInput.get(pickingCentroids.get(i), j));
                }
            }
        }

        // Returns true if clusters didn't change. That is, the algorithm has converged!
        // If the clusters didn't change, then the centroids are the same and there is no
        // point in running another iterations.
        public Boolean iterateOnce() {
            mPreviousClusterIndices = mClusterIndices;
            mClusterIndices = new int[mProblemInput.getRowNum()];
            assignClusters();
            calculateCentroids();
            return java.util.Arrays.equals(mClusterIndices, mPreviousClusterIndices);
        }

        protected void assignClusters() {
            double minDistance = Double.MAX_VALUE;
            int minCluster = -1;
            double distance;
            double totalCost = 0;

            for (int i = 0; i < mClusterIndices.length; i++) {
                minDistance = Double.MAX_VALUE;
                minCluster = -1;
                for (int j = 0; j < mCentorids.getRowNum(); j++) {
                    distance = mDistanceFunc.getFuncValue(mCentorids.getRow(j), mProblemInput.getRow(i));
                    if (distance < minDistance) {
                        minDistance = distance;
                        minCluster = j;
                    }
                }
                mClusterIndices[i] = minCluster;
                totalCost += minDistance;
            }
            mTotalScore = totalCost;
        }

        public void calculateCentroids() {
            mCentorids.setAll(0);
            mNumOfElementsInCluster.setAll(0);

            // Adding all points that belong to the same cluster together
            for (int i = 0; i < mClusterIndices.length; i++) {
                addVectorToExisting(mCentorids.getRow(mClusterIndices[i]), mProblemInput.getRow(i));
                final int temp = mNumOfElementsInCluster.get(mClusterIndices[i]);
                mNumOfElementsInCluster.set(mClusterIndices[i], temp + 1);
            }

            // Dividing the sum of cluster point by the number of elements in the cluster
            // to get the centroids
            for (int i = 0; i < mNumClusters; i++) {
                divideVectorByInt(mCentorids.getRow(i), mNumOfElementsInCluster.get(i));
            }
        }

        public void printPartitionToSysout() {
            System.out.println("Partition:");
            for (int i = 0; i < mClusterIndices.length; i++) {
                System.out.print(mClusterIndices[i]);
                if (i != mClusterIndices.length - 1) {
                    System.out.print(",");
                }
            }
            System.out.println("");

            System.out.println("Centroids:");
            System.out.println(mCentorids.toString());
        }

        @Override
        public Double getScore() {
            return mTotalScore;
        }

        public void addVectorToExisting(IDoubleVector x, IDoubleVector y) {
            if (x.getLength() != y.getLength()) {
                throw new AdeCoreIllegalArgumentException("Distance function: vectors are not of the same length");
            }

            double temp;
            for (int i = 0; i < x.getLength(); i++) {
                temp = x.get(i);
                x.set(i, temp + y.get(i));
            }
        }

        public void divideVectorByInt(IDoubleVector x, int y) {
            if (y == 0) {
                return;
            }

            for (int i = 0; i < x.getLength(); i++) {
                final double temp = x.get(i);
                x.set(i, temp / (double) y);
            }
        }

        @Override
        public int getNumElements() {
            return mClusterIndices.length;
        }

        @Override
        public int getNumClusters() {
            return mNumOfElementsInCluster.getLength();
        }

        @Override
        public int getNumOfElementsInCluster(int clusterIndex) {
            if (clusterIndex < 0 || clusterIndex >= mNumClusters) {
                return -1;
            }
            return mNumOfElementsInCluster.get(clusterIndex);
        }

        public IDoubleVector getCentroidOfCluster(int clusterIndex) {
            if (clusterIndex < 0 || clusterIndex >= mNumClusters) {
                return new DoubleVector(0); 
                
            }
            return mCentorids.getRow(clusterIndex);
        }

        @Override
        public TreeSet<Integer> getClusterElements(int clusterNumber) {
            if (clusterNumber < 0 || clusterNumber >= mNumClusters) {
                return null;
            }
            final TreeSet<Integer> clusterElements = new TreeSet<>();
            for (int i = 0; i < mClusterIndices.length; i++) {
                if (mClusterIndices[i] == clusterNumber) {
                    clusterElements.add(i);
                }
            }

            return clusterElements;
        }

        @Override
        public int[] getClusterIndices() {
            return mClusterIndices;
        }

        @Override
        public double getClusterScore(int clusterIndex) {
            if (clusterIndex < 0 || clusterIndex >= mNumClusters) {
                return Double.NaN;
            }
            double distance = 0;
            int count = 0;
            for (int i = 0; i < mClusterIndices.length; i++) {
                if (mClusterIndices[i] == clusterIndex) {
                    distance += mDistanceFunc.getFuncValue(mCentorids.getRow(clusterIndex), mProblemInput.getRow(i));
                    count++;
                }
            }
            return distance / count;
        }

    }

    /** Run on given matrix.
     * Each row represents a point to be clustered
     */
    public KMeansPartition run(IDoubleMatrix input) {
        mProblemInput = input;
        if (mNumClusters == -1) {
            mNumClusters = (int) Math.round(Math.sqrt(input.getRowNum()));
        }

        mRunSummary = new ArrayList<>();

        KMeansPartition minPartition = null;
        double minScore = Double.MAX_VALUE;

        for (int i = 0; i < mNumRuns; i++) {
            final long localSeed = mGlobalRandom.nextLong();
            mRandom = new Random(localSeed);
            mPartition = new KMeansPartition();

            final long startTime = System.currentTimeMillis();
            // The below loop runs kMeans once till convergence
            int j = 0;
            while (!mPartition.iterateOnce() && j < mMaxIterations) {
                j++;
            }
            final long stopTime = System.currentTimeMillis();
            final long duration = stopTime - startTime;

            mRunSummary.add(new RunSummary(costFunction(), j, duration, localSeed));
            // We have a new winner!
            if (minScore > costFunction()) {
                minPartition = mPartition;
                minScore = costFunction();
            }
        }
        return minPartition;
    }

    public double costFunction() {
        return mPartition.mTotalScore;
    }

    @Override
    public void setRunNum(int runNum) {
        mNumRuns = runNum;
    }

    @Override
    public void setInitialPartition(int[] partition) {
        throw new AdeCoreUnsupportedOperationException("Initial partition currently not supported for k-means");
    }

    public void setDistanceFunction(IBinaryDoubleVectorFunc dist) {
        mDistanceFunc = dist;
    }

    @Override
    public void collectRunsSummary() {
        // runs summary are collected always
    }

    @Override
    public List<? extends RunSummary> getRunsSummary() {
        return mRunSummary;
    }

}
