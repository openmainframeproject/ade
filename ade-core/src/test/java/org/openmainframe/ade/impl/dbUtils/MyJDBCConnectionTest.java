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

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dbUtils.MyJDBCConnection;

public class MyJDBCConnectionTest {

    /* NOTE:  This testcase relies on using a Derby database as there is 
     * Derby specific code in MyJDBCConnection. */
    private static final Database db = new DerbyDatabase();

    @BeforeClass
    public static void initDbProperties() {
        MyJDBCConnection.setDbConnectionProperties(db.getUrl(), db.getUser(), db.getPassword());
    }

    @Before
    public void setup() throws Exception {
        db.dropDatabase();
    }

    @Test
    public void testInitConnectionWithCreateNoExceptions() throws Exception {
        MyJDBCConnection.initConnection(true);

        Connection conn = DriverManager.getConnection(db.getUrl());
        assertNotNull("Verify that the connection is not null.", conn);
    }

    @Test(expected = AdeInternalException.class)
    public void testInitConnectionWithoutCreateThrowsMIEIfDatabaseDoesNotExists() throws Exception {
        MyJDBCConnection.initConnection(false);
    }

    @Test
    public void testInitConnectionWithoutCreateNoExceptions() throws Exception {
        db.createDatabase();
        MyJDBCConnection.initConnection(false);

        Connection conn = DriverManager.getConnection(db.getUrl());
        assertNotNull("Verify that the connection is not null.", conn);
    }

    @Test
    public void testGetConnectionReturnsSameConnectionEachTime() throws Exception {
        MyJDBCConnection.initConnection(true);

        Connection c1 = MyJDBCConnection.getConnection();
        Connection c2 = MyJDBCConnection.getConnection();

        assertSame("Assert both connections are the same", c1, c2);
    }

    @Test
    public void testGetConnectionInitializesNewConnection() throws Exception {
        Connection c1 = MyJDBCConnection.getConnection();
        assertNotNull("Assert connection not null", c1);
    }

    @Test
    public void testCloseClosesConnection() throws Exception {
        db.createDatabase();
        Connection c1 = MyJDBCConnection.getConnection();

        MyJDBCConnection.close();

        assertTrue("Assert connection is closed", c1.isClosed());
    }

    @Test
    public void testGetConnectionGetsNewConnectionAfterClose() throws Exception {
        db.createDatabase();
        Connection c1 = MyJDBCConnection.getConnection();
        MyJDBCConnection.close();
        Connection c2 = MyJDBCConnection.getConnection();

        assertNotSame("Assert c1 and c2 are not the same object", c1, c2);
        assertFalse("Assert connection 2 is not closed", c2.isClosed());
    }

    @Test
    public void testGetConnectionGetsNewConnectionAfterFlagForRenewal() throws Exception {
        db.createDatabase();
        Connection c1 = MyJDBCConnection.getConnection();
        MyJDBCConnection.flagForRenewal();
        Connection c2 = MyJDBCConnection.getConnection();

        assertNotSame("Assert c1 and c2 are not the same object", c1, c2);
        assertFalse("Assert connection 2 is not closed", c2.isClosed());
    }

}
