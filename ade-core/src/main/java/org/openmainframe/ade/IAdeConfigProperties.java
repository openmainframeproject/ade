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
package org.openmainframe.ade;

import java.io.File;
import java.util.SortedMap;
import java.util.TimeZone;

import org.openmainframe.ade.data.IPeriod.PeriodMode;
import org.openmainframe.ade.dbUtils.DriverType;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.flow.AnalysisGroupToFlowNameMapper;
import org.openmainframe.ade.output.OutputFilenameGenerator;
import org.openmainframe.ade.scores.AdeWeightedMessageAnomalyScorerLogNormal;

/**
 * Ade Configuration properties.
 */
public interface IAdeConfigProperties {

    /**
     * Specifies the file containing list of critical words
     * @return the file containing list of critical words
     */
    String getCriticalWordsFile();

    /**
     * Specifies the xml layout file name containing the flow definitions for each analysis group.
     * @return the flow layout file name.
     */
    String getFlowLayoutFile();

    /**
     * Speciifies if running ADE on Spark logs.
     * @return boolean : True if running on Spark logs.
     */

    Boolean getUseSparkLogs();

    /**
     * @return the mode of the period, which is an enum
     * describing the duration (and alignment) of the period
     * @throws AdeInternalException
     */
    PeriodMode getPeriodMode() throws AdeInternalException;

    /** Returns the directory for xsl/xsd files */
    File getXsltDir();

    /** Returns time zone used when parsing log files */
    public TimeZone getInputTimeZone();

    /** Returns time zone used when creating periods and producing output */
    public TimeZone getOutputTimeZone();

    /** Returns root directory of output */
    public String getOutputPath();

    /** Returns root directory of analysis output */
    public String getAnalysisOutputPath();

    /** Returns the path to the user specified temporary directory */
    public String getTempPath();

    /** The file where the user rules file is, or null if not specified */
    String getUserRulesFile();

    /** The name of the condition class for user rules */
    String getUserRulesCondition();

    interface IDebugParameters {
        /** An optional property with default value 'false' 
         * When set to true, the log parser assigns special meaning to certain input lines
         * allowing controlling ade using the input log, e.g., create an artificial exception and such.
         * Used mainly for testing.
         */
        boolean getDebugParserCodes();

        /** An optional property with default value "" 
         * This string is used for activating experimental features.
         */
        String getDebugExperimentCode();

        /** Obsolete */
        File getExperimentalModelFile();

        /** An optional property with default value 'false'.
         * When set to true, various dates and ade version are set to a predefined constant,
         * to ease regression process
         */
        boolean getRegressionMode();

        int isDebugMessageIdGeneration();
    }

    /** Interface for parameters relating to the algorithms that analyze log data */
    interface IScoringParameters {

        /** A mapping between statistics as computed by message scores and the output names.
         *  Statistics not included in this mapping will be discarded all together.
         *  If returns null, all statistics are dumped to the output unchanged.
         */
        SortedMap<String, String> getResultMapping();

        /**
         * @return the weight of the log normal in the logProb calculation in the 
         * {@link AdeWeightedMessageAnomalyScorerLogNormal} scorer.
         */
        double getMessageLogNormalWeight();

        /**
         * @return the weight of the log bernoulli in the logProb calculation in the 
         * {@link AdeWeightedMessageAnomalyScorerLogNormal} scorer.
         */
        double getMessageLogBernoulliWeight();
    }

    /** An interface for controlling database access */
    interface IDatabaseParameters {

        /** Returns the datastore's database url */
        String getDatabaseUrl();

        /** Returns the driver used to access the database */
        String getDatabaseDriver();

        /**
         * @return the username used to access the database
         */
        String getDatabaseUser();

        /**
         * @return the password used to access the database
         */
        String getDatabasePassword();

        /** Should the strings stored in the database (mainly text-sample and text-summary) be kept in the database */
        boolean keepOnlyAscii();

        /** Returns the type of the datastore.
         * Currently, only SQL database is supported
         */
        String getDataStoreType();

        DriverType getDriverType();

        boolean UNSAFE_DbNoLocks();

        String getDatabaseSchema();

        void setDatabasePassword(String password) throws AdeUsageException;
    }

    /** An interface for defining summary */
    interface ILogProcessingParameters {

        /**
         * @return the expiration time of the file size table 
         */
        long getFileSizeExpirationTime();

        /**
         * @return the times between updates of the file size database  
         */
        long getFileSizeUpdateTime();

        /**
         * @return The amount of milliseconds the java-tail process will sleep if no logs are changed.  
         */
        long getJavaTailSleepTime();

    }

    /** @return An interface for defining summary */
    ILogProcessingParameters logProcessing();

    /** @return An interface for scoring algorithms parameters */
    IScoringParameters scoring();

    /** @return An interface for debug parameters */
    IDebugParameters debug();

    /** @return An interface for database parameters */
    IDatabaseParameters database();

    /**
     * @return the number of periods required to exist in the database for a specific 
     * sourcegroup to train on.
     */
    int getMinimalRequieredTrainPeriod();

    /**
     * @return the possibly user provided class instantiation for generating
     * output file names
     */
    OutputFilenameGenerator getOutputFilenameGenerator();

    Class<? extends AnalysisGroupToFlowNameMapper> getAnalysisGroupToFlowNameMapper();

    boolean getOverrideVersionCheck();

    long getSourcesMinRefreshTime();

}
