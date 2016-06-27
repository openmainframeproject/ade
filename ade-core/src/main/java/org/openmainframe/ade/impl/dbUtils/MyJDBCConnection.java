/*
 
    Copyright IBM Corp. 2009, 2016
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
package org.openmainframe.ade.impl.dbUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.IAdeConfigProperties;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for managing thread local Connection instances.
 */

public final class MyJDBCConnection {
    private static final Logger logger = LoggerFactory.getLogger(MyJDBCConnection.class);
    private static String connectionUrl = null;
    private static String dbUser = null;
    private static String dbPassword = null;

    private static Long renewConnectionTime = 0L;

    private static ThreadLocal<Long> threadConnectionTime = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return Long.valueOf(0);
        }
    };

    private static ThreadLocal<Connection> projectCon = new ThreadLocal<Connection>() {
        @Override
        protected Connection initialValue() {
            return null;
        }
    };

    private MyJDBCConnection() {
        // Prevent instantiation of utility class
    }

    /**
     * Sets the database connection URL, user and password.  If these properties are
     * not set by explicitly calling this method, the first call to initConnection()
     * will use the AdeConfigProperties object from the Ade singleton.
     * 
     * @param connectionUrl - The JDBC connection URL for the desired database
     * @param dbUser - The user to connect to the database.  If null or the empty
     *     string, no user will be used.
     * @param dbPassword - The password to connect to the database.
     */
    public static synchronized void setDbConnectionProperties(String connectionUrl,
            String dbUser, String dbPassword) {
        MyJDBCConnection.connectionUrl = connectionUrl;
        MyJDBCConnection.dbUser = dbUser;
        MyJDBCConnection.dbPassword = dbPassword;
    }

    /**
     * Associate a database connection with the current thread.  Future calls
     * to getConnection() will return this connection, until it is requested
     * to be renewed by a call to needsRenewed().
     * 
     * <p>The database connection properties are those set by a prior call to 
     * setDbConnectionProperties() or, if that method has not been called, 
     * from the AdeConfigProperties associated with the Ade singleton.
     * 
     * @param create - If true, create the database if necessary.  This option
     *     is only supported on certain database implementations.  If the database
     *     does not support creation on demand, this flag is a no-op.
     *     
     * @throws AdeException if there was a problem initializing the connection.
     */
    public static void initConnection(boolean create) throws AdeException {
        if (connectionUrl == null) {
            final IAdeConfigProperties properties = Ade.getAde().getConfigProperties();
            MyJDBCConnection.connectionUrl = properties.database().getDatabaseUrl();
            MyJDBCConnection.dbUser = properties.database().getDatabaseUser();
            MyJDBCConnection.dbPassword = properties.database().getDatabasePassword();
        }

        String url = connectionUrl;
        if (create && connectionUrl !=null && connectionUrl.contains("jdbc:derby")) {
            url += ";create=true";
        }

        logger.info("Connecting to database " + connectionUrl);

        try {
            synchronized (MyJDBCConnection.class) {
                if (dbUser == null || dbUser.isEmpty()) {
                    setConnection(DriverManager.getConnection(url));
                } else {
                    setConnection(DriverManager.getConnection(url, dbUser, dbPassword));
                }

                threadConnectionTime.set(renewConnectionTime);
            }
            logger.info("Thread " + Thread.currentThread().getName()
                    + " using Connection object " + projectCon.get());
        } catch (SQLException e) {
            throw new AdeInternalException("Failed obtaining connection", e);
        }
    }

    /**
     * Returns the connection associated with this thread.  If no connection is
     * associated with the current thread, a new connection is obtained and 
     * returned.
     * 
     * @return the connection associated with this thread.
     * 
     * @throws AdeException if a Connection could not be returned
     */
    public static Connection getConnection() throws AdeException {
        if (needsRenewed() || (projectCon.get() == null)) {
            logger.info("Renewing (or initializing) database connection");
            initConnection(false);
        }

        return projectCon.get();
    }

    /**
     * Close the current thread's Connection object and set it to null
     * so that a new instance will be created on the next request.
     *
     * @throws AdeInternalException if the Connection could not be closed.
     */
    public static void close() throws AdeInternalException {
        if (projectCon.get() == null) {
            return;
        }

        try {
            projectCon.get().close();
            projectCon.set(null);
        } catch (SQLException e) {
            throw new AdeInternalException("Failed closing connection ", e);
        }
    }

    /**
     * Indicate that all threads should close their current connection and
     * obtain a new instance the next time they call get().
     */
    public static synchronized void flagForRenewal() {
        /* Flag connections for renewal by setting renewConnectionTime
         * to the current time.  As threads request a reference to their
         * Connection instance, they will use this timestamp to determine
         * if it needs to be renewed (by closing their current instance
         * and constructing another).
         */
        renewConnectionTime = new Date().getTime();
    }

    /**
     * Set the current thread's Connection object to the supplied object.
     * Any current Connection object is closed first.
     * 
     * @param conn - the Connection to set
     * 
     * @throws AdeInternalException if the current connection could not 
     *     be closed.
     */
    public static void setConnection(Connection conn) throws AdeInternalException {
        final Connection current = projectCon.get();
        if (current != null) {
            try {
                logger.info("Closing Connection object " + current);
                current.close();
            } catch (SQLException e) {
                logger.error("Failed to close current Connection", e);
                throw new AdeInternalException("Failed closing connection", e);
            }
        }
        projectCon.set(conn);
    }

    private static synchronized boolean needsRenewed() {
        /* If the timestamp of the current thread's connection is before the
         * most recent renew connection timestamp, indicated that it needs
         * to be renewed. */
        return threadConnectionTime.get().longValue() < renewConnectionTime;
    }

}
