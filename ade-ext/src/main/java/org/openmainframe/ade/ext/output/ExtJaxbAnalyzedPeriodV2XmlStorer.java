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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.ext.utils.EXT_TABLES_SQL;
import org.openmainframe.ade.ext.xml.v2.Systems;
import org.openmainframe.ade.ext.xml.v2.SystemsIntervalType;
import org.openmainframe.ade.ext.xml.v2.SystemsSystemType;
import org.openmainframe.ade.ext.xml.v2.Systems.ModelInfo;
import org.openmainframe.ade.ext.xml.v2.Systems.NumberIntervals;
import org.openmainframe.ade.ext.xml.v2.types.ExtLimitedModelIndicator;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.impl.data.PeriodImpl;
import org.openmainframe.ade.impl.data.PeriodUtils;
import org.openmainframe.ade.impl.dataStore.DatastorePeriodAndSerialNumFinder;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.impl.dbUtils.QueryPreparedStatementExecuter;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.FileUtils;
import org.openmainframe.ade.output.AnalyzedIntervalOutputer;
import org.openmainframe.ade.utils.AdeFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Stores the interval analysis results (score and number of messages ids) in XML format. The results can be later read and be visualized.
 */
public class ExtJaxbAnalyzedPeriodV2XmlStorer extends AnalyzedIntervalOutputer {
    /**
     * Whether index.xml file should be refreshed every 2 minutes
     */
    @Property(key = "outputOnTheFly", help = "Refresh the index file with every new interval.", required = false)
    private boolean m_refreash_periods = false;

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
    private static Logger s_logger = LoggerFactory.getLogger(ExtJaxbAnalyzedPeriodV2XmlStorer.class);

    /**
     * formatter to round number to single digit
     */
    private final static DecimalFormat SingleDigitFormatter = new DecimalFormat("#.#");

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
    private static final Class[] ADEEXT_JAXB_CONTEXT = { Systems.class, ModelInfo.class, NumberIntervals.class,
            SystemsIntervalType.class, SystemsSystemType.class };

    /**
     * The schema file
     */
    public static final String XML_PLEX_V2_XSD = "/xml/AdeCorePlexV2.xsd";

    /**
     * The directory of the XSL file, this will be outputted to the XML header.
     */
    private static final String XSL_FILENAME = "./xslt/AdeCorePlexV2.xsl";

    /**
     * The list of all XSLT resources
     */
    static final String[] s_xslResources = { "AdeCorePlexV2.xsl", "global.css" };

    /**
     * Reason for missing intervals
     */
    private static final String MISSING_INTERVAL_REASON_NO_CONNECTION = "not connected";

    /**
     * The marshaller object
     */
    protected static Marshaller s_marshaller;

    /**
     * The XML Version
     */
    private static final int XML_VERSION = 2;

    /**
     * The model meta data
     */
    private XMLMetaDataRetriever m_xmlMetaData;

    /**
     * The output filename
     */
    private String m_outputFileName;

    /**
     * The period directory
     */
    private File m_periodDir;

    /**
     * Number of intervals for the current period
     */
    private int m_numIntervals;

    /**
     * The startDate of the current period
     */
    private Date m_periodStartDate;

    /**
     * The cachedPeriod
     */
    private PeriodImpl m_cachedPeriod;

    /**
     * The last know model internal ID
     */
    private Integer m_lastKnownModelInternalID = -1;

    /**
     * GregorianCalendar object to generate XML date
     */
    private GregorianCalendar m_gc;

    /**
     * Date factory
     */
    private static DatatypeFactory s_dataTypeFactory = null;

    /**
     * The source this period represent
     */
    private ISource m_source;

    /**
     * the framing flow type
     */
    private FramingFlowType m_framingFlowType;

    /**
     * The period finder
     */
    protected DatastorePeriodAndSerialNumFinder m_periodFinder;

    /**
     * Array containing data for the interval
     */
    private AnalyzedIntervalData[] m_aiVec;

    /**
     * Whether we are currently in a period.
     */
    protected boolean m_inPeriod = false;

    /**
     * Class storing interval data
     */
    static protected class AnalyzedIntervalData {
        protected String m_results_file;

        protected int m_num_unique_msg_ids;

        protected int m_numNewMessages;

        protected int m_numNeverSeenBeforeMessages;

        protected double m_anomaly_score;

        protected ExtLimitedModelIndicator m_modelQualityIndicator = ExtLimitedModelIndicator.Unknown;

