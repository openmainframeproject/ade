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
package org.openmainframe.ade.impl.dbUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.dbUtils.PreparedStatementWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;

public final class SpecialSqlQueries {
    
    private SpecialSqlQueries() {
        // Private constructor to hide the implicit public one.
    }

    /**
     * Returns last auto generated key for this session.
     * Useful after inserts to tables with auto generated key columns.
     * 
     * @return last auto-generated key 
     * @throws AdeException
     */
    public static int getLastKey() throws AdeException {
        if ((Ade.getAde().getConfigProperties().database().getDatabaseDriver().contains("mysql")) ||
	    (Ade.getAde().getConfigProperties().database().getDatabaseDriver().contains("mariadb"))) {
            return executeIntQuery("SELECT LAST_INSERT_ID()");
        }
        return executeIntQuery("values IDENTITY_VAL_LOCAL()");
    }

    /** Executes a given query, that is expected to return an integer result. (e.g., select count(*) from...)
     * Expects exactly one integer result.
     * Uses the default connection.
     */
    public static int executeIntQuery(String sql) throws AdeException {
        return executeIntQuery(sql, AdeInternal.getDefaultConnection(), true);
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
    public static Integer executeIntQuery(String sql, Connection con, boolean mandatory) throws AdeException {
        final IntQueryExecuter executer = new IntQueryExecuter(sql, con, mandatory);
        executer.executeQuery();
        if (mandatory && executer.m_result == null) {
            throw new AdeInternalException("Query " + sql + " returned null result");
        }
        return executer.m_result;
    }

    /** Executes a given query, that is expected to return a String result. 
     * Expects exactly one String result.
     * Uses the default connection.
     */
    public static String executeStringQuery(String sql) throws AdeException {
        return executeStringQuery(sql, AdeInternal.getDefaultConnection(), true);
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
    public static String executeStringQuery(String sql, Connection con, boolean mandatory) throws AdeException {
        final StringQueryExecuter executer = new StringQueryExecuter(sql, con, mandatory);
        executer.executeQuery();
        return executer.m_result;
    }

    /** Executes a query that is expected to returned a column of integers.
     *  Uses the default connection
     */
    public static ArrayList<Integer> executeIntListQuery(String sql) throws AdeException {
        return executeIntListQuery(sql, AdeInternal.getDefaultConnection());
    }

    /** Executes a query that is expected to returned a column of integers.
     *  
     * @param sql Query to execute
     * @param con connection to use
     * @return the resulting column as an array list
     * @throws AdeException
     */

    public static ArrayList<Integer> executeIntListQuery(String sql, Connection con) throws AdeException {
        final IntListQueryExecuter executer = new IntListQueryExecuter(sql, con);
        executer.executeQuery();
        return executer.m_result;
    }

    /** Executes a given query, that is expected to return a Timestamp result. 
     * Expects exactly one Timestamp result.
     * Uses the default connection.
     */
    public static Date executeTimestampQuery(String sql) throws AdeException {
        return executeTimestampQuery(sql, AdeInternal.getDefaultConnection(), true);
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
    public static Date executeTimestampQuery(String sql, Connection con, boolean mandatory) throws AdeException {
        final TimestampQueryExecuter executer = new TimestampQueryExecuter(sql, con, mandatory);
        executer.executeQuery();
        return executer.m_result;
    }

    private static class IntListQueryExecuter extends QueryStatementExecuter {
        public ArrayList<Integer> m_result = new ArrayList<Integer>();

        public IntListQueryExecuter(String sql, Connection con) {
            super(sql, con);
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            m_result.add(rs.getInt(1));
        }
    }

    /** Parent class to inner classes that allow one or less results.
     */
    abstract private static class OneOrNoneQueryExecuter extends QueryStatementExecuter {
        protected boolean m_wasLoaded = false;

        public OneOrNoneQueryExecuter(String sql, Connection con, boolean mandatory) {
            super(sql, con, mandatory ? 1 : 0, 1);
        }

        public boolean wasLoaded() {
            return m_wasLoaded;
        }
    }

    /** Executes a query that is expected to return a single Integer value.
     * Use wasLoaded() method to determine whether query returned results at all.
     * Use m_result to get the result. If wasLoaded() and m_result==null, it means query returned a single null value.
     */
    public static class IntQueryExecuter extends OneOrNoneQueryExecuter {
        public Integer m_result = null;

        public IntQueryExecuter(String sql, Connection con, boolean mandatory) {
            super(sql, con, mandatory);
        }

        @Override
        protected final void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            m_wasLoaded = true;
            m_result = rs.getInt(1);
            if (rs.wasNull()) {
                m_result = null;
            }
        }
    }

    /** Executes a query that is expected to return a single String value.
     * Use wasLoaded() method to determine whether query returned results at all.
     * Use m_result to get the result. If wasLoaded() and m_result==null, it means query returned a single null value.
     */
    public static class StringQueryExecuter extends OneOrNoneQueryExecuter {
        public String m_result = null;

        public StringQueryExecuter(String sql, Connection con, boolean mandatory) {
            super(sql, con, mandatory);
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            m_wasLoaded = true;
            m_result = rs.getString(1);
        }
    }

    /** Executes a query that is expected to return a single Timestamp value.
     * Use wasLoaded() method to determine whether query returned results at all.
     * Use m_result to get the result. If wasLoaded() and m_result==null, it means query returned a single null value.
     */
    public static class TimestampQueryExecuter extends OneOrNoneQueryExecuter {
        public Date m_result = null;

        public TimestampQueryExecuter(String sql, Connection con, boolean mandatory) {
            super(sql, con, mandatory);
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            m_wasLoaded = true;
            m_result = PreparedStatementWrapper.getResultSetTimestamp(rs, 1);
        }
    }

}
