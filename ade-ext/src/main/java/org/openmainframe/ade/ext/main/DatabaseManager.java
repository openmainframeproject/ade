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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.ext.service.AdeExtInternalException;
import org.openmainframe.ade.ext.service.AdeExtUsageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a wrapper for DB access.
 * <p>
 * This is similar to the MyJDBCConnection class.  The reason we are introducing
 * this class is because MyJDBCConnection class under the covers relies 
 * on an individual Ade instance to be created, which requires an individual
 * system properties file.  However, we now have utilities that are common
 * to all, like DB back-up, restore, and consistency check, that cannot be
 * logically tied to a single Ade instance.
 * <p>
 * This class reads in the DB config parameters from a properties file given to it.
 * Therefore, in principle one can use this class from individual Ade instances
 * as well.
 */
public class DatabaseManager {
    /**
     * The name of the properties file from which DB config parameters
     * will be read.
     */
    private String propertiesFileName;

    /**
     * The indicator whether DB parameters are initialized or not.
     */
    private boolean dbPropertiesInitialized;

    /**
     * The DB driver to read off from config file.
     */
    private String dbDriver;

    /**
     * The DB URL to read off.
     */
    private String dbURL;

    /**
     * The DB name to parse from DB URL.
     */
    private String dbName;

    /**
     * The DB user name, if exists, from config file.
     */
    private String dbUserName;

    /**
     * The DB user password, if exists, from config file.
     */
    private String dbUserPassword;

    /**
     * The singleton DB connetcion.
     */
    private static Connection dbConnection = null;

    /**
     * Connection to the DB in embedded mode.
     */
    private static Connection embeddedDBConnection = null;

    /**
     * The singleton instance for this class.
     */
    private static DatabaseManager manager = null;

    /**
     * The logger for this class.
     */
    private static final Logger mLogger = LoggerFactory.getLogger(DatabaseManager.class);

    // database URLs

    public static final String DATABASE_URL_PARAM = "ade.databaseUrl";

    public static final String DATABASE_DRIVER_PARAM = "ade.databaseDriver";

    public static final String DATABASE_PASSWORD_PARAM = "ade.databasePassword";

    public static final String DATABASE_USER_PARAM = "ade.databaseUser";

    /**
     * Derby JDBC driver.
     */
    private static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    /**
     * Derby DB protocol.
     */
    private static final String PROTOCOL = "jdbc:derby:";

    /**
     * DB name to be used in embedded connection.
     */
    private static final String DEFAULT_DBNAME = "databases/db1";

    /**
     * Double-boot SQL state.
     * <p>
     * This state will be associated with an exception thrown 
     * when embedded Derby is in use and another process
     * attempts to connect to it.
     */
    public static final String DOUBLE_BOOT_SQL_STATE = "XSDB6";

    /**
     * Double-boot error code. 
     * <p>
     * This error code will be associated with an exception thrown 
     * when embedded Derby is in use and another process
     * attempts to connect to it.
     */
    public static final int DOUBLE_BOOT_ERROR_CODE = 45000;

    /**
     * Instantiates the manager from a given properties file.
     *
     * @param propertiesFileName the file from where config parameters will be read off
     */
    private DatabaseManager(String propertiesFileName) {
        super();
        this.propertiesFileName = propertiesFileName;
        try {
            init();
        } catch (AdeException e) {
            mLogger.error("Error encountered initilizing the database manager.", e);
            dbPropertiesInitialized = false;
        }
    }

    /**
     * Returns the singleton instance for this class.
     *
     * @param propertiesFileName the file from where config parameters will be read off
     * @return the singleton instance
     */
    static public DatabaseManager getDatabaseManager(String propertiesFileName) {
        if (manager == null) {
            manager = new DatabaseManager(propertiesFileName);
        }

        return manager;
    }

