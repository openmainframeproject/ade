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
package org.openmainframe.ade.impl.dataStore;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.dbUtils.DriverType;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.summary.SummarizationProperties;
import org.openmainframe.ade.utils.patches.Version;

/**
 * SQL table definitions
 * 
 *
 */
public enum SQL {

    /**
     * SQL table definitions
     */

    TRACKED_FILE_SIZES("FILE_NAME    VARCHAR(200)  NOT NULL"
            + ",FILE_SIZE   BIGINT      NOT NULL"
            + ",LAST_UPDATE TIMESTAMP NOT NULL"
            + ",PRIMARY KEY (FILE_NAME)"),
    /**
     * Message id key used to track message strings or message identifier supplied as part
     * of message
     */
    MESSAGE_IDS("MESSAGE_INTERNAL_ID   INTEGER     GENERATED ALWAYS AS IDENTITY NOT NULL"
            + ",MESSAGE_ID           VARCHAR(200) NOT NULL"
            + ",PRIMARY KEY (MESSAGE_INTERNAL_ID)"),
    /**
     * Component id is name of component used to divide message strings into categories used
     * during generation of message ids
     */
    COMPONENT_IDS("COMPONENT_INTERNAL_ID  SMALLINT      GENERATED ALWAYS AS IDENTITY NOT NULL"
            + ",COMPONENT_ID          VARCHAR(200)  NOT NULL"
            + ",COMPONENT_DESC        VARCHAR(100) "
            + ",PRIMARY KEY (COMPONENT_INTERNAL_ID)"),
            
    /**
     * The membership rules associated with groups.
     */
    RULES("RULE_INTERNAL_ID INTEGER GENERATED ALWAYS AS IDENTITY NOT NULL, "
            + "RULE_NAME VARCHAR(200) NOT NULL, "
            + "DESCRIPTION VARCHAR(1000), "
            + "RULE VARCHAR(256) NOT NULL, "
            + "PRIMARY KEY (RULE_INTERNAL_ID)"),    
            
    /**
     * Source is the origin of the message stream    /**
     *  Stores group information that are associated with sources.
     */
    GROUPS("GROUP_INTERNAL_ID INTEGER GENERATED ALWAYS AS IDENTITY NOT NULL, "
            + "GROUP_NAME VARCHAR(200) NOT NULL, "
            + "GROUP_TYPE SMALLINT NOT NULL, "
            + "DATA_TYPE SMALLINT NOT NULL,"
            + "RULE_INTERNAL_ID INTEGER NOT NULL,"
            + "EVALUATION_ORDER SMALLINT NOT NULL,"
            + "PRIMARY KEY (GROUP_INTERNAL_ID)"),          
            

    SOURCES("SOURCE_INTERNAL_ID   INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)"
            + ",SOURCE_ID           VARCHAR(200) NOT NULL"
            +",ANALYSIS_GROUP       INTEGER "
            + ",FILE_NAME           VARCHAR(200)"
            + ",LOG_TYPE            VARCHAR(200)"
            + ",PRIMARY KEY (SOURCE_INTERNAL_ID)"),          

