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
package org.openmainframe.ade.ext.main;

import org.openmainframe.ade.dataStore.IDataStore;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;
import org.openmainframe.ade.ext.service.AdeExtMessageHandler;
import org.openmainframe.ade.ext.utils.TableManagerExt;

/** Main for a utility allowing simple db operations */
public class ControlDB extends org.openmainframe.ade.main.ControlDB {
    /**
     * The input parameters
     */
    private String[] m_myArgs;

    /**
     * Variable pointing to the DataStore
     */
    private IDataStore m_dataStore;

    /**
     * The entry point of ControlDB
     * 
     * @param args
     * @throws AdeException
     */
    public static void main(String[] args) throws AdeException {
        final AdeExtRequestType requestType = AdeExtRequestType.CONTROL_DB;
        System.err.println("Running Ade: " + requestType);

        final AdeExtMessageHandler messageHandler = new AdeExtMessageHandler();

        final ControlDB controlDB = new ControlDB();
        try {
            controlDB.run(args);
        } catch (AdeUsageException e) {
            messageHandler.handleUserException(e);
        } catch (AdeInternalException e) {
            messageHandler.handleAdeInternalException(e);
        } catch (AdeException e) {
            messageHandler.handleAdeException(e);
        } catch (Throwable e) {
            messageHandler.handleUnexpectedException(e);
        } finally {
            controlDB.quietCleanup();
        }

    }

    /**
     * Parse the input arguments
     */
    protected void parseArgs(String[] args) throws AdeUsageException {
        if (args.length == 0) {
            usageError("No arguments supplied");
        }
        m_myArgs = args;
    }

    @Override
    protected boolean doControlLogic() throws AdeException {
        // Convert first argument to an enum
        final ControlDBOperator operator = ControlDBOperator.getOperatorType(m_myArgs[0]);

        // Create operation requires special call to Ade, to handle the case of non-existant db 
        // all other operations simply use getDataStore
        if (operator == ControlDBOperator.CreateDB) {
            m_dataStore = a_ade.createDataStore();
        } else {
            m_dataStore = a_ade.getDataStore();
        }

        // Handle specific operations
        switch (operator) {
            case CreateDB:
                return doCreate();
            case Drop:
                return doDrop();
            case DeleteData:
                return doReset();
            case Query:
                if (m_myArgs.length != 2) {
                    throw new AdeUsageException("The query operator requires a single additional parameter");
                }
                doQuery(m_myArgs[1]);
                break;
            case Dml:
                if (m_myArgs.length != 2) {
                    throw new AdeUsageException("The dml operator requires a single additional parameter");
                }
                doDml(m_myArgs[1]);
                break;
            default:
                throw new AdeInternalException("Cannot handle " + operator);
            }

        return true;
    }

    /**
     * Enum with all the supported (including the previously supported)
     * operators.
     */
    public enum ControlDBOperator {
        //Creates the database
        CreateDB("create"),
        //delete from DB tables
        DeleteData("delete"),
        //Executes a query and prints its content
        Query("query"), Dml("dml"),
        //Delete the database tables, and keep the database
        Drop("drop"), Patch11("patch11"), Patch15("patch15"), Patch16("patch16"), Patch18("patch18"),
        // Unknown operator
        Unknown("unknown");

        private String m_operatorName;

        ControlDBOperator(String operatorName) {
            m_operatorName = operatorName;
        }

        public String getOperatorName() {
            return m_operatorName;
        }

        /** Find enum value matching given string */
        public static ControlDBOperator getOperatorType(String operatorName) throws AdeException {
            for (ControlDBOperator val : values()) {
                if (val.getOperatorName().equalsIgnoreCase(operatorName)) {
                    return val;
                }
            }
            usageError("Illegal argument value: " + operatorName);
            return null;
        }
    }

    /**
     * Output the syntax of ControlDB together with an error message.
     * 
     * @param errorMsg
     * @throws AdeUsageException
     */
    private static void usageError(String errorMsg) throws AdeUsageException {
        System.out.flush();
        System.err.println("Usage:");
        System.err.println("\tcontroldb create");
        System.err.println("\tcontroldb delete");
        System.err.println("\tcontroldb drop");
        System.err.println("\tcontroldb query <sql>");
        System.err.println("\tcontroldb dml <sql>");
        System.err.println("");
        System.err.flush();
        throw new AdeUsageException(errorMsg);
    }

    /**
     * Create the Database Table for AdeExt
     * 
     * @param dataStore
     * @return
     * @throws AdeException
     */
    protected boolean doCreate() throws AdeException {
        /* Note: Creation of Ade Table is done in the caller method */

        /* Create AdeExt Tables */
        System.out.println("Creating database for AdeExt: "
                + a_ade.getConfigProperties().database().getDatabaseUrl());
        final TableManagerExt tableManagerExt = new TableManagerExt();
        tableManagerExt.dropTables();
        tableManagerExt.createTables();
        System.out.println("Done.");
        return true;
    }

    /**
     * Delete all the database tables.
     * 
     * @param dataStore
     * @return
     * @throws AdeException
     */
    protected boolean doDrop() throws AdeException {
        System.out.println("Deleting the database Tables");

        /* Delete the AdeExt Tables */
        new TableManagerExt().dropTables();

        /* Delete the Ade Tables */
        super.doDrop(a_ade.getDataStore());

        return true;
    }

    /**
     * Delete all the content within the existing database tables.
     * 
     * @param dataStore
     * @return
     * @throws AdeException
     */
    protected boolean doReset() throws AdeException {
        System.out.println("Deleting the content of Database Tables");

        /* Delete the content in the Ade Tables */
        super.doReset(a_ade.getDataStore());

        /* Delete the content in the AdeExt Tables */
        new TableManagerExt().deleteAll();
        m_dataStore.deleteAllContent();

        return true;
    }

    /**
     * Perform Query given a SQL statement
     */
    protected boolean doQuery(String queryStr) throws AdeException {
        System.out.println("Querying the databse");
        return super.doQuery(m_dataStore, queryStr);

    }

    /**
     * Print results of a given sql query. Multiple queries are allowed,
     * separated by ;
     */
    private void doDml(String text) throws AdeException {
        System.out.println("Executing DML statements");

        final String[] stats = text.split(";");

        for (String stat : stats) {
            System.out.println("Executing: " + stat);
            m_dataStore.user().executeDml(stat);
        }
    }

}
