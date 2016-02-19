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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.main.DatabaseManager;
import org.openmainframe.ade.ext.service.AdeExtInternalException;
import org.openmainframe.ade.ext.service.AdeExtUsageException;
import org.openmainframe.ade.ext.utils.NativeCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the base class for FFDC DB data collection utility classes.
 * 
 * This class will have the common behavior for all FFDC utility classes.
 * These include getting a connection to DB, getting environment
 * variables, archiving and compressing FFDC files, etc. 
 */
public abstract class DBDataCollector {
    /**
     * The name of the properties file from where we will read off DB config and
     * target back-up directory name.
     */
    private String propertiesFileName;

    /**
     * The name of the root FFDC directory,
     */
    private String ffdcRootDirectoryName;

    /**
     * The subdirectory where the individual FFDC files will be created.
     * The contents of this directory will be deleted once an archived and
     * compressed tarball is created.
     */
    private String ffdcDirectoryName;

    /**
     * The name of the properties file from where we will read off DB config
     * properties.
     * 
     * This can be overwritten by using the following Java property:
     * CONFIG_PROPERTY_FILE_NAME.
     */
    private static final String DEFAULT_CONFIG_FILE_NAME = "conf/setup.props";

    /**
     * The Java system property setting to overwrite the default config file.
     */
    private static final String CONFIG_PROPERTY_FILE_NAME = "ade.setUpFilePath";

    /**
     * This is the subdirectory in which the flat files that have the exported DB
     * rows will be stored.
     */
    private static final String DB_FFDC_SUB_DIRECTORY_NAME = "dbffdc";

    /**
     * The extension for the archived and compressed tarball.
     */
    public static final String ARCHIVED_COMPRESSED_FILE_EXTENSION = ".tgz";

    /**
     * The logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(DBDataCollector.class);

    /**
     * Instantiates a default object.
     */
    public DBDataCollector() {
        super();
        propertiesFileName = DEFAULT_CONFIG_FILE_NAME;
        final String userDefinedPropertiesFileName = System.getProperty(CONFIG_PROPERTY_FILE_NAME);
        if (userDefinedPropertiesFileName != null) {
            propertiesFileName = userDefinedPropertiesFileName;
        }
    }

    /**
     * Executes the FFDC data collection.
     * 
     * @param args arguments to the program
     * 
     * @throws AdeException if there is an issue in data collection
     */
    public final boolean run(String[] args) throws AdeException {
        Connection con = null;
        try {
            final boolean initialized = init();

            if (initialized) {
                /*
                 * Get a connection to DB.
                 */
                con = getDBConnection();

                if (con != null) {
                    final File ffdcDir = parseFFDCDBDataDirectory(args);

                    if (ffdcDir != null) {
                        collectFFDCDBData(con);

                        return true;
                    } else {
                        throw new AdeExtUsageException("FFDC DB directory " + args[0] + " could not be created.");
                    }
                } else {
                    throw new AdeExtUsageException("Could not connect to DB for FFDC data collection.");
                }
            } else {
                throw new AdeExtUsageException("Could not initialize the utility to collect FFDC DB data..");
            }
        } catch (AdeException e) {
            throw e;
        } catch (Exception e) {
            throw new AdeExtInternalException("DB FFDC collection failed", e);
        } catch (Throwable e) {
            throw new AdeExtInternalException("Internal bug", e);
        } finally {
            try {
                if (con != null) {
                    closeConnection(con);
                }
            } catch (AdeException e) {
                logger.error("Error encountered closing the connection.", e);
            }
        }
    }

    /**
     * Collects the FFDC DB data.
     *
     * @param con the connection to the DB
     * 
     * @throws an exception if there is a issue in collecting FFDC data from DB
     */
    abstract protected void collectFFDCDBData(Connection con) throws AdeException;

    /**
     * Forms the name of the flat file in which the contents of a DB table
     * will be stored.
     *
     * @param tableName the name of the DB table
     * 
     * @return the file name
     */
    protected final String formFFDCDBExportedTableFileName(String tableName) {
        final String outputFileName = ffdcDirectoryName
                + File.separator + tableName + ".dat";

        return outputFileName;
    }

    /**
     * Returns a DB connection to Ade DB.
     * 
     * @return a DB connection instance
     */
    protected final Connection getDBConnection() throws AdeException {
        final DatabaseManager dbManager = DatabaseManager.getDatabaseManager(propertiesFileName);
        final Connection con = dbManager.getConnection();

        return con;
    }

    /**
     * Closes the DB connection to Ade DB.
     * 
     * @param the DB connection instance
     */
    protected final void closeConnection(Connection con) throws AdeException {
        final DatabaseManager dbManager = DatabaseManager.getDatabaseManager(propertiesFileName);
        dbManager.close();
    }

