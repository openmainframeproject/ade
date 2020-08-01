/*
 
    Copyright IBM Corp. 2008, 2016
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
package org.openmainframe.ade.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.InvalidParameterException;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.openmainframe.ade.IAdeConfigProperties;
import org.openmainframe.ade.data.IPeriod.PeriodMode;
import org.openmainframe.ade.dbUtils.DriverType;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.flow.AnalysisGroupToFlowNameMapper;
import org.openmainframe.ade.flow.AnalysisGroupToFlowNameUnityMapper;
import org.openmainframe.ade.impl.PropertyAnnotation.ClassPropertyFactory;
import org.openmainframe.ade.impl.PropertyAnnotation.MissingPropertyException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.impl.PropertyAnnotation.PropertyFactoryByString;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.openmainframe.ade.impl.utils.FileUtils;
import org.openmainframe.ade.output.OutputFilenameGenerator;
import org.openmainframe.ade.utils.ConfigPropertiesWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple implementation for {@link IAdeConfigProperties}
 */
public class AdeConfigPropertiesImpl implements IAdeConfigProperties {

    private static final String ADE_PREFIX = "ade.";
    private static final String ADE_SETUP_DIRECTORY = ADE_PREFIX + "setUpFilePath";
    private static final String ADE_DEBUG_PREFIX = ADE_PREFIX + "debug.";
    private static final String ADE_SCORING_PREFIX = ADE_PREFIX + "scoring.";
    private static final String ADE_LOG_PROCESSING_PREFIX = ADE_PREFIX + "logProcessing.";

    // Also used from outside this class
    public static final String ADE_OVERRIDE_VERSION_CHECK = ADE_PREFIX + "overrideVersionCheck";

    private static final double s_logNormalLogBernoulliMix = 0.2;
    private static final Logger logger = LoggerFactory.getLogger(AdeConfigPropertiesImpl.class);
    
    private ConfigPropertiesWrapper m_props = new ConfigPropertiesWrapper(ADE_PREFIX);

    /*************************************************************************************
     * Property fields
     *************************************************************************************/

    /***************** File Paths ***************/
    @Property(key = ADE_PREFIX + "setUpFilePath", required = false, help = "Optional path to setup.props file")
    private String m_setUpFilePath = null;

    @Property(key = ADE_PREFIX + "xml.xsltDir", required = false, 
            help = "Optional path to directory containing the following files:"
            + " AnalyzedInterval.xsl, AdeCorePlex.xsl, global.css")
    private File m_xsltDir = null;

    @Property(key = ADE_PREFIX + "outputPath",
            help = "Path in which Ade will store all output files and directories")
    private String m_outputPath;

    @Property(key = ADE_PREFIX + "analysisOutputPath", required = false,
            help = "Optional path to store analysis files")
    private String m_analysisOutputPath = null;

    /// Note that though the default here seem null, it will actually be set to m_outputPath/temp
    @Property(key = ADE_PREFIX + "tempPath", required = false, help = "Optional path to store temporary files")
    private String m_tempPath = null;

    @Property(key = ADE_PREFIX + "useSparkLogs", help = "Type of logs to run ade on")
    private boolean m_useSparkLogs;

    @Property(key = ADE_PREFIX + "flowLayoutFile", help = "Path to Flow Layout file")
    private String m_flowLayoutFile;

    @Property(key = ADE_PREFIX + "flowLayoutFileSpark",
                help = "Path to Flow Layout file for spark (matters only when ade.useSparkLogs=true)")
    private String m_flowLayoutFileSpark;

    @Property(key = ADE_PREFIX + "userRulesFile", required = false, help = "Optional path to User Rules file")
    private String m_userRulesFile = null;

    @Property(key = ADE_PREFIX + "userRulesCondition", required = false,
            help = "The name of the condition class of user rules")
    private String m_userRulesCondition = "org.openmainframe.ade.impl.userRules.UserRuleSimpleCondition";

    @Property(key = ADE_PREFIX + "criticalWords.file", help = "Path to file containing critical words")
    private String m_criticalWordsFile;

