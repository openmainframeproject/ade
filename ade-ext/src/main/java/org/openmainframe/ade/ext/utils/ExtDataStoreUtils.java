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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.IAdeConfigProperties;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.ext.AdeExt;
import org.openmainframe.ade.impl.dbUtils.MyJDBCConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to execute SQL query and data manipulation (dml) statements
 * against the Analytics Database using a JDBC Connection object.  That Connection is
 * realized using the database URL specified for use by the Ade core component.
 * The latter is instantiated so that it can divulge its configuration.
 *
 * Please note that the Ade core component itself offers services for simple
 * SQL query and dml statement execution.  These can and should be used by callers
 * whose requirements do not exceed the capabilities of these services.  This class
 * should be used for those callers who require:
 * - special purpose processing (e.g., "getOrAddXxx()" )
 * - more extensive logging on statement execution
 * - scrollable/long-persisting JDBC ResultSets
 * - simple JDBC batchExecute() capabilities
 * - immunity from Analytics Database recycling
 *
 */
public final class ExtDataStoreUtils {

    //* number of seconds we'll allow jdbc driver to check connectivity
    private static final int MAX_CONNECTION_PROBE_TIME = 5;
    //* avoid deadlocks with concurrent db users           
    static final boolean INPLACE_COMPRESS = true;
    private static final String DERBY_SESSION_SEVERITY = "08"; 
    private static final String DERBY_DB_NOT_FOUND = "XJ004"; 
    private static final String DERBY_CONNECTION_REFUSED = "08004"; 
    private static final String DERBY_JDBC_TYPE = "jdbc:derby"; 
    private static Logger logger = LoggerFactory.getLogger(ExtDataStoreUtils.class);

    private ExtDataStoreUtils() {
        //private constructor
    }
    
    /**                                                                  
     * Mark the database connections held by Ade for closure.  This   
     * method should be invoked if the database has gone away, but the   
     * JVM holding the connection continues running.                     
     *                                                                   
     * @throws AdeException                                           
     */
    
    public static void closeCoreDBConnections() { 
        synchronized (MyJDBCConnection.class) { 
            MyJDBCConnection.flagForRenewal(); 
        } 
    } 

    /**
     * Create the Ade and AdeExt objects if necessary
     *
     * @throws AdeException
     */
    public static void initAde() throws AdeException { 
        if (!Ade.isCreated()) {
            Ade.create();
            AdeExt.create(Ade.getAde());
            logger.info("initAde() - created Ade and AdeExt singletons");
        } else {
            logger.warn("Attempt to initialize Ade twice.");
        }
    }

    /**
     * Load the ade database driver, as per the ade configuration.
     * The Ade instance must already have been created.
     *
     * @throws AdeException
     */
    public static void loadAdeDatabaseDriver() throws AdeException {
        final IAdeConfigProperties properties = getAdeConfigProperties();
        final String driver = properties.database().getDatabaseDriver();

        try {
            logger.info("Loading ade database driver: " + driver);
            Class.forName(driver);
        } catch (Exception e) {
            throw new AdeInternalException("Could not load ade database driver", e);
        }
    }

    /**
     * Return the ade configuration properties object.  Initialize Ade
     * first.
     *
     * @return AdeConfigProperties object
     */
    public static IAdeConfigProperties getAdeConfigProperties() {

        IAdeConfigProperties configProperties = null;

        try {
            initAde();

            configProperties = Ade.getAde().getConfigProperties();
            return configProperties;
        } catch (AdeException e) {
            logger.error("Error encountered retrieving Ade's properties.", e);
            throw new RuntimeException("Exception during Ade initialization", e);
        }

    } 

    /**
     * Extract Database Url configuration property, if specified
     *
     * @return String value or empty string if not configured
     */
    private static String getDatabaseUrl() { 
        return getDatabaseUrl(getAdeConfigProperties());
    } 

    /**
     * Efficiency Overload
     */
    private static String getDatabaseUrl(IAdeConfigProperties configProperties) {

        String databaseUrl = "";
        if (configProperties != null) {
            databaseUrl = configProperties.database().getDatabaseUrl();
        }
        logger.trace("getDatabaseUrl() returns database url: " + databaseUrl);

        return databaseUrl;
    } 

    /**
     * Extract JDBC Driver Name configuration property, if specified
     *
     * @return String value or empty string if not configured
     */
    public static String getJdbcDriverName() { 
        return getJdbcDriverName(getAdeConfigProperties());
    } 

    /**
     * Efficiency Overload
     */
    private static String getJdbcDriverName(IAdeConfigProperties configProperties) {

        String jdbcDriverName = "";
        if (configProperties != null) {
            jdbcDriverName = configProperties.database().getDatabaseDriver();
        }
        logger.trace("getJdbcDriverName() returns jdbc driver name: " + jdbcDriverName);

        return jdbcDriverName;
    } 
    /**
     * Extract (optional) Database User configuration property, if specified
     *
     * @return String value or empty string if not configured
     */
    public static String getDbConnectionUserid() { 
        return getDbConnectionUserid(getAdeConfigProperties());
    } 

