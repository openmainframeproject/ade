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
package org.openmainframe.ade.ext.os;

import java.util.Collection;

import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;

/**
 * The AdeExtProperties class carries properties/configuration information
 * from the start of the AdeExt Main Class (e.g. upload/analyze etc) to 
 * all the follow-on processing (e.g. reading log files)
 */

public abstract class AdeExtProperties {
    /**
     * Whether a parse report is requested
     */
    private boolean m_isParseReportRequested = false;

    /**
     * The Request Type
     */
    private AdeExtRequestType m_requestType = null;

    /**
      * Whether source option (-s) was provided when Analyze was invoked.
      */
    private boolean m_isSourceOptionProvided = false;

    /**
      * The source specified in the command line, when Analyze was invoked.
      */
    private Collection<ISource> m_sources = null;
    
    /**
     * Name of the last newly seen source.
     */
    private String m_lastNewlySeenSourceId = null;

    /**
     * Return the request Type
     * @return
     */
    public AdeExtRequestType getRequestType() {
        return m_requestType;
    }

    /**
     * Set the requestType.
     * 
     * @param requestType
     * @return
     */
    public void setRequestType(AdeExtRequestType requestType) {
        m_requestType = requestType;
    }

    /**
     * Whether parseReport is requested
     * @return
     */
    public boolean isParseReportRequested() {
        return m_isParseReportRequested;
    }

    /**
     * Set whether parseReport is requested.
     * 
     * @param requested
     * @return
     */
    public void setParseReportRequested(boolean requested) {
        m_isParseReportRequested = requested;
    }

    public Collection<ISource> getSources() {
        return m_sources;
    }

    public void setSources(Collection<ISource> m_sources) {
        this.m_sources = m_sources;
    }

    public boolean isSourceOptionProvided() {
        return m_isSourceOptionProvided;
    }

    public void setSourceOptionProvided(boolean m_isSourceOptionProvided) {
        this.m_isSourceOptionProvided = m_isSourceOptionProvided;
    }
    
    public String getLastNewlySeenSourceId() {
        return m_lastNewlySeenSourceId;
    }

    public void setLastNewlySeenSourceId(String m_lastNewlySeenSourceId) {
        this.m_lastNewlySeenSourceId = m_lastNewlySeenSourceId;
    }

}
