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
package org.openmainframe.ade.impl.flow.modules;

/**
 * An Interval Separator that is for an interval that is not aligned
 * on a normal interval boundary.
 */
public class TemporaryIntervalSeparator extends IntervalSeparator {


    /**
     * The boundary-unaligned interval start time.
     */
    private long fullIntervalStartTime;

    /**
     * Sole constructor to create a TemporaryIntervalSeparator.
     *
     * @param intervalStartTime boundary-aligned interval start time
     * @param fullIntervalStartTime boundary-unaligned interval start time
     */
    public TemporaryIntervalSeparator(Long intervalStartTime, long fullIntervalStartTime) {
        super(intervalStartTime);
        setFullIntervalStartTime(fullIntervalStartTime);
    }

    /**
     * Sets the full interval start time to the specified value.
     *
     * @param fullIntervalStartTime the long value to set the full interval start time to
     */
    private void setFullIntervalStartTime(long fullIntervalStartTime) {
        this.fullIntervalStartTime = fullIntervalStartTime;
    }

    /**
     * Returns a long whose value is the full interval start time.
     *
     * @return long whose value is the full interval start time
     */
    public final long getFullIntervalStartTime() {
        return fullIntervalStartTime;
    }

}
