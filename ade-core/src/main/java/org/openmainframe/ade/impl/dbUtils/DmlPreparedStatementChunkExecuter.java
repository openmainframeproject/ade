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
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.openmainframe.ade.exceptions.AdeException;

/**
 * An abstract Class generalize prepare statement for update/insert/delete queries
 * Every extended class needs to implement the setParameter function
 *
 */

/*
 * Changes: 
 * QueryDDL -> Dml 
 * setAllParameters instead of setParameters
 * single call does all
 * moves autocommit responsibility to caller
 */

public abstract class DmlPreparedStatementChunkExecuter extends DmlStatementExecuter {

    static public final int INSERT_CHUNK_SIZE = 5000;
    private int m_stmtCount = 0;
    private PreparedStatement m_preparedStatement = null;

    public DmlPreparedStatementChunkExecuter(String sqlString) {
        super(sqlString);
    }

    /**
     * creates a new SQL chunk statement executer with a connection different than the
     * default Ade connection (obtained by {@link MyJDBCConnection#getConnection()}.
     * This is useful for connecting other databases.
     * 
     * @param conn The DB connection. Must be open (e.g. with {@link DriverManager#getConnection(String)). 
     * @param sqlString the string to execute
     */
    public DmlPreparedStatementChunkExecuter(Connection conn, String sqlString) {
        super(conn, sqlString);
    }

    protected void createAndExecute(String sqlString) throws SQLException, AdeException {
        m_stmt = m_preparedStatement = m_con.prepareStatement(sqlString);
        setAllParameters(m_preparedStatement);
        if (m_stmtCount > 0)
            flush();
    }

    protected abstract void setAllParameters(PreparedStatement stmt) throws SQLException, AdeException;

    private void flush() throws SQLException {
        m_stmt.executeBatch();
        if (!m_con.getAutoCommit()) {
            m_con.commit();
        }
        m_stmtCount = 0;
    }

    protected void addBatch() throws SQLException {
        m_preparedStatement.addBatch();
        m_stmtCount++;
        //Query the database, storing the result
        // in an object of type ResultSet
        if (m_stmtCount >= INSERT_CHUNK_SIZE) {
            flush();
        }
    }
}
