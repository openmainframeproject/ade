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
package org.openmainframe.ade.ext.main.helper;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.openmainframe.ade.AdeInputStream;
import org.openmainframe.ade.AdeMessageReader;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.os.AdeExtProperties;
import org.openmainframe.ade.impl.data.FileSeperator;
import org.openmainframe.ade.impl.flow.modules.AdeInputStreamHandler;
import org.openmainframe.ade.utils.AdeFileUtils;

/**
 * The AdeInputStreamHandlerExt handles AdeInputStream for all the Linux
 * operating system types.
 */
public class AdeInputStreamHandlerExt extends AdeInputStreamHandler {

    /**
     * The AdeExt properties that will be used for processing
     */
    protected AdeExtProperties m_adeExtProperties;

    protected AdeInputStream a_adeInputStream;

    /**
     * Constructor
     * 
     * @param adeExtProperties
     * @throws AdeException
     */
    public AdeInputStreamHandlerExt(AdeExtProperties adeExtProperties) throws AdeException {
        super();
        m_adeExtProperties = adeExtProperties;

    }

    @Override
    protected final AdeMessageReader getReader(AdeInputStream stream) throws AdeException {
        return stream.getReader();
    }

    /**
     * Process the Log Messages coming from STDIN.
     * 
     * @throws AdeException
     */
    public final void incomingStreamFromSTDIN() throws AdeException {
        /* Create a AdeInputStream, note that properties is not used by
         * Anomaly Detection Engine */
        final Properties props = new Properties();
        /* Retrieve the name of the file. */
        final String name = getNameForStdin();
        final String parseReportFilename = getParseReportFilename(name);
        a_adeInputStream = new AdeInputStreamExt(System.in, props, m_adeExtProperties, parseReportFilename);

        /* Send the stream for further processing */
        incomingObject(a_adeInputStream);
    }

    /**
     * Process the Log Messages coming from a file.
     * 
     * @param file
     * @throws AdeException
     */
    public final void incomingStreamFromFile(File file) throws AdeException {
        final InputStream is = AdeFileUtils.openLogFileAsInputStream(file);

        /* Create a AdeInputStream, note that properties is not used by
         * Anomaly Detection Engine */
        final Properties props = new Properties();
        /* Retrieve the name of the file.  FilenameUtil is used here to extract a path without any prefix.
         * Note: Drive Letters show up on Windows System as prefix.  getPath() will return the full path without the drive. */
        final String filename = FilenameUtils.getPath(file.getAbsolutePath()) + file.getName();
        final String parseReportFilename = getParseReportFilename(filename);

        a_adeInputStream = new AdeInputStreamExt(is, props, m_adeExtProperties, parseReportFilename);

        /* Indicate this is a new file, this will allow an interval broken into 
         * to log files. */
        incomingSeparator(new FileSeperator(file.getName()));

        /* Send the stream for further processing */
        incomingObject(a_adeInputStream);
    }

    /**
     * Get the parse report filename based on the logFileName.
     * 
     * @param logfileName
     * @throws AdeException
     */
    protected final String getParseReportFilename(String name) throws AdeException {
        if (name.contains(".")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        name += "_parsing_report.txt";

        return name;
    }

    /**
     * Generate a name for STDIN.
     * @return
     */
    protected final String getNameForStdin() {
        final SimpleDateFormat F = new SimpleDateFormat("-hhmmss-MMddyyyy");
        return ("stdin" + F.format(new Date()));
    }
}
