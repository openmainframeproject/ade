/*
 
    Copyright IBM Corp. 2011, 2016
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

import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInputStream;
import org.openmainframe.ade.AdeMessageReader;
import org.openmainframe.ade.actions.IParsingQualityReporter;
import org.openmainframe.ade.data.IDataFactory;
import org.openmainframe.ade.data.DataType;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.dataStore.IDataStoreSources;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeParsingException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.AdeExt;
import org.openmainframe.ade.ext.data.GroupsQueryImpl;
import org.openmainframe.ade.ext.data.ManagedSystemInfo;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;
import org.openmainframe.ade.ext.stats.MessageRateStats;
import org.openmainframe.ade.ext.stats.MessagesWithParseErrorStats;
import org.openmainframe.ade.ext.stats.MessagesWithUnexpectedSource;
import org.openmainframe.ade.ext.utils.ExtFileUtils;
import org.openmainframe.ade.impl.data.TextClusteringComponentModel;
import org.openmainframe.ade.impl.data.TextClusteringModel;
import org.openmainframe.ade.impl.data.IThresholdSetter;
import org.openmainframe.ade.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;

import static org.openmainframe.ade.ext.os.parser.ReaderLoggerMessages.*;

/**
 * The reader for Linux SysLogs.
 * Note: ParseQualityReport infrastructure is defined in this class.  But, it's not being
 * used to output any parse error messages.  Parse Error messages are replaced by the
 * MessagesWithParseErrorStats class.
 */
public class SparklogMessageReader extends AdeMessageReader {

    /**
     * The default UNASSIGNED analysis group. Note: This is NOT the internal id of 
     * the UNASSIGNED analysis group.
     */
    public static final int UNASSIGNED_ANALYSIS_GROUP_ID = -1;
    /**
     * Default value for when a component doesn't exist in the message.
     */
    public static final String LINUX_LINE_NO_COMPONENT_NAME = "(NO_COMPONENT)";
    /**
     * The ASCII controlled characters, 0x00-0x1F and 0x7F.
     */
    public static final String ASCII_CONTROLLED_CHARACTERS = "\\p{Cntrl}";

    /**
     * Pattern of IO Exception messages that indicates the reading from the input stream
     * should be terminated gracefully.
     * Example of syntax:
     * "(Connection reset by peer|.*connection terminated.*)"
     */
    final static String IOEXCEPTION_TERMINATE_GRACEFULLY_STRING = "(.*Connection reset by peer.*|.*Connection timed out.*)".toUpperCase();

    /**
     * The default value for when a GMT offset is invalid.
     */
    public static final long GMT_OFFSET_INVALID = 362340;
    
    /**
     * The threshold percentage to determine if the log data was successfully parsed.
     */
    private static final double goodPercentThreshold = .05;
    /**
     * The default logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(SparklogMessageReader.class);
    /**
     * Object to create and keep track of textual clusters.
     */
    private TextClusteringComponentModel m_textClusteringComponentModel;
    /**
     * Keep track of message instances that are waiting to be read.
     */
    private IMessageInstance m_messageInstanceWaiting = null;
    /**
     * The previous message instance to be read.
     */
    private IMessageInstance m_prevMessageInstance = null;
    /**
     * DataFactory to create message instances.
     */
    private IDataFactory m_dataFactory;
    /**
     * For preprocessing Linux messages.
     */
    private LinuxMessageTextPreprocessor m_messageTextPreprocessor;

    /**
     * Number of lines that have parsing errors.
     */
    private int m_errorLineCount = 0;
    /**
     * Number of lines that do not have a source.
     */
    private int m_unexpectedSourceLineCount = 0;

    /**
     *  Number of lines where the component name is missing.
     */
    private int m_componentMissingLineCount = 0;

    /**
     * The starting time of the parser.
     */
    private long m_parserStartTime;
    /**
     * The starting date of the parser.
     */
    private Date m_parserStartDate = new Date();

    /**
     * Whether the message returned from readMessageInstance() is the 2nd message 
     * generated from a wrapper message.
     */
    private boolean m_isWrapperMessage = false;
    /**
     * The number of wrapper messages.
     */
    private long m_wrapperMessageCount = 0;
    /**
     * The number of non-wrapper messages.
     */
    private long m_nonWrapperMessageCount = 0;

    /**
     * Number of suppressed messages remaining.
     */
    private int m_suppressedMessagesRemaining = 0;
    /**
     * Number of non-wrapper messages count.
     */
    private long m_suppressedNonWrapperMessageCount = 0;