    /**************** Database *****************/
    @Property(key = ADE_PREFIX + "databaseUrl", help = "Url for the provided DB")
    private String m_databaseUrl;

    @Property(key = ADE_PREFIX + "databaseDriver", help = "Class of the JDBC driver")
    private String m_databaseDriver;

    @Property(key = ADE_PREFIX + "dataStoreType", help = "?")
    private String m_dataStoreType;

    @Property(key = ADE_PREFIX + "databaseUser", required = false, help = "userid for the provided DB")
    private String a_adeDbUserName = "";

    @Property(key = ADE_PREFIX + "databasePassword", required = false, help = "password for the provided DB")
    private String a_adeDbPassword = null;

    @Property(key = ADE_PREFIX + "database.keepOnlyAscii", required = false, help = "?")
    private boolean m_keepOnlyAscii = false;

    @Property(key = ADE_PREFIX + "database.UNSAFE_NO_PARALLEL_ADE", required = false, help = "?")
    private boolean m_singleAdeOnlyUnsafeDB = false;

    @Property(key = ADE_PREFIX + "database.schema", required = false,
            help = "Schema of DB. Useful when schema differs from database user name")
    private String m_dbSchema = null;

    /***************** Debug ********************/
    @Property(key = ADE_DEBUG_PREFIX + "parserCodes", required = false, help = "?")
    private boolean m_debugParserCodes = false;

    @Property(key = ADE_DEBUG_PREFIX + "experimentCode", required = false, help = "?")
    private String m_debugExperimentCode = "";

    @Property(key = ADE_DEBUG_PREFIX + "regressionMode", required = false, help = "?")
    private boolean m_regressionMode = false;

    @Property(key = ADE_DEBUG_PREFIX + "experimentalModelFile", required = false, help = "?")
    private File m_experimentalModelFile = null;

    @Property(key = ADE_DEBUG_PREFIX + "messageIdGeneration", required = false, help = "?")
    private int m_debugMessageIdGeneration = -1;

    @Property(key = ADE_PREFIX + "outputFilenameGenerator", required = false,
            factory = OutputFilenameGeneratorClassFactory.class,
            help = "Optional class for customizing output file names. Must extend OutputFilenameGenerator")
    private Class<? extends OutputFilenameGenerator> m_outputFilenameGeneratorClass = OutputFilenameGenerator.class;

    /******************* Misc *******************/
    @Property(key = ADE_PREFIX + "periodMode", help = "Enumerated value for the duration of the periods")
    private PeriodMode m_periodMode;

    @Property(key = ADE_PREFIX + "training.minimalRequieredTrainPeriod", required = false, help = "?")
    private int m_minimalRequieredTrainPeriod = 0;

    @Property(key = ADE_PREFIX + "inputTimeZone", required = false, factory = TimeZoneFactory.class, help = "?")
    private TimeZone m_inputTimeZone = TimeZone.getDefault();

    @Property(key = ADE_PREFIX + "outputTimeZone", required = false, factory = TimeZoneFactory.class, help = "?")
    private TimeZone m_outputTimeZone = TimeZone.getDefault();

    @Property(key = ADE_LOG_PROCESSING_PREFIX + "fileSizeExpirationTime", required = false, help = "?")
    private static final int m_fileSizeExpirationTime = 86400;

    @Property(key = ADE_LOG_PROCESSING_PREFIX + "fileSizeUpdateTime", required = false, help = "?")
    private static final int m_fileSizeUpdateTime = 300;

    @Property(key = ADE_LOG_PROCESSING_PREFIX + "javaTailSleepTime", required = false, help = "?")
    private static final int m_javaTailSleepTime = 1000;

    @Property(key = ADE_SCORING_PREFIX + "messageLogNormal.logNormalWeight", required = false, help = "?")
    private double m_logNormalWeight = s_logNormalLogBernoulliMix;

    @Property(key = ADE_SCORING_PREFIX + "messageLogNormal.logBernoulliWeight", required = false, help = "?")
    private double m_logBernoulliWeight = 1 - s_logNormalLogBernoulliMix;

