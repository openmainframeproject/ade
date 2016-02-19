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
import org.openmainframe.ade.flow.IAdeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract Class that can execute a prepare Statement using the queryString
 * Every extended class needs to implement the setParameter and handleResultSet functions
 */
public class QueryStatementIterator implements IAdeIterator<ResultSet> {

    private static final Logger logger = LoggerFactory.getLogger(QueryStatementIterator.class);
    
    protected Statement m_stmt;
    private ResultSet m_rs = null;
    // the query string to be prepared 
    final protected String m_queryString;

    protected Connection m_con;

    public QueryStatementIterator(String query) {
        m_queryString = query;
    }

    public QueryStatementIterator(String query, Connection connection) {
        this(query);
        m_con = connection;
    }

    @Override
    public final void open() throws AdeException {
        try {
            //Get a connection from the connection pool
            m_con = MyJDBCConnection.getConnection();
            m_rs = obtainResultSet(m_con, m_queryString);
        } catch (SQLException e) {
            throw new AdeInternalException("Db access error", e);
        }
    }

    @Override
    public final ResultSet getNext() throws AdeInternalException {
        try {
            if (m_rs.next()) {
                return m_rs;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new AdeInternalException("Db access error", e);
        }
    }

    @Override
    public final void close() throws AdeException {
        try {
            if (m_rs != null) {
                m_rs.close();
                m_rs = null;
            }
            if (m_stmt != null) {
                m_stmt.close();
                m_stmt = null;
            }
        } catch (SQLException e) {
            throw new AdeInternalException("Db access error", e);
        }
    }

    protected final ResultSet obtainResultSet(Connection con, String queryString) throws SQLException, AdeException {
        m_stmt = con.createStatement();
        return m_stmt.executeQuery(queryString);
    }

    @Override
    public final void quietCleanup() {

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