    /**
     * Name of the last newly seen source.
     */
    private String m_lastNewlySeenSourceId = null;
    
    /**
     * The parser for a line of Linux syslogs.
     */
    private SparklogLineParser[] m_lineParsers;

    /**
     * Hashmap mapping SysId to Source ID.
     */
    private Map<String, String> sourceToSourceIdMap = new HashMap<String, String>();

    /**
     * The Linux specific properties to be used containing configurations from start of AdeExt main class.
     */
    private LinuxAdeExtProperties m_adeExtProperties;

    /**
     * Holds system specific information.
     */
    private ManagedSystemInfo m_info = null;

    /**
     * An object used for monitoring parsing quality, or null if none.
     */
    private IParsingQualityReporter m_parsingQualityReport = null;

    /**
     * Constructs a reader for a given input stream and initializes member variables.
     * @param stream Input stream for parsing.
     * @param parseReportFilename the name of the parse report.
     * @param adeProperties Configuration flags used to specify time zone and whether to use debug parser codes.
     * @throws AdeInternalException
     */
    public SparklogMessageReader(AdeInputStream stream, String parseReportFilename,
            LinuxAdeExtProperties adeExtProperties) throws AdeException {
        super(stream);
        m_dataFactory = Ade.getAde().getDataFactory();

        m_textClusteringComponentModel = Ade.getAde().getActionsFactory().getTextClusteringModel(true);
        m_messageTextPreprocessor = new LinuxMessageTextPreprocessor();
        m_textClusteringComponentModel.setMessageTextPreprocessor(m_messageTextPreprocessor);

        initializeOtherInformation(adeExtProperties, parseReportFilename);
    }

    /**
     * Main logic for this class. Reads the message and stores the information extracted from it in a 
     * MessageInstance object. First it checks if there is a wrapper or suppressed message if so,
     * this message will be returned. Then we parse the current line. If the current line is null, then 
     * we are done reading the input stream. If it is not null, we check and see if it is a suppressed message. 
     * If so, we collect message information and return the previous message instance. If not, we loop
     * through all possible line parsers and find one that can capture the current line. If one is found
     * we generate a message id and process the source id. Then we return the message instance. If one is
     * not found then we log this as an error.
     * @return MessageInstance object that stores all the necessary information of a message.
     */
    @Override
    public final IMessageInstance readMessageInstance() throws IOException, AdeException {
        String currentLine;
        boolean gotLine = false;
        boolean unexpectedSource = false;        
        if (m_messageInstanceWaiting != null) {
            return getMessageInstanceWaiting();
        }
        if (m_suppressedMessagesRemaining > 0) {
            updateSuppressedMessageStats();
            return m_prevMessageInstance;
        }
        while (!gotLine) {
            currentLine = getCurrentLine();
            if (currentLine != null) {
                currentLine = currentLine.replaceAll(ASCII_CONTROLLED_CHARACTERS, "");
            }
            if (currentLine == null){
                handleEndOfStream();
                return null;
            }
            for (SparklogLineParser lineParser : m_lineParsers) {
                gotLine = lineParser.parseLine(currentLine);
                if (gotLine) {
                    String msgId = getMessageId(lineParser);
                    DateTime dateTime = handleDateTime(lineParser);
                    final String sourceId = getAndProcessSourceId(lineParser.getSource());
                    if (sourceId == null) {
                        gotLine = false;
                        unexpectedSource = true;                
                        MessagesWithUnexpectedSource.addMessage(lineParser.getSource(),
                                lineParser.m_msgTime.getTime(), currentLine);              
                        break;
                    }
                    m_isWrapperMessage = false;
                    m_nonWrapperMessageCount++;              
                    InputTimeZoneManager.updateTimezone(sourceId, dateTime);               
                    m_prevMessageInstance = m_dataFactory.newMessageInstance(
                            sourceId,
                            lineParser.getMsgTime(),
                            msgId,
                            lineParser.getMessageBody(),
                            lineParser.getSource(),
                            lineParser.getSeverity()); // Severity = null for Spark 
                    /* Setting the messageInstanceWaiting to null, which would stop wrappers such as SUDO or CRON
                       to be passed to ade. */              
                    m_messageInstanceWaiting = null;              
                    return m_prevMessageInstance;
                }
            }
            if (!gotLine) {
                if (unexpectedSource) {
                    m_unexpectedSourceLineCount++;
                    unexpectedSource = false;
                } else if (currentLine.length() != 0) {
                    final MessagesWithParseErrorStats stats = MessagesWithParseErrorStats.getParserErrorStats();
                    stats.addMessage(currentLine);
                    m_errorLineCount++;
                }
            }
        }
        return null;
    }
    
