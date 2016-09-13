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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.openmainframe.ade.core.MapCounter;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.utils.FileUtils;
import org.openmainframe.ade.scores.AbstractClusteringScorer;
import org.openmainframe.ade.scores.AbstractClusteringScorer.ClusterData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusteringPartition implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory
			.getLogger(ClusteringPartition.class);
	/**
	 * A map for storing an initial clustering partition.
	 */
	private HashMap<String, ArrayList<Integer>> m_initialClusters = null;

	private HashMap<String, Collection<Integer>> m_finalClusters = null;
	private transient Map<Integer, String> m_finalClusterNames = null;

	private int[] m_initialPartition = null;
	private String m_initialPartitionFileName = null;
	/**
	 * A threshold for similarity between two clusters. This threshold indicates
	 * the part of elements from the first cluster that should be in in both
	 * clusters.
	 */
	private double m_clusterSimilarityLevel;

	private int m_numClusters;

	private ArrayList<String> m_clusteringDetails = null;

	/**
	 * Construct a new ClusteringPartition.
	 * 
	 * @param numClusters
	 *            the number of clusters
	 * @param clusterSimilarityLevel
	 *            the threshold for similarity between two clusters. This
	 *            threshold indicates the part of elements from the first
	 *            cluster that should be in in both clusters.
	 */
	public ClusteringPartition(int numClusters, double clusterSimilarityLevel)
			throws AdeException {
		// init:
		m_clusterSimilarityLevel = clusterSimilarityLevel;
		m_numClusters = numClusters;
		m_clusteringDetails = new ArrayList<String>();
	}

	/**
	 * Notice that we are building a new set of indexes based on the order of
	 * the keys in the TreeMap.
	 * 
	 * @param messageCounter
	 *            a {@link MapCounter} containing the counts of message IDs
	 * @param minNumClusters
	 *            the minimum number of clusters
	 * @param numUniqueMsgIds
	 *            the number of unique message IDs
	 */
	public final void createInitialPartitionByOccurrences(
			MapCounter<Integer> messageCounter, int minNumClusters,
			int numUniqueMsgIds) {

		m_initialPartitionFileName = null;
		final Set<Integer> uniqNumOccurances = new HashSet<Integer>();
		final Collection<Integer> integers = messageCounter.getMap().values();
		uniqNumOccurances.addAll(integers);
		if (uniqNumOccurances.size() >= minNumClusters) {
			m_numClusters = uniqNumOccurances.size();
			m_initialPartition = new int[numUniqueMsgIds];

			int i = 0;
			for (int numOccurrences : integers) {
				m_initialPartition[i] = numOccurrences;
				i++;
			}
		} else {
			m_numClusters = minNumClusters;
		}
	}

	/**
	 * Read and use the contents of a file to create an initial partition.
	 * 
	 * @param file
	 *            the file to read. Must be in UTF-8 format.
	 * @param mutualInformationMatrixHolder
	 *            a {@link IMutualInformationHolder} that contains the message
	 *            IDs
	 */
	public final void createInitialPartitionFromFile(File file,
			IMutualInformationHolder mutualInformationMatrixHolder)
			throws AdeInternalException {
		m_initialPartitionFileName = file.getPath();
		final int numUniqueMsgIds = mutualInformationMatrixHolder
				.getMutualInformationMatrix().getRowNum();
		int[] initialPartition = new int[numUniqueMsgIds];
		final int defaultVal = -1;
		Arrays.fill(initialPartition, defaultVal);
		int clusterIndex = -1;
		int numClusters = m_numClusters;
		m_initialClusters = new HashMap<String, ArrayList<Integer>>();
		FileInputStream fis = null;
		BufferedReader br = null;
		try {
			fis = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(fis,
					StandardCharsets.UTF_8));
			String nextLine = null;

			while ((nextLine = br.readLine()) != null) {
				clusterIndex++;
				final String[] path = nextLine.split("\t");
				final ArrayList<Integer> members = new ArrayList<Integer>();
				for (int i = 1; i < path.length; i++) {
					final int id = Integer.parseInt(path[i]);
					final int index = mutualInformationMatrixHolder
							.getIndexOfMsgId(id);
					if (index != -1) {
						initialPartition[index] = clusterIndex;
						members.add(id);
					}
				}
				if (members.isEmpty()) {
					m_initialClusters.put(path[0], members);
				}
			}

		} catch (FileNotFoundException e) {
			logger.error("The file " + m_initialPartitionFileName
					+ " is not found", e);
			initialPartition = null;
			return;
		} catch (IOException e) {
			logger.error(
					"Error reading the file " + m_initialPartitionFileName, e);
			initialPartition = null;
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e1) {
				logger.error("Error closing the file "
						+ m_initialPartitionFileName, e1);
			}
			return;
		} finally {
			try {
				if (br != null) {
					br.close();
				}
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				logger.error(
						"Error closing the file " + m_initialPartitionFileName, e);
			}
		}
		if (clusterIndex >= numClusters) {
			numClusters = clusterIndex + 2;
			logger.info("Number of cluster set to be " + numClusters);
		}
		final ArrayList<Integer> indices = new ArrayList<Integer>();
		int numOfUnclusteredElements = 0;
		for (int i = 0; i < numUniqueMsgIds; ++i) {
			if (initialPartition[i] == defaultVal) {
				indices.add(i);
				numOfUnclusteredElements++;
			}
		}
		Collections.shuffle(indices, new Random());

		final int numOfEmptyClusters = numClusters - clusterIndex - 1;
		final int threshould = Math.min(numOfEmptyClusters,
				numOfUnclusteredElements);
		for (int i = 0; i < threshould; ++i) {
			clusterIndex++;
			for (int j = i; j < numOfUnclusteredElements; j += numOfEmptyClusters) {
				initialPartition[indices.get(j)] = clusterIndex;
			}
		}
		m_numClusters = clusterIndex + 1;
		logger.info("Number of clusters to initial Iclust is " + m_numClusters);
		m_initialPartition = initialPartition;
	}

	/**
	 * Prints out the cluster information.
	 * 
	 * @param out
	 *            the {@link PrintWriter} to which the information is sent
	 * @param clusterData
	 *            the cluster data to print out
	 * @param matIndexToMsgInternalId
	 *            an array that maps matrix indices to message ID.
	 */
	public final void printClusters(PrintWriter out,
			List<ClusterData> clusterData, int[] matIndexToMsgInternalId)
			throws AdeException {

		if (m_finalClusters == null) {
			updateClusters(clusterData, matIndexToMsgInternalId);
		}

		for (Map.Entry<String, Collection<Integer>> entry : m_finalClusters
				.entrySet()) {
			final String n = entry.getKey();
			final Collection<Integer> c = entry.getValue();
			out.printf("%s\t", n);
			for (int m : c) {
				out.printf("%d\t", matIndexToMsgInternalId[m]);
			}
			out.println();
		}
	}

	/**
	 * Update the clusters using the passed in cluster data.
	 * 
	 * @param clusterData
	 *            the cluster data to use for updating
	 * @param matIndexToMsgInternalId
	 *            an array that maps matrix indices to message ID.
	 */
	public final void updateClusters(List<ClusterData> clusterData,
			int[] matIndexToMsgInternalId) {

		final Map<String, Collection<Integer>> names = new HashMap<String, Collection<Integer>>();
		int count = 0;
		String cName = null;
		m_finalClusterNames = new HashMap<Integer, String>();

		for (int i = 0; i < clusterData.size(); ++i) {
			final ClusterData cd = clusterData.get(i);
			final Collection<Integer> c = cd.m_cluster;
			if (cd.m_clusterUsage == AbstractClusteringScorer.ClusterUsage.USED) {
				// There is no initial partition
				if (m_initialClusters == null) {
					cName = "CLUSTER_" + count++;
					names.put(cName, c);
				} else {
					final ClusterMeasures cM = findClusterName(c,
							matIndexToMsgInternalId);
					if (cM.m_name == null) {
						cM.m_name = findNewName(names);
						names.put(cM.m_name, c);
						updateClusteringDetails(null, null, cM, c,
								matIndexToMsgInternalId);
					} else {
						if (names.containsKey(cM.m_name)) {
							cM.m_name = solveDuplicatedNames(cName, names);
						}
						names.put(cM.m_name, c);
						updateClusteringDetails(cM.m_name,
								m_initialClusters.get(cM.m_name), cM, c,
								matIndexToMsgInternalId);
					}
				}
				final ClusterMeasures cM = findClusterName(c,
						matIndexToMsgInternalId);
				m_finalClusterNames.put(cd.m_id, cM.m_name);
			}
		}
		m_finalClusters = (HashMap<String, Collection<Integer>>) names;
	}

	private ClusterMeasures findClusterName(Collection<Integer> c,
			int[] matIndexToMsgInternalId) {

		final ClusterMeasures cM = new ClusterMeasures();

		// There is no initial partition
		if (m_initialClusters == null) {
			return cM;
		}

		for (Map.Entry<String, ArrayList<Integer>> entry : m_initialClusters
				.entrySet()) {
			final String n = entry.getKey();
			final ArrayList<Integer> members = entry.getValue();
			int intersectionCount = 0;
			int unionCount = members.size();

			for (int m : c) {
				if (members.contains(matIndexToMsgInternalId[m])) {
					intersectionCount++;
				} else {
					unionCount++;
				}
			}
			if ((double) intersectionCount / unionCount > m_clusterSimilarityLevel) {
				cM.m_name = n;
				cM.m_intersection = intersectionCount;
				cM.m_measure = (double) intersectionCount / unionCount;
				return cM;
			}
		}
		return cM;
	}

	private String findNewName(Map<String, Collection<Integer>> names) {
		int index = 0;
		while (true) {
			final String n = "CLUSTER_" + index;
			if (!names.containsKey(n) && !m_initialClusters.containsKey(n)) {
				return n;
			}
			index++;
		}
	}

	private static String solveDuplicatedNames(String cName,
			Map<String, Collection<Integer>> names) {
		int index = 1;
		while (true) {
			final String n = cName + "_" + index;
			if (!names.containsKey(n)) {
				return n;
			}
			index++;
		}
	}

	private void updateClusteringDetails(String previousName,
			Collection<Integer> previousCluster,
			ClusterMeasures currentDetails, Collection<Integer> currentCluster,
			int[] matIndexToMsgInternalId) {

		// Start with an initial capacity of 100 so as to not have resize if
		// ever.
		final StringBuilder newString = new StringBuilder(100);
		if (previousName == null) {
			newString.append("Create new cluster: ");
		} else {
			newString.append(previousName);
			newString.append(" [");
			for (int m : previousCluster) {
				newString.append(m);
				newString.append(" ");
			}
			newString.append("] -> ");
		}
		newString.append(currentDetails.m_name + " [");
		for (int m : currentCluster) {
			newString.append(matIndexToMsgInternalId[m]);
			newString.append(" ");
		}
		if (previousName == null) {
			newString.append("]\n");
		} else {
			newString.append("]");
			newString.append(" measure: ");
			newString.append(currentDetails.m_measure);
			newString.append(" intersection: ");
			newString.append(currentDetails.m_intersection);
			newString.append("\n");
		}
		m_clusteringDetails.add(newString.toString());
	}

	/**
	 * Write the clustering details into a file.
	 * 
	 * @param file
	 *            the file to where the clustering details are to be written.
	 */
	public final void printClusteringDetails(File file) throws AdeException {
		final PrintWriter out = FileUtils.openPrintWriterToFile(file, true);

		out.printf("%s%n", m_clusteringDetails);

		out.close();
	}

	/**
	 * Write the initial clustering changes to a file.
	 * 
	 * @param file
	 *            the file to where the initial clustering changes are to be
	 *            written.
	 * @param clusterData
	 *            the cluster data
	 * @param matIndexToMsgInternalId
	 *            an array that maps matrix indices to message ID.
	 */
	public final void printInitialClusteringChanges(File file,
			List<ClusterData> clusterData, int[] matIndexToMsgInternalId)
			throws AdeUsageException {
		if (m_initialClusters == null) {
			return;
		}

		final PrintWriter out = FileUtils.openPrintWriterToFile(file, true);

		if (m_finalClusters == null) {
			updateClusters(clusterData, matIndexToMsgInternalId);
		}

		int countNoChange = 0;
		int countContained = 0;
		for (Map.Entry<String, ArrayList<Integer>> entry : m_initialClusters
				.entrySet()) {
			final String name = entry.getKey();
			final ArrayList<Integer> members = entry.getValue();

			out.printf("%s %s ", name, members);
			int countC = 0;
			boolean splitFlag = true;
			final ArrayList<String> names = new ArrayList<String>();
			for (Map.Entry<String, Collection<Integer>> newEntry : m_finalClusters
					.entrySet()) {
				final Collection<Integer> newMembers = newEntry.getValue();
				final String newName = newEntry.getKey();
				int countM = 0;
				boolean updateC = true;
				for (int m : newMembers) {
					if (members.contains(matIndexToMsgInternalId[m])) {
						if (updateC) {
							countC++;
							updateC = false;
							names.add(newName);
						}
						countM++;
					}
				}
				if (countM == members.size()) {
					splitFlag = false;
					if (members.size() == newMembers.size()) {
						out.printf(" did not change.%n");
						countNoChange++;
					} else {
						out.printf(" is contained in %s.%n", newName);
						countContained++;
					}
					break;
				}
			}
			if (splitFlag) {
				out.printf("split to %d clusters: %s%n", countC, names);
			}
		}
		out.printf(
				"Summary:%n%d clusters did not change.%n%d clusters are contained.%n%d clusters split.%n",
				countNoChange, countContained, m_initialClusters.size()
						- countNoChange - countContained);
		out.close();
	}

	/**
	 * Get the initial partition.
	 * 
	 * @return the initial partition
	 */
	public final int[] getInitialPartition() {
		final int[] copyOfInitialPartition = new int[m_initialPartition.length];
		System.arraycopy(m_initialPartition, 0, copyOfInitialPartition, 0,
				m_initialPartition.length);
		return copyOfInitialPartition;
	}

	/**
	 * Set the initial partition.
	 * 
	 * @param initialPartition
	 *            the initial partition.
	 */
	public final void setInitialPartition(int[] initialPartition) {
		final int[] copyOfInitialPartition = new int[initialPartition.length];
		System.arraycopy(initialPartition, 0, copyOfInitialPartition, 0,
				initialPartition.length);
		this.m_initialPartition = copyOfInitialPartition;
	}

	/**
	 * Get the number of clusters.
	 * 
	 * @return the number of clusters
	 */
	public final int getNumClusters() {
		return m_numClusters;
	}

	/**
	 * Set the number of clusters.
	 * 
	 * @param numClusters
	 *            the number of clusters
	 */
	public final void setNumClusters(int numClusters) {
		this.m_numClusters = numClusters;
	}

	/**
	 * Get the cluster name.
	 * 
	 * @param key
	 *            of the desired cluster.
	 * @return the cluster matching the passed in key
	 */
	public final String getClusterName(int key) {
		if (m_finalClusterNames == null) {
			return null;
		}
		return m_finalClusterNames.get(key);
	}

	/**
	 * For every cluster, the class holds the similarity measurements between
	 * itself and the cluster most similar to it (in the initial partition).
	 */
	private static final class ClusterMeasures {
		private String m_name = null;
		private double m_measure = Double.NaN;
		private double m_intersection = Double.NaN;

		private ClusterMeasures() {
			// private constructor
		}
	}

}
