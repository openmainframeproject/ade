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

package org.openmainframe.ade.ext.main;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.service.AdeExtInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the utility class to collect FFDC DB data. When its main method is
 * executed, it takes a back-up of database, archives, and compresses the
 * contents of the backed-up DB.
 * 
 * The DB snapshot will be archived, compressed and stored in a directory.
 * 
 * Small amounts of data < 5k
 * 
 * Models contained within database
 * 
 *      controldb query "select * from models
 * 
 * Sources contained within database
 * 
 *      controldb query "select * from sources
 * 
 * Periods contained within database for each source
 * 
 *      controldb query "select period_internal_id, start_time, source_internal_id from periods
 * 
 * Messages contained within database
 * 
 *      controldb query "select * from message_ids
 */
public class DBMinimalFFDCDataCollector extends DBDataCollector {
    /**
     * The name of the models DB table.
     */
    private static String MODELS_TABLE_NAME = "MODELS";

    /**
     * The name of the sources DB table.
     */
    private static String SOURCES_TABLE_NAME = "SOURCES";

    /**
     * The name of the periods DB table.
     */
    private static String PERIODS_TABLE_NAME = "PERIODS";

    /**
     * The name of the message_ids DB table.
     */
    private static String MESSAGE_IDS_TABLE_NAME = "MESSAGE_IDS";

    /**
     * The list of DB tables to be exported minimally for problem analysis.
     */
    private static String[] MINIMAL_TABLES_TO_DUMP = { MODELS_TABLE_NAME, SOURCES_TABLE_NAME, PERIODS_TABLE_NAME, MESSAGE_IDS_TABLE_NAME };

    /**
     * Query to export a table to a flat file.
     */
    private static final String DB_QUERY_TO_EXPORT_DB_FILE = "CALL SYSCS_UTIL.SYSCS_EXPORT_TABLE (?,?,?,?,?,?)";

    /**
     * The logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(DBMinimalFFDCDataCollector.class);

    /**
     * The main method to call to use the FFDC DB data collection utility.
     * 
     * This program should be called as follows:
     * 
     *      java -cp $CLASSPATH org.openmainframe.ade.z.main.DatabaseFFDCCollector
     *  
     * @param args arguments to run this utility as a stand-alone program
     */
    public static void main(String[] args) {
        /*
         * Instantiate an instance and call the run method.
         */
        final DBMinimalFFDCDataCollector ffdcDBDataCollector = new DBMinimalFFDCDataCollector();

        try {
            final boolean isFFDCDBDataCollectionSuccessful = ffdcDBDataCollector.run(args);
            logger.info(isFFDCDBDataCollectionSuccessful ? "FFDC DB Data Collection was successful." : "FFDC DB Data Collection failed.");
        } catch (AdeException e) {
            logger.error("FFDC DB Data Collection failed.", e);
        }
    }

    /**
     * Instantiates a default object.
     */
    public DBMinimalFFDCDataCollector() {
        super();
    }

    /**
     * Captures the minimal amount of DB data for FFDC.
     * 
     * @param con connection to the database
     * @throws AdeException if there is a problem in exporting tables
     */
    protected final void collectFFDCDBData(Connection con) throws AdeException {
        try {
            captureMinimalDBData(con);

            final long fileLength = archiveExportedDBTables();
            logger.debug("The length of the compressed archive is " + fileLength);
        } catch (AdeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AdeExtInternalException("Exception in collecting FFDC DB data : ", e);
        }
    }

    /**
     * This method will export the minimal amount of data from database
     * for concurrent dump.
     * <p>
     * The list of tables are selected based on input from past analysis
     * as outlined below.  Note that we will ship the entire DB if it can
     * be compressed into a size that can be transmitted via call home.
     * <p>
     * This limit is 64 MBs as of zHelix GA1.
     * 
     * Small amounts of data < 5k
     * 
     *  Models contained within database
     * 
     *      controldb query "select * from models
     * 
     *  Sources contained within database
     * 
     *      controldb query "select * from sources
     * 
     *  Periods contained within database for each source
     * 
     *      controldb query "select period_internal_id, start_time, source_internal_id from periods
     * 
     *  Messages contained within database
     * 
     *       controldb query "select * from message_ids
     *
     * @param con connection to the database
     * @throws AdeException if there is a problem in exporting tables
     */
    protected final void captureMinimalDBData(Connection con) throws AdeException {
        logger.debug("Starting to export tables to DB.");
        PreparedStatement ps = null;

        String tableName = null;
        try {
            for (int i = 0; i < MINIMAL_TABLES_TO_DUMP.length; i++) {
                tableName = MINIMAL_TABLES_TO_DUMP[i];
                final String ffdcFileName = formFFDCDBExportedTableFileName(tableName);
                final File f = new File(ffdcFileName);

                /*
                 * If the exported file is there, delete it, otherwise the DB
                 * query to export will fail.
                 */
                if (f.exists()) {
                    f.delete();
                }

                logger.debug("Exporting table  " + tableName);
                ps = con.prepareStatement(DB_QUERY_TO_EXPORT_DB_FILE);
                ps.setString(1, null);
                ps.setString(2, tableName);
                ps.setString(3, ffdcFileName);
                ps.setString(4, ",");
                ps.setString(5, null);
                ps.setString(6, null);
                ps.execute();
            }
        } catch (SQLException e) {
            throw new AdeExtInternalException("Got an SQL exception", e);
        } catch (Exception e) {
            throw new AdeExtInternalException("Exception in exporting DB table, tableName=" + tableName, e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    logger.debug("Exception in closing statement.", e);
                }
            }
        }
    }
}