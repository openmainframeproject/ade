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
package org.openmainframe.ade.impl.dataStore;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.dbUtils.DriverType;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.dataStore.DataStoreSqlImpl;
import org.openmainframe.ade.impl.dbUtils.Database;
import org.openmainframe.ade.impl.dbUtils.DerbyDatabase;
import org.openmainframe.ade.impl.dbUtils.MyJDBCConnection;
import org.openmainframe.ade.utils.LazyObj;
import org.openmainframe.ade.utils.LazyObj.ObjectCreationException;
import org.openmainframe.ade.utils.patches.Version;

public class DataStoreSqlImplTest {

    private static final Database db = new DerbyDatabase();

    private static Ade ade;

    private static LazyObj<DriverType> s_driverType;
    
    @BeforeClass
    public static void setup() throws Exception {
        /* Set the properties for our local database */
        MyJDBCConnection.setDbConnectionProperties(db.getUrl(), db.getUser(), db.getPassword());

        /* Create a mock Ade object, stubbing the necessary method calls that are
         * encountered on our way. */
        ade = mock(Ade.class, RETURNS_DEEP_STUBS);
        when(ade.getConfigProperties().database().getDatabaseDriver()).thenReturn("derby");
        when(ade.getConfigProperties().database().getDriverType()).thenReturn(DriverType.DERBY);
        when(ade.getConfigProperties().getOverrideVersionCheck()).thenReturn(true);
        when(ade.getDbVersion()).thenReturn(new Version(1, 0));
        
        Ade.create(ade);
    }

    @Before
    public void dropDatabase() throws Exception {
        db.dropDatabase();
    }

    @Test
    public void testThatConstructorWithCreateCreatesTables() throws Exception {
        new DataStoreSqlImpl(ade, null, null, null, null, null, null, null, null, null, true);

        assertThat("The database should be created with the appropriate tables.", db.listTables(),
                hasItems("ANALYSIS_RESULTS", "INTERVALS", "ADE_VERSIONS", "MESSAGE_IDS"));
    }

    @Test(expected = AdeException.class)
    public void testThatConstructorThrowsAdeExceptionIfDBDoesNotExistAndCreateFalse() throws Exception {
        new DataStoreSqlImpl(ade, null, null, null, null, null, null, null, null, null, false);
    }
}