    /**
     * Periods is the time period (days) used for creation of model of expected behavior 
     */
    PERIODS("PERIOD_INTERNAL_ID         INTEGER       GENERATED ALWAYS AS IDENTITY NOT NULL"
            + ",SOURCE_INTERNAL_ID       INTEGER "
            + ",EXCLUDE_FROM_TRAINING    SMALLINT"
            + ",STATUS                   INTEGER"
            + ",COMMENT                  VARCHAR(200)"
            + ",START_TIME               TIMESTAMP NOT NULL"
            + ",END_TIME                 TIMESTAMP NOT NULL"
            + ",PRIMARY KEY (PERIOD_INTERNAL_ID)"
            + ",FOREIGN KEY (SOURCE_INTERNAL_ID) REFERENCES " + SOURCES + " (SOURCE_INTERNAL_ID) ON DELETE CASCADE ON UPDATE RESTRICT"),
    /**
     * Period summaries tracks the periods which have a message stream available for a particular 
     * source
     */
    PERIOD_SUMMARIES("PERIOD_SUMMARY_INTERNAL_ID  INTEGER       GENERATED ALWAYS AS IDENTITY NOT NULL"
            + ",PERIOD_INTERNAL_ID         INTEGER "
            + ",SUMMARY_TYPE_INTERNAL_ID   SMALLINT"
            + ",PRIMARY KEY (PERIOD_SUMMARY_INTERNAL_ID)"
            + ",FOREIGN KEY (PERIOD_INTERNAL_ID) REFERENCES  " + PERIODS + " (PERIOD_INTERNAL_ID) ON DELETE CASCADE ON UPDATE RESTRICT"),
    /**
     * Periods are divided into a set number intervals
     * Interval keeps information about each interval with a period for a message stream
     */
    INTERVALS("PERIOD_SUMMARY_INTERNAL_ID       INTEGER     NOT NULL "
            + ",INTERVAL_SERIAL_NUM             INTEGER     NOT NULL "
            + ",NUM_UNIQUE_MESSAGE_IDS          INTEGER     NOT NULL "
            + ",INTERVAL_START_TIME             BIGINT      NOT NULL"
            + ",CLASSIFICATION_INTERNAL_ID      SMALLINT    NOT NULL "
            + ",ADE_VERSION                  VARCHAR(" + Version.VERSION_MAX_STRING_LENGTH + ") NOT NULL "
            + ",COVERAGE_FACTOR                 DOUBLE"
            + ",PRIMARY KEY (PERIOD_SUMMARY_INTERNAL_ID,INTERVAL_SERIAL_NUM)"
            + ",FOREIGN KEY (PERIOD_SUMMARY_INTERNAL_ID) REFERENCES " + PERIOD_SUMMARIES + " (PERIOD_SUMMARY_INTERNAL_ID) ON DELETE CASCADE ON UPDATE RESTRICT"),
    /**
     * Summary of message ids within an intervals
     */
    MESSAGE_SUMMARIES("PERIOD_SUMMARY_INTERNAL_ID  INTEGER "
            + ",INTERVAL_SERIAL_NUM        INTEGER    NOT NULL "
            + ",MESSAGE_INTERNAL_ID        INTEGER   NOT NULL "
            + ",NUM_MESSAGES               INTEGER    NOT NULL "
            + ",TEXT_SUMMARY               VARCHAR(1000) "
            + ",TEXT_SAMPLE                VARCHAR(1500) "
            + ",CRITICAL_WORDS_SCORE       INTEGER"
            + ",SEVERITY                   INTEGER   NOT NULL"
            + ",ENCODED_TIME_VECTOR	CHAR(" + SummarizationProperties.ENCODED_TIMELINE_LENGTH + ")"
            + ",FOREIGN KEY (PERIOD_SUMMARY_INTERNAL_ID) REFERENCES " + PERIOD_SUMMARIES + " (PERIOD_SUMMARY_INTERNAL_ID) ON DELETE CASCADE ON UPDATE RESTRICT"),             
             
    /**
     * Store description of model when created, for what time frame, and which analysis group
     */
    MODELS("MODEL_INTERNAL_ID      INTEGER      GENERATED ALWAYS AS IDENTITY NOT NULL "
            + ",ANALYSIS_GROUP        INTEGER"
            + ",START_TIME            TIMESTAMP       NOT NULL "
            + ",END_TIME              TIMESTAMP       NOT NULL "
            + ",IS_DEFAULT            SMALLINT        NOT NULL "
            + ",EXTERNAL_FILE_NAME    VARCHAR(200)"
            + ",ADE_VERSION        VARCHAR(" + Version.VERSION_MAX_STRING_LENGTH + ") NOT NULL"
            + ",CREATION_TIME         TIMESTAMP NOT NULL"
            + ",PRIMARY KEY (MODEL_INTERNAL_ID) "
            + ",FOREIGN KEY (ANALYSIS_GROUP) REFERENCES " + GROUPS + " (GROUP_INTERNAL_ID) "
                    + "ON DELETE CASCADE ON UPDATE RESTRICT"),

