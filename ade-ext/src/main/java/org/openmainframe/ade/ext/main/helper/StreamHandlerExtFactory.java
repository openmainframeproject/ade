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

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.os.AdeExtProperties;

/**
 * Factory class that returns an object of type StreamHandler
 */
public class StreamHandlerExtFactory {

    /**
     * Use getStreamHandler to get platform specific options. 
     * @param String value that represents the stream handler type and the adeExtProperties for the specified platform.
     * @return returns the platform specific stream handler. 
     * @throws AdeException 
     */
    public AdeInputStreamHandlerExt getStreamHandler(AdeExtOperatingSystemType streamHandlerType, AdeExtProperties adeExtProperties) throws AdeException {

        if (streamHandlerType == AdeExtOperatingSystemType.LINUX) {
            return new AdeInputStreamHandlerLinux(adeExtProperties);
        }
        return null;
    }
}