    /**
     * Executes a query,
     * 
     * @param s the statement
     * @param sqlStatement the SQL statement
     * 
     * @return the result set from query
     */
    protected final ResultSet executeQuery(Statement s, String sqlStatement) {
        ResultSet resultSet = null;

        if (s == null) {
            logger.error("executeQuery() <--exit (error: Statement unavailable)");

            return null;
        }

        try {
            resultSet = s.executeQuery(sqlStatement);
            logger.debug("executeQuery() <--exit (normal): " + (null == resultSet));
        } catch (java.sql.SQLNonTransientException nte) {
            logger.debug("executeQuery(): SQLNonTransientException", nte);
        } catch (Exception e) {
            logger.error("executeQuery() <--exit (error: Unexpected exception caught)", e);
        }

        return resultSet;
    }

    /**
     * Create and return a JDBC Statement object associated with a given JDBC
     * Connection object. Specify properties which ensure that any ResultSet
     * object produced by the Statement is both scrollable and read-only.
     * 
     * @param con connection to DB
     * 
     * @return - JDBC Statement object
     * 
     * @throws if there is an issue in creating a statement
     */
    protected final Statement getStatement(Connection con) throws AdeException {
        Statement s = null;
        try {
            if (con != null) {
                s = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            }
        } catch (SQLException e) {
            throw new AdeExtInternalException("Cannot create statement.", e);
        }

        return s;
    }

    /**
     * Initializes the DB configuration properties and diagnostics directory
     * name for the exported DB files.
     * 
     * @return true, if can be initialized with required properties.
     */
    protected final boolean init() throws AdeException {
        boolean initialized = false;

        final Properties prop = new Properties();

        try (FileInputStream fis = new FileInputStream(propertiesFileName)) {
            prop.load(fis);
            initialized = true;
        } catch (FileNotFoundException e) {
            throw new AdeExtUsageException("Properties file " + propertiesFileName + " not found.", e);
        } catch (IOException e) {
            throw new AdeExtUsageException("Problem in reading properties file "
                    + propertiesFileName
                    + ".", e);
        } catch (Exception e) {
            throw new AdeExtUsageException("Problem in processing properties file "
                    + propertiesFileName
                    + ".", e);
        }

        return initialized;
    }

    /**
     * Creates the sub-directory where this particular back-up instance will be kept.
     * 
     * @param targetDirectory the target root directory for back-up
     */
    protected final File createDBFFDCDirectory() {
        File dbffdcDir = new File(ffdcDirectoryName);

        /*
         * First of all verify that the back-up directory exists.
         * If not, create it.
         */
        if (!dbffdcDir.exists()) {
            /*
             * Create the root directory for back-ups.
             */
            final boolean isDBFFDcDirCreated = dbffdcDir.mkdirs();
            if (isDBFFDcDirCreated) {
                dbffdcDir = new File(ffdcDirectoryName);

                return dbffdcDir;
            }
        } else if (!dbffdcDir.isDirectory()) {
            /*
             * This is a file and not a directory.  Cannot proceed.
             */
            return null;
        }

        return dbffdcDir;
    }

    /**
     * Creates a gzipped archive of the exported DB tables.
     *  
     * @throws Exception if there is a problem in creating the archive
     */
    protected final long archiveExportedDBTables() throws Exception {
        /*
         * Execute tar with gzip compression command in native environment
         * 
         * Command is:
         * 
         * tar -czf $archiveddbdir/foo.tgz -C $backupdbdir db/ 
         * creates gzipped tar archive of the directory bar located in $backupdbdir 
         * called foo.tgz to be created in $archiveddbdir.
         * 
         * tar -xzf foo.tgz -C bar/ extract gzipped foo.tgz after
         * changing directory to bar
         */
        final NativeCommand nativeCommand = new NativeCommand();
        final String compressedArchivedDBFileName = getArchivedCompressedDBName();
        final String command = "tar -zcvf " + compressedArchivedDBFileName
                + " -C "
                + ffdcRootDirectoryName
                + " "
                + DB_FFDC_SUB_DIRECTORY_NAME;

        logger.debug("Archiving exported DB tables using command \"" + command + "\"");

        final int rc = nativeCommand.exec(command);

        if (rc != 0) {
            final String failureReason = nativeCommand.getCommandOutput();

            throw new Exception(failureReason);
        }

        /*
         * Look up the size of the file.
         */
        long fileSize = 0;
        final File f = new File(compressedArchivedDBFileName);
        if (f.exists()) {
            fileSize = f.length();
        }

        /*
         * Now that the DB was archived successfully, remove the backed-up directory.
         */
        final String ffdcDBDirectoryName = ffdcDirectoryName;

        logger.debug("Deleting the temporary FFDC directory directory " + ffdcDBDirectoryName);
        deleteDBFFDCFiles(ffdcDBDirectoryName);

        return fileSize;
    }