    @Property(key = ADE_SCORING_PREFIX + "resultMapping", required = false,
            factory = ResultMappingFactory.class, help = "?")
    private SortedMap<String, String> m_resultMapping = null;

    @Property(key = ADE_PREFIX + "analysisGroupToFlowNameMapperClass", required = false,
            factory = FlowMapperClassFactory.class, help = "Optional class for mapping analysis groups to flow names." 
            + "Must extend AnalysisGroupToFlowNameMapper")
    private Class<? extends AnalysisGroupToFlowNameMapper> m_analysisGroupToFlowNameMapper
            = AnalysisGroupToFlowNameUnityMapper.class;

    @Property(key = ADE_PREFIX + "analysisGroupToFlowNameMapperClassSpark", required = false,
            factory = FlowMapperClassFactory.class, help = "Optional class for mapping analysis groups to flow names.(Spark)"
            + "Must extend AnalysisGroupToFlowNameMapper. Used only when ade.useSparkLogs=true")
    private Class<? extends AnalysisGroupToFlowNameMapper> m_analysisGroupToFlowNameMapperSpark
            = AnalysisGroupToFlowNameUnityMapper.class;

    @Property(key = ADE_OVERRIDE_VERSION_CHECK, required = false,
            help = "Allow Ade to run with a database version different from the JAR version")
    private boolean m_overrideVersionCheck = false;

    @Property(key = ADE_PREFIX + "minimalSourceTabelRefreshTime", required = false,
            help = "Refresh Source dictionary from DB table if older then so many milli-secounds")
    private final static long m_sourcesMinRefreshTime = (long) 60 * 1000;
    /*************************************************************************************/

    private IDebugParameters m_debugParameters;
    private IScoringParameters m_scoringParameters;
    private IDatabaseParameters m_databaseParameters;
    private ILogProcessingParameters m_logProcessingParamters;
    private OutputFilenameGenerator m_outputFilenameGenerator;

    public AdeConfigPropertiesImpl(Properties configProps, Properties systemProps) throws AdeException {
        m_props.addProperties(configProps, "Configuration file");
        m_props.addProperties(systemProps, "System property");
        initProperties();
    }

    public AdeConfigPropertiesImpl(String propertyFile) throws AdeException {
        m_props.addPropertyFile(propertyFile);

        final File localFile = new File(new File(propertyFile).getParentFile(), "local.props");
        if (localFile.exists()) {
            logger.info("Overriding parameters with " + localFile.getPath());
            m_props.addOverrides(localFile.getPath());
        }
        m_props.addProperties(System.getProperties(), "System property");

        initProperties();
    }

    private void initProperties() throws AdeException {

        // 'touch' this property to mark it as used
        m_props.hasProperty(ADE_SETUP_DIRECTORY);

        try {
            PropertyAnnotation.setProps(this, m_props);
        } catch (MissingPropertyException e) {
            throw new AdeUsageException("", e);
        }
        validateProps();

        m_databaseParameters = new DatabaseParametersImpl();
        m_debugParameters = new DebugParametersImpl();
        m_scoringParameters = new ScoringParametersImpl();
        m_logProcessingParamters = new LogProcessingParametersImpl();
        try {
            m_outputFilenameGenerator = m_outputFilenameGeneratorClass.newInstance();
        } catch (IllegalAccessException e) {
            throw new AdeUsageException(String.format("The %s implementing class provided property must have"
                    + " the default constructor (no arguments) visible (public)", m_outputFilenameGeneratorClass), e);
        } catch (InstantiationException e) {
            throw new AdeUsageException(String.format("The %s implementing class provided property must have"
                    + " include the default constructor (no arguments)", m_outputFilenameGeneratorClass), e);
        }

        m_props.verifyAllPropertiesUsed();
    }

