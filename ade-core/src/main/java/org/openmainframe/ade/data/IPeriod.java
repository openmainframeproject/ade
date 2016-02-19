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

import java.io.Serializable;
import java.util.Date;

/**
 * A Period object represents a substantial period of time for which data was collected or analyzed.
 * Currently it is usually a day or an hour.
 * A Period is associated with a source. 
 */
public interface IPeriod extends Serializable {

    public enum PeriodMode {
        HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY
    }

    /** Returns this period's source */
    ISource getSource();

    /** Returns this period's start time */
    Date getStartTime();

    /** Returns this period's end time */
    Date getEndTime();

    /** Returns number of milliseconds in this period */
    long getLengthInMillis();

    /** Returns number of minutes in this period */
    int getLengthInMinutes();

    int getStatus();

    void setStatus(int status);

    boolean getExcludeFromTraining();

    void setExcludeFromTraining(boolean excludeFromTraining);

    String getComment();

    void setComment(String comment);

}
