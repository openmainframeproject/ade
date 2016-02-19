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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.openmainframe.ade.output.AnalyzedIntervalOutputer;

public class ExtAnalyzedIntervaFast18lXmlStorer extends AnalyzedIntervalOutputer {

    private File m_outFile = null;
    private PrintStream m_outStream;
    @Property(key = "outputTimeZone", help = "Time zone used to output analysed intervals", required = false)
    private TimeZone m_outputTimeZone = DateTimeUtils.GMT_TIMEZONE;
    private GregorianCalendar m_gc;
    private static DatatypeFactory s_dataTypeFactory = null;
    private ISource m_source;
    private int m_offset;
    private FramingFlowType m_framingFlowType;

    @Override
    public void setupSourceAndFlowType(ISource source,
            FramingFlowType framingFlowType) throws AdeException {
        m_source = source;
        if (s_dataTypeFactory == null) {
            try {
                s_dataTypeFactory = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException e) {
                throw new AdeInternalException("Failed to instantiate data factory for calendar", e);
            }
        }

        // FIXME this is not thread safe
        m_gc = new GregorianCalendar(m_outputTimeZone);
        m_framingFlowType = framingFlowType;
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        // TODO Auto-generated method stub

    }

    @Override
    public void incomingObject(IAnalyzedInterval analyzedInterval) throws AdeException,
            AdeFlowException {
        openFile(analyzedInterval);

        m_offset = 0;
        printHeader();
        printOpenInterval();
        m_offset += 4;
        printIntervalInfo(analyzedInterval);
        for (IAnalyzedMessageSummary ams : analyzedInterval.getAnalyzedMessages()) {
            printMessage(ams);
        }
        m_offset -= 4;
        printCloseInterval();
        printFooter();
        closeFile();
    }

    protected void openFile(IAnalyzedInterval analyzedInterval)
            throws AdeException {
        /* Get the XML Filename.  The filename in the OutputFilenameGenerator is for 3X XML file. 
         * We need to convert the filename into 1.8 XML file.  */
        m_outFile = getInterval18XMLFile(analyzedInterval);

        if (m_verbose) {
            System.out.println("saving xml in " + m_outFile.getAbsolutePath());
        }

        try {
            File parentdir = m_outFile.getParentFile();
            parentdir.mkdirs();

            m_outStream = new PrintStream(m_outFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void closeFile() {
        m_outStream.close();
    }

    private void printHeader() {
        out("<?xml version='1.0' encoding='UTF-8' ?>");
        out("<?xml-stylesheet href='./xslt/AdeCoreInterval.xsl' type='text/xsl' ?>");
    }

    private void printOpenInterval() {
        out("<interval xsi:noNamespaceSchemaLocation=\"xslt/AdeCoreInterval.xsd\" xmlns=\"http://www.example.org/AdeCoreInterval\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");

    }

    @SuppressWarnings("deprecation")
    private void printIntervalInfo(IAnalyzedInterval ai) throws AdeException {
        out("<sys_id>" + m_source + "</sys_id>");
        m_gc.setTimeInMillis(ai.getInterval().getIntervalStartTime());
        out("<start_time>" + s_dataTypeFactory.newXMLGregorianCalendar(m_gc) + "</start_time>");
        m_gc.setTimeInMillis(ai.getInterval().getIntervalEndTime());
        out("<end_time>" + s_dataTypeFactory.newXMLGregorianCalendar(m_gc) + "</end_time>");
        out("<anomaly_score>" + ai.getScore() + "</anomaly_score>");
        out("<model_internal_id>" + ai.getModelInternalId() + "</model_internal_id>");
        out("<ade_version>" + ai.getAdeVersion().toInt() + "</ade_version>");
    }

    private void printCloseInterval() {
        out("</interval>");
    }

    private void printFooter() {
        // nothing to put here for now, AFAICT
    }

    private void printMessage(IAnalyzedMessageSummary ams) throws AdeException {
        out("<interval_message msg_id=\"" + ams.getMessageId() + "\">");
        m_offset += 4;
        out("<num_instances>" + ams.getNumberOfAppearances() + "</num_instances>");
        out("<bernoulli>" + ams.getStatistics().getDoubleStat("FullBernoulliClusterAwareScore.main") + "</bernoulli>");
        out("<cluster_id>" + ams.getStatistics().getStringStat("ClusteringContextScore.clusterId") + "</cluster_id>");
        out("<poisson>" + ams.getStatistics().getDoubleStat("LogNormalScore.main") + "</poisson>");
        out("<anomaly>" + ams.getStatistics().getDoubleStat("anomaly") + "</anomaly>");
        out("<cluster_status>" + ams.getStatistics().getStringStat("ClusteringContextScore.status") + "</cluster_status>");
        out("<critical_words>" + ams.getStatistics().getDoubleStat("CriticalWordsScorer.mail") + "</critical_words>");
        out("<text_sum>" + ams.getTextSummary() + "</text_sum>");
        out("<text_smp>" + ams.getTextSample() + "</text_smp>");
        printTimeLine(ams);
        m_offset -= 4;
        out("</interval_message>");

    }

    protected void printTimeLine(IAnalyzedMessageSummary ams) {
        out("<time_vec>");
        m_offset += 4;
        for (int i = 0; i < ams.getTimeLine().length; ++i) {
            out("<occ>" + ams.getTimeLine()[i] + "</occ>");
        }
        m_offset -= 4;
        out("</time_vec>");
    }

    private void out(String string) {
        m_outStream.println(String.format("%s" + m_offset + "s%s", "", string));
    }

    @Override
    public void endOfStream() throws AdeException, AdeFlowException {
        // TODO Auto-generated method stub

    }

    /**
     * Return the Interval XML Filename 
     * @return
     * @throws AdeException 
     */
    private File getInterval18XMLFile(IAnalyzedInterval analyzedInterval) throws AdeException {
        ExtOutputFilenameGenerator outputFilenameGenerator = (ExtOutputFilenameGenerator) Ade.getAde().getConfigProperties().getOutputFilenameGenerator();

        File outFile = outputFilenameGenerator.getIntervalXmlV1File(analyzedInterval, m_framingFlowType);

        return outFile;
    }

}