    /**
     * Efficiency overload
     */
    private static String getDbConnectionUserid(IAdeConfigProperties configProperties) { 

        String connectionUserid = "";
        if (configProperties != null) {
            connectionUserid = configProperties.database().getDatabaseUser();
        }
        logger.trace("getDbConnectionUserid() returns userid: " + connectionUserid);

        return connectionUserid;
    } 

    /**
     * Extract (optional) Database Password configuration property, if specified
     *
     * @return String value or empty string if not configured
     */
    public static String getDbConnectionPassword() { 
        return getDbConnectionPassword(getAdeConfigProperties());
    } 

    /**
     * Efficiency overload
     */
    private static String getDbConnectionPassword(IAdeConfigProperties configProperties) {

        String connectionPassword = "";
        if (configProperties != null) {
            connectionPassword = configProperties.database().getDatabasePassword();
        }
        if (connectionPassword != null && !connectionPassword.isEmpty()) {
            logger.trace("getDbConnectionPassword() returns a non-empty password");
        }

        return connectionPassword;
    }

    /**
     * Locate and return a new, usable JDBC Connection to the analytics database.
     *
     * Return null to indicate that connectivity does not currently exist (for any
     * number of reasons).
     *
     * Note that this is *not* the persistent "Core" Connection provided via the Ade
     * singleton instance within the current JVM, but rather a short-lived one which
     * is used/useful for a one-off database usage (hence scope == private), and should
     * likely not be surfaced to ExtDataStoreUtils callers, in case they would fail to
     * understand and exercise their cleanup responsibilities for the Connection.
     *
     * @return - JDBC Connection object, or null if no connectivity
     */
    private static synchronized Connection getConnection() {

        Connection C = null;
        final IAdeConfigProperties config = getAdeConfigProperties(); 
        final String databaseUrl = getDatabaseUrl(config); 
        final String connectionUserid = getDbConnectionUserid(config); 
        final String connectionPassword = getDbConnectionPassword(config); 

        logger.trace("getConnection() -->entry");
        if (databaseUrl.isEmpty()) {
            logger.error("getConnection() <--exit (database URL unavailable)");
            return null;
        }
        
        logger.trace(String.format("getConnection() using databaseUrl: %s", databaseUrl)); 

        try {
            //* Determine if user+pwd configured
            if (connectionUserid.isEmpty()) {
                logger.info(String.format("getConnection() -> DriverManager.getConnection(%s)",
                        databaseUrl));
                C = DriverManager.getConnection(databaseUrl); 
            } else {
                logger.info(String.format("getConnection() -> DrverManager.getConnection( %s, %s, <pwd> )",
                        databaseUrl, connectionUserid, connectionPassword)); 
                C = DriverManager.getConnection(databaseUrl, connectionUserid, connectionPassword); 
            }
            
            // Allows dirty reads 
            C.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED); 
            if (isUsableConnection(C)) {
                logger.trace("getConnection() <--exit (normal)"); 
                return C;
            }
        } catch (SQLNonTransientConnectionException e) {
            handleConnectFailure(e, databaseUrl); 
        } catch (Throwable t) {
            handleConnectFailure(t); 
        }

