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
package org.openmainframe.ade.dataStore;

import java.sql.Connection;

import org.openmainframe.ade.exceptions.AdeException;

/** An interface for manipulating datastore user-defined tables
 * 
 * Care should be taken not to use this interface to manipulate ade-core's table
 */
public interface IDataStoreUser {

    /** return a Connection object for arbitrary database operations */
    Connection getConnection() throws AdeException;

    /** Execute a DML sql statement */
    void executeDml(String sql) throws AdeException;

    /** Drops given table */
    void dropTable(String tableName) throws AdeException;

    /** Prints the result of given query */
    void printQuery(String sql) throws AdeException;
}
