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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.service.AdeExtInternalException;
import org.openmainframe.ade.ext.service.AdeExtUsageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utility class exports all DB tables to flat files. All columns of each
 * DB table will be exported with the exception of MESSAGE_SUMMARIES table. We
 * chose to exclude the following columns of the MESSAGE_SUMMARIES table:
 * 
 * <ul>
 * 
 * <il>TEXT_SUMMARY<il>
 * 
 * <il>TEXT_SAMPLE</il>
 * 
 * <il>CRITICAL_WORDS_SCORE</il>
 * 
 * </ul>
 * 
 * Removal of these columns significantly reduces the disk space required to
 * export the contents of MESSAGE_SUMMARIES table.
 */
public class ExportAllDBTables extends DBDataCollector {
	/**
	 * The size of the compressed archived DB file in bytes.
	 */
	private long fileLength;

	/**
	 * The name of the message summaries table.
	 */
	private static final String MESSAGE_SUMMARIES_TABLE = "MESSAGE_SUMMARIES";

	/**
	 * The columns to be exported from MESSAGE_SUMMARIES table.
	 */
	private static final String SELECTED_COLUMNS = "PERIOD_SUMMARY_INTERNAL_ID, INTERVAL_SERIAL_NUM, MESSAGE_INTERNAL_ID, NUM_MESSAGES";

	/**
	 * The query to read off DB table names.
	 */
	private static final String DB_QUERY_FOR_FILENAMES = "SELECT TABLENAME FROM sys.sysschemas s, sys.systables t  WHERE s.schemaid = t.schemaid AND schemaname = 'DBUSER' ";

	/**
	 * The query to export a DB table to a flat file.
	 */
	private static final String DB_QUERY_TO_EXPORT_DB_FILE = "CALL SYSCS_UTIL.SYSCS_EXPORT_TABLE (?,?,?,?,?,?)";

	/**
	 * The logger for this class.
	 */
	private static final Logger logger = LoggerFactory
			.getLogger(ExportAllDBTables.class);

	/**
	 * This program will be executed as follows:
	 * 
	 * java -cp $ADE_CLASSPATH org.openmainframe.ade.z.main.ExportAllDBTables
	 * $FFDC_DIR
	 * 
	 * The only input to this program is the location of the FFDC directory.
	 * 
	 * @param args
	 *            arguments to the program
	 */
	public final static void main(String[] args) {
		final ExportAllDBTables exportTables = new ExportAllDBTables();
		try {
			final boolean hasRunSuccessfully = exportTables.run(args);
			if (hasRunSuccessfully) {
				final long flength = exportTables.getFileLength();
				logger.info("DB tables exported successfully.  The size of the compressed archive is "
						+ flength + " bytes.");
			} else {
				logger.info("DB tables could not be exported.");
			}
		} catch (AdeException e) {
			logger.error("Exporting DB tables to flat files failed.", e);
		}
	}

	/**
	 * Instantiates a default instance.
	 */
	public ExportAllDBTables() {
		super();
		try {
			init();
		} catch (AdeException e) {
			logger.error("Problem in initialization: " + e);
		}
	}

	/**
	 * Returns the length of the archived and compressed DB file.
	 *
	 * @return the file length
	 */
	public final long getFileLength() {
		return fileLength;
	}

	/**
	 * Returns the archived and compressed DB file.
	 *
	 * @return the archived and compressed DB file
	 */
	public final File getArchivedCompressedDBFile() {
		final String fname = getArchivedCompressedDBName();
		return new File(fname);
	}

	/**
	 * Gathers the contents of every DB table in flat files. Each DB table will
	 * be exported to a flat file that will have the name of the DB table.
	 * 
	 * @param con
	 *            connection to DB
	 */
	protected final void collectFFDCDBData(Connection con) throws AdeException {
		try {
			/*
			 * First, export all DB tables into flat files.
			 */
			exportDBTables(con);

			/*
			 * Then, archive and compress the files.
			 */
			fileLength = archiveExportedDBTables();
		} catch (AdeException e) {
			throw e;
		} catch (Exception e) {
			throw new AdeExtUsageException("Exception in exporting files : ", e);
		}
	}