    /**
     * Returns the waiting message instance and records message diagnostics.
     * @return the message instance waiting to be read.
     */
    private IMessageInstance getMessageInstanceWaiting(){
        final IMessageInstance tmp = m_messageInstanceWaiting;
        m_messageInstanceWaiting = null;
        m_isWrapperMessage = true;
        m_wrapperMessageCount++;
        return tmp;
    }
    /**
     * Returns the suppressed message and records message diagnostics. If the suppressed message 
     * is a wrapper, the wrapper message also need to be outputted.
     * @return the suppressed message instance.
     * @throws AdeException 
     * @throws AdeInternalException 
     */
    private void updateSuppressedMessageStats() throws AdeException{
        m_suppressedNonWrapperMessageCount++;
        m_suppressedMessagesRemaining--;
        m_messageInstanceWaiting = m_messageTextPreprocessor.getExtraMessage(m_prevMessageInstance);
    }

    /**
     * Retrieves the current line using this reader.
     * @return the current line that was parsed. If null, then we have reached the end of the input stream.
     * @throws AdeException
     */
    private String getCurrentLine() throws AdeException{
        String currentLine;
        try {
            currentLine = this.readLine();
        } catch (IOException e) {
            final String exceptionMsg = e.getMessage().toUpperCase();
            if (exceptionMsg.matches(IOEXCEPTION_TERMINATE_GRACEFULLY_STRING)) {
                logger.warn("Normal IOException: " + SparklogMessageReader.class.getSimpleName()
                        + " will be terminated gracefully.", e);
                currentLine = null;
            } else {
                throw new AdeParsingException("Failed reading from log file", e);
            }
        }
        return currentLine;
    }
    
    /**
     * Updates the last determined date time and returns the updated value. 
     * Note: dateTime is null when it is a 3164 message. so if the GMT offset is not defined in
     * the -g option or if it is a 3164 message, it will still be set as the default
     * invalid GMT offset value. If dateTime is not null then we want to update the GMT offset 
     * unless it was already previously defined by the -g option. In that case, 
     * we will just use that value.
     * @param lineParser the parser being used to parse the line.
     * @return the updated date time.
     */
    private DateTime handleDateTime(SparklogLineParser lineParser){
        final DateTime dateTime = lineParser.getLastDeterminedDateTime();
        if (dateTime != null && !m_adeExtProperties.isGmtOffsetDefined()) {
            updateGmtOffset(dateTime);
        }
        return dateTime;
    } 

    /**
     * Updates the time offset and sets it in the ManagedSystemInfo object.
     * @param dateTime the date/time-stamp of the current message.
     */
    private void updateGmtOffset(DateTime dateTime) {
        final long gmtOffsetInMillis = dateTime.getZone().getOffset(dateTime.getMillis());
        final long gmtOffset = TimeUnit.MILLISECONDS.toHours(gmtOffsetInMillis);
        m_info.setGmtOffset(gmtOffset);
    }
    /**
     * Generates message id based on the component's clustering model.
     * @param lineParser the parser being used to parse the line.
     * @param thresholdSetter threshold for comparing two strings.
     * @return the generated message id.
     * @throws AdeException
     */
    private String getMessageId(SparklogLineParser lineParser) throws AdeException{
        final Pair<String, IThresholdSetter> p = m_messageTextPreprocessor.updateComponent(lineParser.m_component, lineParser.m_text);
        lineParser.m_component = p.m_first;
        final IThresholdSetter thresholdSetter = p.m_second;
        return generateMessageId(lineParser, thresholdSetter);
    }

    /**
     * Generates message id based on the component's clustering model.
     * @param lineParser the parser being used to parse the line.
     * @param thresholdSetter threshold for comparing two strings.
     * @return the generated message id.
     * @throws AdeException
     */
    private String generateMessageId(SparklogLineParser lineParser, IThresholdSetter thresholdSetter) throws AdeException {
        if (thresholdSetter == null) {
            thresholdSetter = new TextClusteringComponentModel.SimpleThresholdSetter();
        }
        final TextClusteringModel model = m_textClusteringComponentModel.getTextClusteringModel(lineParser.m_component, thresholdSetter);
        return model.getComponentName() + "_" + model.getOrAddCluster(lineParser.m_text, lineParser.m_msgTime).getClusterId();
    }

