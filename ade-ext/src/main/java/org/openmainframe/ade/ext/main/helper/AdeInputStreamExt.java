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
package org.openmainframe.ade.ext.main.helper;

import java.io.InputStream;
import java.util.Properties;

import org.openmainframe.ade.AdeInputStream;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.os.AdeExtProperties;
import org.openmainframe.ade.ext.os.parser.ReaderFactory;

/**
 * Constructs a reader based on the AdeInputStream given and keeps track of other ade-z related items related to the stream. 
 */
public class AdeInputStreamExt extends AdeInputStream {

    protected AdeExtProperties m_adeExtProperties;

    /**
     * Reader factory to return AdeMessageReader objects
     */
    protected ReaderFactory readerFactory = new ReaderFactory();

    public AdeInputStreamExt(InputStream is, Properties props, AdeExtProperties adeExtProperties, String parseReportFilename) throws AdeException {
        super(is, props);
        m_adeExtProperties = adeExtProperties;
        m_parseReportFilename = parseReportFilename;
        constructReader();

    }

    private void constructReader() throws AdeException {
        a_adeMessageReader = readerFactory.getReader(this, m_parseReportFilename, m_adeExtProperties);
    }

}