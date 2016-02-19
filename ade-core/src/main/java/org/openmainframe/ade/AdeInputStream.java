/*
 
    Copyright IBM Corp. 2013, 2016
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
package org.openmainframe.ade;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.impl.flow.modules.AdeInputStreamHandler;

/**
 * A {@link AdeInputStream} is a wrapper for an {@link InputStream} that holds additional data on
 * the stream. <br>
 * It is used as an input to the {@link AdeInputStreamHandler} that produces a stream of
 * {@link IMessageInstance} objects from it, using a {@link AdeMessageReader}. The additional data
 * can be used by the a class that extends {@link AdeMessageReader} to assist in creating the
 * {@link IMessageInstance} objects.
 */
public class AdeInputStream extends InputStreamReader {
    protected Properties m_props;
    protected AdeMessageReader a_adeMessageReader;
    protected String m_parseReportFilename;

    /**
     * Create a new {@link AdeInputStream} from an existing {@link InputStream} and a set of {@link Properties}
     * @param is - the existing input stream
     * @param props - set of specific properties to associate with the new {@link AdeInputStream}
     */
    public AdeInputStream(InputStream is, Properties props) {
        super(is, StandardCharsets.UTF_8);
        m_props = props;
    }

    public final Properties getProps() {
        return m_props;
    }

    public final AdeMessageReader getReader() {
        return a_adeMessageReader;
    }

    public final void setParseReportFileName(String parseReportFilename) {
        m_parseReportFilename = parseReportFilename;
    }

    public final String getParseReportFilename() {
        return m_parseReportFilename;
    }

}
