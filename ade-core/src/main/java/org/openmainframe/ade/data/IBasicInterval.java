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
import java.util.Comparator;

/** A common interface to Interval and Analyzed Interval. */
public interface IBasicInterval {

    /**
     * Returns the start time of the interval.
     * @return long attribute representing the start time of the interval
     */
    long getIntervalStartTime();

    /**
     * Returns the end time of the interval.
     * @return long attribute representing the end time of the interval (non-inclusive)
     */
    long getIntervalEndTime();

    /** Class to compare two intervals by their start times. */
    static class ByStartTimeComparator implements Comparator<IBasicInterval>, Serializable {

        private static final long serialVersionUID = -8669048623637585674L;
        
        public ByStartTimeComparator() {
            //Take the default construction
        }

        @Override
        public int compare(IBasicInterval arg0, IBasicInterval arg1) {
            return Long.compare(arg0.getIntervalStartTime(),arg1.getIntervalStartTime());
        }

    }

}
