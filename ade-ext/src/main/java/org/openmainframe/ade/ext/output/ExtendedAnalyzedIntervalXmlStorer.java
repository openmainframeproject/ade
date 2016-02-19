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
package org.openmainframe.ade.ext.output;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.output.AnalyzedIntervalXmlStorer;
import org.openmainframe.ade.utils.AdeFileUtils;

/**
 * This class is used to customize the Interval XML output file.  
 * 
 * This class take the output XML data from the Ade 3x, pass it to an XSLT 
 * processor that convert the 3x format into 1.8, then, it write the output to
 * filesystem.
 */
public abstract class ExtendedAnalyzedIntervalXmlStorer extends AnalyzedIntervalXmlStorer {
    
    protected XMLMetaDataRetriever m_xmlMetaData;
    /**
     * Default constructor
     * @throws AdeException 
     */
    public ExtendedAnalyzedIntervalXmlStorer() throws AdeException {
        super();
    }

    /**
     * Determine whether xsl directory is needed
     */
    @Override
    protected void createXsltDirectory(IAnalyzedInterval analyzedInterval) throws AdeException {
        if (isCreateXSLDirectory()) {
            ExtOutputFilenameGenerator outputFilenameGenerator = (ExtOutputFilenameGenerator) Ade.getAde().getConfigProperties().getOutputFilenameGenerator();
            File periodDir = outputFilenameGenerator.getIntervalXmlStorageDir(analyzedInterval);
            File xsltDir = new File(periodDir, "xslt");

            if (!xsltDir.exists()) {
                AdeFileUtils.createDirs(xsltDir);
            }

            File inputXsltDir = Ade.getAde().getConfigProperties().getXsltDir();
            for (String resource : getXSLResources()) {
                File resourceOutputFile = new File(xsltDir, resource);
                if (resourceOutputFile.exists()) {
                    continue;
                }

                final File xslResourceFile;
                if (inputXsltDir != null) {
                    xslResourceFile = new File(inputXsltDir, resource);
                } else {
                    URL xslResourceUrl = Ade.class.getResource("/xml/" + resource);
                    try {
                        xslResourceFile = new File(xslResourceUrl.toURI());
                    } catch (URISyntaxException e) {
                        throw new AdeInternalException("could not transform URL to URI: " + xslResourceUrl, e);
                    }
                }

                AdeFileUtils.copyFile(xslResourceFile, resourceOutputFile);
            }
        }
    }

    /**
     * Whether the XSL directory should be created.
     * @return
     * @throws AdeException
     */
    public abstract Boolean isCreateXSLDirectory() throws AdeException;
}
