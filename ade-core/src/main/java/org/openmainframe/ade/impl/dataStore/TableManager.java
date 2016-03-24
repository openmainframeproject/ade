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
package org.openmainframe.ade.impl.dataStore;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.dbUtils.DriverType;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.dbUtils.TableGeneralUtils;

public class TableManager {

    /** 
     * List of all table names in dependency order, 
     * i.e, if B depends on A then A will appear first 
     * */
    private static final SQL[] ALL_TABLES = {
            SQL.MESSAGE_IDS,
            SQL.COMPONENT_IDS,
            SQL.RULES,            
            SQL.GROUPS,
            SQL.SOURCES,
            SQL.PERIODS,
            SQL.PERIOD_SUMMARIES,
            SQL.ADE_VERSIONS,
            SQL.MODELS,
            SQL.TEXT_CLUSTERS,
            SQL.MESSAGE_SUMMARIES,
            SQL.INTERVALS,
            SQL.ANALYSIS_RESULTS
    };

    /**
     * Invokes methods to create tables, indexes, and set version of database definition
     * @param
     * @throws AdeException
     */
    public final void createAll() throws AdeException {
        createTables(ALL_TABLES);
        createIndices();
        initTables();
    }

    /**
     * Creates database tables
     * @param tables
     * @throws AdeException
     */
    public static void createTables(SQL... tables) throws AdeException {
        for (SQL table : tables) {
            final String sql = String.format("create table %s (%s)", table.toString(), table.create());
            ConnectionWrapper.executeDmlDefaultCon(sql);
        }
    }

    /**
     * Builds list of database tables in reverse order (order tables need to be dropped in)
     * @return
     */
    public final SQL[] getAllTablesInReverseDependencyOrder() {
        final SQL[] res = new SQL[ALL_TABLES.length];
        for (int i = 0; i < ALL_TABLES.length; ++i) {
            res[i] = ALL_TABLES[ALL_TABLES.length - i - 1];
        }
        return res;
    }

    /**
     * Deletes all tables one at a time in reverse order of creation
     * @throws AdeException
     */
    public final void deleteAll() throws AdeException {
        deleteTables(getAllTablesInReverseDependencyOrder());
    }

    /**
     * Deletes a table
     * @param tables - name of table
     * @throws AdeException
     */
    public final void deleteTables(SQL[] tables) throws AdeException {
        if (tables == null) {
            return;
        }
        for (SQL table : tables) {
            TableGeneralUtils.deleteTable(table.toString());
        }
    }

    /**
     * Drops all tables one at a time in reverse order of creation
     * @throws AdeException
     */
    public final void dropAll() throws AdeException {
        dropTables(getAllTablesInReverseDependencyOrder());
    }

    /**
     * Drops a table
     * @param tables - name of table
     * @throws AdeException
     */
    public final void dropTables(SQL[] tables) throws AdeException {
        if (tables == null) {
            return;
        }
        for (int i = 0; i < tables.length; i++) {
            TableGeneralUtils.dropTable(tables[i].toString());
        }
    }

    public final void printAll() throws AdeException {
        printTables(ALL_TABLES);
    }

    /**
     * Creates indicies
     * @throws AdeException
     */
    private void createIndices() throws AdeException {
        ConnectionWrapper.executeDmlDefaultCon("create index message_summaries_by_period_summary_internal_id on " + SQL.MESSAGE_SUMMARIES + " (PERIOD_SUMMARY_INTERNAL_ID)");
        ConnectionWrapper.executeDmlDefaultCon("create index ANALYSIS_GROUP_INDEX on "+SQL.SOURCES+" (ANALYSIS_GROUP)");
    }

    /**
     * Initializes the Ade Version table with version and current time
     * @throws AdeException 
     */
    final void initTables() throws AdeException {
        // set the version to be the current one

        final String driver = Ade.getAde().getConfigProperties().database().getDatabaseDriver();

        String query = "insert into " + SQL.ADE_VERSIONS + " (ADE_VERSION, PATCHED_TIME) values ('" + Ade.getAde().getDbVersion() + "', current timestamp)";
        if ((DriverType.parseDriverType(driver) == DriverType.MY_SQL) ||
            (DriverType.parseDriverType(driver) == DriverType.MARIADB)) {
            query = query.replace("current timestamp", "current_timestamp");
        }

        ConnectionWrapper.executeDmlDefaultCon(query);
    }

    public final void printTables(SQL[] tables) throws AdeException {
        if (tables == null) {
            return;
        }
        for (int i = 0; i < tables.length; i++) {
            TableGeneralUtils.printTable(tables[i].toString());
        }
    }

}
