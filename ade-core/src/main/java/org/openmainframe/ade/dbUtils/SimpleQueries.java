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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dbUtils.SpecialSqlQueries;

/** An interface for performing simple sql queries, such as retreiving integers 
 */
public class SimpleQueries {

    private Connection m_connection;

    SimpleQueries(Connection con) {
        m_connection = con;
    }

    /**
     * Returns last auto generated key for this session.
     * Useful after inserts to tables with auto generated key columns.
     * 
     * @return last auto-generated key 
     * @throws AdeException
     */
    public final int getLastKey() throws AdeException {
        if ((Ade.getAde().getConfigProperties().database().getDatabaseDriver().contains("mysql")) ||
            (Ade.getAde().getConfigProperties().database().getDatabaseDriver().contains("mariadb"))) {
            return executeIntQuery("SELECT LAST_INSERT_ID()", true);
        }
        return executeIntQuery("values IDENTITY_VAL_LOCAL()", true);
    }

    /**
     * Executes a given query, that is expected to return an integer result. (e.g., select count(*) from...)
     * If the query returns more than one result, an exception is thrown.
     * If it returns no result, and mandatory flag is true an exception is thrown.
     * Otherwise null is returned.
     * To distinguish between no result and null in the database use the IntQueryExecuter inner class. 
     * @param sql query to execute
     * @param con connection to use
     * @param mandatory Setting this to true causes an exception to be thrown when the query returns no results.
     * @return Result of query or null if query returned no results.
     * @throws AdeException
     */
    public final Integer executeIntQuery(String sql, boolean mandatory) throws AdeException {
        return SpecialSqlQueries.executeIntQuery(sql, m_connection, mandatory);
    }

    /**
     * Executes a given query, that is expected to return a String result.
     * If the query returns more than one result, an exception is thrown.
     * If it returns no result, and mandatory flag is true an exception is thrown.
     * Otherwise null is returned.
     * To distinguish between no result and null in the database use the StringQueryExecuter inner class. 
     * @param sql query to execute
     * @param con connection to use
     * @param mandatory Setting this to true causes an exception to be thrown when the query returns no results.
     * @return Result of query or null if query returned no results.
     * @throws AdeException
     */
    public final String executeStringQuery(String sql, boolean mandatory) throws AdeException {
        return SpecialSqlQueries.executeStringQuery(sql, m_connection, mandatory);
    }

    /**
     * Executes a given query, that is expected to return a list of strings.
     * @param sql query to execute
     * @param con connection to use
     * @param mandatory Setting this to true causes an exception to be thrown when the query returns no results.
     * @return Result of query or null if query returned no results.
     * @throws AdeException
     */
    public final ArrayList<String> executeStringListQuery(String sql) throws AdeException {
        final ArrayList<String[]> temp = executeStringListQuery2d(sql);
        final ArrayList<String> res = new ArrayList<String>(temp.size());
        for (int i = 0; i < temp.size(); ++i) {
            final String[] row = temp.get(i);
            if (row.length != 1) {
                throw new AdeInternalException("Row " + (i + 1) + " of " + sql + " returned " + row.length + " strings instead of 1");
            }
            res.add(row[0]);
        }
        return res;
    }

    /**
     * Executes a given query, that is expected to return a list of strings.
     * @param sql query to execute
     * @param con connection to use
     * @param mandatory Setting this to true causes an exception to be thrown when the query returns no results.
     * @return Result of query or null if query returned no results.
     * @throws AdeException
     */
    public final ArrayList<String[]> executeStringListQuery2d(String sql) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(m_connection);
        final ArrayList<String[]> result = new ArrayList<String[]>();
        try {
            final PreparedStatement ps = cw.preparedStatement(sql).getPreparedStatement();
            final ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                final String[] row = new String[rs.getMetaData().getColumnCount()];
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); ++i) {
                    row[i - 1] = rs.getString(i);
                }
                result.add(row);
            }
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        return result;
    }

    /** Executes a query that is expected to returned a column of integers.
     *  
     * @param sql Query to execute
     * @param con connection to use
     * @return the resulting column as an array list
     * @throws AdeException
     */

    public final List<Integer> executeIntListQuery(String sql) throws AdeException {
        return SpecialSqlQueries.executeIntListQuery(sql, m_connection);
    }

    /**
     * Executes a given query, that is expected to return a Timestamp result.
     * If the query returns more than one result, an exception is thrown.
     * If it returns no result, and mandatory flag is true an exception is thrown.
     * Otherwise null is returned.
     * To distinguish between no result and null in the database use the TimestampQueryExecuter inner class. 
     * @param sql query to execute
     * @param con connection to use
     * @param mandatory Setting this to true causes an exception to be thrown when the query returns no results.
     * @return Result of query or null if query returned no results.
     * @throws AdeException
     */
    public final Date executeTimestampQuery(String sql, boolean mandatory) throws AdeException {
        return SpecialSqlQueries.executeTimestampQuery(sql, m_connection, mandatory);
    }

}
