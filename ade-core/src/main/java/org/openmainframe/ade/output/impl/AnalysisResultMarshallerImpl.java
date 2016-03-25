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
package org.openmainframe.ade.output.impl;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.jaxb.AnalyzedIntervalType;
import org.openmainframe.ade.output.IAnalysisResultMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class AnalysisResultMarshallerImpl implements IAnalysisResultMarshaller {

    protected static Logger s_logger = LoggerFactory.getLogger(AnalysisResultMarshallerImpl.class);
    private JAXBContext m_jaxbContext;
    private Marshaller m_intervalMarshaller;
    private Unmarshaller m_intervalUnmarshaller;

    public AnalysisResultMarshallerImpl() throws AdeException {
        try {
            m_jaxbContext = JAXBContext.newInstance("org.openmainframe.ade.impl.jaxb");
            final SchemaFactory sf = SchemaFactory.newInstance(
                    javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);

            m_intervalMarshaller = m_jaxbContext.createMarshaller();
            m_intervalUnmarshaller = m_jaxbContext.createUnmarshaller();
            final File analyzedIntervalXmlURL = new File("conf/xml/AnalyzedInterval.xsd");
            final Schema intervalSchema = sf.newSchema(analyzedIntervalXmlURL);
            m_intervalMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            m_intervalMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            m_intervalMarshaller.setSchema(intervalSchema);

        } catch (JAXBException e) {
            throw new AdeInternalException("JAXB exception ", e);
        } catch (SAXException e) {
            throw new AdeInternalException("SAXE exception ", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final AnalyzedIntervalType loadAnalyzedInterval(File f)
            throws AdeException {
        if (!f.exists()) {
            throw new AdeInternalException("The analzyed interval XML file does not exist. Expected to be at: " + f.getAbsolutePath());
        }

        JAXBElement<AnalyzedIntervalType> analyzedIntervalElement;
        try {
            analyzedIntervalElement = (JAXBElement<AnalyzedIntervalType>) m_intervalUnmarshaller.unmarshal(f);
        } catch (JAXBException e) {
            throw new AdeInternalException("JAXB exception occured in file: " + f.getAbsolutePath(), e);
        }
        return analyzedIntervalElement.getValue();
    }

}
