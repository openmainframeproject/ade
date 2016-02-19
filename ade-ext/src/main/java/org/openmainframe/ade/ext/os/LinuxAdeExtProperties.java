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

/**
 * The LinuxAdeExtProperties class carries properties/configuration information
 * from the start of the AdeExt Main Class (e.g. upload/analyze etc) to 
 * all the follow-on processing (e.g. reading log files in LinuxReader.java)
 */

public class LinuxAdeExtProperties extends AdeExtProperties {
    /**
     * Year
     */
    private static final int YEAR_NOT_DEFINED = -1;
    private int year = YEAR_NOT_DEFINED;

    /**
     * boolean value that tells us if the GmtOffset has been set in the -g option ONLY. 
     */
    private boolean m_isGmtOffsetDefined = false;

    /** 
     * Managed System time offset (hours) from Greenwich Mean Time 
     */
    private long m_gmtOffset;

    /**
     * Return the year
     * @return
     */
    public int getYear() {
        return year;
    }

    /**
     * Set the year
     * @param year
     */
    public void setYear(int year) {
        this.year = year;
    }

    /**
     * Set the year
     * @param year
     */
    public boolean isYearDefined() {
        return (year != YEAR_NOT_DEFINED);
    }

    /**
     * Return the GMT Offset
     * @return
     */
    public long getGmtOffset() {
        return m_gmtOffset;
    }

    /**
     * Set the GMT Offset
     * @param gmtOffset
     */
    public void setGmtOffset(long gmtOffset) {
        this.m_gmtOffset = gmtOffset;
    }

    /** 
     * Returns boolean value, isGmtOffsetDefined
     */
    public boolean isGmtOffsetDefined() {
        return m_isGmtOffsetDefined;
    }

    /**
     * @param boolean value that says whether GmtOffset has been defined. If the GmtOffset is NOT defined in the -g option
     * this method should not get set to true. 
     * @return 
     */
    public void setIsGmtOffsetDefined(boolean isGmtOffsetDefined) {
        this.m_isGmtOffsetDefined = isGmtOffsetDefined;
    }

}
