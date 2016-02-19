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
package org.openmainframe.ade.data;

import java.util.Date;

/**
 * Provides meta-data for a single configuration. 
 */
public interface IConfigurationData {

    /**
     * Origin of a data for a source. This could be either
     * {@link SourceDataOrigin#FILE} or {@link SourceDataOrigin#DB}.
     */
    public enum SourceDataOrigin {
        /**
         * Indicates that the meta-data points to a file on the file system.
         */
        FILE,

        /**
         * Indicates that the meta-data points to a database.
         */
        DB
    }

    /**
     * @return The {@link ISource} this configuration belongs to.
     */
    ISource getSource();

    /**
     * @return The {@link String} identifying the configuration.
     *     e.g. For {@link SourceDataOrigin#FILE}, the file system
     *     path to the file can be stored. 
     */
    String getConfigurationId();

    /**
     * @return The kind of origin this configuration originates from.
     *     Usually, this field is used to decide how to parse the identity
     *     data {@link String} obtained by {@link #getConfigurationId()}.
     */
    SourceDataOrigin getSourceDataOrigin();

    /**
     * @return The {@link Date} this configuration data was extracted at.
     *     It should represent the number of milliseconds
     *     from the Epoch <b>as if it was taken in GMT+00</b>. For example,
     *     if the original date was 2012/02/11 16:00:00.000 EST, then
     *     the time zone (EST) is overlooked, and the returned {@link Date}
     *     will represent 2012/02/11 16:00:00.000 GMT
     */
    Date getTimestamp();

}