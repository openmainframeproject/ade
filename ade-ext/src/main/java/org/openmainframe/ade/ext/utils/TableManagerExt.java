/*
 
    Copyright IBM Corp. 2012, 2016
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
package org.openmainframe.ade.ext.utils;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.dataStore.IDataStoreUser;
import org.openmainframe.ade.exceptions.AdeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Class for creating/dropping/deleting Ext specific tables. 
 */
public class TableManagerExt {

    /** 
     * All table names in dependency order, i.e, 
     * if B depends on A then A will appear first. 
     */
    private static final EXT_TABLES_SQL[] ALL_TABLES = {
            EXT_TABLES_SQL.LOG_FILES,
            EXT_TABLES_SQL.MANAGED_SYSTEMS,
            EXT_TABLES_SQL.GROUP_TIMESTAMPS,
            EXT_TABLES_SQL.ANALYSIS_RESULTS_ADEEXT
            };

    private static final Logger logger = LoggerFactory.getLogger(TableManagerExt.class);
    
    
    /**
     * Empty constructor.
     */
    public TableManagerExt() {
        
    }


    /**
     * Create all tables & indices.
     *
     * @throws - AdeException if unable to create the tables or indices
     */
    public final void createTables() throws AdeException {

        logger.trace("-->entry");
        createTables(ALL_TABLES);
        createIndices();
        logger.trace("<--exit");

    } 

    /**
     * Create given tables.
     *
     * @param tables - an array of Ade Ext tables
     * @throws - AdeException if unable to access the datastore
     */
    public final void createTables(EXT_TABLES_SQL[] tables) throws AdeException {

        if (tables == null) {
            return;
        }

        final IDataStoreUser ud = Ade.getAde().getDataStore().user();
        //* Loop through the tables
        for (int i = 0; i < tables.length; i++) {
            final String sql = String.format("CREATE TABLE %s (%s)", tables[i].name(), tables[i].create());
            logger.trace(sql);
            ud.executeDml(sql);
        } 
    } 

    /**
     * Returns list of tables in order safe for delete/drop.
     */
    public final EXT_TABLES_SQL[] getAllTablesInReverseDependencyOrder() {

        final EXT_TABLES_SQL[] res = new EXT_TABLES_SQL[ALL_TABLES.length];

        for (int i = 0; i < ALL_TABLES.length; ++i) {
            res[i] = ALL_TABLES[ALL_TABLES.length - i - 1];
        }

        return res;
    } 

    /**
     * Delete content of all tables.
     */
    public final void deleteAll() throws AdeException {
        logger.trace("-->entry");
        deleteTables(getAllTablesInReverseDependencyOrder());
        logger.trace("<--exit");
    } 

    /**
     * Delete content of all given tables.
     * 
     * @param tables - an array of Ade Ext tables.
     *
     */
    public final void deleteTables(EXT_TABLES_SQL[] tables) throws AdeException {

        if (tables == null) {
            return;
        }

        for (int i = 0; i < tables.length; i++) {
            Ade.getAde().getDataStore().user().executeDml("delete from " + tables[i].name());
        }
    }

    /**
     * Drop content of all tables.
     */
    public final void dropTables() throws AdeException {
        logger.trace("-->entry");
        dropTables(getAllTablesInReverseDependencyOrder());
        logger.trace("<--exit");
    } 

    /**
     * Drop content of all given tables.
     * 
     * @param tables - an array of Ade Ext tables
     *
     */
    public final void dropTables(EXT_TABLES_SQL[] tables) throws AdeException {

        if (tables == null) {
            return;
        }

        final IDataStoreUser ud = Ade.getAde().getDataStore().user();
        for (int i = 0; i < tables.length; i++) {
            ud.dropTable(tables[i].name());
        }
    } 

    /**
     * Create indices.
     */
    private void createIndices() throws AdeException {
    }

} 
