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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.ext.service.AdeExtUsageException;
import org.openmainframe.ade.ext.xml.v2.Interval;
import org.openmainframe.ade.ext.xml.v2.IntervalMessageType;
import org.openmainframe.ade.ext.xml.v2.IntervalTimeVectorType;
import org.openmainframe.ade.ext.xml.v2.RuleType;
import org.openmainframe.ade.ext.xml.v2.Interval.ModelInfo;
import org.openmainframe.ade.ext.xml.v2.Interval.MsgSummary;
import org.openmainframe.ade.ext.xml.v2.IntervalMessageType.Bernoulli;
import org.openmainframe.ade.ext.xml.v2.IntervalMessageType.Periodicity;
import org.openmainframe.ade.ext.xml.v2.IntervalMessageType.Poisson;
import org.openmainframe.ade.ext.xml.v2.types.PeriodicityStatus;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static org.openmainframe.ade.ext.output.StatisticsChartConstants.*;

/**
 * Store the Version 2 of Interval XML file using JAXB
 *
 */
public class ExtJaxbAnalyzedIntervalV2XmlStorer extends ExtendedAnalyzedIntervalXmlStorer {
    /**
     * Whether the xsl directory should be created
     */
    @Property(key = "createXSLDirectory", help = "Whether to create the XSL directory with files associated with this XML.", required = false)
    private boolean m_createXSLDirectory = false;

    /**
     * Whether the XML output should be formatted
     */
    @Property(key = "formatXMLOutput", help = "Whether to format the XML output.", required = false)
    private boolean m_formatXMLOutput = false;

    /**
     * The logger object
     */
    private static Logger s_logger = LoggerFactory.getLogger(ExtJaxbAnalyzedIntervalV2XmlStorer.class);

    /**
     * formatter to round number to single digit
     */
    private final static DecimalFormat SingleDigitFormatter = new DecimalFormat("#.#");

    /**
     * The directory of the XSL file, this will be outputted to the XML header.
     */
    private static final String XSL_FILENAME = "./xslt/AdeCoreIntervalV2.xsl";

    /**
     * JAXB context
     *
     * Note: JAXB context cannot be defined at Package level (i.e. org.openmainframe.ade.z.xml),
     * and ObjectFactory cannot be included.
     * When JAXB context is defined at package level or ObjectFactory is included,
     * all the classes under the package or listed in ObjectFactory will be considered.
     * i.e. if our goal is to marshall the Interval XML file, the Index XML file will also
     * be "considered".
     *
     * Since Interval XML file and Index XML file has two different namespace,
     * both namespace will be included in the XML file (one as default, one as ns2).
     */
    @SuppressWarnings("rawtypes")
    private static final Class[] ADEEXT_JAXB_CONTEXT = { Interval.class, ModelInfo.class, MsgSummary.class,
            IntervalMessageType.class, Bernoulli.class, Periodicity.class, Poisson.class, IntervalTimeVectorType.class,
            RuleType.class };

    public static final String XML_INTERVAL_V2_XSD = "/xml/AdeCoreIntervalV2.xsd";

    /**
     * The marshaller object
     */
    protected static Marshaller s_marshaller;

    /**
     * The XML Version
     */
    private static final int XML_VERSION = 2;

    /**
     * The list of xsl resources to be copied to the Interval XML XSL directory.
     * This include all the XSL resources from the super class, and this class.
     */
    static String[] s_xslResources = null;

    static final String[] s_thisXSLResources = { "AdeCoreIntervalV2.xsl", "global.css" };

    /**
     * The model meta-data.
     */
    private XMLMetaDataRetriever m_xmlMetaData;
    
    /**
     * The latest output filename;
     */
    private File outFile;

    /**
     * Default constructor
     * @throws AdeException
     */
    public ExtJaxbAnalyzedIntervalV2XmlStorer() throws AdeException {
        super();
        m_xmlMetaData = new XMLMetaDataRetriever();
    }

