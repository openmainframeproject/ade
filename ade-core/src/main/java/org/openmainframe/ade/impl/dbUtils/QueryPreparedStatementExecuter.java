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
import java.sql.ResultSet;
import java.sql.SQLException;

import org.openmainframe.ade.exceptions.AdeException;

/**
 * An abstract Class that can execute a prepare Statement using the queryString
 * Every extended class needs to implement the setParameter and handleResultSet functions
 */
public abstract class QueryPreparedStatementExecuter extends QueryStatementExecuter {

    private PreparedStatement m_ps = null;

    public QueryPreparedStatementExecuter(String query) {
        super(query);
    }

    public QueryPreparedStatementExecuter(String query, int expectedCount) {
        super(query, expectedCount);
    }

    public QueryPreparedStatementExecuter(String query, Connection con) {
        super(query, con);
    }

    public QueryPreparedStatementExecuter(String query, Connection con, int minExpectedCount, int maxExpectedCount) {
        super(query, con, minExpectedCount, maxExpectedCount);
    }

    @Override
    protected final ResultSet obtainResultSet(Connection con) throws SQLException, AdeException {
        m_ps = con.prepareStatement(m_queryString);
        m_stmt = m_ps;
        setParameters(m_ps);
        return m_ps.executeQuery();
    }

    protected abstract void setParameters(PreparedStatement stmt) throws SQLException, AdeException;

}
