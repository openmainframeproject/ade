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
package org.openmainframe.ade.ext.utils;

import java.util.StringTokenizer;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.utils.ConfigPropertiesWrapper;

/** Configuration properties specific to extensions */
public class AdeExtConfigProperties {

    /* Prefix used by ext-specific property names */
    private static final String ADE_EXT_PREFIX = "adeext.";

    /* Constants for config property key names */
    private static final String RMI_PORT_PARAM = "adeext.rmi.port";
    private static final String RMI_CODE_BASE_PARAM = "adeext.rmi.codeBase";
    private static final String TRAINING_SCRIPT_FILE_PARAM = "adeext.trainingScript.file";
    private static final String RUNTIME_MODEL_DATA_STORE_AT_SOURCE = "adeext.runtimeModelDataStoreAtSource";
    private static final String PARSE_ERROR_TO_KEEP = "adeext.parseErrorToKeep";
    private static final String PARSE_ERROR_DAYS_TO_TOLERATE = "adeext.parseErrorDaysTolerate";
    private static final String PARSE_ERROR_TRACK_NULL_COMPONENT = "adeext.parseErrorTrackNullComponent";
    private static final String MSG_RATE_REPORT_FREQ = "adeext.msgRateReportFreq";
    private static final String MSG_RATE_MSG_TO_KEEP = "adeext.msgRateMsgToKeep";
    private static final String MSG_RATE_10MIN_SLOTS_TO_KEEP = "adeext.msgRate10MinSlotsToKeep";
    private static final String MSG_RATE_10MIN_SUBINTERVAL_LIST = "adeext.msgRate10MinSubIntervalList";
    private static final String MSG_RATE_MERGE_SOURCE = "adeext.msgRateMergeSource";
    private static final String STATS_ROOT_DIR = "adeext.statsRootDir";
    private static final String USE_SPARK = "adeext.useSparkLogs";
  
    /* Constants for config property default values */
    private static final String DEFAULT_STATS_ROOT_DIR = "output/ade-stats";

    /* Member variables */
    private final String m_rmiServerCodeBase;
    private final String m_trainingScriptFilePath;
    private final int m_rmiPort;
    private final Boolean m_isRuntimeModelDataStoreAtSource;
    private final int m_parseErrorsToKeep;
    private final int m_parseErrorDaysToTolerate;
    private final boolean m_parseErrorTrackNullComponent;
    private final String m_msgRateReportReq;
    private final int m_msgRateMsgToKeep;
    private final short m_msgRate10MinIntervalToKeep;
    private final short[] m_msgRate10MinSubIntervalList;
    private final boolean m_isMsgRateMergeSource;
    private final String m_statsRootDir;
    private final boolean m_useSparkLogs;