        public AnalyzedIntervalData() {
            super();
        }

        public void clear() {
            m_num_unique_msg_ids = 0;
            m_results_file = "";
            m_anomaly_score = 0.0;
            m_modelQualityIndicator = ExtLimitedModelIndicator.Unknown;
            m_numNewMessages = 0;
            m_numNeverSeenBeforeMessages = 0;
        }

        public int getNumUniqueMsgIds() {
            return m_num_unique_msg_ids;
        }

        public int getNumNewMessages() {
            return m_numNewMessages;
        }

        public int getNumNeverSeenBeforeMessages() {
            return m_numNeverSeenBeforeMessages;
        }

        public double getAnomalyScore() {
            return m_anomaly_score;
        }

        public String getResultFile() {
            return m_results_file;
        }

        public ExtLimitedModelIndicator getModelQualityIndicator() {
            return m_modelQualityIndicator;
        }

        public void setNumUniqueMsgIds(int numUniqueMsgIds) {
            m_num_unique_msg_ids = numUniqueMsgIds;
        }

        public void setNumNewMessages(int numNewMessages) {
            m_numNewMessages = numNewMessages;
        }

        public void setNumNeverSeenBeforeMessages(int numNeverSeenBeforeMessages) {
            m_numNeverSeenBeforeMessages = numNeverSeenBeforeMessages;
        }

        public void setAnomalyScore(double anomalyScore) {
            m_anomaly_score = anomalyScore;
        }

        public void setResultFile(String resultFile) {
            m_results_file = resultFile;
        }

        public void setModelQualityIndicator(ExtLimitedModelIndicator modelQualityIndicator) {
            m_modelQualityIndicator = modelQualityIndicator;
        }

    }

    /**
     * Default constructor
     * @throws AdeException
     */
    public ExtJaxbAnalyzedPeriodV2XmlStorer() throws AdeException {
        super();
        m_gc = new GregorianCalendar();
        try {
            s_dataTypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new AdeInternalException("Failed to instantiate data factory for calendar", e);
        }
    }

    /**
     * Setup the source and flowlayout
     */
    @Override
    public void setupSourceAndFlowType(ISource source, FramingFlowType framingFlowType) throws AdeException {
        m_source = source;
        m_framingFlowType = framingFlowType;
        m_xmlMetaData = new XMLMetaDataRetriever();
        /* Use the new period finder */
        m_periodFinder = new DatastorePeriodAndSerialNumFinder(m_source, m_framingFlowType,
                XMLUtil.getXMLHardenedDurationInMillis(framingFlowType));
        m_aiVec = null;
    }

    /**
     * Whether we want to create the xsl directory
     */
    public Boolean isCreateXSLDirectory() throws AdeException {
        return m_createXSLDirectory;
    }

    /**
     * Create the XSLT Directory
     * @param analyzedInterval
     * @throws AdeException
     */
    protected void createXsltDirectory(IAnalyzedInterval analyzedInterval) throws AdeException {
        if (isCreateXSLDirectory()) {
            File xsltDir = new File(m_periodDir, "xslt");

            if (!xsltDir.exists()) {
                AdeFileUtils.createDirs(xsltDir);
                File inputXsltDir = Ade.getAde().getConfigProperties().getXsltDir();
                for (String resource : s_xslResources) {
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
                s_marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, XML_PLEX_V2_XSD);
            } catch (PropertyException e) {
                throw new AdeInternalException("failed to set formatted output for JAXB Marshaller object", e);
            }

            SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);

            File xmlParent = Ade.getAde().getConfigProperties().getXsltDir().getAbsoluteFile();
            xmlParent = xmlParent.getParentFile();
            File systemSchema = new File(xmlParent, XML_PLEX_V2_XSD);

            Schema schema;
            try {
                URL systemSchemaURL = systemSchema.toURI().toURL();
                schema = sf.newSchema(systemSchemaURL);
            } catch (SAXException e) {
                throw new AdeInternalException("failed to create XML Schemal for event log analysis results", e);
            } catch (MalformedURLException e) {
                throw new AdeInternalException("failed to create URL from Schema path: "
                        + systemSchema.getAbsolutePath(), e);
            }
            s_marshaller.setSchema(schema);
        }

        /* Retrieve the Model Data Here.  Force refresh, in case the Model's Analysis Group Change without impacting
         * the model internal ID. */
        m_xmlMetaData.retrieveXMLMetaData(m_lastKnownModelInternalID, true, m_framingFlowType.getDuration());

