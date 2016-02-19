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
import java.sql.SQLException;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;

/**
 * 
 * QueryDDL -> Dml
 * Treating db closing & exceptions: not final 
 * query in constructor 
 */
public class DropIfExistsStatementExecuter extends DmlStatementExecuter {

    public DropIfExistsStatementExecuter(String sqlString) throws AdeInternalException {
        super(sqlString);
        sqlString = sqlString.trim();
        if (!sqlString.toUpperCase().startsWith("DROP")) {
            throw new AdeInternalException("Only 'drop' statements allowed");
        }
    }

    /**
     * creates a new SQL drop statement executer with a connection different than the
     * default Ade connection (obtained by {@link MyJDBCConnection#getConnection()}.
     * This is useful for connecting other databases.
     * 
     * @param conn The DB connection. Must be open (e.g. with {@link DriverManager#getConnection(String)). 
     * @param sqlString the string to execute
     */
    public DropIfExistsStatementExecuter(Connection conn, String sqlString) {
        //TODO make sure it calls DropIf...(String) constructor, and not the parent class constructor
        super(conn, sqlString);
    }

    @Override
    protected void createAndExecute(String sqlString) throws SQLException, AdeException {
        try {
            m_stmt = m_con.createStatement();
            m_stmt.execute(sqlString);
        } catch (SQLException e) {
            // if the SQL exception is due to a missing table, ignore it. The SQL states
            // below correspond to Derby or DB2 respectively (and MySQL)
            if (!e.getSQLState().equalsIgnoreCase("42Y55") && !e.getSQLState().equals("42704") 
                    && !e.getSQLState().equalsIgnoreCase("42S02")) {
                throw e;
            }
        }
    }

}