    /**
     * Whether the message returned from readMessageInstance() is the generated message of a wrapper message.
     * @return true if the message is a generated wrapper message.
     */
    public final boolean isWrapperMessage() {
        return m_isWrapperMessage;
    }

    /**
     * Returns the sourceID given a sysId. This method handles multiple sources coming from the log stream.
     * However, this is not currently necessary because all streams are expected to contain messages from a 
     * single source. This method will adds the source ID to the database if it's not in the database already. 
     * This method provide an opportunity to transform the SysId into something else, which would be used as 
     * the sourceId processed by Ade. This method also sets the mapping between the sourceId to an Analysis Group.
     * @throws AdeException
     */
    private String getAndProcessSourceId(String source) throws AdeException {
        /* Make source case insensitive for Linux systems. */
        source = source.toLowerCase();

        /* Note: the source coming from Linux is the HOSTNAME field in the Syslog message.  */
        String sourceId = sourceToSourceIdMap.get(source);
        if (sourceId != null) {
            /** 
             * This SysId and SourceId have already been processed.  Don't need to
             * perform the rest of the processing.
             */
            return sourceId;
        } else {
            /**
             * If option is provided, then need to make sure the only sources being 
             * analyzed is from the sources in the -s option.
             */
            final AdeExtRequestType requestType = m_adeExtProperties.getRequestType();
            if (m_adeExtProperties.isSourceOptionProvided() && (requestType.name()).equalsIgnoreCase("ANALYZE")) {
                final Collection<ISource> sources = m_adeExtProperties.getSources();
                boolean isValidSource = false;
                for (ISource s : sources) {
                    if ((s.getSourceId()).equalsIgnoreCase(source)) {
                        isValidSource = true;
                        break;
                    }
                }

                /* Return null, and do not update the sourceToSourceIdMap */
                if (!isValidSource) {
                    return null;
                }
            }

            /* Update database to add the source, then update m_info with the source. */
            final IDataStoreSources dataStoreSources = Ade.getAde().getDataStore().sources();

            /* For Linux, the sourceId is the same as source. */
            sourceId = source;
            m_lastNewlySeenSourceId = sourceId;
            m_adeExtProperties.setLastNewlySeenSourceId(m_lastNewlySeenSourceId);

            /* Read the RuntimeModelData from file. */
            final RuntimeModelDataManager runtimeModelDataManager = new RuntimeModelDataManager();
            runtimeModelDataManager.readModelDataFromFile(source);

            /* Provide a mapping between source to analysisGroup. */
            dataStoreSources.addSourceAndAnalysisGroup(sourceId, UNASSIGNED_ANALYSIS_GROUP_ID);

            final ISource S = dataStoreSources.getOrAddSource(source);
            if (m_info != null) {
                /* Add m_info to the database.  It's only added when SysInfo is available. */
                m_info.updateDataStore(S);
            }

            /* Update the analysis group by calling an atomic method that evaluates analysis group rules, 
             * and commits to the database in an atomic transaction. */
            final String analysisGroup = GroupsQueryImpl.updateSourcesAnalysisGroup(sourceId);
            MessageRateStats.addSourceAndAnalysisGroup(sourceId, analysisGroup);

            logger.trace("Datastore updated for Linux system: " + source
                    + ", that maps to sourceId: " + sourceId);

            /* Add the source to the mapping */
            sourceToSourceIdMap.put(source, sourceId);

            return sourceId;
        }
    }

    /**
     * Private method to initialize other information. When initializing the lineparsers,
     * The order is important. The 5424 parser is first because we expect it to be the 
     * parser used during normal operations.  The 3164 parsers are available for bulkload.  
     * Within the 3164 parsers, they must be ordered from most-specific match to most-generic 
     * match. We also set the parsing start time, the parsingQualityReporter and managed system
     * information.
     * @param adeExtProperties Properties file that contains AdeExt configurations.
     * @param parseReportFilename the file name of the parse report.
     * @throws AdeException
     */
    private void initializeOtherInformation(LinuxAdeExtProperties adeExtProperties, String parseReportFilename)
            throws AdeException {
        m_parserStartTime = System.nanoTime();
        m_lineParsers = new SparklogLineParser[] {
                new SparklogParser(),
        };
        m_adeExtProperties = adeExtProperties;
        SparklogParserBase.setAdeExtProperties(m_adeExtProperties);
        setParsingQualityReporterIfRequested(parseReportFilename);
        try {
            if (m_adeExtProperties.isGmtOffsetDefined()) {
                m_info = new ManagedSystemInfo(m_adeExtProperties.getGmtOffset(), "linux");
            } else {
                m_info = new ManagedSystemInfo(GMT_OFFSET_INVALID, "linux");
            }
        } catch (IllegalArgumentException e) {
            throw new AdeUsageException("Invalid SysInfo argument(s)", e);
        }
    }

