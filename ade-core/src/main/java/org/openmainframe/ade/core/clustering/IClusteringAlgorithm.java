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
import java.util.List;
import java.util.TreeSet;

/**
 * 
 * Interface for a clustering algorithm.
 * 
 *  Note that it doesn't have a run method, since specific child classes my either run on points or on distance matrix. 
 * 
 *
 */

public interface IClusteringAlgorithm {

    /** A class that holds a partition, the return value of the clustering algorithm */
    public interface IPartition {
        /** Returns the score of the partition, according to the algorithm target function */
        Double getScore();

        /** Returns the cluster index of the i'th element */
        int getClusterOfElement(int i);

        /** Returns the number of elements in the partition. The elements are referred by index 0...<num-1> */
        int getNumElements();

        int getNumClusters();

        /** Returns the number of elements in a cluster */
        int getNumOfElementsInCluster(int clusterIndex);

        /** Returns a collection of the elements in a cluster */
        TreeSet<Integer> getClusterElements(int clusterNumber);

        /** Returns an array of the cluster's index of each element */
        int[] getClusterIndices();

        /** Returns the score in a cluster */
        double getClusterScore(int clusterIndex);
    }

    /** A class (simple container) that holds running statistics.
     * If there are runs from multiple starting points, there'll be one object for each */
    public class RunSummary implements Serializable {
        private static final long serialVersionUID = 1L;

        /** The score this run reached */
        public double mScore;
        /** The number of iterations used by this run */
        public int mIterations;
        /** The number of milliseconds this run took */
        public long mTime;
        /** A seed for recreating the same run */
        public long mSeed;

        public RunSummary(double score, int iterations, long time, long seed) {
            mScore = score;
            mIterations = iterations;
            mTime = time;
            mSeed = seed;
        }

        public String toString() {
            return String.format("[Clustering run: score=%f, time(in seconds)=%5.2f, iterations=%d, seed=%d]",
                    mScore, (double) mTime / 1000, mIterations, mSeed);
        }

        public String csvString() {
            return String.format("%f, %d, %d, %d",
                    mScore, mTime, mIterations, mSeed);
        }
    }

    /** Sets the desired number of clusters. A value of -1 (the default) means automatically calculated value */
    void setClusterNum(int numClusters);

    /** For iterative algorithms, sets the maximal number of iterations per run. */
    void setNumOfMaxIterationsPerRun(int maxIterations);

    /** Change the global seed */
    void setSeed(int seed);

    /** Set number of runs (initial random starting points). */
    void setRunNum(int runNum);

    /** Set an initial partition.*/
    void setInitialPartition(int[] partition);

    /** Turns on run summary collection */
    void collectRunsSummary();

    /**  Returns a list of RunSummary objects that describes each run */
    List<? extends RunSummary> getRunsSummary();
}
