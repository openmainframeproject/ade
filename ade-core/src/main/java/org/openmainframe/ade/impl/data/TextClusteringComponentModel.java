/*
 
    Copyright IBM Corp. 2011, 2016
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

import java.util.HashMap;
import java.util.Map;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IMessageTextPreprprocessor;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.impl.dbUtils.DbDictionary;

/**
 * Online text clustering algorithm based on Levenshtein distance.
 * Keeps track of a textual cluster representative for each cluster. Generates a separate clustering solution according
 * to a given key (component).
 *
 */
public class TextClusteringComponentModel {
    /**
     * Maps a component to a clustering object of all message texts coming from this component.
     */
    private Map<String, TextClusteringModel> m_clusteringMap;

    /**
     * Determines whether the model is consistent with the database or not. If <code> true </code>, 
     * additions are stored in the database, and the model is sensitive to changes in the database, that is -
     * if clusters were added to the database, these changes are also reflected in the model.   
     */
    private boolean m_updateDataStore;

    private int m_maxClusterId;

    private IMessageTextPreprprocessor m_messageTextPreprocessor = null;

    /**
     * Constructs a new empty {@link TextClusteringComponentModel}. The model can be linked to the database, 
     * so additions to the model instantly get stored in the database, and the model is sensitive to changes 
     * in the database, that is - if clusters were added to the database, these changes are also reflected in the model.
     * 
     * <p>If the model is not linked to the database, it can be filled externally from the database. If it does,
     * it can either be filled externally, or it will be automatically filled upon first call to get a cluster,
     * following the detection of inconsistency between database and model. 
     * 
     * @param updateDataStore should be true if model should be linked to the database.
     */
    public TextClusteringComponentModel(boolean updateDataStore) {
        m_updateDataStore = updateDataStore;
        m_clusteringMap = new HashMap<>();
    }

    /**
     * Returns true if the model should be linked to the database.
     * 
     * @return true if the model should be linked to the database.
     */
    public final boolean isUpdatingDataStore() {
        return m_updateDataStore;
    }

    /**
     * Set the link to the database.
     * 
     * @param updateDataStore should be true if the model should be linked to the database.
     */
    public final void setUpdateDataStore(boolean updateDataStore) {
        m_updateDataStore = updateDataStore;
    }

    /**
     * Returns the clustering model for the given component. Creates a new clustering model if no clustering model
     * exists for the given component.
     * 
     * @param component a string representing the key for the clustering model.
     * @param thresholdSetter the {@link IThresholdSetter}
     * @return a clustering model matching the given component.
     */
    public final TextClusteringModel getTextClusteringModel(String component, IThresholdSetter thresholdSetter)
            throws AdeException {
        TextClusteringModel textClusteringModel = getTextClusteringModel(component);
        String componentCopy = component;
        if (textClusteringModel == null) {
            // make sure the component name is not too long:
            if (componentCopy.length() > SQL.MAX_LEN_DICTIONARY) {
                componentCopy = componentCopy.substring(0, SQL.MAX_LEN_DICTIONARY - 1);
            }

            // the component should be inserted before refreshing the TextClusteringModel from db

            final DbDictionary componentDictionary = AdeInternal.getAdeImpl().getDataStore().
                    getAdeDictionaries().getComponentIdDictionary();
            componentDictionary.addWord(componentCopy);

            textClusteringModel = new TextClusteringModel(componentCopy, m_updateDataStore, this, thresholdSetter);
            textClusteringModel.setMessageTextPreprocessor(m_messageTextPreprocessor);
            m_clusteringMap.put(componentCopy, textClusteringModel);
        }

        return textClusteringModel;
    }

    /**
     * Get the {@link TextClusteringModel} for the specified component.
     * 
     * @param component a string representing the key for the clustering model.
     * @return the {@link TextClusteringModel}
     */
    public final TextClusteringModel getTextClusteringModel(String component) {
        return m_clusteringMap.get(component);
    }

    /**
     * Set maxClusterId to the passed in value.
     * 
     * @param maxClusterId the maxClusterId to set
     */
    public final synchronized void setMaxClusterId(int maxClusterId) {
        if (m_maxClusterId < maxClusterId) {
            m_maxClusterId = maxClusterId;
        }
    }

    /**
     * Get the maxClusterId.
     * 
     * @return the maxClusterId
     */
    public final synchronized int getMaxClusterId() {
        return m_maxClusterId;
    }

    /**
     * Increment the maxClusterId by one.
     *  
     * @return incremented value of maxClusterId
     */
    public final synchronized int increaseMaxClusterId() {
        return ++m_maxClusterId;
    }

    /**
     * Get the clusters map.
     * 
     * @return the clusters map
     */
    public final Map<String, TextClusteringModel> getClusters() {
        return m_clusteringMap;
    }

    /**
     * Set the {@link IMessageTextPreprprocessor}.
     * 
     * @param messageTextPreprocessor the {@link IMessageTextPreprprocessor}
     */
    public final void setMessageTextPreprocessor(
            IMessageTextPreprprocessor messageTextPreprocessor) {
        m_messageTextPreprocessor = messageTextPreprocessor;
    }
    
    /**
     * Implements the {@link IThresholdSetter} interface using a simple threshold calculation. Allows you to
     * set the delta and factor values (via the constructor) used in calculating the threshold or use the defaults.
     *
     */
    public static class SimpleThresholdSetter implements IThresholdSetter {
        private static final int DEFAULT_DELTA = 2;
        private static final double DEFAULT_FACTOR = 3.33;
        private double m_factor;
        private int m_delta = DEFAULT_DELTA;

        /**
         * Construct a new SimpleThresholdSetter using the default values.
         */
        public SimpleThresholdSetter() {
            this(DEFAULT_FACTOR);
        }

        /**
         * Construct a new SimpleThresholdSetter by specifying the factor value. The default will be used
         * for the delta value.
         * 
         * @param factor the factor used in calculating the threshold.
         */
        public SimpleThresholdSetter(double factor) {
            m_factor = factor;
        }

        /**
         * Construct a new SimpleThresholdSetter by specifying the factor and the delta values.
         * 
         * @param factor the factor used in calculating the threshold.
         * @param delta the delta used in calculating the threshold.
         */
        public SimpleThresholdSetter(double factor, int delta) {
            this(factor);
            m_delta = delta;
        }

        @Override
        public final int getThreshold(int numWordsA, int numWordsB) {
            return (int) Math.round(Math.max(0, (Math.min(numWordsA, numWordsB) - m_delta) / m_factor));
        }

    }

}