	/**
	 * Exports all tables into flat files.
	 * 
	 * @param con
	 *            connection to DB
	 * 
	 * @throws AdeException
	 *             if there is an issue with consistency check
	 */
	private void exportDBTables(Connection con) throws AdeException {
		logger.debug("Starting to export tables to DB.");

		/*
		 * Read off all table names
		 */
		final String fileNamesQueryStr = DB_QUERY_FOR_FILENAMES;

		Statement statement = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			statement = getStatement(con);
			rs = executeQuery(statement, fileNamesQueryStr);

			while (rs != null && rs.next()) {
				final String tableName = rs.getString(1);
				final String ffdcFileName = formFFDCDBExportedTableFileName(tableName);
				final File f = new File(ffdcFileName);

				/*
				 * If the exported file is there, delete it, otherwise the DB
				 * query to export will fail.
				 */
				if (f.exists()) {
					f.delete();
				}

				/*
				 * All tables other than message summaries will be exported as
				 * is.
				 */
				if (!tableName.equals(MESSAGE_SUMMARIES_TABLE)) {
					logger.debug("Exporting table  " + tableName + " to file "
							+ ffdcFileName);
					ps = con.prepareStatement(DB_QUERY_TO_EXPORT_DB_FILE);
					ps.setString(1, null);
					ps.setString(2, tableName);
					ps.setString(3, ffdcFileName);
					ps.setString(4, ",");
					ps.setString(5, null);
					ps.setString(6, null);
					ps.execute();
				} else {
					/*
					 * Special treatment for the monster.
					 * 
					 * We will export only a subset of MESSAGE_SUMMARIES table,
					 * that is, we will throw away three columns.
					 */
					readMessageSummariesTable(con);
				}
			}
		} catch (SQLException e) {
			throw new AdeExtInternalException("Got an SQL exception=" + e);
		} catch (AdeException e) {
			throw e;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					logger.error("Exception in closing result set.", e);
				}
			}

			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					logger.error("Exception in closing statement.", e);
				}
			}

			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					logger.error("Exception in closing statement.", e);
				}
			}
		}

	}

	/**
	 * Exports the contents of the MESSAGE_SUMMARIES table by excluding
	 * TEXT_SUMMARY, TEXT_SAMPLE, and CRITICAL_WORDS_SCORE. Discarding these
	 * columns helps cut down the size of the content significantly. Also, these
	 * columns are not needed for problem analysis.
	 * 
	 * @param con
	 *            connection to DB
	 * @throws AdeException
	 *             if there is an issue in exporting MESSAGE_SUMMARIES table
	 */
	private void readMessageSummariesTable(Connection con) throws AdeException {
		FileOutputStream fos = null;
		Writer w = null;
		PreparedStatement s = null;
		try {
			/*
			 * PERIOD_SUMMARY_INTERNAL_ID INTERVAL_SERIAL_NUM
			 * MESSAGE_INTERNAL_ID NUM_MESSAGES TEXT_SUMMARY TEXT_SAMPLE
			 * CRITICAL_WORDS_SCORE
			 */
			final String outputFileName = formFFDCDBExportedTableFileName(MESSAGE_SUMMARIES_TABLE);
			fos = new FileOutputStream(outputFileName);
			w = new BufferedWriter(new OutputStreamWriter(fos,
					StandardCharsets.UTF_8));
		    s = con.prepareStatement("SELECT "
					+ SELECTED_COLUMNS + " FROM " + MESSAGE_SUMMARIES_TABLE);
			final ResultSet resultSet = s.executeQuery();
			while (resultSet.next()) {
				final int periodSummaryInternalID = resultSet.getInt(1);
				final int intervalSerialNum = resultSet.getInt(2);
				final int messageIntervalId = resultSet.getInt(3);
				final int numMessages = resultSet.getInt(4);
				w.write(periodSummaryInternalID + "," + intervalSerialNum + ","
						+ messageIntervalId + "," + numMessages + "\n");

			}

		} catch (SQLException e) {
			throw new AdeExtInternalException("SQLException caught: " + e, e);
		} catch (IOException e) {
			throw new AdeExtInternalException("IOException caught: " + e, e);
		} finally {
			try {
				if (s != null){
					s.close();
				}
				if (w != null) {
					w.flush();
					w.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (SQLException e) {
				logger.error("SQLException caught during close: " + e, e);	
			} catch (IOException e) {
				logger.error("IOException caught during close: " + e, e);
			}
		}
	}
}