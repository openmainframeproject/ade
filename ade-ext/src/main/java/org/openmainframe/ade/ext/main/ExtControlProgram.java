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

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.AdeExt;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;
import org.openmainframe.ade.ext.service.AdeExtMessageHandler;
import org.openmainframe.ade.main.ControlProgram;

/** Parent class to all classes having a main function
 *  - It has abstract parseArgs() and controlLogic() method that perform the body of the operation
 *  - Handle and reports fatal exception
 *  - Measure time of main body
 *  - Creates return value for caller                                           A8644
 *      
 *      generic AdeUsageException is     100                                 A8644
 *      generic AdeException is          101 
 *      generic AdeInternalException is  102  
 *      generic UnexpectedException is      103 
 *       
 *  
 *      cause of failure is also set based on exception message                 A8644
 *                  Insufficient data               10
 *                  Insufficient periods(days)      11
 *                  Model did not converge          12
 *                  Model does not exist            13
 *                  Number of intervals with
 *                      message number insufficient 14
 *                  Priming request contained
 *                      all failing messages id     15
 *                  Unable to read data             40
 *                  Database error (read)           41
 *                  Database inconsistency error    42
 *                  Unable to write data            60
 *                  Database error (write)          61
 *                  Analytics error                 90                              
 *                                                                              A8644
 *                  
 *  */
public abstract class ExtControlProgram extends ControlProgram {
    /**
     * A reference to the AdeExt singleton object.
     */
    protected AdeExt m_adeExt;

    /**
     * The request type
     */
    protected AdeExtRequestType m_requestType;

    /**
     * Object that handle messages, return value, asyn messages.
     */
    protected AdeExtMessageHandler messageHandler;

    /**
     * Constructor
     */
    protected ExtControlProgram(AdeExtRequestType requestType) {
        m_requestType = requestType;

        messageHandler = new AdeExtMessageHandler();
    }

    /**
     * 
     * @return
     */
    public final AdeExtRequestType requestType() {
        return m_requestType;
    }

    /** 
     * Initialize the ControlProgram 
     */
    @Override
    protected final void init(String[] args) throws AdeException {
        super.init(args);

        if (!AdeExt.isCreated()) {
            AdeExt.create(a_ade);
        }
        m_adeExt = AdeExt.getAdeExt();
    }

    /**
     * Return the MessageHandler
     * @return
     */
    protected final AdeExtMessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     * Copied from super class, so that the execute() from this class can be run.
     */
    @Override
    public final boolean run(String[] args) throws AdeException {
        init(args);
        try {
            parseArgs(args);
        } catch (AdeUsageException e) {
            String msg = e.getMessage();
            if (msg != null) {
                logger.error(msg);
            }
            return false;
        }

        final boolean success = execute();
        return success;
    }

    /** 
     * Call the doControlLogic() method defined in subclass, and 
     * provide additional traces, result handling to the doControlLogic(). 
     */
    protected final boolean execute() throws AdeException {
        logger.info("Starting execution: " + m_requestType.name());
        final long start = System.currentTimeMillis();

        final boolean success = doControlLogic();

        final long total = System.currentTimeMillis() - start;
        logger.info(m_requestType.name() + (success ? " done. " : " failed. ") + "Total time elapsed: "
                + millisecondsToHumanTime(total));

        return success;
    }

    /** 
     * Closes the two Ade objects 
     */
    @Override
    protected final void closeAll() throws AdeException {
        if (m_adeExt != null) {
            logger.info("Closing Ade Ext");
            m_adeExt.close();
            m_adeExt = null;
        }

        super.closeAll();
    }

    /** 
     * cleanup for handling exceptions 
     */
    @Override
    protected final void quietCleanup() {
        if (m_adeExt != null) {
            m_adeExt.quietCleanup();
            m_adeExt = null;
        }

        super.quietCleanup();
    }

    /** 
     * Converts a timestamp to a nice string 
     */
    public static String millisecondsToHumanTime(long period) {
        final long MILLIS_IN_SECOND = 1000L;
        final long HALF_MILLIS_IN_SECOND = 500L;
        final long SECONDS_IN_MINUTE = 60L;
        final long MINUTES_IN_HOUR   = 60L;
        
        if (period % MILLIS_IN_SECOND >= HALF_MILLIS_IN_SECOND) {
            period += MILLIS_IN_SECOND;
        }
        period /= MILLIS_IN_SECOND;
        long secs = period % SECONDS_IN_MINUTE;
        period /= SECONDS_IN_MINUTE;
        long mins = period % MINUTES_IN_HOUR;
        period /= MINUTES_IN_HOUR;
        long hours = period;
        return String.format("%02d:%02d:%02d (hh:mm:ss)", hours, mins, secs);
    }

}
