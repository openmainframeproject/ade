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
package org.openmainframe.ade.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openmainframe.ade.dataStore.IDataStore;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.dataStore.TableManager;
import org.openmainframe.ade.utils.AdeIoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a main Ade class that serves as a simple interface to the Ade internal database.
 * It may be used to create an empty Ade schema in a target database, delete database 
 * content, and execute an SQL query against the database.
 */
public class ControlDB extends ControlProgram {

    private static final Logger logger = LoggerFactory.getLogger(ControlDB.class);
    private ControlDBOperator m_op;
    private boolean m_forceOp = false;

    /**
     * operation specific arguments
     */
    private String[] m_args = null;

    /**
     * enum for database operations.
     * <p>
     * Create creates the database
     * Drop deletes the database tables 
     * Reset dleetes the contents of the db tables
     * Query executes a query against the db and prints its contents
     */
    private enum ControlDBOperator {
        Create,
        Drop,
        Reset,
        Query;
    }

    @Override
    protected void parseArgs(String[] args) throws AdeException {

        final Option helpOpt = new Option("h", "help", false, "Print help message and exit");
        final Option forceOpt = new Option("f", "force", false, "Force operation. Do not prompt for confirmation");

        OptionBuilder.withLongOpt("create");
        OptionBuilder.hasArg(false);
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Create Ade DB tables");
        final Option createOpt = OptionBuilder.create('c');

        OptionBuilder.withLongOpt("drop");
        OptionBuilder.hasArg(false);
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Drops all Ade DB tables, and clears the data store dictionaries");
        final Option deleteOpt = OptionBuilder.create('d');

        OptionBuilder.withLongOpt("reset");
        OptionBuilder.hasArg(false);
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Clears all Ade DB tables content");
        final Option resetOpt = OptionBuilder.create('r');

        OptionBuilder.withLongOpt("query");
        OptionBuilder.hasArg();
        OptionBuilder.withArgName("SQL query string");
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Performs the input query");
        final Option queryOpt = OptionBuilder.create('q');

        final OptionGroup actionGroupOpt = new OptionGroup()
                .addOption(createOpt)
                .addOption(deleteOpt)
                .addOption(resetOpt)
                .addOption(queryOpt);
        actionGroupOpt.setRequired(true);

        final Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(forceOpt);
        options.addOptionGroup(actionGroupOpt);

        final CommandLineParser parser = new GnuParser();
        CommandLine line = null;

        try {
            // parse the command line arguments
            line = parser.parse(options, args);
        } catch (MissingOptionException exp) {
            logger.error("Command line parsing failed.", exp);
            new HelpFormatter().printHelp(ControlDB.class.getName(), options);
            System.exit(0);
        } catch (ParseException exp) {
            // oops, something went wrong
            logger.error("Parsing failed.  Reason: " + exp.getMessage());
            throw new AdeUsageException("Argument Parsing failed", exp);
        }

        if (line.hasOption('h')) {
            new HelpFormatter().printHelp(getClass().getSimpleName(), options);
            closeAll();
            System.exit(0);
        }

        if (line.hasOption('f')) {
            m_forceOp = true;
        }

        if (line.hasOption('c')) {
            m_op = ControlDBOperator.Create;
        } else if (line.hasOption('d')) {
            m_op = ControlDBOperator.Drop;
        } else if (line.hasOption('r')) {
            m_op = ControlDBOperator.Reset;
        } else if (line.hasOption('q')) {
            m_op = ControlDBOperator.Query;
            m_args = new String[] { line.getOptionValue('q') };
        }
    }

    @Override
    protected boolean doControlLogic() throws AdeException {
        final String msg = String.format("Performing %s operation on: %s. Are you sure? ",
                m_op, a_ade.getConfigProperties().database().getDatabaseUrl());
        if (!m_forceOp && !AdeIoUtils.promptUser(msg)) {
            return true;
        }

        // Important!!! this line is required even if not used. It set the JDBC connection!
        final IDataStore dataStore = m_op == ControlDBOperator.Create ? a_ade.createDataStore() : a_ade.getDataStore();
        switch (m_op) {
            case Create:
                return doCreate(dataStore);
            case Drop:
                return doDrop(dataStore);
            case Reset:
                return doReset(dataStore);
            case Query:
                if (m_args == null || m_args.length != 1) {
                    throw new AdeInternalException("Exactly one argument is expected for the Query operation");
                }
                return doQuery(dataStore, m_args[0]);
            default:
                throw new AdeInternalException("Cannot handle " + m_op);
        }
    }

    protected boolean doCreate(IDataStore dataStore) throws AdeException {
        // do nothing. The data store was already created
        return true;
    }

    protected boolean doDrop(IDataStore dataStore) throws AdeException {
        new TableManager().dropAll();
        return true;
    }

    protected boolean doReset(IDataStore dataStore) throws AdeException {
        dataStore.deleteAllContent();
        return true;
    }

    protected boolean doQuery(IDataStore dataStore, String queryStr) throws AdeException {
        for (String query : queryStr.split(";")) {
            dataStore.user().printQuery(query);
        }
        return true;
    }

}
