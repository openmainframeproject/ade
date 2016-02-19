/*
 
    Copyright IBM Corp. 2015, 2016
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
package org.openmainframe.ade.ext.xml.v2.types;

public enum PeriodicityStatus {
    IN_SYNC("IN_SYNC"), NOT_IN_SYNC("NOT_IN_SYNC"), NON_PERIODIC("NOT_PERIODIC"), NEW("NEW");

    private String status;

    PeriodicityStatus(String value) {
        status = value;
    }

    /**
     * Return the periodicity status object given a string
     * @param status
     * @return
     */
    static public PeriodicityStatus getPeriodicityStatus(String status) {
        for (PeriodicityStatus aStatus : PeriodicityStatus.values()) {
            if (aStatus.status.equalsIgnoreCase(status)) {
                return aStatus;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return status;
    }
}