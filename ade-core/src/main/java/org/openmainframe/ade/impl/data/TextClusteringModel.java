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
package org.openmainframe.ade.impl.data;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IMessageTextPreprprocessor;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dataStore.DataStoreTextClusteringModelsImpl;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.impl.summary.LevenshteinTextSummary;
import org.openmainframe.ade.impl.summary.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for a text clustering model which is associated with a given component.
 * The model holds a list of text representatives, and a new cluster is added when it encounters new text
 * which is considered far away from all current representatives. The sets of cluster IDs of this clustering model
 * and all other clustering models that share the same parent model are disjoint (The parent model is responsible
 * for distributing disjoint cluster IDs).
 */
public class TextClusteringModel {

    /**
     * Clusters, sorted by the traversal order.
     */
    private LinkedList<TextClusterData> m_clustersData;

    /**
     * Max cluster id for this component's model.
     */
    private int m_maxClusterId = 0;

    private int m_componentId;

    private String m_componentName;

    private boolean m_updateDataStore;

    private TextClusteringComponentModel m_parentModel;

    private DataStoreTextClusteringModelsImpl m_dsTextClustering;

    private IThresholdSetter m_thresholdSetter;

    private IMessageTextPreprprocessor m_messageTextPreprocessor = null;
    
    private static final Logger logger = LoggerFactory.getLogger(TextClusteringModel.class);

    /**
     * Creates a new text clustering model which is associated with the given component.
     *   
     * @param component the component this model is associated with
     * @param updateDataStore should be true if new clusters should be added to the datastore.
     * @param textClusteringComponentModel the parent model
     * @param thresholdSetter a thresholdSetter functor for two strings. Implements the logic of strings closeness 
     *        for deciding whether a given text belongs to one of the clusters in the model, or whether it should
     *        initiate a new cluster. 
     * @throws AdeException if an internal error occurred
     */
    public TextClusteringModel(String component, boolean updateDataStore,
            TextClusteringComponentModel textClusteringComponentModel,
            IThresholdSetter thresholdSetter) throws AdeException {
        m_componentName = component;
        m_componentId = AdeInternal.getAdeImpl().getDictionaries().getComponentIdDictionary().addWord(component);

        m_updateDataStore = updateDataStore;

        m_clustersData = new LinkedList<TextClusterData>();
        m_parentModel = textClusteringComponentModel;
        m_dsTextClustering = AdeInternal.getAdeImpl().getDataStore().textClustering();
        m_maxClusterId = m_dsTextClustering.refreshClustersFromDataStore(m_componentId, m_maxClusterId, m_clustersData);
        m_parentModel.setMaxClusterId(m_maxClusterId);
        m_thresholdSetter = thresholdSetter;
    }

    /**
     * Get a TextClusterData from this set that has the given message id.
     * Mainly from statistics.
     * 
     * @param id the ID of the desired cluster.
     * @return The cluster matching the passed in ID. Returns null if no match found. 
     */
    public final TextClusterData getCluster(int id) {
        final Iterator<TextClusterData> e = m_clustersData.iterator();
        while (e.hasNext()) {
         // this is needed from the first time on.
            final TextClusterData cluster = e.next();
            if (cluster.getClusterId() == id) {
                return cluster;
            }
        }
        return null;
    }

    /**
     * Returns the cluster id for the given text. Searches for the first cluster representative that is similar
     * to the given text. If no cluster matches the text, creates a new cluster with the given text as the cluster
     * representative and returns it.
     * 
     * @param text (this is internally limited to SQL.MAX_LEN_TEXT)
     * @param timeStamp the time at which the text was observed
     * @return cluster for the given text.
     * @throws AdeException if an internal error occurred
     */
    public final TextClusterData getOrAddCluster(String text, Date timeStamp) throws AdeException {
        String textHead = text;
        if (m_messageTextPreprocessor != null) {
            textHead = m_messageTextPreprocessor.processString(text);
        }
        if (SQL.MAX_LEN_TEXT >= 0 && textHead.length() > SQL.MAX_LEN_TEXT) {
            textHead = textHead.substring(0, SQL.MAX_LEN_TEXT);
        }
        final TextClusterData cluster = findCluster(textHead, timeStamp, true);
        if (cluster != null) {
            return cluster;
        }
        return safelyAddCluster(textHead, timeStamp);
    }

