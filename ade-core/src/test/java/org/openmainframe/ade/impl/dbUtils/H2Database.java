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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class H2Database extends Database {

    private static String DB_DRIVER = "org.h2.Driver";

    public H2Database() {
        super("jdbc:h2:mem:tempdb;DB_CLOSE_DELAY=-1", DB_DRIVER, null, null);
    }

    @Override
    public void createDatabase() throws SQLException {
        // H2 automatically creates a new database if one does not exist
    }

    @Override
    public void dropDatabase() throws SQLException {
        Connection conn = DriverManager.getConnection(getUrl());
        Statement stmt = conn.createStatement();
        try {
            stmt.executeUpdate("DROP ALL OBJECTS DELETE FILES");
        } finally {
            conn.close();
        }
    }

    @Override
    public List<String> listTables() throws SQLException {
        throw new UnsupportedOperationException();
    }

}
