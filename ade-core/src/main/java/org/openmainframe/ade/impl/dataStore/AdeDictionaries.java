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
package org.openmainframe.ade.impl.dataStore;

import java.util.Map;
import java.util.TreeMap;

import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.dbUtils.DbDictionary;

/**
 * Store the Code dictionary of all relevant data found in the logs such as:
 * messageId, severity, components, systems.
 */
public class AdeDictionaries {


    /**
     * Stores the {@link WordIdDictionary} found in the logs.
     */
    private DbDictionary m_messageIds;
    private DbDictionary m_componentIds;
    private DbDictionary m_sourceIds;
    private Map<String, String> m_src2AnalysisGrpMap = new TreeMap<>();

    /**
     * Constructor to create a AdeDictionaries that contains the specified dictionaries.
     *
     * @param messageIds DbDictionary containing messageIds
     * @param componentIds DbDictionary containing componentIds
     * @param systemIds DbDictionary containing systemIds
     */
    public AdeDictionaries(DbDictionary messageIds,
            DbDictionary componentIds,
            DbDictionary systemIds) {
        m_messageIds = messageIds;
        m_componentIds = componentIds;
        m_sourceIds = systemIds;
    }

    public final DbDictionary getMessageIdDictionary() {
        return m_messageIds;
    }

    public final DbDictionary getComponentIdDictionary() {
        return m_componentIds;
    }

    public final DbDictionary getSourceIdDictionary() {
        return m_sourceIds;
    }

    public final Map<String, String> getSrc2AnalysisGrpMap() {
        return m_src2AnalysisGrpMap;
    }

    /**
     * Adds message-id and component-id to dictionaries.
     *
     * @param messageInstance the message that should be added to the dictionaries
     * @return message-internal-id the id of the newly create message
     * @throws AdeException when an error occurs while accessing the datastore
     */
    public final int addMessageToDictionaries(IMessageInstance messageInstance) throws AdeException {
        final int res = getMessageIdDictionary().addWord(messageInstance.getMessageId());
        final String compId = messageInstance.getComponentId();
        if (compId != null && compId.length() > 0) {
            getComponentIdDictionary().addWord(compId);
        }
        return res;
    }

    /**
     * Clears all values from this dictionary.
     */
    public final void clearAll() {
        m_messageIds.clear();
        m_componentIds.clear();
        m_sourceIds.clear();
        m_src2AnalysisGrpMap.clear();
    }
}