    /**
     * Find the first cluster that is close to the passed in text.
     * 
     * @param text the text to search for in the clusters
     * @param timeStamp the time at which the text was observed
     * @return the first cluster that is close to the given text, or null if no cluster passed the 
     *         proximity threshold
     * @throws AdeException if an internal error occurred
     */
    private TextClusterData findCluster(String text, Date timeStamp, boolean isTrace) throws AdeException {
        boolean pathsAreOneToken = false;
        if (m_messageTextPreprocessor != null) {
            pathsAreOneToken = m_messageTextPreprocessor.treatPathsAsOneToken();
        }

        Word[] words;
        if (pathsAreOneToken) {
            words = LevenshteinTextSummary.prepareStringToken(text);
        } else {
            words = LevenshteinTextSummary.prepareString(text);
        }
        
        final Iterator<TextClusterData> e = m_clustersData.iterator();
        while (e.hasNext()) {
         // this is needed from the first time on.
            final TextClusterData cluster = e.next();
            final String clusterRepresentative = cluster.getTextRepresentative();
            Word[] clusterRepresentativeWords;
            if (pathsAreOneToken) {
                clusterRepresentativeWords = LevenshteinTextSummary.prepareStringToken(clusterRepresentative);
            } else {
                clusterRepresentativeWords = LevenshteinTextSummary.prepareString(clusterRepresentative);
            }
            final int threshold = m_thresholdSetter.getThreshold(clusterRepresentativeWords.length, words.length);
            final boolean areClose = areClose(clusterRepresentativeWords, words, threshold, isTrace);
            boolean updatedLastObserved;
            if (areClose) {
                updatedLastObserved = cluster.setLastObserved(timeStamp);
                // move to the head of the queue
                e.remove();
                m_clustersData.push(cluster);

                if (m_updateDataStore && updatedLastObserved) {
                    m_dsTextClustering.updateTextClusterTimeStamp(cluster.getClusterId(), cluster.getLastObserved());
                }
                return cluster;
            }
        }
        return null;
    }

    /**
     * Set the {@link IMessageTextPreprprocessor}.
     * @param messageTextPreprocessor the {@link IMessageTextPreprprocessor}
     */
    public final void setMessageTextPreprocessor(
            IMessageTextPreprprocessor messageTextPreprocessor) {
        m_messageTextPreprocessor = messageTextPreprocessor;
    }

    private boolean modelInSync() throws AdeException {
        final int maxClusterId = m_dsTextClustering.getMaxClusterId(m_componentId);
        if (Ade.getAde().getConfigProperties().debug().isDebugMessageIdGeneration() >= 0
            && maxClusterId != m_maxClusterId) {
            logger.info("   @@@@@@@@@@@@@@@@@@ MODELS ARE OUT OF SYNC -  REREADING  @@@@@@@@@@@@"
                        + maxClusterId + "!=" + m_maxClusterId + "@@@@@@[" + m_componentName + "]");
        }
        return maxClusterId == m_maxClusterId;
    }

    private TextClusterData safelyAddCluster(String text, Date timeStamp) throws AdeException {
        TextClusterData cluster = null;
        // no cluster representative in the model is close to the given text.
        // if the model is linked to the database, test whether we need to refresh the model from the database. Maybe
        // the cluster representative of one of the new clusters in the database is close to the given text. 
        if (m_updateDataStore) {
            Integer clusterId = null;
            final int debugMessageIdGeneration =
                    Ade.getAde().getConfigProperties().debug().isDebugMessageIdGeneration();
            if (debugMessageIdGeneration >= 0) {
                logger.info("   ---------   adding new cluster for " + text + " on " + timeStamp + "   ["
                        + m_componentName + "]");
            }
            final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
            try {
                cw.startTransaction();
                cw.lockTableExclusive(SQL.TEXT_CLUSTERS);
                if (!modelInSync()) {
                    m_maxClusterId = m_dsTextClustering.refreshClustersFromDataStore(m_componentId,
                            m_maxClusterId, m_clustersData);
                    m_parentModel.setMaxClusterId(m_maxClusterId);

                    cluster = findCluster(text, timeStamp, false);
                    if (cluster != null) {
                        if (debugMessageIdGeneration >= 0) {
                            logger.info("=====================> found a matching cluster in the reread model =======["
                                    + m_componentName + "]");
                        }
                        return cluster;
                    }
                }

                clusterId = m_dsTextClustering.storeTextCluster(text, m_componentId, timeStamp);
                // the new cluster should be maximal, but just in case we take max again.
                if (clusterId != null) {
                    m_maxClusterId = Math.max(clusterId, m_maxClusterId);
                }
            } catch (SQLException e) {
                cw.failed(e);
            } finally {
                cw.quietCleanup();
            }
            if (clusterId == null) {
                throw new AdeInternalException("clusterId not set for cluster=" + cluster
                        + ". Shuold have never reached this line");
            }
            cluster = new TextClusterData(text, timeStamp, clusterId);

            m_clustersData.push(cluster);

            return cluster;
        } else {
            final int maxClusterId = m_parentModel.increaseMaxClusterId();
            cluster = new TextClusterData(text, timeStamp, maxClusterId);

            m_clustersData.push(cluster);
            return cluster;
        }

    }

