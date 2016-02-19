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
package org.openmainframe.ade.dataStore;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.scoringApi.IMainScorer;

/**
 *
 * An interface for accessing the datastore
 *
 */
public interface IDataStore {

    /** An interface for manipulating sources */
    IDataStoreSources sources();

    /** An interface for manipulating periods */
    IDataStorePeriods periods();
    
    /** An interface for manipulating groups */
    IDataStoreGroups groups();
    
    /** An interface for manipulating rules */
    IDataStoreRules rules();

    /** An interface for manipulating event log models */
    IDataStoreModels<IMainScorer> models();

    /** An interface for manipulating user tables */
    IDataStoreUser user();

    /** An interface for manipulating messages 
     * @throws AdeException */
    IDataStoreDictionaryApi messages() throws AdeException;

    /** Deletes all table content */
    void deleteAllContent() throws AdeException;

}
