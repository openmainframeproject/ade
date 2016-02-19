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
package org.openmainframe.ade.ext.output;

import org.openmainframe.ade.ext.main.helper.AdeExtOperatingSystemType;

/**
 * Enum for the supported Log Types.
 */
public enum LogType {

    LOG_TYPE_UNIX_STYLE_SYSLOG("Unix style syslog");

    private final String type;

    private LogType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static String getSupportedLogType(AdeExtOperatingSystemType osType) {
        if (osType == AdeExtOperatingSystemType.LINUX) {
            return LOG_TYPE_UNIX_STYLE_SYSLOG.toString();
        }
        return "";
    }
}
