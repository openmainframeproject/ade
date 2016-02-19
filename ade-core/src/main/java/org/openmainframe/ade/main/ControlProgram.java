/*
 
    Copyright IBM Corp. 2008, 2016
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

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run a control task.
 * <p>
 * All control programs take a single optional argument 
 * which points to a file system directory were this program expects 
 * to find the setup properties file called "setup.props".
 * <p>
 * If no arguments are provided this program takes a default location.
 * <p>
 * If the setup properties file is not found, an error is reported.
 */
public abstract class ControlProgram {

    private void handleUserException(AdeUsageException e) {
        logger.error("An error occurred: " + e.getMessage() + ". See the Ade log for more details.");
        System.exit(1);
    }

    /**
     * The singleton {@link Ade} object. May be referenced by subclasses of this class.
     */
    protected Ade a_ade;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    // utility method - close all closeables at the end of the control task.
    protected void closeAll() throws AdeException {
        if (a_ade != null) {
            logger.info("Closing ade");
            a_ade.close();
            a_ade = null;
        }
    }

    /**
     * The actual logic of a control program is performed here.
     * This is the main method subclasses should implement in order to run a Ade main
     * program. It is called by {@link ControlProgram#run(String, String[])} after calling 
     * {@link ControlProgram#parseArgs(String[])}.
     * <br>
     * After the method returns, {@link ControlProgram#closeAll()} is called to cleanly 
     * terminate the process.
     * 
     * @throws AdeUsageException if the program failed due to a user error (e.g. parameter 
     * misspelling in configuration file etc.)
     * @throws AdeInternalException if the program failed due to an unexpected error (e.g.
     * I/O or database connection failure, or an unexpected internal application state.   
     */
    protected abstract boolean doControlLogic() throws AdeException;

    private final boolean execute() throws AdeException {
        logger.info("Starting: " + this.getClass());
        final long start = System.currentTimeMillis();
        final boolean success = doControlLogic();
        final long total = System.currentTimeMillis() - start;
        logger.info(this.getClass() + " succeeded. Total time elapsed: "
            + DateTimeUtils.millisecondsToHumanTime(total));
        return success;
    }

    protected void init(String[] args) throws AdeException {
        Ade.createIfNeeded();
        a_ade = Ade.getAde();
        a_ade.setCommandLineArguments(args);
    }

    /**
     * Command line arguments parsing is performed by this method.
     * Subclasses should implement this method in order to run a Ade main program
     * that accept command line arguments. Data members that hold the parsed arguments
     * may be used later on by {@link ControlProgram#doControlLogic()} as it is called
     * by {@link ControlProgram#run(String, String[])} right after calling this method.
     *  
     * @param args command line arguments. These are passed from the <code>main</code> 
     * method to this method through {@link ControlProgram#run(String, String[])}.
     * 
     * @throws AdeUsageException upon bad command line arguments.
     * @throws AdeInternalException upon any other unexpected failure. 
     */
    protected abstract void parseArgs(String[] args) throws AdeException;

    protected void quietCleanup() {
        if (a_ade != null) {
            a_ade.quietCleanup();
            a_ade = null;
        }
    }

    /**
     * Creates and run a Ade main program. Any usage error is caught here and 
     * communicated to the user. Other errors cause a {@link AdeInternalException}.
     * 
     * This method should be invoked by any subclass of {@link ControlProgram} to start the program, 
     * usually passing the <code>args</code> parameter from the subclass <code>main</code> method.
     * the run method initializes Ade log functionality and Ade singleton objects, and takes care
     * of closing them when it exits. It also takes care of logging exceptions that occurred during
     * the run and communicating them to the user.
     * 
     * @param args optional arguments that are passed to the {@link ControlProgram#doControlLogic()}.
     * @throws AdeInternalException if an unexpected error occurred during the run.
     */
    public boolean run(String[] args) throws AdeException {
        init(args);
        try {
            parseArgs(args);
        } catch(AdeUsageException e) {
            String msg = e.getMessage();
            if (msg != null) {
                logger.error(msg);
            }
            return false;
        }
       
        final boolean success = execute();
        return success;
    }

    public void runMain(String[] args) throws AdeException {
        boolean success = false;
        try {
            // This block logs exception caught. 
            // The init method above is outside it since it initializes the logger
            try {
                success = run(args);
            } catch (Throwable t) {
                logger.error("Exception occured", t);
                throw t;
            }
            closeAll();
        } catch (AdeUsageException e) {
            handleUserException(e);
        } catch (AdeException e) {
            throw e;
        } catch (Throwable e) {
            throw new AdeInternalException("Internal bug", e);
        } finally {
            quietCleanup();
        }

        final int exitCode = success ? 1 : 0;
        System.exit(exitCode);
    }
}
