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
import java.sql.Statement;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * QueryDDL -> Dml
 * Treating db closing & exceptions: not final 
 * query in constructor 
 */
public class DmlStatementExecuter {

    private static Logger logger = LoggerFactory.getLogger(DmlStatementExecuter.class);

    // the query string to be prepared   
    final private String m_sqlString;

    protected Connection m_con = null;

    protected Statement m_stmt = null;

    public DmlStatementExecuter(String sqlString) {
        m_sqlString = sqlString;
    }

    /**
     * creates a new SQL statement executer with a connection different than the
     * default Ade connection (obtained by {@link MyJDBCConnection#getConnection()}.
     * This is useful for connecting other databases.
     * 
     * @param conn The DB connection. Must be open (e.g. with {@link DriverManager#getConnection(String)). 
     * @param sqlString the string to execute
     */
    public DmlStatementExecuter(Connection conn, String sqlString) {
        this(sqlString);
        m_con = conn;
    }

    public void execute() throws AdeException {
        try {
            //Get a connection from the connection pool
            m_con = MyJDBCConnection.getConnection();
            createAndExecute(m_sqlString);
            m_stmt.close();
            m_stmt = null;
        } catch (SQLException e) {
            final SQLException e2 = e.getNextException();
            if (e2 != null) {
                logger.error("An sql exception occured", e);
                logger.error("The root cause of this exception is added to the following ade internal exception");
                e = e2;
            }
            emergencyClean();
            throw new AdeInternalException("failed executing sql statement: " + m_sqlString, e);
        } catch (AdeException e) {
            emergencyClean();
            throw e;
        } catch (Throwable e) {
            emergencyClean();
            throw new AdeInternalException("failed executing sql statement: " + m_sqlString, e);
        }
    }

    private void emergencyClean() {
        try {
            if (m_con != null) {
                m_con.rollback();
            }
        } catch (Throwable any) {
            logger.error("Internal error encountered while trying to rollback the Connection.", any);
        }
        try {
            if (m_stmt != null) {
                m_stmt.close();
            }
        } catch (Throwable any) {
            logger.error("Internal error encountered while closing the Statement.", any);
        }
    }

    protected void createAndExecute(String sqlString) throws SQLException, AdeException {
        m_stmt = m_con.createStatement();
        m_stmt.execute(sqlString);
    }

}