    /**
     * Sets the parse quality report. First it checks to see if it was requested, if not then we
     * return. Otherwise, we initialize the parse report output directory and the parse report
     * itself.
     * @param parseReportFilename The name of the parse report file.
     * @throws AdeException
     */
    public final void setParsingQualityReporterIfRequested(String parseReportFilename) throws AdeException {
        if (!m_adeExtProperties.isParseReportRequested()) {
            return;
        }
        final File sumLogDirectory = AdeExt.getAdeExt().getOutputDirectoryManager().getOutputHome();
        ExtFileUtils.createDir(sumLogDirectory);
        final File resultFile = new File(sumLogDirectory, parseReportFilename);
        resultFile.getParentFile().mkdirs();
        m_parsingQualityReport = Ade.getAde().getActionsFactory().createParsingQualityReporter();
        m_parsingQualityReport.open(resultFile.getPath());

        for (SparklogLineParser lineParser : m_lineParsers) {
            lineParser.setParseQualityReport(m_parsingQualityReport);
        }
    }

    /**
     * Logs summary messages indicating how the parsing went. If more than 5% of lines 
     * had messages than we write a success message otherwise, we write an error message.
     * @throws AdeException 
     */
    private void printStatEof() throws AdeException {

        Format formatter;
        formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
        String message;
        final int m_statCounterRawLines = getLineNumber();
        final long endTime = System.nanoTime();
        final long elapsedTime = endTime - m_parserStartTime;
        final double seconds = Math.ceil(elapsedTime / 1.0E09);
        final AdeExtRequestType requestType = m_adeExtProperties.getRequestType();

        message = String.format(PARSED_DATA_STATS_MSG, m_statCounterRawLines,m_nonWrapperMessageCount,
                m_suppressedNonWrapperMessageCount, m_wrapperMessageCount, m_errorLineCount,
                m_componentMissingLineCount, m_unexpectedSourceLineCount, seconds);
        logger.info(message);

        final String StartDate = formatter.format(m_parserStartDate);
        String sourceId;
        if (m_prevMessageInstance != null) {
            sourceId = m_prevMessageInstance.getSourceId();
        } else if (m_adeExtProperties.isSourceOptionProvided()) {
            final Collection<ISource> sources = m_adeExtProperties.getSources();
            sourceId = (sources.iterator().next()).getSourceId();
        } else {
            sourceId = "Unknown";
        }
        final double goodPercent = ((double) m_nonWrapperMessageCount) / ((double) m_statCounterRawLines);

        switch (requestType) {
            case UPLOAD: {
                try {
                    if (goodPercent > goodPercentThreshold) {
                        message = String.format(GOOD_UPLOAD_MSG, StartDate, m_statCounterRawLines, sourceId, 
                                m_nonWrapperMessageCount, DataType.SYSLOG.name());
                    } else {
                        message = String.format(BAD_UPLOAD_MSG, StartDate, m_statCounterRawLines, sourceId, 
                                m_nonWrapperMessageCount, DataType.SYSLOG.name());
                    }
                } catch (Throwable t) {
                    logger.error("An error occured - Internal Error: Building Notification ", t);
                }
                break;
            }
            case ANALYZE:
                try {
                    if (goodPercent > goodPercentThreshold) {
                        message = String.format(GOOD_ANALYZE_MSG, StartDate, m_statCounterRawLines, DataType.SYSLOG.name(),
                                sourceId, m_nonWrapperMessageCount);
                    } else {
                        if (goodPercent == 0) {
                            message = String.format(NO_MSGS_PARSED_MSG, StartDate, m_statCounterRawLines, DataType.SYSLOG.name(),
                                    sourceId);
                        } else {
                            message = String.format(BAD_ANALYZE_MSG, StartDate, m_statCounterRawLines, DataType.SYSLOG.name(), 
                                    sourceId, m_nonWrapperMessageCount);
                        } 
                    }
                } catch (Throwable t) {
                    logger.error("An error occured - Internal Error: Building Notification ", t);
                }
                break;
        }
        logger.info(message);
    }

    /**
     * Perform actions at the end of stream.
     * @throws AdeException
     */
    private void handleEndOfStream() throws AdeException {
        if (m_parsingQualityReport != null) {
            m_parsingQualityReport.close();
        }

        printStatEof();
    }

}