    /**
     * Initializes the DB configuration properties and back-up directory name.
     * 
     * @return true, if can be initialized with required properties.
     */
    private void init() throws AdeException {
        dbPropertiesInitialized = false;

        final Properties prop = new Properties();

        try (FileInputStream fis = new FileInputStream(propertiesFileName)) {
            prop.load(fis);

            /*
             * Read off DB configuration.
             */
            dbURL = prop.getProperty(DATABASE_URL_PARAM);
            dbDriver = prop.getProperty(DATABASE_DRIVER_PARAM);
            dbUserName = prop.getProperty(DATABASE_USER_PARAM);
            dbUserPassword = prop.getProperty(DATABASE_PASSWORD_PARAM);

            if (dbURL != null) {
                dbPropertiesInitialized = true;
                dbURL = dbURL.trim();

                /*
                 * Parse DB name.
                 */
                dbName = parseDBName();
            }

            if (dbDriver != null) {
                dbDriver = dbDriver.trim();
            } else {
                dbPropertiesInitialized = false;
            }

            if (dbUserName != null) {
                dbUserName = dbUserName.trim();
            }

            if (dbUserPassword != null) {
                dbUserPassword = dbUserPassword.trim();
            }
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
    }

    /**
     * Parses the DB name.
     * <p>
     * Note that back-up procedure produces a DB named db1 whereas the URL has
     * the DB name as databases/db1. This is the reason why this method would
     * simply return db1 rather than databases/db1.
     * 
     * @return the DB name
     */
    private String parseDBName() {
        String dbname = null;

        final int dbnameStartPosition = dbURL.lastIndexOf(File.separator);
        final int dburlLength = dbURL.length();
        dbname = dbURL.substring(dbnameStartPosition + 1, dburlLength);

        return dbname;
    }

    /**
     * Initializes a connection object.
     *
     * @throws AdeException if there is a problem in constructing a DB connection instance
     */
    private void initConnection() throws AdeException {
        if (!dbPropertiesInitialized) {
            throw new AdeInternalException("DB configuration is undefined.");
        }

        mLogger.debug("Loading driver " + dbDriver);

        try {
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            throw new AdeInternalException("Loading driver failed.", e);
        }

        mLogger.debug("Connecting to database " + dbURL);

        try {
            if (dbUserName == null || dbUserPassword == null) {
                dbConnection = DriverManager.getConnection(dbURL);
            } else {
                dbConnection = DriverManager.getConnection(dbURL, dbUserName, dbUserPassword);
            }
        } catch (SQLException e) {
            throw new AdeInternalException("Failed obtaining connection.", e);
        } catch (Throwable t) {
            throw new AdeInternalException("Failed obtaining connection.", t);
        }
    }

    /**
     * Returns the DB name.
     *
     * @return the DB name
     */
    public String getDBName() {
        return dbName;
    }

    /**
     * Returns the DB connection.
     *
     * @return the DB connection
     * 
     * @throws AdeException if there is a problem in getting a connection
     */
    public Connection getConnection() throws AdeException {
        if (dbConnection == null) {
            initConnection();
        }

        return dbConnection;
    }

    /**
     * Returns the DB connection for embedded Derby DB..
     *
     * @param dbLocation the location of the database
     * @return embeddedDBConnection the DB connection
     * @throws AdeException if there is a problem in getting a connection
     */
    public Connection getConnection(String dbLocation) throws AdeException {
        if (embeddedDBConnection == null) {
            embeddedDBConnection = getEmbeddedDBConnection(dbLocation);
        }

        return embeddedDBConnection;
    }

    /**
     * Closes the connection.
     * <p>
     * It will close the server connection or the embedded connection.  Note 
     * that we can have only one at any given time.
     *
     * @throws AdeInternalException if there is a problem in closing the connection
     */
    public void close() throws AdeInternalException {
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                throw new AdeInternalException("Failed closing connection ", e);
            } finally {
                dbConnection = null;
            }
        }

