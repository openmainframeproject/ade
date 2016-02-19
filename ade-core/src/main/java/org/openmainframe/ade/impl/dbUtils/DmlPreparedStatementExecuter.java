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
 * 
 * QueryDDL -> Dml
 * Treating db closing & exceptions: not final 
 * query in constructor 
 */
public abstract class DmlPreparedStatementExecuter extends DmlStatementExecuter {

    public DmlPreparedStatementExecuter(String sqlString) {
        super(sqlString);
    }

    /**
     * creates a new SQL prepared statement executer with a connection different than the
     * default Ade connection (obtained by {@link MyJDBCConnection#getConnection()}.
     * This is useful for connecting other databases.
     * 
     * @param conn The DB connection. Must be open (e.g. with {@link DriverManager#getConnection(String)). 
     * @param sqlString the string to execute
     */
    public DmlPreparedStatementExecuter(Connection conn, String sqlString) {
        super(conn, sqlString);
    }

    protected void createAndExecute(String sqlString) throws SQLException, AdeException {
        final PreparedStatement ps = m_con.prepareStatement(sqlString);
        m_stmt = ps;
        setParameters(ps);
        ps.execute();
    }

    protected abstract void setParameters(PreparedStatement stmt) throws SQLException, AdeException;

}
