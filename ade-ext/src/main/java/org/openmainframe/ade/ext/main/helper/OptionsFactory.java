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
 * Factory class that returns an object of type AdeExtOptions. 
 */
public class OptionsFactory {

    /**
     * Use getOptions to get platform specific options. 
     * @param String value that represents the platform specific options. 
     * @return returns the platform specific options object. 
     */
    public AdeExtOptions getOptions(AdeExtOperatingSystemType optionsType) {

        if (optionsType == AdeExtOperatingSystemType.LINUX) {
            return new LinuxOptions();
        }
        return null;
    }
}
