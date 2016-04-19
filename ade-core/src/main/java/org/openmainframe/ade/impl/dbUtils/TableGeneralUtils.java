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

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dataStore.SQL;

public final class TableGeneralUtils {

    private static int m_transactionRefCount = 0;
    
    private TableGeneralUtils() {
        // Private constructor to hide the implicit public one.
    }

    private static class QueryPrinter extends QueryStatementExecuter {
        PrintStream m_out;

        QueryPrinter(PrintStream out, String sql) {
            super(sql);
            m_out = out;
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            if (getRowNum() == 0) {
                printMetaData(rs.getMetaData());
            }
            final ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 0; i < metaData.getColumnCount(); ++i) {
                m_out.printf("%s\t", rs.getString(i + 1));
            }
            m_out.println();

        }

        private void printMetaData(ResultSetMetaData metaData) throws SQLException {
            for (int i = 0; i < metaData.getColumnCount(); ++i) {
                m_out.printf("%s(%s)\t", metaData.getColumnName(i + 1), metaData.getColumnTypeName(i + 1));
            }
            m_out.println();
        }

    }

    public static void printTable(String tableName) throws AdeException {
        printTable(System.out, tableName);
    }

    public static void printTable(PrintStream out, String tableName) throws AdeException {
        out.println("Displaying content of " + tableName);
        new QueryPrinter(out, "select * from " + tableName).executeQuery();
    }

    public static void printQueryResults(String query) throws AdeException {
        printQueryResults(System.out, query);
    }

    public static void printQueryResults(PrintStream out, String query) throws AdeException {
        out.println("Displaying result of: " + query);
        new QueryPrinter(out, query).executeQuery();
    }

    public static void deleteTable(String tableName) throws AdeException {
        new DmlStatementExecuter("delete from " + tableName).execute();
    }

    public static void dropTable(String tableName) throws AdeException {
        new DropIfExistsStatementExecuter("drop table " + tableName).execute();
    }

    public static void executeDml(String sql) throws AdeException {
        new DmlStatementExecuter(sql).execute();
    }

    public synchronized static void startTransaction() throws AdeException {
        m_transactionRefCount++;
        if (m_transactionRefCount > 1) {
            return;
        }
        final Connection con = MyJDBCConnection.getConnection();
        try {
            con.setAutoCommit(false);
        } catch (SQLException e) {
            throw new AdeInternalException("Failed starting transaction", e);
        }
    }

    public synchronized static void endTransaction() throws AdeException {
        m_transactionRefCount--;
        if (m_transactionRefCount > 0) {
            System.out.println("m_transactionRefCount =" + m_transactionRefCount);
            return;
        }
        final Connection con = MyJDBCConnection.getConnection();
        try {
            unlockTables();
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            throw new AdeInternalException("Failed ending transaction", e);
        }
    }

    public static void lockTableExclusive(SQL table) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        final String tableName = table.toString();
        try {
            if ((Ade.getAde().getConfigProperties().database().getDatabaseDriver().contains("mysql")) || 
                (Ade.getAde().getConfigProperties().database().getDatabaseDriver().contains("mariadb"))) {
                cw.executeDml("lock tables " + tableName + " write");
                cw.close();
            } else {
                cw.executeDml("lock table " + tableName + " in exclusive mode");
                cw.close();
            }
        } catch (SQLException e) {
            throw new AdeInternalException("Failed locking table " + tableName, e);
        }
    }

    public static void lockTableShare(SQL table) throws AdeException {
        final String tableName = table.toString();
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        try {
            if ((Ade.getAde().getConfigProperties().database().getDatabaseDriver().contains("mysql")) ||
                (Ade.getAde().getConfigProperties().database().getDatabaseDriver().contains("mariadb"))) {
                cw.executeDml("lock tables " + tableName + " read");
                cw.close();

            } else {
                cw.executeDml("lock table " + tableName + " in share mode");
                cw.close();
            }
        } catch (SQLException e) {
            throw new AdeInternalException("Failed locking table " + tableName, e);
        }

    }

    public static void setPreparedStatementTimestamp(PreparedStatement statement, int pos,
            Date date) throws SQLException {
        if (date == null) {
            statement.setNull(pos, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(pos, new java.sql.Timestamp(date.getTime()));
        }
    }

    public static void setPreparedStatementString(PreparedStatement statement, int pos, String text, int maxLen) throws SQLException {
        if (text == null) {
            statement.setNull(pos, Types.VARCHAR);
        } else {
            if (maxLen >= 0 && text.length() > maxLen) {
                text = text.substring(0, maxLen);
            }
            statement.setString(pos, text);
        }
    }

    private static void unlockTables() throws AdeException {
        if ((!Ade.getAde().getConfigProperties().database().getDatabaseDriver().contains("mysql")) ||
            (!Ade.getAde().getConfigProperties().database().getDatabaseDriver().contains("mariadb"))) {
            return;
        }
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        try {
            cw.executeDml("unlock tables");
            cw.close();
        } catch (SQLException e) {
            throw new AdeInternalException("Failed unlocking tables", e);
        }

    }
}
