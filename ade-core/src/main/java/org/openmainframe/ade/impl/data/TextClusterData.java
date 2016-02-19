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

import java.util.Date;

import org.openmainframe.ade.impl.utils.DateTimeUtils;

/**
 * Represents a single cluster of texts. 
 * 
 */
public class TextClusterData {

    private String m_representativeText;
    /**
     * Latest time a message from this cluster was observed. Kept in resolution
     * of days - the time represents the starting time of the day this cluster was last
     * observed.
     */
    private Date m_lastObserved;
    private int m_clusterId;

    public TextClusterData(String representativeText, Date lastObserved, int clusterId) {
        m_representativeText = representativeText;
        m_lastObserved = new Date(DateTimeUtils.getDayStartTimeLocal(lastObserved.getTime()));
        m_clusterId = clusterId;
    }

    public final void setRepresentativeText(String representativeText) {
        m_representativeText = representativeText;
    }

    public final String getTextRepresentative() {
        return m_representativeText;
    }

    public final boolean setLastObserved(Date timeStamp) {
        return setLastObserved(timeStamp, false);
    }

    /* return true only if the time stamp is in a new day */
    public final boolean setLastObserved(Date timeStamp, boolean force) {

        final long dayStartTimeOfTimeStamp = DateTimeUtils.getDayStartTimeLocal(timeStamp.getTime());
        if (force || dayStartTimeOfTimeStamp > m_lastObserved.getTime()) {
            m_lastObserved = new Date(dayStartTimeOfTimeStamp);
            return true;
        }
        return false;
    }

    public final Date getLastObserved() {
        return m_lastObserved;
    }

    public final int getClusterId() {
        return m_clusterId;
    }

    public final String getLinkedMessageId() {
        return getLinkedMessageId("CLUSTERED_MESSAGE_");
    }

    public final String getLinkedMessageId(String componentName) {
        return componentName + "_" + m_clusterId;
    }

}