        /* Write out the period when it's begin of stream, within a period */
        if (m_inPeriod) {
            writePeriod();
        }
    }

    /**
     * Override superclass to use the new JAXB classes.
     */
    @Override
    public void incomingObject(IAnalyzedInterval analyzedInterval) throws AdeException, AdeFlowException {
        if (analyzedInterval.getInterval().getSource().getSourceId().equals(m_source)) {
            throw new AdeFlowException("Cannot process analyzed interval of source other than "
                    + m_source.getSourceId() + ". Got analyzed interval from source "
                    + analyzedInterval.getInterval().getSource().getSourceId());
        }
        Date periodStart = PeriodUtils.getContainingPeriodStart(new Date(analyzedInterval.getIntervalEndTime() - 1));
        if (!periodStart.equals(m_periodStartDate)) {
            if (m_inPeriod) {
                closePeriod();
            }
            startNewPeriod(analyzedInterval);
        }

        int index = getIntervalIndex(analyzedInterval);

        m_lastKnownModelInternalID = analyzedInterval.getModelInternalId();
        /* Retrieve the Model Data Here.  Force refresh, in case the Model's Analysis Group Change without impacting
         * the model internal ID. */
        /* Note: Checking of model version is not required here.  This method is used to output the analysis result
         * using an existing model.  If the model is not understandable by our code, this method would not have been
         * called.
         */
        m_xmlMetaData.retrieveXMLMetaData(m_lastKnownModelInternalID, true, m_framingFlowType.getDuration());

        /* Note: the value in m_aiVec must be the same as the value from AdeCore.  Any manipulation of the
         * value to be output to the XML is done in writePeriod(). */
        m_aiVec[index].m_results_file = Ade.getAde().getConfigProperties().getOutputFilenameGenerator()
                .getIntervalXmlFileRelativeToIndex(analyzedInterval, m_framingFlowType);
        m_aiVec[index].m_num_unique_msg_ids = analyzedInterval.getNumUniqueMessageIds();
        m_aiVec[index].m_anomaly_score = analyzedInterval.getScore();
        m_aiVec[index].m_modelQualityIndicator = m_xmlMetaData.getLimitedModelIndicator();
        NewAndNeverSeenBeforeMessages messages = NewAndNeverSeenBeforeMessagesUtils.processAnalyzedInterval(analyzedInterval);
        m_aiVec[index].m_numNewMessages = messages.getNumNewMessages();
        m_aiVec[index].m_numNeverSeenBeforeMessages = messages.getNumNeverSeenBeforeMessages();

        if (m_refreash_periods) {
            writePeriod();
        }
    }

    /**
     * Handle the the of stream
     */
    @Override
    public void endOfStream() throws AdeException {
        if (m_inPeriod) {
            closePeriod();
        }
    }

    /**
     * Determine the index for the interval.
     */
    protected int getIntervalIndex(IAnalyzedInterval analyzedInterval) throws AdeException {
        m_periodFinder.setIntervalStartTime(analyzedInterval.getIntervalStartTime());

        /* Note: cannot use periodFinder.getLastSerialNum() to get the serial number for partial interval before
         * Ade terminate.  This last interval will have interval start line up to the Analysis Window Length.
         * And, interval end points to the timestamp of last log message processed.
         *
         * periodFinder.getLastSerialNum() will based on the current interval start to determine the serial.  Since XML
         * Hardened interval is different than the analysis Windows, we need to use the interval end time instead.
         */
        return ExtOutputFilenameGenerator.getIntervalSerialNumber(
                analyzedInterval.getIntervalEndTime(), m_framingFlowType);
    }

    /**
     * Handle the starting of a new period.
     *
     * @param ai
     * @throws AdeException
     */
    private void startNewPeriod(IAnalyzedInterval ai) throws AdeException {
        long m_oneIntervalEndTime = ai.getIntervalEndTime() - 1;
        Date intervalEndDate = new Date(m_oneIntervalEndTime);
        m_periodStartDate = PeriodUtils.getContainingPeriodStart(intervalEndDate);
        m_periodFinder.setIntervalStartTime(m_periodStartDate.getTime());
        m_cachedPeriod = m_periodFinder.getLastPeriod();

        m_numIntervals = m_periodFinder.getIntervalsPerPeriod();
        if (m_aiVec == null || m_aiVec.length != m_numIntervals) {
            m_aiVec = new AnalyzedIntervalData[m_numIntervals];
            for (int i = 0; i < m_numIntervals; ++i) {
                m_aiVec[i] = new AnalyzedIntervalData();
            }
        }
        for (int i = 0; i < m_numIntervals; ++i) {
            m_aiVec[i].clear();
        }
        // load from the DB any pre-analyzed intervals
        new ExtLoadAnalyzedIntervals().executeQuery();
        new ExtLoadAnalyzedResultsExtIntervals().executeQuery();

        m_periodDir = Ade.getAde().getConfigProperties().getOutputFilenameGenerator()
                .getPeriodDir(ai.getInterval().getSource(), m_periodStartDate);
        FileUtils.createDirs(m_periodDir);
        createXsltDirectory(ai);

        m_outputFileName = m_periodDir.getPath() + "/index.xml";
        m_inPeriod = true;
    }

    /**
     * Close the period
     * @throws AdeException
     */
    private void closePeriod() throws AdeException {
        writePeriod();
        m_inPeriod = false;
    }

    /**
     * Write out the period
     * @param outputFileName
     * @throws AdeException
     */
    private void writePeriod() throws AdeException {
        /* Initialize the Period JAXB classes */
        Systems systems = new Systems();

        /*
         * Write out the header
         */
        systems.setVersion(XML_VERSION);

        Date startTime = PeriodUtils.getContainingPeriodStart(m_periodStartDate);
        m_gc.setTimeInMillis(startTime.getTime());
        XMLGregorianCalendar startXMLDate = s_dataTypeFactory.newXMLGregorianCalendar(m_gc);
        systems.setStartTime(startXMLDate);

        Date endTime = PeriodUtils.getNextPeriodStart(m_periodStartDate);
        m_gc.setTimeInMillis(endTime.getTime());
        XMLGregorianCalendar endXMLDate = s_dataTypeFactory.newXMLGregorianCalendar(m_gc);
        systems.setEndTime(endXMLDate);

        systems.setGmtOffset(m_xmlMetaData.getGMTOffset(m_source.getSourceId()));

        systems.setIntervalSize(m_xmlMetaData.getIntervalLengthInSeconds());

        /* NumberIntervals complex type */
        NumberIntervals numberOfIntervals = new NumberIntervals();
        systems.setNumberIntervals(numberOfIntervals);
        numberOfIntervals.setValue(XMLUtil.getNumberOfSnapshots(m_framingFlowType));
        numberOfIntervals.setAnalysisSnapshotSize(XMLUtil.getXMLHardenedDurationInSeconds(m_framingFlowType));

        /* ModelInfo complex type */
        ModelInfo modelInfo = new ModelInfo();
        systems.setModelInfo(modelInfo);
        modelInfo.setAnalysisGroup(m_xmlMetaData.getAnalysisGroupName());
        modelInfo.setModelCreationDate(m_xmlMetaData.getModelCreationDate());
        modelInfo.setTrainingPeriod(m_xmlMetaData.getNumberOfDaysInTraining());

        /*
         * Write out the interval
         */
        SystemsSystemType system = new SystemsSystemType();
        systems.setSystem(system);

        system.setLogType(m_xmlMetaData.getLogType());
        system.setSysId(m_source.getSourceId());

        /* Write out the list of system */
        List<SystemsIntervalType> listOfInterval = system.getInterval();
        for (int i = 0; i < m_numIntervals; ++i) {
            SystemsIntervalType intervalType = new SystemsIntervalType();
            listOfInterval.add(intervalType);

            if (m_aiVec[i].m_results_file != null && m_aiVec[i].m_results_file.length() > 0) {
                intervalType.setMissing(false);
            } else {
                intervalType.setMissing(true);
                intervalType.setMissingReason(MISSING_INTERVAL_REASON_NO_CONNECTION);
            }

            intervalType.setIndex(i);

            double value = Double.parseDouble(SingleDigitFormatter.format(m_aiVec[i].m_anomaly_score * 100));
            intervalType.setAnomalyScore(value);
            intervalType.setNumUniqueMsgIds(m_aiVec[i].m_num_unique_msg_ids);
            intervalType.setNumNewMessages(m_aiVec[i].m_numNewMessages);
            intervalType.setNumNeverSeenBeforeMessages(m_aiVec[i].m_numNeverSeenBeforeMessages);
            intervalType.setLimitedModel(m_aiVec[i].m_modelQualityIndicator.toString());
        }

        /* Write out the XML */
        writeToXML(systems, s_marshaller);
    }

    /**
     * Output the content to a XML file.  This method intended for override by subclass
     * to customize the XML output format.
     *
     * @param analyzedInterval
     * @param jaxbSystem
     * @param marshaller
     * @param source
     * @throws AdeException
     */
    private void writeToXML(Systems jaxbSystem, Marshaller marshaller) throws AdeException {
        File outFile = new File(m_outputFileName);

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
            xmlStreamWriter = new OutputStreamWriter(new FileOutputStream(tempOutputFile), StandardCharsets.UTF_8);
            xmlStreamWriter.write("<?xml version='1.0' encoding='UTF-8' ?> \n");
            xmlStreamWriter.write("<?xml-stylesheet href='" + XSL_FILENAME + "' type='text/xsl' ?>\n");

        } catch (IOException e) {
            throw new AdeInternalException("Failed to create xml file for system " + outFile.getName()
                    + " of source " + m_source.getSourceId(), e);
        }
        try {
            marshaller.marshal(jaxbSystem, xmlStreamWriter);
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
     * Inner class responsible to load period data from the database.
     */
    protected class ExtLoadAnalyzedIntervals extends QueryPreparedStatementExecuter {

        public ExtLoadAnalyzedIntervals() {
            super("SELECT INTERVAL_SERIAL_NUM,INTERVAL_SCORE,NUM_UNIQUE_MESSAGE_IDS,START_TIME,MODEL_INTERNAL_ID,ADE_VERSION FROM "
                    + SQL.ANALYSIS_RESULTS + " where PERIOD_INTERNAL_ID = ?");
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException, AdeException {
            stmt.setInt(1, m_cachedPeriod.getInternalId());
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException, AdeException {
            int pos = 1;
            int serialNum = rs.getInt(pos++);
            double score = rs.getDouble(pos++);
            int numUniqueMessages = rs.getInt(pos++);
            Long startTime = rs.getLong(pos++);

            if (serialNum < m_aiVec.length) {
                /* Note: the value in m_aiVec must be the same as the value from AdeCore.  Any manipulation of the
                 * value to be output to the XML is done in writePeriod(). */
                m_aiVec[serialNum].m_anomaly_score = score;
                m_aiVec[serialNum].m_num_unique_msg_ids = numUniqueMessages;
                m_aiVec[serialNum].m_results_file = getIntervalXMLFileName(serialNum, startTime);
            }
        }

        /**
         * Determine the interval XML filename using the serial num
         */
        protected String getIntervalXMLFileName(int serialNum, long startTime) throws AdeException {
            ExtOutputFilenameGenerator outputFilenameGenerator = (ExtOutputFilenameGenerator) Ade.getAde()
                    .getConfigProperties().getOutputFilenameGenerator();
            return outputFilenameGenerator.getIntervalXmlFileRelativeToIndex(serialNum);
        }
    }

    /**
     * Inner class responsible to load period data from the database.
     */
    protected class ExtLoadAnalyzedResultsExtIntervals extends QueryPreparedStatementExecuter {

        public ExtLoadAnalyzedResultsExtIntervals() {
            super("SELECT INTERVAL_SERIAL_NUM,NUM_NEW_MESSAGES,NUM_NEVER_SEEN_BEFORE_MESSAGES, LIMITED_MODEL FROM " + EXT_TABLES_SQL.ANALYSIS_RESULTS_ADEEXT + " where PERIOD_INTERNAL_ID = ?");
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException, AdeException {
            stmt.setInt(1, m_cachedPeriod.getInternalId());
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException, AdeException {
            int pos = 1;
            int serialNum = rs.getInt(pos++);
            int numNewMessages = rs.getInt(pos++);
            int numNeverSeenBeforeMessages = rs.getInt(pos++);
            ExtLimitedModelIndicator modelQualityIndicator = ExtLimitedModelIndicator.valueOf(rs.getString(pos++));

            if (serialNum < m_aiVec.length) {
                m_aiVec[serialNum].setNumNewMessages(numNewMessages);
                m_aiVec[serialNum].setNumNeverSeenBeforeMessages(numNeverSeenBeforeMessages);
                m_aiVec[serialNum].setModelQualityIndicator(modelQualityIndicator);
            }
        }
    }
}