    private void validateProps() throws AdeUsageException {
        try {
            FileUtils.assertExists(new File(m_criticalWordsFile), new File(m_flowLayoutFile));
            if (m_useSparkLogs){
                FileUtils.assertExists(new File(m_criticalWordsFile), new File(m_flowLayoutFileSpark));
            }
            
        } catch (FileNotFoundException e) {
            throw new AdeUsageException("File specified in setup properties not found!", e);
        }
        if (m_xsltDir != null && !m_xsltDir.isDirectory()) {
            throw new AdeUsageException("Specified XSLT directory does not exist: " + m_xsltDir.getAbsolutePath());
        }

    }
    

    /*************************************************************************************
     * Getters
     *************************************************************************************/

    @Override
    public final PeriodMode getPeriodMode() throws AdeInternalException {
        return m_periodMode;
    }

    @Override
    public final File getXsltDir() {
        return m_xsltDir;
    }

    @Override
    public final String getCriticalWordsFile() {
        return m_criticalWordsFile;
    }

    @Override
    public final String getFlowLayoutFile() {
        if (m_useSparkLogs){
            return m_flowLayoutFileSpark;
        }
        return m_flowLayoutFile;
    }

    @Override
    public final Boolean getUseSparkLogs() {
        return m_useSparkLogs;
    }

    @Override
    public final TimeZone getOutputTimeZone() {
        return m_outputTimeZone;
    }

    @Override
    public final TimeZone getInputTimeZone() {
        return m_inputTimeZone;
    }

    @Override
    public final String getOutputPath() {
        return m_outputPath;
    }

    @Override
    public final String getAnalysisOutputPath() {
        return m_analysisOutputPath;
    }

    @Override
    public final String getTempPath() {
        if (m_tempPath == null) {
            m_tempPath = new File(m_outputPath, "temp").getPath();
        } else {
            return m_tempPath;    
        }
        return m_tempPath;
    }

    @Override
    public final OutputFilenameGenerator getOutputFilenameGenerator() {
        return m_outputFilenameGenerator;
    }

    @Override
    public final String getUserRulesFile() {
        return m_userRulesFile;
    }

    @Override
    public final IDebugParameters debug() {
        return m_debugParameters;
    }

    @Override
    public final IScoringParameters scoring() {
        return m_scoringParameters;
    }

    @Override
    public final IDatabaseParameters database() {
        return m_databaseParameters;
    }

    @Override
    public final ILogProcessingParameters logProcessing() {
        return m_logProcessingParamters;
    }

    @Override
    public final int getMinimalRequieredTrainPeriod() {
        return m_minimalRequieredTrainPeriod;
    }

    @Override
    public final Class<? extends AnalysisGroupToFlowNameMapper> getAnalysisGroupToFlowNameMapper() {
        if (m_useSparkLogs){
            return m_analysisGroupToFlowNameMapperSpark;
        }
        return m_analysisGroupToFlowNameMapper;
    }

    @Override
    public final String getUserRulesCondition() {
        return m_userRulesCondition;
    }

    @Override
    public final boolean getOverrideVersionCheck() {
        return m_overrideVersionCheck;
    }

    @Override
    public final long getSourcesMinRefreshTime() {
        return m_sourcesMinRefreshTime;
    }

    /*************************************************************************************
     * Property factories
     *************************************************************************************/

    private static class ResultMappingFactory extends PropertyFactoryByString<SortedMap<String, String>> {
        
        private static final int MAX_PP_LENGTH = 2;
        
        public ResultMappingFactory() {
            // Do nothing.    
        }
        
        @Override
        public SortedMap<String, String> create(String propVal) {
            final String[] parts = propVal.trim().split(",");
            final TreeMap<String, String> res = new TreeMap<String, String>();

            if (parts == null || parts.length == 0) {
                return res;
            }
            for (String part : parts) {
                String src;
                String dst;
                if (part.contains("->")) {
                    final String[] pp = part.split("->");
                    if (pp.length != MAX_PP_LENGTH) {
                        throw new InvalidParameterException("Invalid usage of -> in result mapping \"" + part + "\"");
                    }
                    src = pp[0];
                    dst = pp[1];
                } else {
                    src = dst = part;
                }
                if (src == null || src.length() == 0 || dst == null || dst.length() == 0) {
                    throw new InvalidParameterException("Invalding result mapping \"" + part + "\"");
                }
                if (res.containsKey(src) || res.containsValue(dst)) {
                    throw new InvalidParameterException("Duplicate mapping \"" + part + "\"");
                }
                res.put(src, dst);
            }
            return res;
        }
    }