    /**
     * Returns the name of the archived and compressed DB file.
     *
     * @return  the name of the archived and compressed DB file
     */
    protected final String getArchivedCompressedDBName(String[] args) {
        if (ffdcRootDirectoryName == null) {
            try {
                final String ffdcDir = getFFDCDBWorkspaceDirectory(args);

                if (ffdcDir == null) {
                    return null;
                }
            } catch (AdeException e) {
                logger.error("FFDC DB data directory name could not be parsed: ", e);

                return null;
            }
        }

        final String compressedArchivedDBFileName = ffdcRootDirectoryName + File.separator + DB_FFDC_SUB_DIRECTORY_NAME + ARCHIVED_COMPRESSED_FILE_EXTENSION;

        return compressedArchivedDBFileName;
    }

    /**
     * Returns the name of the archived and compressed DB file.
     *
     * @return  the name of the archived and compressed DB file
     */
    protected final String getArchivedCompressedDBName() {
        final String compressedArchivedDBFileName = ffdcRootDirectoryName + File.separator + DB_FFDC_SUB_DIRECTORY_NAME + ARCHIVED_COMPRESSED_FILE_EXTENSION;

        return compressedArchivedDBFileName;
    }

    /**
     * Deletes the backed-up DB directory.
     * 
     * Note that a directory can be deleted only if there are no files in it
     *
     * @param directoryToDelete the name of the backed-up DB directory to delete
     */
    protected final void deleteDBFFDCFiles(String directoryToDelete) throws AdeException {
        try {
            final File dirToDelete = new File(directoryToDelete);
            String[] fileList = dirToDelete.list();
            if (fileList.length == 0) {
                dirToDelete.delete();
            } else {
                for (int i = 0; i < fileList.length; i++) {
                    final File f = new File(dirToDelete, fileList[i]);
                    if (f.isDirectory()) {

                        final String filePath = f.getPath();
                        deleteDBFFDCFiles(filePath);

                        continue;
                    }

                    /*
                     * This is a file under Ade DB root directory, delete it.
                     */
                    f.delete();
                }

                /*
                 * Now, see if directory has no files.  If so, delete the directory itself.
                 */
                fileList = dirToDelete.list();
                if (fileList.length == 0) {
                    dirToDelete.delete();
                }
            }
        } catch (Exception e) {
            throw new AdeInternalException("Failed to delete the backed-up Ade DB directory", e);
        }
    }

    /**
     * Parses the FFDC DB data directory name,.
     * 
     * @param args arguments to the program
     */
    protected final File parseFFDCDBDataDirectory(String[] args) throws AdeException {
        if (args.length < 1) {
            usageError("Missing command-line arguments.");
        }

        logger.debug("Parsing arguments: args[0] = " + args[0]);

        ffdcRootDirectoryName = args[0];
        if (ffdcRootDirectoryName != null) {
            ffdcRootDirectoryName = ffdcRootDirectoryName.trim();
        }
        ffdcDirectoryName = ffdcRootDirectoryName + File.separator + DB_FFDC_SUB_DIRECTORY_NAME;

        final File ffdcDir = createDBFFDCDirectory();

        return ffdcDir;
    }

    /**
     * Parses the FFDC DB data directory name,.
     * 
     * @param args arguments to the program
     */
    protected final String getFFDCDBWorkspaceDirectory(String[] args) throws AdeException {
        if (ffdcDirectoryName != null) {
            return ffdcDirectoryName;
        }

        if (args.length < 1) {
            return null;
        }

        logger.debug("Parsing arguments: args[0] = " + args[0]);

        ffdcRootDirectoryName = args[0];
        if (ffdcRootDirectoryName != null) {
            ffdcRootDirectoryName = ffdcRootDirectoryName.trim();
        }
        ffdcDirectoryName = ffdcRootDirectoryName + File.separator + DB_FFDC_SUB_DIRECTORY_NAME;

        return ffdcDirectoryName;
    }

    /**
     * Returns the indicator that the subdirectory exists.
     *
     * @param args the arguments to parse
     * @return true, if the sub directory exists
     * @throws an exception if there is an issue
     */
    public final boolean doesFFDCDirectoryExist(String[] args) throws AdeException {
        final String dirName = getFFDCDBWorkspaceDirectory(args);
        if (dirName != null) {
            final File ffdcDir = new File(dirName);

            if (ffdcDir != null && ffdcDir.exists()) {
                return true;
            }

        }

        return false;
    }

    /**
     * Returns the FFDC workspace directory.
     *
     * @param args the arguments to parse
     * @return ffdc workspace directory
     */
    public final File getFFDCWorkspaceDirectory(String[] args) throws AdeException {
        final String dirName = getFFDCDBWorkspaceDirectory(args);
        if (dirName != null) {
            final File ffdcDir = new File(dirName);

            return ffdcDir;
        }

        return null;
    }

    /**
     * Handles usage errors when run as a standalone utility.
     *
     * @param errorMsg the error message 
     * @throws AdeUsageException usage exception
     */
    protected final void usageError(String errorMsg) throws AdeUsageException {
        System.err.flush();
        System.err.println("This program expects the following command-line parameter:");
        System.err.println("\tLocation of the FFDC DB data directory");
        System.err.flush();

        throw new AdeUsageException(errorMsg);
    }
}
