/*
 
    Copyright IBM Corp. 2011, 2016
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

import java.sql.Connection;

import org.openmainframe.ade.dataStore.IDataStoreUser;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.dbUtils.MyJDBCConnection;
import org.openmainframe.ade.impl.dbUtils.TableGeneralUtils;

/**
 * Implements methods for manipulating datastore user-defined tables.
 */
public class DataStoreUserImpl implements IDataStoreUser {


    /**
     * Constructor to create an empty DataStoreUserImpl.
     */
    public DataStoreUserImpl() {
        /* Nothing to do here, just construct the parent */
        super();
    }

    @Override
    public final Connection getConnection() throws AdeException {
        return MyJDBCConnection.getConnection();
    }

    @Override
    public final void executeDml(String sql) throws AdeException {
        /* Execute the sql statement over the default connection */
        ConnectionWrapper.executeDmlDefaultCon(sql);
    }

    @Override
    public final void dropTable(String tableName) throws AdeException {
        TableGeneralUtils.dropTable(tableName);
    }

    @Override
    public final void printQuery(String sql) throws AdeException {
        TableGeneralUtils.printQueryResults(sql);
    }

}
