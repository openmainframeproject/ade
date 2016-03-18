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

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.dbUtils.DriverType;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.summary.SummarizationProperties;
import org.openmainframe.ade.utils.patches.Version;

/** Create sql statements of ext-specific tables */
public enum EXT_TABLES_SQL {

    LOG_FILES(
            "LOG_FILE_INTERNAL_ID  INTEGER  GENERATED ALWAYS AS IDENTITY NOT NULL,"
                    + "FILE_NAME  VARCHAR(200) NOT NULL, "
                    + "START_TIME  TIMESTAMP, "
                    + "END_TIME  TIMESTAMP, "
                    + "UPLOAD_TIME  TIMESTAMP NOT NULL, "
                    + "PRIMARY KEY (LOG_FILE_INTERNAL_ID)"),

    MANAGED_SYSTEMS(
            "SYS_INTERNAL_ID  INTEGER  GENERATED ALWAYS AS IDENTITY NOT NULL,"
                    + "SOURCE_INTERNAL_ID  INTEGER  NOT NULL, "
                    + "GMT_OFFSET  INTEGER, "
                    + "OPERATING_SYSTEM  VARCHAR(100), "
                    + "PRIMARY KEY  (SYS_INTERNAL_ID),"
                    + "FOREIGN KEY (SOURCE_INTERNAL_ID) REFERENCES SOURCES (SOURCE_INTERNAL_ID)"
                    + " ON DELETE CASCADE ON UPDATE RESTRICT"),                   

    GROUP_TIMESTAMPS("LAST_TIMESTAMP   TIMESTAMP"),

    ANALYSIS_RESULTS_ADEEXT(
            " PERIOD_INTERNAL_ID INTEGER NOT NULL, "
                    + "INTERVAL_SERIAL_NUM INTEGER NOT NULL, "
                    + "NUM_NEW_MESSAGES INTEGER NOT NULL, "
                    + "NUM_NEVER_SEEN_BEFORE_MESSAGES INTEGER NOT NULL, "
                    + "LIMITED_MODEL VARCHAR(7) NOT NULL, "
                    + "PRIMARY KEY (PERIOD_INTERNAL_ID, INTERVAL_SERIAL_NUM)");

    private final String m_create;

    EXT_TABLES_SQL(String create) {
        this.m_create = create;
    }

    public String create() throws AdeException {
        String createString = m_create;

        final String driver = Ade.getAde().getConfigProperties().database().getDatabaseDriver();

        if ((DriverType.parseDriverType(driver) == DriverType.MY_SQL) ||
            (DriverType.parseDriverType(driver) == DriverType.MARIADB)) {
            createString = createString.replace("GENERATED ALWAYS AS IDENTITY NOT NULL", "NOT NULL AUTO_INCREMENT");
        }
        return createString;
    }
}