    private static class OutputFilenameGeneratorClassFactory extends ClassPropertyFactory<OutputFilenameGenerator> {
        public OutputFilenameGeneratorClassFactory() {
            super(OutputFilenameGenerator.class);
        }
    }

    private static class FlowMapperClassFactory extends ClassPropertyFactory<AnalysisGroupToFlowNameMapper> {
        public FlowMapperClassFactory() {
            super(AnalysisGroupToFlowNameMapper.class);
        }
    }

    private static class TimeZoneFactory extends PropertyFactoryByString<TimeZone> {
        public TimeZoneFactory() {
            super();
        }

        @Override
        public TimeZone create(String propVal) {
            try {
                return DateTimeUtils.parseTimeZone(propVal);
            } catch (AdeUsageException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    /*************************************************************************************
     * Container Classes
     *************************************************************************************/

    private class DatabaseParametersImpl implements IDatabaseParameters {
        private final DriverType m_driverType;

        DatabaseParametersImpl() throws AdeInternalException {
            m_driverType = DriverType.parseDriverType(m_databaseDriver);
        }

        @Override
        public String getDatabaseUrl() {
            return m_databaseUrl;
        }

        @Override
        public String getDatabaseDriver() {
            return m_databaseDriver;
        }

        @Override
        public String getDataStoreType() {
            return m_dataStoreType;
        }

        @Override
        public String getDatabasePassword() {
            return a_adeDbPassword;
        }

        @Override
        public String getDatabaseUser() {
            return a_adeDbUserName;
        }

        @Override
        public boolean keepOnlyAscii() {
            return m_keepOnlyAscii;
        }

        @Override
        public boolean UNSAFE_DbNoLocks() {
            return m_singleAdeOnlyUnsafeDB;
        }

        @Override
        public void setDatabasePassword(String password) throws AdeUsageException {
            if (a_adeDbPassword != null) {
                throw new AdeUsageException("Trying to set a db password when one already exists");
            }
            a_adeDbPassword = password;
        }

        @Override
        public DriverType getDriverType() {
            return m_driverType;
        }

        @Override
        public String getDatabaseSchema() {
            return m_dbSchema;
        }
    }

    private class DebugParametersImpl implements IDebugParameters {

        public DebugParametersImpl() {
            // Do nothing.    
        }
        
        @Override
        public boolean getDebugParserCodes() {
            return m_debugParserCodes;
        }

        @Override
        public String getDebugExperimentCode() {
            return m_debugExperimentCode;
        }

        @Override
        public File getExperimentalModelFile() {
            return m_experimentalModelFile;
        }

        @Override
        public boolean getRegressionMode() {
            return m_regressionMode;
        }

        @Override
        public int isDebugMessageIdGeneration() {
            return m_debugMessageIdGeneration;
        }

    }

    private class ScoringParametersImpl implements IScoringParameters {
        
        public ScoringParametersImpl() { 
            // Do nothing. 
        }
        
        @Override
        public SortedMap<String, String> getResultMapping() {
            return m_resultMapping;
        }

        @Override
        public double getMessageLogNormalWeight() {
            return m_logNormalWeight;
        }

        @Override
        public double getMessageLogBernoulliWeight() {
            return m_logBernoulliWeight;
        }
    }

    private class LogProcessingParametersImpl implements ILogProcessingParameters {
        
        public LogProcessingParametersImpl() {
            // Do nothing. 
        }
        
        @Override
        public long getFileSizeExpirationTime() {
            return m_fileSizeExpirationTime;
        }

        @Override
        public long getFileSizeUpdateTime() {
            return m_fileSizeUpdateTime;
        }

        @Override
        public long getJavaTailSleepTime() {
            return m_javaTailSleepTime;
        }

    }
}
