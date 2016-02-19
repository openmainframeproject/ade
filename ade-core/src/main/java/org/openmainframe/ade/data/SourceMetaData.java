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
package org.openmainframe.ade.data;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.dataStore.GroupRead;

/** Data associated with a source */
public class SourceMetaData {

    /** The source for which this meta data concerns */
    public ISource m_source;
    /** Type of log: determines which parser is required for parsing it */
    public String m_logType;
    /** Anyalsis group: sources with the same analysis group id will be trained
     * together and will share a single model
     */
    public int m_analysisGroupId;

    /** Log file path associated with this source */
    public String m_fileName;

    public SourceMetaData(ISource source) {
        source = source;
    }

    /**
     * 
     * @param source The source for which this meta data concerns
     * @param logType  determines which parser is required for parsing it
     * @param analysisGroup Sources with the same analysis group will be trained together and will share a single model
     * @param runGroup sources with the same run group will be tracked using the same process
     * @param fileName Log file path associated with this source
     */
    public SourceMetaData(ISource source, String logType, int analysisGroupId, String fileName) {
       source = source;
        m_logType = logType;
        m_analysisGroupId = analysisGroupId;
        m_fileName = fileName;
    }

    public String toString() {
        return m_source + " logType=" + m_logType + " analysisGroup=" + m_analysisGroupId + " file=" + m_fileName;
    }
    
    /**
     * Each source is associated with a group in the GROUPS table. This method 
     * retrieves the group name using the analysis group internal id.
     * @return the analysis group name
     * @throws AdeException
     */
    public String getAnalysisGroupName() throws AdeException{
        return GroupRead.getAnalysisGroupName(m_analysisGroupId);
    }

}
