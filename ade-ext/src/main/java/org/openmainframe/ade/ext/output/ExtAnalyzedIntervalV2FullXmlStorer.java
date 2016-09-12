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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.impl.jaxb.AnalyzedIntervalType;
import org.openmainframe.ade.impl.jaxb.ObjectFactory;

/**
 * This class take the output XML data from the Ade 3.1 (Called V2 Full Format), write it out to the
 * output filename.
 */
public class ExtAnalyzedIntervalV2FullXmlStorer extends ExtendedAnalyzedIntervalXmlStorer {
    
    /**
     * Whether the output should be in GZip format
     */
    @Property(key = "outputInGZipFormat", help = "Whether the output should be in GZip format.  If false, output will be in XML.", required = false)
    private boolean m_outputInGZipFormat = true;

    /**
     * Whether the xsl directory should be created
     */
    @Property(key = "createXSLDirectory", help = "Whether to create the XSL directory with files associated with this XML.", required = false)
    private boolean m_createXSLDirectory = false;

    /**
     * The directory of the XSL file, this will be outputted to the XML header.
     */
    private static final String XSL_FILENAME = "./xslt/AnalyzedInterval.xsl";

    /**
     * The list of xsl resources to be copied to the Interval XML XSL directory.
     * This include all the XSL resources from the super class, and this class.
     */
    private static final String[] s_thisXSLResources = { "AnalyzedInterval.xsl", "global.css" };

    /**
     * Default constructor
     * @throws AdeException 
     */
    public ExtAnalyzedIntervalV2FullXmlStorer() throws AdeException {
        super();
    }

    /**
     * Beginning of stream
     */
    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        super.beginOfStream();

        /* Overwrite the Namespace Schema location */
        try {
            s_marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "xslt/AnalyzedInterval.xsd");
        } catch (PropertyException e) {
            throw new AdeInternalException("failed to set formatted output for JAXB Marshaller object", e);
        }
        m_xmlMetaData = new XMLMetaDataRetriever();
    }

    public void incomingObject(IAnalyzedInterval analyzedInterval) throws AdeException,
            AdeFlowException { 
        /* Do not write out the V2F file until the end of Hardening interval */
        long intervalEndTime = analyzedInterval.getInterval().getIntervalEndTime();
        if (intervalEndTime % XMLUtil.getXMLHardenedDurationInMillis(m_framingFlowType) != 0) {
            return;
        }

        super.incomingObject(analyzedInterval);
    }

    /**
     * Output the content to a XML file.  This method intended for override by subclass 
     * to customize the XML output format.
     * 
     * @param analyzedInterval 
     * @param jaxbAnalyzedInterval
     * @param marshaller 
     * @param source
     * @throws AdeException 
     */
    @Override
    protected void writeToXML(IAnalyzedInterval analyzedInterval, AnalyzedIntervalType jaxbAnalyzedInterval,
            Marshaller marshaller) throws AdeException {
        /* Get the Full XML Filename */
        File outFile = getIntervalV2FullXMLFile(analyzedInterval, m_outputInGZipFormat);

        if (m_verbose) {
            System.out.println("saving Ade V2 full xml in " + outFile.getAbsolutePath());
        }
        Writer xmlStreamWriter = null;
        FileOutputStream fos = null;
        GZIPOutputStream zos = null;
        try {
            File parentdir = outFile.getParentFile();
            parentdir.mkdirs();
            outFile.createNewFile();

            if (m_outputInGZipFormat) {
                fos = new FileOutputStream(outFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                zos = new GZIPOutputStream(bos);

                xmlStreamWriter = new PrintWriter(zos);
            } else {
            	fos = new FileOutputStream(outFile);
                xmlStreamWriter = new OutputStreamWriter(fos, "UTF-8");
            }
            xmlStreamWriter.write("<?xml version='1.0' encoding='UTF-8' ?> \n");
            xmlStreamWriter.write("<?xml-stylesheet href='" + XSL_FILENAME + "' type=\"text/xsl\" ?> \n");
            
            ObjectFactory factory = new ObjectFactory();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(factory.createAnalyzedInterval(jaxbAnalyzedInterval), xmlStreamWriter);

        } catch (IOException e) {
            throw new AdeInternalException("Failed to create xml file for interval " + outFile.getName()
                    + " of source " + m_source.getSourceId(), e);
        } catch (JAXBException e) {
            throw new AdeInternalException("Failed to write xml file for interval " + outFile.getName()
                    + " of source " + m_source.getSourceId(), e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    throw new AdeInternalException("Failed to close ZIPOutputStream: " + outFile.getName(), e);
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    throw new AdeInternalException("Failed to close FileOutputStream: " + outFile.getName(), e);
                }
            }
            if (xmlStreamWriter != null) {
                try {
                    xmlStreamWriter.close();
                } catch (IOException e) {
                    throw new AdeInternalException("Failed to close file: " + outFile.getName(), e);
                }
            }
        }
    }

    /**
     * XSL Resources
     */
    @Override
    protected String[] getXSLResources() {
        return s_thisXSLResources;
    }

    /**
     * Return the Interval XML Filename 
     * @return
     * @throws AdeException 
     */
    private File getIntervalV2FullXMLFile(IAnalyzedInterval analyzedInterval, boolean isZip) throws AdeException {
        ExtOutputFilenameGenerator outputFilenameGenerator = (ExtOutputFilenameGenerator) Ade.getAde()
                .getConfigProperties().getOutputFilenameGenerator();

        File outFile;
        if (isZip) {
            outFile = outputFilenameGenerator.getIntervalXmlV2FFileInZIP(analyzedInterval, m_framingFlowType);
        } else {
            outFile = outputFilenameGenerator.getIntervalXmlV2FFile(analyzedInterval, m_framingFlowType);
        }

        return outFile;
    }

    /**
     * Whether to create the xsl directory 
     */
    @Override
    public Boolean isCreateXSLDirectory() throws AdeException {
        return m_createXSLDirectory;
    }

}