        //* just in case 
        C = null; 
        logger.error("getConnection() <--exit (no connection)"); 
        return null;

    } 

    /**
     * Create and return a JDBC Statement object associated with
     * the JDBC Connection object which Ade provides for the
     * purpose of User Datastore manipulations.  Specify properties
     * which ensure that any ResultSet object produced by the Statement
     * is both scrollable and read-only.
     *
     *
     * @return - JDBC Statement object
     */
    private static Statement getStatement() {

        Statement S = null;
        logger.trace("getStatement() -->entry");
        try {
            final Connection C = getConnection();
            if (C != null) {
                S = C.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY); // order-sensitive
            } else {
                logger.error("getStatement() could not create jdbc STATEMENT");
            }
        } catch (Throwable t) {
            surfaceThrowable("getStatement() called Connection.createStatement()", t);
        }
        logger.trace("getStatement() <--exit");

        return S;

    } 

    /**
     * Return an Integer object whose value is that of the column designated
     * by 'colName' (from the ResultSet produced by executing 'querySql' against
     * the Derby Database to which we are currently connected).
     *
     * If the querySql does not produce a row, assume that the Database does not
     * contain the row, and try to add the row by executing 'insertSql'.  Then
     * rerun the original query.
     *
     * In either case, return the appropriate Integer object which wraps the
     * int value, or a null ref to indicate Sql error(s).  Note that a query
     * producing multiple rows (though not necessarily a good idea) is tolerated,
     * with the value being taken from the first ResultSet row.
     *
     * @param querySql - String aimed at producing a ResultSet with
     *                   1 or more rows, having at least column 'colName'
     * @param insertSql - String aimed at populating the Database Table
     *                    named in 'querySql' with a new row which will
     *                    then allow 'querySql' to fulfill its aim.
     * @param colName - String which names a column (typically: Primary
     *                  Key, though not necessarily) in the ResultSet
     *                  produced by 'querySql'.
     * @return - Instance of Integer, or null ref to indicate that Sql
     *           error(s) occurred
     */
    public static Integer getOrAddIntCol(String querySql, String insertSql, String colName) {

        boolean rowIsInDatabase = false;
        Throwable insertT = null, queryT = null, getT = null;
        ResultSet R = null;
        Integer I = null;

        logger.trace(String.format("getOrAddIntCol( %s ) -->entry", colName));
        try {
            R = executeQuery(querySql);
            rowIsInDatabase = nonemptyQueryResult(R);
        } catch (Throwable t) {
            queryT = t;
        }

        //* Determine if insert seems needed
        if (!rowIsInDatabase) {
            try {
                cleanup(R);
                logger.trace("getOrAddIntCol() -->insert: " + insertSql);
                execute(insertSql);
                R = executeQuery(querySql);
                rowIsInDatabase = nonemptyQueryResult(R);
            } catch (Throwable t) {
                insertT = t;
            }
        } 

        if (rowIsInDatabase) {
            try {
                R.first();
                I = R.getInt(colName);
            } catch (Throwable t) {
                getT = t;
                I = null;
            }
        } 

        if (I == null) {
            if (insertT != null) {
                surfaceThrowable("getOrAddIntCol", insertT);
            }
            if (queryT != null) {
                surfaceThrowable("getOrAddIntCol", queryT);
            }
            if (getT != null) {
                surfaceThrowable("getOrAddIntCol", getT);
            }
        } else {
            logger.trace("getOrAddIntCol() value: " + I.toString());
        }

        cleanup(R);
        logger.trace("getOrAddIntCol() <--exit");

        return I;
    } 

    /**
     * Execute the input Sql query, which is expected to produce a JDBC ResultSet
     * whose rows have a column corresponding to the input colName, with data
     * type String.  Extract those String values and return them in an ArrayList.
     * Return an empty list if the query ResultSet is empty.  Throw a Ade
     * Exception if the query execution or the column values extraction(s)
     * malfunction.
     *
     * @param sqlQuery - String which produces the described ResultSet
     * @param colName - String which names the column whose values are to
     *                  be extracted
     * @return - ArrayList of String(s)
     * @throws AdeException
     */
    public static List<String> getStringColList(String sqlQuery,
            String colName)
                    throws AdeException {
        final ArrayList<String> colList = new ArrayList<String>();
        ResultSet R = null;

        logger.trace(String.format("getStringColList( %s ) -->entry", colName));
        try {
            R = executeQuery(sqlQuery);
            //* Determine if have a ResultSet
            if (R != null) {
                //* Loop through ResultSet rows
                while (R.next()) {
                    colList.add(R.getString(colName));
                } 
            } 
        } catch (Exception e) {
            throw adeExceptionOf(e);
        } finally {
            cleanup(R);
        }

        logger.trace("getStringColList() <--exit: " + colList.size());
        return colList;
    } 

    /**
     * Execute the input Sql query, which is expected to produce a JDBC ResultSet
     * whose rows have columns as defined by the DbResultParser.  Extract those values
     * and return them in an ArrayList.Return an empty list if the query ResultSet is empty.
     * Throw a Ade Exception if the query execution or the column values extraction(s)
     * malfunction.
     *
     * @param sqlQuery - String which produces the described ResultSet
     * @param resultParser - DBResultParser which defines how rows should be interpreted
     * @return - ArrayList of String(s)
     * @throws AdeException
     */
    public static <T> List<T> getCustomList(String sqlQuery,
            IDbResultParser<T> resultParser)
                    throws AdeException {
        final ArrayList<T> colList = new ArrayList<T>();
        ResultSet R = null;

        logger.trace(String.format("getCustomList( %s ) -->entry", sqlQuery));
        try {
            R = executeQuery(sqlQuery);
            if (R != null) {
                while (R.next()) {
                    colList.add(resultParser.parseResult(R));
                }
            } 
        } catch (Exception e) {
            throw adeExceptionOf(e);
        } finally {
            cleanup(R);
        }

        logger.trace("getCustomList() <--exit: " + colList.size());
        return colList;
    } 

    /**
     * This method is a thin wrapper for the JDBC execute.
     * It is offered in this way to ensure consistency of
     * error logging.
     *
     * WARNING:
     * boolean return value here comes from JDBC and is not
     * fully understood at this time.  It may return false
     * even when dml is successfully executed, so cannot
     * be used as a judgment of statement execution success.
     *
     * @param sqlStatement  - instance of String
     * @return boolean true or false per the JDBC execute().
     */
    public static synchronized boolean execute(String sqlStatement) {

        boolean executeOk = false;
        logger.trace(String.format("execute( %s ) -->entry", sqlStatement));
        final Statement S = getStatement();
        if (S == null) {
            logger.error("execute() <--exit (error: Statement unavailable)");
            return false;
        }

        try {
            executeOk = S.execute(sqlStatement);
        } catch (Throwable t) {
            surfaceThrowable(String.format("execute( %s )", sqlStatement), t);
        } finally {
            cleanup(S);
        }

        logger.trace("execute() <--exit (normal): " + executeOk);
        return executeOk;
    }

    /**
     * This method is a thin wrapper for JDBC executeQuery().
     * It exists primarily so that we can be assured of the
     * properties of the ResultSet which is returned, and for
     * consistency of error logging.
     *
     * @param sqlStatement - instance of String
     * @return - instance of ResultSet which is both
     *           scrollable and read-only.
     */
    public static synchronized ResultSet executeQuery(String sqlStatement) {

        logger.trace(String.format("executeQuery( %s ) -->entry", sqlStatement));
        ResultSet R = null;
        final Statement S = getStatement();

        if (S == null) {
            logger.error("executeQuery() <--exit (error: Statement unavailable)");
            return null;
        }

        try {
            R = S.executeQuery(sqlStatement);
            logger.trace("executeQuery() <--exit (normal): " + (null == R));
            // No cleanup (persist ResultSet)
            return R; // No cleanup (persist ResultSet)
        } catch (Throwable t) {
            surfaceThrowable(String.format("executeQuery( %s )", sqlStatement), t);
            // Cleanup (nothing to persist)
            cleanup(S, R);
        //* end outer catch
        } 

        return null;
    } 

    /**
     * This method executes a batch of input Sql Statements, as a
     * "rollback-able" transaction.  Though these will typically be
     * dml statements, they need not be.  However, since any and
     * all ResultSets produced by query statements which might be
     * included in the input sqlStatement List<> are implicitly
     * discarded when the associated Statement is close()'d, it
     * is currently nonsensical to include query statements.  If
     * and when that sort of functionality is required, this method
     * might be changed to return some sort of "result array".
     *
     * NOTE(S):  Ade core's usage of Derby assumes that the
     * current database has "autocommit==true", by default.  This
     * effectively rules out multi-statement transactions.  We take
     * the same approach as Ade core, explicitly suspending
     * the Connection's autocommit for the duration of the batch
     * execution.  Any table-specific serialization should be done
     * thru SQL (e.g., "lock table XYZ in exclusive mode"), and
     * this kind of statement would presumably be specified by the
     * caller as something like sqlStatement[0].
     *
     * @param ArrayList<String> sqlStatement whose elements are
     *        to be executed as a batch via JDBC executeBatch()
     * @return - boolean true to indicate that batch execution
     *           of the input SQL statement(s) did not raise
     *           any exceptions, false to indicate that it did.
     */
    public static synchronized boolean executeBatch(List<String> sqlStatement) { 

        boolean batchOk = false;
        logger.trace("executeBatch() -->entry");
        final Statement S = getStatement();

        if (S == null) {
            logger.error("executeBatch() <--exit (Statement unavailable)");
            return false;
        }

        try {
            //* Loop through the ordered list of statements
            for (int i = 0; i < sqlStatement.size(); i++) {
                S.addBatch(sqlStatement.get(i)); 
            } 
            logger.trace("executeBatch(): starting Transaction...");
            startTransaction(S.getConnection());
            S.executeBatch();
            batchOk = true;
        } catch (Throwable t) {
            surfaceThrowable("executeBatch() failed", t);
        } finally {
            try {
                endTransaction(S.getConnection(), batchOk);
            } catch (Throwable t) {
                logger.error("Error encountered ending the transaction.", t);
            }
            cleanup(S);
        }

        logger.trace("executeBatch() <--exit: " + batchOk);
        return batchOk;
    } 

    /**
     * Executes the specified AtomicTransaction as a single
     * database transaction
     *
     * @param at the AtomicTransaction to perform
     * @return a boolean indicating whether the AtomicTransaction returned true
     */
    
    public static synchronized boolean executeAtomicTransaction(AtomicTransaction at) {
        boolean transactionOk = false;
        logger.trace("executeAtomicTransaction() -->entry");

        final Connection c = getConnection();

        if (c == null) {
            logger.error("executeAtomicTransaction() <--exit (Connection unavailable)");
            return false;
        }

        try {
            startTransaction(c);
            transactionOk = at.beginAtomicTransaction(c);
        } catch (Throwable t) {
            surfaceThrowable("executeAtomicTransaction() failed", t);
        } finally {
            try {
                endTransaction(c, transactionOk);
            } catch (Throwable t) {
                logger.error("Error encountered ending the transaction.", t);
            }
            cleanup(c);
        }

        logger.trace("executeAtomicTransaction() <--exit: " + transactionOk);
        return transactionOk;
    }

    /**
     * Close the input JDBC ResultSet, Statement, and Connection objects,
     * as appropriate.
     *
     * Any error(s) in close processing is surfaced, but this
     * seems to be about all that can be done. (?)
     *
     * @param S - JDBC Statement object which is to be closed
     * @param R - JDBC ResultSet object which is to be closed
     */
    private static synchronized void cleanup(Statement S, ResultSet R) {

        logger.trace("cleanup() -->entry");
        Connection C = null;
        /******************************************
         * These must be done in order (of scope)
         ******************************************/
        if (R != null) {
            logger.trace("cleanup() --> ResultSet.close()");
            try {
                R.close();
            } catch (Throwable t) {
                surfaceThrowable("cleanup() called ResultSet.close()", t);
            }
        }
        
        if (S != null) {
            try {
                C = S.getConnection();
            } catch (Throwable t) {
                surfaceThrowable("cleanup() called Statement.getConnection()", t);
            }
            logger.trace("cleanup() --> Statement.close()");
            try {
                S.close();
            } catch (Throwable t) {
                surfaceThrowable("cleanup() called Statement.close()", t);
            }
        } //* end if have Statement
        if (C != null) {
            logger.trace("cleanup() --> Connection.close()");
            try {
                C.close();
            } catch (Throwable t) {
                surfaceThrowable("cleanup() called Connection.close()", t);
            }
        }
        logger.trace("cleanup() <--exit");

    } 

    /**
     * Overload for when caller just has Connection object.
     *
     * @param C - instance of jdbc Connection
     */
    private static synchronized void cleanup(Connection C) {

        logger.trace("cleanup() -->entry");
        if (C != null) {
            logger.trace("cleanup() --> Connection.close()");
            try {
                C.close();
            } catch (Throwable t) {
                surfaceThrowable("cleanup() called Connection.close()", t);
            }
        }
        logger.trace("cleanup() <--exit");

    } 

    /**
     * Overload of cleanup( S, R ) when only R is visible to caller.
     *
     * @param R - JDBC ResultSet object
     */
    public static synchronized void cleanup(ResultSet R) {

        Statement S = null;
        if (R != null) {
            try {
                S = R.getStatement();
            } catch (Throwable t) {
                surfaceThrowable("cleanup() called ResultSet.getStatement()", t);
            }
            cleanup(S, R);
        }

    } 

    /**
     * Overload of cleanup( S, R ) when only S is visible to caller.
     *
     * @param S - JDBC Statement object
     */
    public static synchronized void cleanup(Statement S) {
        if (S != null) {
            cleanup(S, null);
        }
    } 

    /**
     * Self-explanatory boolean function
     *
     * @param R - instance of a JDBC ResultSet
     * @return true if R is measurable and has
     *         1 or more rows, false if otherwise
     */
    public static boolean nonemptyQueryResult(String sqlQuery) {

        boolean nonEmpty = false;
        ResultSet R = null;

        try {
            R = executeQuery(sqlQuery);
            /***********************************************
             * Do it this way instead of using sizeofSet()
             * to avoid useless expense in case of large R
             **********************************************/
            nonEmpty = nonemptyQueryResult(R);
        } catch (Throwable t) {
            logger.error("Error encountered executing the query.", t);
        } finally {
            cleanup(R);
        }

        return nonEmpty;
    } 

    /**
     * Since the ResultSet class mysteriously lacks a "size()" method, and
     * since simply iterating thru what might be a large ResultSet could be
     * a costly exercise, we play the following games.
     * We take care to try and leave R as we found it, cursor-wise.
     *
     * @param R - instance of jdbc ResultSet
     * @return - boolean true if R has 1 or more rows, false if not.
     */
    public static boolean nonemptyQueryResult(ResultSet R) {

        logger.trace("nonemptyQueryResult(R)");
        boolean nonEmpty = false;
        if (R == null) {
            return false;
        }

        try {
            if (R.getRow() != 0) {
                nonEmpty = true;
            } else {
                logger.trace("nonemptyQueryResult(R) - check R.first()...");
                nonEmpty = R.first();
                R.beforeFirst();
            }
        } catch (Throwable t) {
            surfaceThrowable("nonemptyQueryResult()", t);
        }

        return nonEmpty;

    } 

    /**
     * Surface a String describing a caught Throwable via this class' Logger.
     *
     * @param whereThrown - String which should provide a footprint
     *                      describing what (method) was in progress when
     *                      the Throwable was caught.
     * @param t - Instance of Throwable
     */
    public static void surfaceThrowable(String whereThrown, Throwable t) { 

        if (t instanceof SQLException) {
            logger.error(String.format("%s: caught SQLException(s):", whereThrown));
            surfaceSqlException((SQLException) t);
            return;
        }

        logger.error(String.format("%s: caught Throwable (%s)\n",
                whereThrown,
                t.toString()), t); 

        t = t.getCause();

        if (t != null && t instanceof SQLException) {
            logger.error("Caused by SQLException(s):");
            surfaceSqlException((SQLException) t);
            // We've already logged the entired caused by stack trace above    
        }

    } 
    
    
    /**
     * Return a String representation of the input Throwable's call stack.
     * This is used instead of the typical StringWriter(PrintWriter()) usage
     * to get redundant "Caused by" information, since the latter would be
     * abbreviated and we would like full stacks for causers.
     *
     * @param t - Instance of Throwable
     * @return - String as described
     */
    private static String stackTraceToString(Throwable t) { 
        final StringBuilder sb = new StringBuilder();
        //* to mimic the standard
        final String indent = "\tat "; 
        for (StackTraceElement frame : t.getStackTrace()) {
            sb.append(indent);
            sb.append(frame.toString());
            sb.append("\n");
        }
        return sb.toString();
    }                                                               
    
    public static void handleConnectFailure(SQLNonTransientConnectionException e, String databaseUrl) { 

        logger.trace("handleConnectFailure( SQLNonTransientConnectionException )");

        boolean suppress = false;
        try {
            suppress = isDbNotFoundException(e);
        } catch (AdeInternalException i) {
            logger.error("Unable to determine sqlstate", i);
        }

        if (suppress) {
            logger.info("Suppress connection failure (nonexistent db), url: " + databaseUrl);
        } else {
            handleConnectFailure(e);
        }

    } 
    
    /**
     * Surface a failed Connection
     *
     * @param e - Instance of Throwable
     */
    public static void handleConnectFailure(Throwable t) { 

        logger.error("handleConnectFailure() - throwable of type: " + t.getClass().getName());

        surfaceThrowable("(datastore connect)", t);

    } 
    
    
    /**
     * Surface a String describing a SQL Exception via
     * this class' Logger.  Since SQL Exceptions typically have
     * a chain of related exceptions, do the same for these.  This
     * chaining appears to superscede the normal exception "causedBy"
     * paradigm.
     *
     * @param e - Instance of SQLException
     */
    public static void surfaceSqlException(SQLException e) { 

        boolean chained = false;
        String loggerMsg;

        while (e != null) {
            if (chained) {
                loggerMsg = String.format("Next in SQLException chain:  State=%s, ErrorCode=%d, Message=(%s)\n%s",
                        e.getSQLState(),
                        e.getErrorCode(),
                        e.getMessage(),
                        stackTraceToString(e));
            } else {
                loggerMsg = String.format("SQLException:  State=%s, ErrorCode=%d, Message=(%s)\n%s",
                        e.getSQLState(),
                        e.getErrorCode(),
                        e.getMessage(),
                        stackTraceToString(e));
                chained = true;
            } 

            e = e.getNextException();
            logger.error(loggerMsg);
        //* end loop thru possible chain
        } 

    } 

    /**
     * Return an instance of AdeException (typically: AdeInternalException)
     * which either is, or represents, the input Exception.  Trusting that usage errors
     * would be certain to throw a AdeUsageException, it seems safe to throw a
     * AdeInternalException in all other cases.
     *
     * @param e - instance of Exception
     * @return - instance of AdeException
     */
    private static AdeException adeExceptionOf(Throwable t) {

        if (t instanceof AdeException) {
            return (AdeException) t;
        } else {
            return new AdeInternalException("adeExceptionOf( " + t.getMessage() + " )", t);
        }

    } 

    /**
     * Thin wrapper around JDBC Connection.setAutoCommit() to
     * effectively suspend autocommit processing, allowing
     * for multi-statement transactions.  It is offered in
     * this way only to ensure consistent error logging.
     *
     * @throws - instance of AdeException
     */
    private static synchronized void startTransaction(Connection C) throws AdeException {

        logger.trace("startTransaction() -->entry");
        try {
            C.setAutoCommit(false);
        } catch (Throwable t) {
            surfaceThrowable("startTransaction() called Connection.setAutocomit()", t);
            throw adeExceptionOf(t);
        }
        logger.trace("startTransaction() <-- exit");

    } 

    /**
     * Thin wrapper around JDBC Connection.setAutoCommit() to
     * effectively resuming autocommit processing, allowing
     * for multi-statement transactions.  It is offered in
     * this way only to ensure consistent error logging.
     *
     * @param successful - boolean, with true indicating that the
     *                     current transaction should be commit()'d,
     *                     false indicating that the current transaction
     *                     should be rollback()'d (i.e., discarded).
     * @throws - instance of AdeException
     */
    private static synchronized void endTransaction(Connection C, boolean successful) throws AdeException {

        logger.trace("endTransaction() -->entry");
        try {
            if (successful) {
                C.commit();
            } else {
                C.rollback();
            }
            C.setAutoCommit(true);
        } catch (Throwable t) {
            surfaceThrowable("endTransaction() invoked Connection method", t);
            throw adeExceptionOf(t);
        }
        logger.trace("endTransaction() <-- exit");

    } 

    /**
     * Method to deal with the set of connectivity issues introduced by our
     * penchant for restarting Derby independent of other processes.  The
     * Connection.isValid() method takes a timeout value in units==seconds,
     * which apparently gives us control over how patient we wish to be
     * (as it probes?).  We have chosen an arbitrary value for this.
     *
     * @param C
     * @return
     */
    public static boolean isUsableConnection(Connection C) { 

        boolean open = false, valid = false; 
        final int timeoutSeconds = MAX_CONNECTION_PROBE_TIME;

        logger.trace("isUsableConnection() -->entry");
        if (C == null) {
            logger.error("isUsableConnection() <-- exit (No Connection)");
            return false;
        }

        try {
            open = !C.isClosed();
        } catch (Throwable t) {
            surfaceThrowable("isUsableConnection called Connection.isClosed()", t);
        }
        try {
            valid = C.isValid(timeoutSeconds);
        } catch (Throwable t) {
            surfaceThrowable("isUsableConnection called isValid()", t);
        }

        logger.trace(String.format("isUsableConnection() <-- exit (Connection is open: %b, is valid: %b)", open, valid));
        return open && valid;
    } 

    /**                                                                                    
     * Method to try to determine the broad category (class) of
     * SQL error the input exception represents, and whether or
     * not that class is Connection-oriented.
     *
     * Note that most online doc uses terminology oriented towards
     * "SESSION" when describing this class of errors, but we
     * equate this to "Connection".
     *
     * For better or for worse, each database implementation is
     * free to see the world thru its own pair of rose glasses,
     * and so we must ask about the underlying database implementation
     * as we proceed.  Currently only Derby is supported.
     *
     * The Derby client returns all errors as SQLExceptions, and
     * offers the getSqlState() method as a way of accessing the
     * state, which is normally in  5-digit XXYYY String format
     * (at least in the cases we care about). Here XX represents the
     * "broad category" we seek (YYY represents something specific within
     * that category).
     *
     * Note that we should like to use constants from:
     *        org.apache.derby.shared.common.reference.SQLState
     *
     * but this package does not appear to be included in the derby jar
     * files that we use...(? Not quite sure why...), so we make due with
     * constant DERBY_SESSION_SEVERITY above, until such time as we figure
     * out how to properly access the official constants...
     *
     * @param e instance of SQLException which is to be judged
     * @throws AdeInternalException when basis for judgment is lacking
     *
     */
    public static boolean isConnectionException(SQLException e) throws AdeInternalException {

        boolean isSessionSeverity = false;
        logger.trace("isConnectionException()  --> entry");
        final String connectionUrl = getDatabaseUrl();

        if (connectionUrl == null) {
            throw new AdeInternalException("isConnectionException() - unknown jdbc driver");
        }
        if (!connectionUrl.contains(DERBY_JDBC_TYPE)) {
            throw new AdeInternalException("isConnectionException() - unsupported jdbc driver: " + connectionUrl);
        }
        final String sqlState = e.getSQLState();
        if (sqlState == null || sqlState.isEmpty()) {
            throw new AdeInternalException("isConnectionException() - unknown sql state");
        }

        isSessionSeverity = sqlState.startsWith(DERBY_SESSION_SEVERITY);
        logger.trace(String.format("isConnectionException()  <-- exit (%b)", isSessionSeverity));
        return isSessionSeverity;
    } 

    public static boolean isDbNotFoundException(SQLException e) throws AdeInternalException {

        boolean isNotFound = false;
        logger.trace("isDbNotFoundException()  --> entry");

        final String sqlState = e.getSQLState();
        final String msg = e.getMessage();
        if (sqlState == null || sqlState.isEmpty()) {
            throw new AdeInternalException("isDbNotFoundException() - unknown sql state");
        }

        logger.trace(String.format("isDbNotFoundException() - sqlstate=%s, msg=%s",
                sqlState, msg));
        isNotFound = sqlState.startsWith(DERBY_DB_NOT_FOUND)
                || (sqlState.startsWith(DERBY_CONNECTION_REFUSED) && (msg.contains("not found")));
        logger.trace(String.format("isDbNotFoundException()  <-- exit (%b)", isNotFound));
        return isNotFound;
    } 

    /**
     * Method to write a persistent String value to the analytics database, to be
     * associated with the input property name.  Considerations about authorization
     * may one day be relevant, but currently are considered to be moot.
     *
     * @param propertyName
     * @param propertyValue
     */
    public static void setDatabaseProperty(String propertyName, String propertyValue) {

        logger.trace(String.format("setDatabaseProperty( %s, %s ) -->entry",
                propertyName, propertyValue));
        final String sql = String.format("CALL  SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('%s','%s')",
                propertyName, propertyValue);
        execute(sql);
        logger.trace("setDatabaseProperty() <--exit");

    } 

    /**
     * Method to read a persisted String value from the analytics database, or null.
     * If not null, that value was likely written by an earlier call to setDatabaseProperty().
     *
     * @param propertyName
     * @return String property value as described above, or null
     * @throws AdeException
     */
    public static String getDatabaseProperty(String propertyName) throws AdeException {

        final String sqlQuery = String.format("VALUES  SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('%s')", propertyName);
        ResultSet R = null;
        String propertyValue = null;

        logger.trace(String.format("getDatabaseProperty( %s ) -->entry", propertyName));
        try {
            R = executeQuery(sqlQuery);
            if (R != null) {
                R.first();
                propertyValue = R.getString(1); //* column index, well-known
                logger.trace(String.format("getDatabaseProperty( %s ) value: %s",
                        propertyName, propertyValue));
            }
        } catch (Exception e) {
            throw adeExceptionOf(e);
        } finally {
            cleanup(R);
        }

        logger.trace("getDatabaseProperty() <--exit");
        return propertyValue;
    } 

    /**                                                                                  
     * Method for invoking a known database prepared statement to
     * compress a designated table.  Typically this is done after a
     * nontrivial number of deletions from a table have occurred, and
     * the current database implementation is know to be a lazy space
     * reclaimer (e.g., Derby).
     *
     * NOTES:  At this time, Apache Derby is the database implementation
     * in use, and Derby has a few important "oh by the way(s)":
     *
     * 1) Either of the prepared statements:
     *      SYSCS_UTIL.SYSCS_COMPRESS_TABLE( )
     *      SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE( )
     *    should be called in autocommit mode.
     *    This should already be so for a new Connection, by default, but
     *    as a formality, we ensure it.  Since we will not reuse the
     *    Connection object, we will not worry about restoration.
     *
     * 2) See the Derby documentation for discussion of the <short> parameters
     *    to these prepared statements.  They have important consequences for
     *    the type and duration of processing which occurs.
     *
     * 3) In practice, SYSCS_UTIL.SYSCS_COMPRESS_TABLE() seems to incur deadlock
     *    with concurrent Anomaly Detection Engine Analyze processing (and perhaps others, as well),
     *    whereas SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE() does not seem to.
     *    The scoping of this method and the default ("in place == true") reflects
     *    the fact that only the in-place form is trusted by Anomaly Detection Engine at this time
     *    (and so this is currently the only way it is offered publicly).  Note
     *    also that both prepared statements acquire an exclusive table lock for
     *    their duration.  Experience shows that explicit acquisition beforehand
     *    does not seem to provide any help in avoiding deadlock (contrary to any
     *    internet testimonials one may find).
     *
     * @param tableName
     * @param compressInPlace
     * @throws AdeInternalException
     */
    private static void compressTable(String tableName, boolean compressInPlace) throws AdeInternalException {

        logger.trace(String.format("compressTable( %s, %b ) -->entry", tableName, compressInPlace));
        logger.warn(String.format("compressTable( %s ) suppressed (currently unsafe!)", tableName)); 
        /***********************************************************************************************
         * WARNING:  This code is currently unsafe, and should remain disabled
         *           until Derby service exhonerates in-place compression     
         ***********************************************************************************************
         CallableStatement cs = null;
         Connection C = getConnection();
         if ( C == null ) {
          String msg = String.format( "compressTable( %s ) could not access database", tableName );
          logger.error( msg );
          throw new AdeInternalException( msg );
            }
         try {
           if ( !C.getAutoCommit() )
              C.setAutoCommit( true );
           logger.info( String.format( "compressTable( %s ) - compress", tableName ) );
           if ( compressInPlace ) {
              cs = C.prepareCall( "CALL SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE(?, ?, ?, ?, ?)" );
              cs.setString( 1, SCHEMA_NAME );
              cs.setString( 2, tableName );
              cs.setShort( 3, (short) 1 );
              cs.setShort( 4, (short) 1 );
              cs.setShort( 5, (short) 1 );
              }
           else {
              cs = C.prepareCall( "CALL SYSCS_UTIL.SYSCS_COMPRESS_TABLE(?, ?, ?)" );
              cs.setString( 1, SCHEMA_NAME );
              cs.setString( 2, tableName );
              cs.setShort( 3, (short) 1 );
              }
           cs.execute();
           logger.info( String.format( "compressTable( %s ) - completed normally", tableName ) );
           }  //* end try clause
         catch ( SQLException s ) {
             String msg = String.format( "compressTable( %s )", tableName );      
             surfaceSqlException( s );                                            
             throw new AdeInternalException( msg, s );
             }
         catch ( Throwable t ) {
           String msg = String.format( "compressTable( %s )", tableName );     
             surfaceThrowable( msg, t );                                          
           throw new AdeInternalException( msg, t );
           }  //* end catch
         finally {
           cleanup( C );
           }
           **********************************************************************/
        logger.trace(String.format("compressTable( %s ) <-- exit", tableName));

    } 

    /**                                                                             
     * Self-explanatory.  All names in the array[String] must be valid table
     * names and must case-sensitive match table names define by our schema(s).
     * Compression is in the order of the names as passed.
     *
     * @param tableNamesArray
     * @throws AdeException
     */
    public static void compressDbTables(String[] tableNamesArray) throws AdeException {
        logger.trace("compressDbTables( names[Array] ) -> entry");
        for (String curr : tableNamesArray) {
            compressTable(curr, INPLACE_COMPRESS);
        }
        logger.trace("compressDbTables( names[Array] ) <- exit");
    } 

    /**                                                                            
     * Convenience overload for callers which have a Set<String>.
     * Order of compression is not predictable.
     *
     * @param tableNamesSet
     * @throws AdeException
     */
    public static void compressDbTables(Set<String> tableNamesSet) throws AdeException {
        logger.trace("compressDbTables( names<Set> ) -> entry");
        final String[] tableNamesArray = tableNamesSet.toArray(new String[tableNamesSet.size()]);
        compressDbTables(tableNamesArray);
        logger.trace("compressDbTables( names<Set> ) <- exit");
    } 

    
//* end class    
} 
