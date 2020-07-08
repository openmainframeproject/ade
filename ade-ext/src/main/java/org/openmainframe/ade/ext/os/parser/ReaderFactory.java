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
package org.openmainframe.ade.ext.os.parser;

import org.openmainframe.ade.ext.AdeExt;
import org.openmainframe.ade.AdeInputStream;
import org.openmainframe.ade.AdeMessageReader;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;
import org.openmainframe.ade.ext.os.AdeExtProperties;
import org.openmainframe.ade.ext.service.AdeExtUsageException;

/**
 * Factory class that returns an object of type AdeMessageReader.
 */
public class ReaderFactory {

    /**
     * Creates a new reader where the type is determined by the instance of adeExtProperties.
     * @param stream The input stream.
     * @param parseReportFilename The name of the parse report file.
     * @param adeExtProperties The configurations/properties information.
     * @return The message reader.
     * @throws AdeException
     */
    public AdeMessageReader getReader(AdeInputStream stream, String parseReportFilename, AdeExtProperties adeExtProperties) throws AdeException {
        if (adeExtProperties instanceof LinuxAdeExtProperties) {
            if (AdeExt.getAdeExt().getConfigProperties().isSparkLog()){
                return new SparklogMessageReader(stream, parseReportFilename,
                    (LinuxAdeExtProperties) adeExtProperties);
            }
            return new LinuxSyslogMessageReader(stream, parseReportFilename,
                    (LinuxAdeExtProperties) adeExtProperties);
        } else {
            throw new AdeExtUsageException("AdeExtProperties type unknown: "
                    + adeExtProperties.getClass().getName());
        }
    }
}