    /**
     * Copy the XSL Resources
     */
    @Override
    protected String[] getXSLResources() {
        if (s_xslResources == null) {
            HashSet<String> allXSLResourcesSet = new HashSet<String>();
            List<String> thisXSLResourcesList = Arrays.asList(s_thisXSLResources);

            allXSLResourcesSet.addAll(thisXSLResourcesList);

            s_xslResources = new String[allXSLResourcesSet.size()];
            s_xslResources = allXSLResourcesSet.toArray(s_xslResources);
        }

        return s_xslResources;
    }

    /**
     * Get the intervalXMLFile
     * @param analyzedInterval
     * @return
     * @throws AdeException
     */
    private File getIntervalV2XMLFile(IAnalyzedInterval analyzedInterval) throws AdeException {
        ExtOutputFilenameGenerator outputFilenameGenerator = (ExtOutputFilenameGenerator) Ade.getAde()
                .getConfigProperties().getOutputFilenameGenerator();

        return outputFilenameGenerator.getIntervalXmlV2File(analyzedInterval, m_framingFlowType);
    }

    /**
     * Begin of Stream
     */
    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        if (s_marshaller == null) {
            JAXBContext jaxbContext;
            try {
                jaxbContext = JAXBContext.newInstance(ADEEXT_JAXB_CONTEXT);
            } catch (JAXBException e) {
                throw new AdeInternalException("failed to create JAXBContext object for package "
                        + Arrays.toString(ADEEXT_JAXB_CONTEXT), e);
            }
            try {
                s_marshaller = jaxbContext.createMarshaller();
            } catch (JAXBException e) {
                throw new AdeInternalException("failed to create JAXB Marshaller object", e);
            }
            try {
                s_marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, m_formatXMLOutput);
                s_marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
                s_marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, XML_INTERVAL_V2_XSD);

            } catch (PropertyException e) {
                throw new AdeInternalException("failed to set formatted output for JAXB Marshaller object", e);
            }

            SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);

            File xmlParent = Ade.getAde().getConfigProperties().getXsltDir().getAbsoluteFile();
            xmlParent = xmlParent.getParentFile();
            File intervalSchema = new File(xmlParent, XML_INTERVAL_V2_XSD);

            Schema schema;
            try {
                URL analyzedIntervalSchema = intervalSchema.toURI().toURL();
                schema = sf.newSchema(analyzedIntervalSchema);
            } catch (SAXException e) {
                throw new AdeInternalException("failed to create XML Schemal for event log analysis results", e);
            } catch (MalformedURLException e) {
                throw new AdeInternalException("failed to create URL from Schema path: "
                        + intervalSchema.getAbsolutePath(), e);
            }
            s_marshaller.setSchema(schema);
        }
        m_xmlMetaData = new XMLMetaDataRetriever();
    }

    /**
     * Override superclass to use the new JAXB classes.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void incomingObject(IAnalyzedInterval analyzedInterval) throws AdeException, AdeFlowException {
        if (analyzedInterval.getInterval().getSource().getSourceId().equals(m_source)) {
            throw new AdeFlowException("Cannot process analyzed interval of source other than "
                    + m_source.getSourceId() + ". Got analyzed interval from source "
                    + analyzedInterval.getInterval().getSource().getSourceId());
        }
        /* Initialize the MetaData */
        long intervalSizeInMillis = analyzedInterval.getInterval().getIntervalSize();

        /* Note: Checking model version is not required here.  This method is used to output the analysis result
         * using an existing model.  If the model is not understandable by our code, this method would not have been
         * called.
         */
        m_xmlMetaData.retrieveXMLMetaData(analyzedInterval.getModelInternalId(), true, intervalSizeInMillis);

        /* Create the XSLT Directory */
        createXsltDirectory(analyzedInterval);

        /* Initialize the data structures used by JAXB */
        Interval jaxbInterval = new Interval();
        jaxbInterval.setModelInternalId(analyzedInterval.getModelInternalId());
        jaxbInterval.setVersion(XML_VERSION);
        jaxbInterval.setAdeVersion(analyzedInterval.getAdeVersion().toInt());
        jaxbInterval.setSysId(m_source.getSourceId());

        m_gc.setTimeInMillis(analyzedInterval.getInterval().getIntervalStartTime());
        jaxbInterval.setStartTime(s_dataTypeFactory.newXMLGregorianCalendar(m_gc));
        m_gc.setTimeInMillis(analyzedInterval.getInterval().getIntervalEndTime());
        jaxbInterval.setEndTime(s_dataTypeFactory.newXMLGregorianCalendar(m_gc));

        double value = Double.valueOf(SingleDigitFormatter.format(analyzedInterval.getScore() * 100));
        jaxbInterval.setAnomalyScore(value);

        jaxbInterval.setGmtOffset(m_xmlMetaData.getGMTOffset(m_source.getSourceId()));

        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setAnalysisGroup(m_xmlMetaData.getAnalysisGroupName());
        modelInfo.setIntervalSizeInSec(m_xmlMetaData.getIntervalLengthInSeconds());
        modelInfo.setModelCreationDate(m_xmlMetaData.getModelCreationDate());
        modelInfo.setTrainingPeriod(m_xmlMetaData.getNumberOfDaysInTraining());
        modelInfo.setLimitedModel(m_xmlMetaData.getLimitedModelIndicator().toString());

        jaxbInterval.setModelInfo(modelInfo);

        /* Add each message */
        int numberOfNewMessages = 0;
        List<IntervalMessageType> jaxbMessageTypeList = jaxbInterval.getIntervalMessage();
        Collection<IAnalyzedMessageSummary> sortedMessages = getSortedMessages(analyzedInterval);
        for (IAnalyzedMessageSummary analyzedMessageSummary : sortedMessages) {
            IntervalMessageType jaxbIntervalMessage = processAnalyzedMessageSummary(analyzedMessageSummary);
            jaxbMessageTypeList.add(jaxbIntervalMessage);

            StatisticsChart statChart = analyzedMessageSummary.getStatistics();            
            String clusterStatus = statChart.getStringStatOrThrow(ClusteringContextScore_status); 
            
            if (clusterStatus.equalsIgnoreCase("New")){
                numberOfNewMessages++;
            }    
        }

        /* MsgSummary complex type */
        MsgSummary msgSummary = new MsgSummary();
        msgSummary.setNumNewMsg(numberOfNewMessages);
        jaxbInterval.setMsgSummary(msgSummary);

        writeToXML(analyzedInterval, jaxbInterval, s_marshaller);
    }

    /**
     * Output the content to a XML file.  This method intended for override by subclass
     * to customize the XML output format.
     *
     * @param analyzedInterval
     * @param jaxbInterval
     * @param marshaller
     * @param source
     * @throws AdeException
     */
    protected void writeToXML(IAnalyzedInterval analyzedInterval, Interval jaxbInterval, Marshaller marshaller)
            throws AdeException {
        outFile = getIntervalV2XMLFile(analyzedInterval);

        /* Write the results to a temporary file and rename the file into
         * its final destination after the write is complete and successful.
         * This lessens the chance for an incomplete results file to be
         * created.
         *
         * Create the temporary file in the same directory as the eventual
         * destination to help avoid potential failures of the renameTo()
         * method that might result if the temporary file were on a separate
         * filesystem.
         */
        File tempOutputFile = new File(outFile.getParent(), outFile.getName() + ".tmp");
        if (m_verbose) {
            System.out.println("saving xml in " + outFile.getAbsolutePath());
        }
        OutputStreamWriter xmlStreamWriter;
        try {
            File parentdir = outFile.getParentFile();
            parentdir.mkdirs();
            xmlStreamWriter = new OutputStreamWriter(new FileOutputStream(tempOutputFile), "UTF-8");
            xmlStreamWriter.write("<?xml version='1.0' encoding='UTF-8' ?> \n");
            xmlStreamWriter.write("<?xml-stylesheet href='" + XSL_FILENAME + "' type='text/xsl' ?> \n");

        } catch (IOException e) {
            throw new AdeInternalException("Failed to create xml file for interval " + outFile.getName()
                    + " of source " + m_source.getSourceId(), e);
        }
        try {
            marshaller.marshal(jaxbInterval, xmlStreamWriter);
            xmlStreamWriter.close();
            xmlStreamWriter = null;

            if (outFile.exists()) {
                /* Delete the renameTo file.  If it exist, rename will fail */
                outFile.delete();
            }
            if (!tempOutputFile.renameTo(outFile)) {
                s_logger.error("failed to rename " + tempOutputFile.getName() + " to " + outFile.getName());
                throw new IOException("failed to rename " + tempOutputFile.getName() + " to " + outFile.getName());
            }

        } catch (JAXBException|IOException e) {
            throw new AdeInternalException("Failed to write xml file for interval " + outFile.getName()
                    + " of source " + m_source.getSourceId(), e);
        } finally {
            org.apache.commons.io.FileUtils.deleteQuietly(tempOutputFile);
        }
    }

    /**
     * Calculates the scaled bernoulli score according to the definition defined by Anomaly Detection 
     * Engine.
     * @param stats The statistics chart
     * @param clusterStatus The status of the cluster.
     * @param FullBernoulliClusterAwareScore_rawAnomaly String.
     * @return bernoulliScore Double
     * @throws AdeException
     */

    private double getScaledBernoulliScore(StatisticsChart stats, String clusterStatus, String fullBernoulliClusterAwareScore) throws AdeException {
        double bernoulliScore;
        if (clusterStatus.equals("NEW")) {
            bernoulliScore = 101;
        } else {
            bernoulliScore = stats.getDoubleStatOrThrow(fullBernoulliClusterAwareScore) * 100;
            if (bernoulliScore < 1) {
                bernoulliScore = 1;
            }
        }
        return bernoulliScore;
    }

    /**
     * @param scoreSet
     * @param stats
     * @throws AdeInternalException
     * @throws AdeExtUsageException
     */
    private void processStatisticsChart(IntervalMessageType jaxbMessageType, StatisticsChart stats)
            throws AdeException {

        Double intCont = stats.getDoubleStatOrThrow(LogProb);
        jaxbMessageType.setIntCont(intCont);

        double normIntCont = stats.getDoubleStatOrThrow(LogProb);
        jaxbMessageType.setNormIntCont(normIntCont);

        double anomaly = stats.getDoubleStatOrThrow(Anomaly);
        if (anomaly < 0) {
            anomaly = 0;
        }
        jaxbMessageType.setAnomaly(anomaly);

        String clusterStatus = stats.getStringStatOrThrow(ClusteringContextScore_status);
        jaxbMessageType.setClusterStatus(clusterStatus);

        double criticalWords = stats.getDoubleStatOrThrow(CriticalWordCountReporter_main);
        jaxbMessageType.setCriticalWords(criticalWords);

        /* Poisson complext type */
        Poisson poisson = new Poisson();
        jaxbMessageType.setPoisson(poisson);

        double poissonScore = stats.getDoubleStatOrThrow(LogNormalScore_main);
        poisson.setValue(poissonScore);

        double poissonMean = stats.getDoubleStatOrThrow(LogNormalScore_mean);
        poisson.setMean(poissonMean);

        /* Bernoulli complex type */
        Bernoulli bernoulli = new Bernoulli();
        jaxbMessageType.setBernoulli(bernoulli);

        double bernoulliScore = getScaledBernoulliScore(stats, clusterStatus, FullBernoulliClusterAwareScore_rawAnomaly);
        bernoulli.setValue(bernoulliScore);

        double bernoulliFrequency = stats.getDoubleStatOrThrow(FullBernoulliClusterAwareScore_frequency);
        if (bernoulliFrequency == 0) {
            bernoulliFrequency = 0;
        } else {
            bernoulliFrequency = m_xmlMetaData.getNumberOfIntervalsInADay() / bernoulliFrequency;
        }
        bernoulli.setFrequency(bernoulliFrequency);

        int clusterId = (int) stats.getDoubleStatOrThrow(ClusteringContextScore_clusterId);
        jaxbMessageType.setClusterId(clusterId);

        /* Periodicity complex type */
        Periodicity periodicity = new Periodicity();
        jaxbMessageType.setPeriodicity(periodicity);

        Double periodicityScore = stats.getDoubleStat(LastSeenScorer_LogProbGivenLast);
        if (periodicityScore != null) {
            periodicity.setScore(periodicityScore);
        }

        PeriodicityStatus periodicityStatus;
        String lastSeenLoggingScorerContinuous = stats.getStringStatOrThrow(LastSeenLoggingScorerContinuous_Main);
        if (periodicityScore != null) {
            if (periodicityScore > 0.7) {
                periodicityStatus = PeriodicityStatus.IN_SYNC;
            } else {
                periodicityStatus = PeriodicityStatus.NOT_IN_SYNC;
            }
        } else if (lastSeenLoggingScorerContinuous.equalsIgnoreCase("New") ||
                lastSeenLoggingScorerContinuous.equalsIgnoreCase("neverSeenBefore")) {
            periodicityStatus = PeriodicityStatus.NEW;
        } else {
            periodicityStatus = PeriodicityStatus.NON_PERIODIC;
        }
        periodicity.setStatus(periodicityStatus.toString());

        String periodicityLastIssueString = stats.getStringStat(LastSeenLoggingScorerContinuous_LastTime);
        if (periodicityLastIssueString != null) {
            Calendar periodicityLastIssueCalendar = DatatypeConverter.parseDateTime(periodicityLastIssueString);
            m_gc.setTimeInMillis(periodicityLastIssueCalendar.getTimeInMillis());
            XMLGregorianCalendar periodicityLastIssue = s_dataTypeFactory.newXMLGregorianCalendar(m_gc);
            periodicity.setLastIssued(periodicityLastIssue);
        }

    }

    /**
     * @param analyzedMessageSummary
     * @return
     * @throws AdeException
     * @throws AdeInternalException
     */
    private IntervalMessageType processAnalyzedMessageSummary(IAnalyzedMessageSummary analyzedMessageSummary)
            throws AdeException, AdeInternalException {
        IntervalMessageType jaxbMessageType = new IntervalMessageType();
        jaxbMessageType.setMsgId(analyzedMessageSummary.getMessageId());
        jaxbMessageType.setAnomaly(analyzedMessageSummary.getFinalAnomaly());
        jaxbMessageType.setNumInstances(analyzedMessageSummary.getNumberOfAppearances());
        jaxbMessageType.setTextSum(analyzedMessageSummary.getTextSummary());
        String cleanedTextSample = cleanTextSample(analyzedMessageSummary.getTextSample());
        jaxbMessageType.setTextSmp(cleanedTextSample);

        /* Time vector */
        short[] timeLine = analyzedMessageSummary.getTimeLine();
        IntervalTimeVectorType tl = processTimeLine(timeLine);
        jaxbMessageType.setTimeVec(tl);

        /* Scorers and it's value */
        StatisticsChart messageSummaryStats = analyzedMessageSummary.getStatistics();
        processStatisticsChart(jaxbMessageType, messageSummaryStats);

        return jaxbMessageType;
    }

    /**
     * @param timeLine
     * @return
     */
    private IntervalTimeVectorType processTimeLine(short[] timeLine) {
        IntervalTimeVectorType jaxbTimeVector = new IntervalTimeVectorType();
        List<Integer> jaxbOccrrence = jaxbTimeVector.getOcc();
        for (short occurrence : timeLine) {
            jaxbOccrrence.add((int) occurrence);
        }
        return jaxbTimeVector;
    }

    /**
     * Whether to create the xsl directory
     * @return
     * @throws AdeException
     */
    @Override
    public Boolean isCreateXSLDirectory() throws AdeException {
        return m_createXSLDirectory;
    }
}