    /**
     * Used to create a message id from a text string using Levenshtein distance 
     */
    TEXT_CLUSTERS("TEXT_CLUSTER_INTERNAL_ID    INTEGER       GENERATED ALWAYS AS IDENTITY NOT NULL"
            + ",COMPONENT_INTERNAL_ID      SMALLINT      NOT NULL"
            + ",TEXT_REPRESENTATIVE        VARCHAR(1000) NOT NULL"
            + ",LAST_OBSERVED              TIMESTAMP     NOT NULL"
            + ",PRIMARY KEY (TEXT_CLUSTER_INTERNAL_ID) "
            + ",FOREIGN KEY (COMPONENT_INTERNAL_ID) REFERENCES " + COMPONENT_IDS + " (COMPONENT_INTERNAL_ID) ON DELETE CASCADE ON UPDATE RESTRICT "),

    /**
    * Contains information about the interval during processed during analyze
    */
    ANALYSIS_RESULTS("PERIOD_INTERNAL_ID     INTEGER    NOT NULL"
            + ",START_TIME             BIGINT     NOT NULL"
            + ",INTERVAL_SERIAL_NUM    INTEGER    NOT NULL"
            + ",INTERVAL_SCORE         DOUBLE     NOT NULL"
            + ",NUM_UNIQUE_MESSAGE_IDS INTEGER    NOT NULL"
            + ",ADE_VERSION VARCHAR(" + Version.VERSION_MAX_STRING_LENGTH + ") NOT NULL"
            + ",MODEL_INTERNAL_ID      INTEGER    NOT NULL"
            + ",PRIMARY KEY (PERIOD_INTERNAL_ID,INTERVAL_SERIAL_NUM)"
            + ",FOREIGN KEY (PERIOD_INTERNAL_ID) REFERENCES " + PERIODS + " (PERIOD_INTERNAL_ID) ON DELETE CASCADE"),

    /**
     * Stores ade version and patch level
     */
    ADE_VERSIONS("ADE_VERSION VARCHAR(" + Version.VERSION_MAX_STRING_LENGTH + ") NOT NULL, "
            + "PATCHED_TIME TIMESTAMP NOT NULL");
    /*
     * Length of text strings added to dictionary of "words" within text stream
     */
    public final static int MAX_LEN_DICTIONARY = 200;
    /**
     * Length of text string extracted from text stream
     */
    public final static int MAX_LEN_TEXT = 1000;

    private final String m_create;

    SQL(String create) {
        this.m_create = create;
    }

    @Override
    public String toString() {
        String schema;
        try {
            schema = Ade.getAde().getConfigProperties().database().getDatabaseSchema();
        } catch (AdeException e) {
            throw new IllegalStateException(e);
        }
        return schema == null ? super.toString() : schema + "." + super.toString();
    };

    /**
     *  Return table definition
     *  
     *  @param 
     *  @return string containing SQL table definitions
     */
    public String create() throws AdeException {
        String createString = m_create;

        final String driver = Ade.getAde().getConfigProperties().database().getDatabaseDriver();

        if ((DriverType.parseDriverType(driver) == DriverType.MY_SQL) ||
            (DriverType.parseDriverType(driver) == DriverType.MARIADB)) {
            createString = createString.replace("GENERATED ALWAYS AS IDENTITY NOT NULL", "NOT NULL AUTO_INCREMENT");
            createString = createString.replace("GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)", "AUTO_INCREMENT");
        }
        return createString;
    }
}