        if (embeddedDBConnection != null) {
            closeEmbeddedDBConnection();
        }
    }

    public void reset() throws AdeInternalException {
        close();
    }

    /**
     * Restores the primary DB from a given backed-up location. Then,
     * reinitializes the connection.
     * 
     * @param backedUpDBLocation the location where we expect the backed-up DB
     * 
     * @return connection to the backed-up DB
     * @throws AdeException if there is a problem in restoring
     */
    public Connection initConnectionRestoreFromBackup(String backedUpDBLocation) throws AdeException {
        final String connectionUrl = dbURL
                + ";rollForwardRecoveryFrom="
                + backedUpDBLocation
                + File.separator
                + dbName;

        mLogger.debug("Loading driver " + dbDriver);

        try {
            Class.forName(dbDriver);
        } catch (ClassNotFoundException e) {
            throw new AdeInternalException("Loading driver failed", e);
        }

        mLogger.info("Connecting to database " + connectionUrl + " to restore from backup.");

        Connection con = null;
        try {

            if (dbUserName == null || dbUserPassword == null) {
                con = DriverManager.getConnection(connectionUrl);
            } else {
                con = DriverManager.getConnection(connectionUrl, dbUserName, dbUserPassword);
            }

            if (con != null) {
                try {
                    con.commit();
                    con.close();
                } catch (SQLException e) {
                    mLogger.error("The restored connection could not be closed.", e);

                    throw new AdeExtUsageException("The connection could not be closed.", e);
                }
            }

            /*
             * Now, reinitialize connection.
             */
            initConnection();
        } catch (SQLException e) {
            throw new AdeInternalException("Failed obtaining connection", e);
        }

        return con;
    }

    /**
     * Restores the primary DB from a given backed-up location. Then,
     * reinitializes the connection.
     * 
     * @param backedUpDBLocation the location where we expect the backed-up DB
     * 
     * @return connection to the backed-up DB
     * @throws AdeException if there is a problem in restoring
     */
    public Connection initEmbeddedDBConnectionRestoreFromBackup(String backedUpDBLocation) 
                                                                  throws AdeException {
        final String dbLocation = System.getProperty("derby.system.home");

        final String connectionUrl = PROTOCOL + DEFAULT_DBNAME
                + ";rollForwardRecoveryFrom="
                + backedUpDBLocation
                + File.separator
                + dbName;

        mLogger.debug("Loading driver " + DRIVER);

        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new AdeInternalException("Loading driver failed", e);
        }

        mLogger.info("Connecting to database " + connectionUrl + " in location " + dbLocation + " to restore from backup.");

        Connection con = null;
        try {
            con = DriverManager.getConnection(connectionUrl);

            if (con != null) {
                try {
                    con.commit();
                } catch (SQLException e) {
                    mLogger.error("The restored connection could not be closed.", e);

                    throw new AdeExtUsageException("The connection could not be closed.", e);
                } finally {
                    closeEmbeddedDBConnection();
                }
            }
        } catch (SQLException e) {
            throw new AdeInternalException("Failed obtaining connection", e);
        }

        return con;
    }

    /**
     * Returns a DB connection to an embedded Derby DB.
     * 
     * @param dirName DB directory name
     * 
     * @return a connection object to Derby DB
     */
    private static Connection getEmbeddedDBConnection(String dirName) throws AdeException {
        final Properties p = System.getProperties();
        p.put("derby.system.home", dirName);
        Connection con = null;

        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new AdeExtInternalException("Loading driver failed, driver=" + DRIVER, e);
        }

        try {
            final String dbConnectionURL = PROTOCOL + DEFAULT_DBNAME + ";create=false";
            con = DriverManager.getConnection(dbConnectionURL);
            con.setAutoCommit(true);

            return con;
        } catch (SQLException e) {
            throw new AdeInternalException("Failed obtaining connection", e);
        }
    }

    /**
     * Closes embedded DB connection.
     */
    private static void closeEmbeddedDBConnection() {
        try {
            final String dbConnectionURL = PROTOCOL + DEFAULT_DBNAME + ";shutdown=true";
            DriverManager.getConnection(dbConnectionURL);
        } catch (SQLException se) {
            /*
             * Ignore the exception.
             */
            mLogger.trace("Database shut down normally.", se);
        } finally {
            embeddedDBConnection = null;
        }
    }
}