    /**
     * Set the AdeExtConfigProperties from the specified property file.
     * 
     * @param propertyFile - The property file
     * @throws AdeException if any of properties could not be parsed or 
     *     if any required properties are missing.
     */
    public AdeExtConfigProperties(String propertyFile) throws AdeException {
        final ConfigPropertiesWrapper m_props = new ConfigPropertiesWrapper(ADE_EXT_PREFIX);

        m_props.addPropertyFile(propertyFile);
        m_props.addProperties(System.getProperties(), "system property");

        m_rmiPort = m_props.getIntProperty(RMI_PORT_PARAM, 0);
        m_rmiServerCodeBase = m_props.getStringProperty(RMI_CODE_BASE_PARAM, null);
        m_trainingScriptFilePath = m_props.getStringProperty(TRAINING_SCRIPT_FILE_PARAM, null);

        /* Optional properties */
        if (m_props.containsKey(PARSE_ERROR_TO_KEEP)) {
            m_parseErrorsToKeep = m_props.getIntProperty(PARSE_ERROR_TO_KEEP);
        } else {
            m_parseErrorsToKeep = -1;
        }

        if (m_props.containsKey(PARSE_ERROR_DAYS_TO_TOLERATE)) {
            m_parseErrorDaysToTolerate = m_props.getIntProperty(PARSE_ERROR_DAYS_TO_TOLERATE);
        } else {
            m_parseErrorDaysToTolerate = -1;
        }

        if (m_props.containsKey(PARSE_ERROR_TRACK_NULL_COMPONENT)) {
            m_parseErrorTrackNullComponent = m_props.getBooleanProperty(PARSE_ERROR_TRACK_NULL_COMPONENT);
        } else {
            m_parseErrorTrackNullComponent = false;
        }

        if (m_props.containsKey(MSG_RATE_MSG_TO_KEEP)) {
            m_msgRateMsgToKeep = m_props.getIntProperty(MSG_RATE_MSG_TO_KEEP);
        } else
            m_msgRateMsgToKeep = -1;

        if (m_props.containsKey(MSG_RATE_10MIN_SLOTS_TO_KEEP)) {
            m_msgRate10MinIntervalToKeep = (short) m_props.getIntProperty(MSG_RATE_10MIN_SLOTS_TO_KEEP);
        } else {
            m_msgRate10MinIntervalToKeep = -1;
        }

        if (m_props.containsKey(MSG_RATE_MERGE_SOURCE)) {
            m_isMsgRateMergeSource = m_props.getBooleanProperty(MSG_RATE_MERGE_SOURCE);
        } else {
            m_isMsgRateMergeSource = false;
        }

        if (m_props.containsKey(RUNTIME_MODEL_DATA_STORE_AT_SOURCE)) {
            m_isRuntimeModelDataStoreAtSource = m_props.getBooleanProperty(RUNTIME_MODEL_DATA_STORE_AT_SOURCE);
        } else {
            m_isRuntimeModelDataStoreAtSource = null;
        }

        /* Retrieve the list of subinterval list */
        if (m_props.containsKey(MSG_RATE_10MIN_SUBINTERVAL_LIST)) {
            final String subIntervalList = m_props.getStringProperty(MSG_RATE_10MIN_SUBINTERVAL_LIST);
            final StringTokenizer tokenizer = new StringTokenizer(subIntervalList, ",");
            m_msgRate10MinSubIntervalList = new short[tokenizer.countTokens()];
            for (int i = 0; i < m_msgRate10MinSubIntervalList.length; i++) {
                m_msgRate10MinSubIntervalList[i] = Short.parseShort(tokenizer.nextToken());
            }
        } else {
            m_msgRate10MinSubIntervalList = null;
        }

        /* Msg Rate Report Feq must be either MONTHLY or a numeric number */
        if (m_props.containsKey(MSG_RATE_REPORT_FREQ)) {
            m_msgRateReportReq = m_props.getStringProperty(MSG_RATE_REPORT_FREQ);
            boolean isNumber = false;
            try {
                Integer.parseInt(m_msgRateReportReq);
                isNumber = true;
            } catch (Exception e) {
            }
            if (!m_msgRateReportReq.equalsIgnoreCase("MONTHLY") &&
                    !isNumber) {
                throw new AdeUsageException(MSG_RATE_REPORT_FREQ + " must be either \"MONTHLY\" or number");
            }
        } else {
            m_msgRateReportReq = "";
        }

        if (m_props.containsKey(STATS_ROOT_DIR)) {
            m_statsRootDir = m_props.getStringProperty(STATS_ROOT_DIR);
        } else {
            m_statsRootDir = DEFAULT_STATS_ROOT_DIR;
        }

        /* Type of logs to use. True: Spark logs. Defaults to Linux Syslogs */

        if (m_props.containsKey(USE_SPARK)){
            m_useSparkLogs = m_props.getBooleanProperty(USE_SPARK);
        }
        else{
            m_useSparkLogs = false;
        }

        m_props.verifyAllPropertiesUsed();
    }

    /** Determines port that Training Queue Server and Client(s) share */
    public final int getRmiPort() {
        return m_rmiPort;
    }

    /** This is essentially just the ADE Classpath */
    public final String getRmiServerCodeBase() {
        return m_rmiServerCodeBase;
    }

    /** Locate train command script to be invoked by Training Queue Manager */
    public final String getTrainingScriptFilePath() {
        return m_trainingScriptFilePath;
    }

    /** Number of Parse Error to keep */
    public final int getParseErrorsToKeep() {
        return m_parseErrorsToKeep;
    }

    /** Parse Error Days to tolerate */
    public final int getParseErrorDaysToTolerate() {
        return m_parseErrorDaysToTolerate;
    }

    /** Whether we want to track null component name as error */
    public final boolean isParseErrorTrackNullComponent() {
        return m_parseErrorTrackNullComponent;
    }

    /** Return the message rate report frequency  */
    public final String getMsgRateReportFreq() {
        return m_msgRateReportReq;
    }

    /** Return the message rate report frequency  */
    public final int getMsgRateMsgToKeep() {
        return m_msgRateMsgToKeep;
    }

    /** Return the message rate report frequency  */
    public final short getMsgRate10MinSlotsToKeep() {
        return m_msgRate10MinIntervalToKeep;
    }

    /** Return the subinterval arrays */
    public final short[] getMsgRate10MinSubIntervals() {
        return m_msgRate10MinSubIntervalList;
    }

    /** Return whether all sources should be merged for the message rate */
    public final boolean isMsgRateMergeSource() {
        return m_isMsgRateMergeSource;
    }

    /** Whether the runtimeModelData should be stored at source level, and null if it's undefined */
    public final Boolean isRuntimeModelDataStoreAtSource() {
        return m_isRuntimeModelDataStoreAtSource;
    }

    /** Return if we're using Spark logs or Linux Syslogs. (true implies Spark logs) */
    public final Boolean isSparkLog(){
        return m_useSparkLogs;
    }

    /**
     * Returns the root directory where statistics are written.
     * 
     * @return the root directory where statistics are written. 
     */
    public final String getStatsRootDir() {
        return m_statsRootDir;
    }
}