    /**
     * Determines whether the Levenshtein distance between two strings is smaller than a given threshold.
     * Calculates a thresholded Levenshtein distance between the strings, and if the distance is larger than the
     * given threshold stops the calculation.
     *    
     * @param strA first string
     * @param strB second string
     * @param threshold maximal allowed edit distance between the two strings.
     * @return true if the distance between the two strings is smaller or equal to the given threshold.
     * @throws AdeException if an internal error occurred
     */
    private boolean areClose(Word[] wordsA, Word[] wordsB, int threshold, boolean isTrace) throws AdeException {
        final int oldThreshold = LevenshteinTextSummary.getThreshold();
        LevenshteinTextSummary.setThreshold(threshold);
        final int distance = LevenshteinTextSummary.calcDistance(wordsA, wordsB);
        LevenshteinTextSummary.setThreshold(oldThreshold);
        boolean magicMatch = true;
     // this may be expensive, so only do this if we really need to
        if (distance <= threshold) {
            magicMatch = doMagicWordsMatch(wordsA, wordsB);
        }
        
        final int debugMessageIdGeneration =
                Ade.getAde().getConfigProperties().debug().isDebugMessageIdGeneration();
        if (debugMessageIdGeneration >= 0 && isTrace && debugMessageIdGeneration > (distance - threshold)) {
            logger.info("comparing " + Arrays.asList(wordsA));
            logger.info("      and " + Arrays.asList(wordsB));
            logger.info("                       distance: " + distance + " threshold: " + threshold
                    + "          [" + m_componentName + "]");
            if (magicMatch) {
                logger.info("                       magic: " + magicMatch);
            }
            if (distance <= threshold && magicMatch) {
                logger.info("      ==================================================================== "
                        + distance + " " + threshold + "   (" + wordsA.length + "," + wordsB.length + ") ==["
                        + m_componentName + "]");
            }
        }
        return distance <= threshold && magicMatch;
    }

    /*
     * Creates separate lists of the magic words in wordsA and wordsB and then compares the two.
     * 
     * Returns true under the following conditions:
     *      if m_messageTextPreprocessor is null and thus does not have a list of magic words to compare to.
     *      if the list of magic words in wordsA is equal to the list of magic words in wordsB.
     *      if magic words were not found in both wordsA and wordsB.
     *
     * Returns false if the list of magic words in wordsA is not equal to the list of magic words in wordsB.
     */
    private boolean doMagicWordsMatch(Word[] wordsA, Word[] wordsB) throws AdeException {
        if (m_messageTextPreprocessor == null) {
            return true;
        }
        final List<String> magicA = new ArrayList<String>();
        final List<String> magicB = new ArrayList<String>();
        for (int i = 0; i < wordsA.length; ++i) {
            if (m_messageTextPreprocessor.isMagicWord(wordsA[i].getStr())) {
                magicA.add(wordsA[i].getStr());
            }
        }
        for (int i = 0; i < wordsB.length; ++i) {
            if (m_messageTextPreprocessor.isMagicWord(wordsB[i].getStr())) {
                magicB.add(wordsB[i].getStr());
            }
        }
        if (magicA.isEmpty() && magicB.isEmpty()) {
            return true;
        }
        if (Ade.getAde().getConfigProperties().debug().isDebugMessageIdGeneration() >= 0
                && magicA.size() != magicB.size()) {
            List<String> longer = magicB;
            List<String> shorter = magicA;
            if (magicA.size() > magicB.size()) {
                longer = magicA;
                shorter = magicB;
            }
            int i;
            String missing = null;
            for (i = 0; i < shorter.size(); ++i) {
                if (!shorter.get(i).equals(longer.get(i))) {
                    missing = longer.get(i);
                    break;
                }
            }
            if (missing == null) {
                missing = longer.get(i);
            }
            final String tmpString = "  @@@@@@@@@@@@@\"";
            if (!m_messageTextPreprocessor.isNonPairedMagicWord(missing)) {
                logger.info(tmpString + missing + "\"@@  MISSING MAGIC WORD????? No match for ");
                logger.info(tmpString + missing + "\"@@  " + longer);
                logger.info(tmpString + missing + "\"@@  " + shorter);
            }
        }
        return magicA.equals(magicB);
    }

    public final Collection<TextClusterData> getClusters() {
        return m_clustersData;
    }

    public final String getComponentName() {
        return m_componentName;
    }
}