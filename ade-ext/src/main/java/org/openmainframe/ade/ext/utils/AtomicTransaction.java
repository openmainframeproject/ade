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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.openmainframe.ade.exceptions.AdeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an interface to perform multi-part transactions that must occur
 * in an atomic fashion. All database activity performed in the performAtomicQuery
 * is guaranteed to occur in a single transaction and will only complete successfully
 * if all transactions complete successfully. Modifying the connection in any way
 * should is strongly discouraged in order to avoid accidental committing of transactions
 */
public abstract class AtomicTransaction {

    private static final Logger logger = LoggerFactory.getLogger(AtomicTransaction.class);
    
    private Connection connection;
    private ArrayList<Statement> createdStatements;

    /**
     * A method to perform multi-part transactions that must occur
     * in an atomic fashion. All database activity performed in this method
     * is guaranteed to occur in a single transaction and will only complete successfully
     * if all transactions complete successfully. When making batch calls from this
     * method the executeBatchInAtomicQuery() method should be used
     * to avoid accidental committing of transactions. All statements should be created
     * via the provided AtomicTransaction methods. Calls to ExtDataStoreUtils will NOT
     * be included in this atomic transaction and therefore should be avoided.
     *
     * @return a boolean value indicating whether this action completed without
     *          throwing additional exceptions
     * @see ExtDataStoreUtils.executeBatchInAtomicQuery
     */
    public abstract boolean performAtomicTransaction() throws AdeException;

    /**
     * Starts the atomic transaction with the specified connection which should
     * have auto-commit already toggled off.
     *
     * @param connection the connection to perform the atomic transaction through
     * @return a boolean indicating whether the AtomicTransaction returned true
     * @throws AdeException if there is an error accessing or connecting to the database
     */
    public final boolean beginAtomicTransaction(Connection connection) throws AdeException {
        this.connection = connection;
        createdStatements = new ArrayList<>();
        final boolean result = performAtomicTransaction();
        for (Statement s : createdStatements) {
            try {
                s.close();
            } catch (SQLException e) {
                logger.error("Error encountered while closing the Statement.", e);
            }
        }

        this.connection = null;
        return result;
    }

    /**
     * Executes the provided sql string against the active database connection
     *
     * @param sql the sql string to perform
     * @throws SQLException if there is an error executing the sql
     */
    public final void execute(String sql) throws SQLException {
        createStatement().execute(sql);
    }

    /**
     * Creates a PreparedStatement for the active connection with the provided
     * SQL string
     *
     * @param sqlString the sql query to prepare a statement for
     * @return a PreparedStatement for the specified sql query
     * @throws SQLException if there is an error preparing the statement
     */
    protected final PreparedStatement prepareStatement(String sqlString) throws SQLException {
        return addAndReturnStatement(createdStatements,
                connection.prepareStatement(sqlString));
    }
    
    protected final PreparedStatement prepareStatement(String sqlString, String[] columnNames) throws SQLException{
        return addAndReturnStatement(createdStatements,
                connection.prepareStatement(sqlString, columnNames));
    }

    /**
     * Creates a PreparedStatement for the active connection with the provided
     * SQL string amd the given result set type and result set concurrency
     *
     * @param sqlString the sql query to prepare a statement for
     * @param resultSetType
     * @param resultSetConcurrency
     * @return a PreparedStatement for the specified sql query
     * @throws SQLException if there is an error preparing the statement
     */
    protected final PreparedStatement prepareStatement(String sqlString, int resultSetType, int resultSetConcurrency) throws SQLException {
        return addAndReturnStatement(createdStatements,
                connection.prepareStatement(sqlString, resultSetType, resultSetConcurrency));
    }

    /**
     * Creates an empty Statement with type TYPE_FORWARD_ONLY and concurrency level CONCUR_READ_ONLY
     * using the active database connection
     *
     * @returnan empty Statement using the active database connection
     * @throws SQLException if there is an error creating the statement
     */
    protected final Statement createStatement() throws SQLException {
        return addAndReturnStatement(createdStatements,
                connection.createStatement());
    }

    /**
     * Creates an empty Statement with custom type and concurrency level
     * using the active database connection
     *
     * @returnan empty Statement using the active database connection
     * @throws SQLException if there is an error creating the statement
     */
    protected final Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return addAndReturnStatement(createdStatements,
                connection.createStatement(resultSetType, resultSetConcurrency));
    }

    /**
     * Executes the given sql statement and returns the resulting ResultSet which will be SCROLL_INSENITIVE and CONCUR_READ_ONLY
     * 
     * @param sqlString the sql query to execute
     * @return the ResultSet that resulted from the given query which will be SCROLL_INSENITIVE and CONCUR_READ_ONLY
     */
    protected final ResultSet executeScrollInsensitiveQuery(String sqlString) throws SQLException {
        final PreparedStatement ps = prepareStatement(sqlString, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        ps.execute();
        return ps.getResultSet();
    }

    /**
     * Executes the given sql statement and returns the resulting ResultSet
     * 
     * @param sqlString the sql query to execute
     * @return the ResultSet that resulted from the given query
     */
    protected final ResultSet executeQuery(String sqlString) throws SQLException {
        final PreparedStatement ps = prepareStatement(sqlString);
        ps.execute();
        return ps.getResultSet();
    }

    /**
     * Executes each entry in the list as a batch element
     *
     * @param batchStringList the list of commands to be executed as a batch
     * @throws SQLException if there is an error excuting a batch statement
     */
    protected final boolean executeBatch(List<String> batchStringList) throws SQLException {
        boolean completedOK = false;

        // Create a statement to update the model  groups
        final Statement modelGroupUpdateStatement = createStatement();

        for (String batchItem : batchStringList) {
            modelGroupUpdateStatement.addBatch(batchItem);
        }

        // Execute the statement and close it
        modelGroupUpdateStatement.executeBatch();

        completedOK = true;
        modelGroupUpdateStatement.close();

        return completedOK;
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
    public final Integer getOrAddIntCol(String querySql, String insertSql, String colName) {

        boolean rowIsInDatabase = false;
        ResultSet R = null;
        Integer I = null;

        try {
            R = executeScrollInsensitiveQuery(querySql);
            rowIsInDatabase = ExtDataStoreUtils.nonemptyQueryResult(R);
        } catch (SQLException e) {
            logger.error("Error encountered while executing the SQL statement.", e);
        }

        if (!rowIsInDatabase) {
            try {
                R.close();

                execute(insertSql);
                R = executeScrollInsensitiveQuery(querySql);
                rowIsInDatabase = ExtDataStoreUtils.nonemptyQueryResult(R);
            } catch (SQLException e) {
                logger.error("Error encountered while working with the ResultSet.", e);
            }
        }

        if (rowIsInDatabase) {
            try {
                R.first();
                I = R.getInt(colName);
            } catch (SQLException e) {
                logger.error("Error encountered while working with the ResultSet.", e);
            }
        } 

        // Close the result set
        try {
            if (R != null) {
                R.close();
            }
        } catch (SQLException e) {
            logger.error("Error encountered while closing the ResultSet.", e);
        }

        return I;
    } 

    private <T extends Statement> T addAndReturnStatement(ArrayList<Statement> statementList, T statement) {
        statementList.add(statement);
        return statement;
    }
}
