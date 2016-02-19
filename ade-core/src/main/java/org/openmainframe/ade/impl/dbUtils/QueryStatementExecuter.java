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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract Class that can execute a prepare Statement using the queryString
 * Every extended class needs to implement the setParameter and handleResultSet functions
 */
public abstract class QueryStatementExecuter {

    private static final Logger logger = LoggerFactory.getLogger(QueryStatementExecuter.class);
    
    protected Statement m_stmt = null;
    protected ResultSet m_rs = null;
    private int m_minExpectedCount = -1;
    private int m_maxExpectedCount = -1;
    // the query string to be prepared 
    final protected String m_queryString;

    protected Connection m_con;
    private int m_rowNum;

    public QueryStatementExecuter(String query) {
        m_queryString = query;
    }

    public QueryStatementExecuter(String query, int expectedCount) {
        m_queryString = query;
        m_minExpectedCount = expectedCount;
        m_maxExpectedCount = expectedCount;
    }

    public QueryStatementExecuter(String query, Connection conn) {
        this(query);
        m_con = conn;
    }

    public QueryStatementExecuter(String query, Connection conn, int minExpectedCount, int maxExpectedCount) {
        this(query, conn);
        m_minExpectedCount = minExpectedCount;
        m_maxExpectedCount = maxExpectedCount;
    }

    public final void executeQuery() throws AdeException {
        m_rowNum = 0;
        try {
            //Get a connection from the connection pool
            m_con = MyJDBCConnection.getConnection();
            m_rs = obtainResultSet(m_con);

            while (m_rs.next()) {
                if (m_maxExpectedCount >= 0 && m_rowNum + 1 > m_maxExpectedCount) {
                    break;
                }
                handleResultSet(m_rs);
                ++m_rowNum;
            }
            m_rs.close();
            m_rs = null;
            m_stmt.close();
            m_stmt = null;
        } catch (AdeException e) {
            emergencyClean();
            throw e;
        } catch (Throwable e) {
            emergencyClean();
            throw new AdeInternalException("failed querying db: " + m_queryString, e);
        }

        if (m_maxExpectedCount >= 0 && m_rowNum > m_maxExpectedCount) {
            throw new AdeInternalException("Query " + m_queryString + " resulted with at least " + m_rowNum + " rows while the maximal expected count was " + m_maxExpectedCount);
        }
        if (m_minExpectedCount >= 0 && m_rowNum < m_minExpectedCount) {
            throw new AdeInternalException("Query " + m_queryString + " resulted with " + m_rowNum + " rows while the minimal expected count was " + m_minExpectedCount);
        }
    }

    protected abstract void handleResultSet(ResultSet rs) throws SQLException, AdeException;

    protected ResultSet obtainResultSet(Connection con) throws SQLException, AdeException {
        m_stmt = con.createStatement();
        return m_stmt.executeQuery(m_queryString);
    }

    protected final int getRowNum() {
        return m_rowNum;
    }

    private void emergencyClean() {

        try {
            if (m_rs != null) {
                m_rs.close();
            }
        } catch (Throwable any) {
            logger.error("Error trying to close the ResultSet.", any);
        }

        try {
            if (m_stmt != null) {
                m_stmt.close();
            }
        } catch (Throwable any) {
            logger.error("Error trying to close the Statement.", any);
        }
    }

}
