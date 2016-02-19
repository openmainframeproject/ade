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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import org.openmainframe.ade.exceptions.AdeInternalException;

/** A thin wrapper over a PreparedStatement object 
 * */
public class PreparedStatementWrapper {

    /** The underlying prepared statement object */
    private PreparedStatement m_ps;

    /** The sql of this prepared statement */
    private String m_sql;

    private int m_batchCount = 0;

    private static final int MAX_BATCH_COUNT = 5000;

    /** Create a prepared statement with given sql */
    PreparedStatementWrapper(Connection con, String sql) throws SQLException {
        m_sql = sql;
        m_ps = con.prepareStatement(m_sql);
    }

    /**
     * Creates a prepared statement with given sql statement and column names.
     * @param con The connection used to connect to the database.
     * @param sql the statement that will be executed.
     * @param columnNames an array of column names for which auto-generated keys should be returned.
     * @throws SQLException
     */
    public PreparedStatementWrapper(Connection con, String sql, String[] columnNames) throws SQLException {
        m_sql = sql;
        m_ps = con.prepareStatement(m_sql,columnNames);
    }

    /** Returns underlying prepared statement */
    public final PreparedStatement getPreparedStatement() {
        return m_ps;
    }

    /** Closes this statement */
    public final void close() throws SQLException {
        if (m_ps != null) {
            m_ps.close();
        }
        m_ps = null;
    }

    /** Sets timestamp at given position.
    * If date is null it sets null
     */
    public final void setTimestamp(int pos, Date date) throws SQLException {
        if (date == null) {
            m_ps.setNull(pos, Types.TIMESTAMP);
        } else {
            m_ps.setTimestamp(pos, new java.sql.Timestamp(date.getTime()));
        }
    }

    /** Sets file in the given position.
     * The file's path is stored as string.
     * If the file is null, null is placed instead
     */
    public final void setFileName(int pos, File file) throws SQLException {
        m_ps.setString(pos, file == null ? null : file.getPath());
    }

    /** @return the timestamp in the given position of the given result set
     * May return null if the result set contains null in the specified position.
     */
    static public Date getResultSetTimestamp(ResultSet rs, int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);
        if (ts == null) {
            return null;
        }
        return new Date(ts.getTime());
    }

    /** @return the timestamp in the given column of the given result set
     * May return null if the result set contains null in the specified position.
     */
    static public Date getResultSetTimestamp(ResultSet rs, String columnName) throws SQLException {
        return getResultSetTimestamp(rs, rs.findColumn(columnName));
    }

    /** Sets date at given position.
     * If date is null it sets null
    */
    public final void setDate(int pos, Date date) throws SQLException {
        if (date == null) {
            m_ps.setNull(pos, Types.DATE);
        } else {
            m_ps.setDate(pos, new java.sql.Date(date.getTime()));
        }
    }

    /** Execute the this statement 
     * 
     * @return the result set obtained after excecuting this statement.
     * @throws SQLException
     */
    public final ResultSet executeQuery() throws SQLException {
        return m_ps.executeQuery();
    }

    /** Execute a dml statement.
     */
    public final boolean execute() throws SQLException {
        return m_ps.execute();
    }

    public final int executeUpdate() throws SQLException {
        return m_ps.executeUpdate();
    }

    /** Add current parameters stored in this statement to a batch.
     * The statement is executed as a batch after MAX_BATCH_COUNT calls,
     * or by explicitly calling flushBatch()
     * @throws SQLException
     */
    public final void addBatch() throws SQLException {
        if (m_batchCount > MAX_BATCH_COUNT) {
            flushBatch();
        }
        m_ps.addBatch();
        ++m_batchCount;
    }

    /** Executes parameters previously stored by addBatch() and not already
     * executed.
     */
    public final void flushBatch() throws SQLException {
        if (m_ps != null && m_batchCount > 0) {
            m_ps.executeBatch();
            m_batchCount = 0;
        }
    }

    /** Verifies given result set contains at least one row.
     * Issues a standard error if not.
     * This row is then ready to be obtained from the result set (next() already called)
     */
    public final void expectAtLeastOneResult(ResultSet rs) throws AdeInternalException, SQLException {
        if (!rs.next()) {
            throw new AdeInternalException("Expecting at least one row from " + m_sql);
        }
    }

    /** Verifies given result set contains no more rows.
     * Issues a standard error if not.    
     */
    public final void expectAtMostOneResult(ResultSet rs) throws AdeInternalException, SQLException {
        if (rs.next()) {
            throw new AdeInternalException("Expecting at most one row from " + m_sql);
        }
    }

}
