/*
 
    Copyright IBM Corp. 2012, 2016
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
package org.openmainframe.ade.ext.data;

import java.util.Locale;

/**
 * Defines available client types for Ade analytics and
 * the corresponding filesystem path.
 *
 * Valid client types are:
 *   LINUX
 */
public enum ClientOSType {
    LINUX("linux", "linux");

    private String type;
    private String path;

    /**
     * A lenient (ie. case-insensitive) version of valueOf().
     * @param name
     * @return
     */
    public static ClientOSType fromString(String name) {
        return ClientOSType.valueOf(name.toUpperCase(Locale.ENGLISH));
    }

    private ClientOSType(String type, String path) {
        this.type = type;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }
}