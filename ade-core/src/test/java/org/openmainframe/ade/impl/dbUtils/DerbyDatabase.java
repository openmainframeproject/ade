/*
 
    Copyright IBM Corp. 2016
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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DerbyDatabase extends Database {

    private static String DB_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    public DerbyDatabase() {
        super("jdbc:derby:memory:tempdb", DB_DRIVER, null, null);
    }

    @Override
    public void createDatabase() throws SQLException {
        DriverManager.getConnection(getUrl() + ";create=true");
    }

    @Override
    public void dropDatabase() throws SQLException {
        try {
            DriverManager.getConnection(getUrl() + ";drop=true");
        } catch (SQLException e) {
            /* If the exception was because the database did not exist,
             * or because it was already dropped, just ignore it.  
             * Otherwise, rethrow the exception. */
            if (e.getMessage().contains("not found") || e.getMessage().contains("dropped")) {
                // Ignore it.
            } else {
                throw e;
            }
        }
    }

    @Override
    public List<String> listTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        Connection conn = DriverManager.getConnection(getUrl());
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT TABLENAME FROM SYS.SYSTABLES");
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        } finally {
            conn.close();
        }
        return tables;
    }

}
