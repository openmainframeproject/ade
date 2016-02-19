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
package org.openmainframe.ade.ext.main.helper;

/**
 * Enum for the supported Ade Operating System Type 
 */

public enum AdeExtOperatingSystemType {
    LINUX;

    /**
     * Return a enum based on the input String
     * @param osType
     * @return
     */
    static public AdeExtOperatingSystemType getAdeExtOperatingSystemType(String osType) {
        for (AdeExtOperatingSystemType definedOSType : AdeExtOperatingSystemType.values()) {
            if (definedOSType.name().equalsIgnoreCase(osType)) {
                return definedOSType;
            }
        }

        return null;
    }

    /**
     * Return a list of enum names defined in this enum.
     * @param osType
     * @return
     */
    static public String getDefinedAdeExtOperatingSystemTypes() {
        StringBuilder bldret = new StringBuilder("");

        for (AdeExtOperatingSystemType definedOSType : AdeExtOperatingSystemType.values()) {
            if (bldret.length() == 0) {
                bldret.append(definedOSType.name());
            } else {
                bldret.append(", " + definedOSType.name());
            }
        }

        return bldret.toString();
    }

}
