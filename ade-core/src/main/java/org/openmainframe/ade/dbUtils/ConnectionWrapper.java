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
package org.openmainframe.ade.dbUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.utils.LazyObj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A thin wrapper over a Connection object.
 * 
 * Possible usages are:
 * 
 * ConnectionWrapper cw=new ConnectionWrapper(..)
 * try {
 * 
 *   // work with statements/prepared statements
 *   
 *   cw.close(); // this will close all preparedstatements created by cw
 *   
 * } catch (SQLException e) {
 *   cw.failed(e);
 * } finally {
 *   cw.quietCleanup();
 * } 
 * 
 * or:
 * ConnectionWrapper cw=new ConnectionWrapper(..)
 * SimpleQueries sq=cw.simpleQueries();
 *  // work with sq, no need to take care of exceptions
 */
public class ConnectionWrapper {

    private static LazyObj<DriverType> s_driverType = new LazyObj<DriverType>() {

        @Override
        protected DriverType create() throws ObjectCreationException {
            Ade ade;
            try {
                ade = Ade.getAde();
            } catch (AdeException e) {
                logger.error("Error obtaining an Ade object.", e);
                throw new ObjectCreationException("Error obtaining an Ade object", e);
            }
            return ade.getConfigProperties().database().getDriverType();
        }
    };

    private Connection m_connection;
    private List<PreparedStatementWrapper> m_preparedStatements;
    private boolean m_returnAutoCommit = false;
    private static Logger logger = LoggerFactory.getLogger(ConnectionWrapper.class);

    /** Creates a ConnectionWrapper object.
     * NOTE: creating this object does nothing except store the connection in a member, and therefore
     * no exceptions or time-delay are expected.
     * @throws AdeException 
     */
    public ConnectionWrapper(Connection con) throws AdeException {
        m_connection = con;
    }

    /** Create a prepared statement.
     * This also stores the prepared statement internally, to make sure it is closed.
     * 
     * @param sql
     * @return An object wrapping the created prepared statement
     * @throws SQLException
     */
    public PreparedStatementWrapper preparedStatement(String sql) throws SQLException {
        final PreparedStatementWrapper psw = new PreparedStatementWrapper(m_connection, sql);
        if (m_preparedStatements == null) {
            m_preparedStatements = new ArrayList<PreparedStatementWrapper>();
        }
        m_preparedStatements.add(psw);
        return psw;
    }   
    
    /**
     * Creates a prepared statement and stores it internally to make sure it is closed.
     * @param sql the sql statement to be executed
     * @param columnNames an array of column names for which auto-generated keys should be returned.
     * @return an object wrapping the created prepared statement.
     * @throws SQLException
     */
    public PreparedStatementWrapper preparedStatement(String sql, String[] columnNames) throws SQLException {
        final PreparedStatementWrapper psw = new PreparedStatementWrapper(m_connection, sql,columnNames);
        if (m_preparedStatements == null) {
            m_preparedStatements = new ArrayList<PreparedStatementWrapper>();
        }
        m_preparedStatements.add(psw);
        return psw;
    }

    /** Throws a ade internal exception with default error message for database errors */
    public void failed(Exception e) throws AdeInternalException {
        if (e instanceof SQLException) {
            final SQLException se = (SQLException) e;
            final Exception cause = se.getNextException();
            if (cause != null) {
                logger.error("An sql exception occured", e);
                logger.error("The root cause of this exception is added to the following ade internal exception");
                e = cause;
            }
        }
        throw new AdeInternalException("Database access error", e);
    }

    /** Returns an interface for performing simple sql queries, such as retrieving integers */
    public SimpleQueries simpleQueries() {
        return new SimpleQueries(m_connection);
    }

    /** Turn off autocommit.
     * If autocommit was previously on, it will be returned to on when calling close()
     * @throws SQLException
     */
    public void startTransaction() throws SQLException {
        final boolean isAutoCommit = m_connection.getAutoCommit();
        if (isAutoCommit) {
            m_connection.setAutoCommit(false);
            m_returnAutoCommit = true;
        }
    }

    public void rollback() throws SQLException {
        m_connection.rollback();
    }

    /** Execute a dml statement */
    public void executeDml(String sql) throws SQLException {
        final PreparedStatementWrapper psw = preparedStatement(sql);
        psw.execute();
    }

    public void executeUpdate(String sql) throws SQLException {
        final PreparedStatementWrapper psw = preparedStatement(sql);
        psw.executeUpdate();
    }

    /** Close all prepared statements created by this object.
     * Return autocommit mode if it was disabled and was previously on.
     * NOTE: does not close the underlying connection.
     * 
     * @throws SQLException
     */
    public void close() throws SQLException {
        endTransaction();
        if (m_preparedStatements != null) {
            for (PreparedStatementWrapper psw : m_preparedStatements) {
                psw.close();
            }
        }
        m_preparedStatements = null;
    }

    /**
     * @throws SQLException
     */
    public void endTransaction() throws SQLException {
        if (m_returnAutoCommit) {
            unlockTables();
            m_connection.setAutoCommit(true);

            m_returnAutoCommit = false;
        }
    }

    private void unlockTables() throws SQLException {
        switch (s_driverType.get()) {
            case MY_SQL:
                executeDml("unlock tables");
                break;
            default:
                // nothing to do for other types
        }
    }

    /** Close everything without throwing exceptions */
    public void quietCleanup() {
        try {
            close();
        } catch (Throwable e) {
            logger.error("Internal error occurred while cleaning up the connection.", e);
        }
    }

    /** Executes a DML statement on the default connection, 
     * This method is allocates and closes all the related db resources, and handles SQL exceptions if the occur 
     */
    static public void executeDmlDefaultCon(String sql) throws AdeException {
        executeDml(AdeInternal.getDefaultConnection(), sql);
    }

    /** Executes a DML statement on the given connection, 
     * This method is allocates and closes all the related db resources, and handles SQL exceptions if the occur 
     * @throws AdeException 
     */
    static public void executeDml(Connection con, String sql) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(con);
        try {
            cw.executeDml(sql);
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
    }

    static public void executeUpdate(Connection con, String sql) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(con);
        try {
            cw.executeUpdate(sql);
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
    }

    public void lockTableExclusive(String tableName) throws SQLException {
        switch (s_driverType.get()) {
            case MY_SQL:
                executeDml("lock table " + tableName + " write");
                break;
            default:
                executeDml("lock table " + tableName + " in exclusive mode");
        }
    }

    public void lockTableShare(String tableName) throws SQLException {
        switch (s_driverType.get()) {
            case MY_SQL:
                executeDml("lock table " + tableName + " read");
                break;
            default:
                executeDml("lock table " + tableName + " in share mode");
        }
    }

    public void lockTableExclusive(SQL table) throws SQLException {
        lockTableExclusive(table.toString());
    }

    public void lockTableShare(SQL table) throws SQLException {
        lockTableShare(table.toString());

    }
}
