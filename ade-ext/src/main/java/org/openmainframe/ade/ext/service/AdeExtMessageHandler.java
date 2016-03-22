/*
 
    Copyright IBM Corp. 2015, 2016
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
package org.openmainframe.ade.ext.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.main.VerifyLinuxTraining;
import org.openmainframe.ade.ext.service.notifications.TrainingPredconditionException;

/**
 *      generic AdeUsageException is     100
 *      generic AdeException is          101
 *      generic AdeInternalException is  102
 *      generic UnexpectedException is      103
 *
 *      cause of failure is also set based on exception message
 *                  Insufficient data               10
 *                  Insufficient periods(days)      11
 *                  Model did not converge          12
 *                  Model does not exist            13
 *                  Number of intervals with
 *                      message number insufficient 14 (set via TrainingPreconditionException)
 *                  Priming request contained
 *                      all failing messages id     15
 *                  Unable to read data             40
 *                  Database error (read)           41
 *                  Database inconsistency error    42
 *                  Unable to write data            60
 *                  Database error (write)          61
 *                  Analytics error                 90
 */

public class AdeExtMessageHandler {
    /**
     * Date formatter for use with notification messages for code not yet using org.joda.time APIs.
     */
    protected static final ThreadLocal<DateFormat> javaUtilDateFormatter = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
        }
    };

    /**
     * Date formatter for use with notification messages for code using org.joda.time APIs
     */
    public static final DateTimeFormatter jodaDateFormatter;

    static {
        jodaDateFormatter = DateTimeFormat.forPattern("E, dd MMM yyyy HH:mm:ss Z");
        jodaDateFormatter.withZoneUTC();
    }

    /**
     * Array of Message Text
     */
    private static String[] messageTextException = new String[100];

    /**
     * Array of return value
     */
    private static int[] returnValueException = new int[100];
    
    /**
     * @param m_requestType
     */
    public AdeExtMessageHandler() {
        initializeMessageList();
    }

    /**
     * Find the message in the list of known messages.
     *
     * convert message text to return value
     * when message text matches string then return cause of error
     *
     * @param messageText
     * @param defaultReturnCode - Return code to use if the message is not found.
     * @return the corresponding return code for the given error.
     */
    private int messageConvertReturnValue(String messageText, int defaultReturnCode) {
        int i = 0;
        for (String workText : messageTextException) {
            if (workText == null) {
                return defaultReturnCode;
            }
            final int workTextLength = workText.length();
            if (messageText.regionMatches(0, workText, 0, workTextLength)) {
                return returnValueException[i];
            }
            i++;
        }
        return defaultReturnCode;
    }

    /**
     * Detected that training would likely not work given the amount of message
     * data we received.
     *
     * @param tpe
     */
    public final void handleTrainingPredconditionException(TrainingPredconditionException tpe) {
        /* This method is a first attempt to use typed exceptions an get away from the
         * brittle message mapping code in this module.
         */
        System.out.flush();

        if (tpe.getMessage() != null) {
            System.out.println("\n"+tpe.getMessage());            
        }

        // Magic number the old version of this code used.
        final int return_value = 14; 

        systemExitAssist(return_value);
    }

    /**
     * Prints error message and exit after setting appropriate rc for TQM
     */
    public final void handleUserException(AdeUsageException e) {
        debugAssist(e);

        /* create return value based on exception message */
        final int return_value = messageConvertReturnValue(e.getMessage(), 100);

        systemExitAssist(return_value);
    }

    /**
     * Prints error message and exit after setting appropriate rc for TQM
     */
    public final void handleAdeInternalException(AdeInternalException e) {
        debugAssist(e);
        /* create return value based on exception message */
        final int return_value = messageConvertReturnValue(e.getMessage(), 102);

        systemExitAssist(return_value);
    }

    /**
     * Prints error message and exit after setting appropriate rc for TQM
     */
    public final void handleAdeException(AdeException e) {
        debugAssist(e);

        // create return value based on exception message
        final int return_value = messageConvertReturnValue(e.getMessage(), 101);

        systemExitAssist(return_value);
    }

    /**
     * Prints error message and exit after setting appropriate rc for TQM
     */
    public final void handleUnexpectedException(Throwable e) {
        debugAssist(e);

        systemExitAssist(103);
    }

    /**
     * Print the exception to stderr and do other setup to
     * assist in debug of the problem.
     * @param e
     */
    private void debugAssist(Throwable e) {
        // Expect this is done to avoid System.out and System.err interleaving when
        // redirected to the same place.
        System.out.flush();

        /* dump traceback stack to stderr */
        e.printStackTrace();
    }

    /**
     * Usage discouraged. Caller should handle their own system.exit
     * @param return_value Return code of the JVM
     */
    private void systemExitAssist(int return_value) {
        System.exit(return_value);
    }

    /**
     * Publish an informational notifications message. Caller provides id and values
     * which is different from the other code in this class. This method does not
     * do System.exit and will not fail the process if the notification fails to publish.
     *
     * @param asynchMsgId
     * @param values
     */
    public final void publishInfoMessage(String asynchMsgId, String... values) {
        System.out.flush();
    }

    /**
     * Prime a master list of error text and corresponding return values.
     *
     * Initialize list of messages
     * @see VerifyLinuxTraining It publishes its own notification in some cases.
     */
    private static void initializeMessageList() {
        messageTextException[0] = "Cannot train with no periods";
        returnValueException[0] = 11;
        messageTextException[1] = "Error: No periods for";
        returnValueException[1] = 11;
        messageTextException[2] = "Clustering did not converge: reached the maximum of";
        returnValueException[2] = 12;
        messageTextException[3] = "No trained model for source";
        returnValueException[3] = 13;
        messageTextException[4] = "Source";
        returnValueException[4] = 13;
        messageTextException[5] = "Source group";
        returnValueException[5] = 13;
        messageTextException[6] = "Cannot open";
        returnValueException[6] = 40;
        messageTextException[7] = "Cannot open file";
        returnValueException[7] = 40;
        messageTextException[8] = "Error in opening file";
        returnValueException[8] = 40;
        messageTextException[9] = "Error parsing xml";
        returnValueException[9] = 40;
        messageTextException[10] = "Error reading";
        returnValueException[10] = 40;
        messageTextException[11] = "Error reading from";
        returnValueException[11] = 40;
        messageTextException[12] = "Error: unable to open static file";
        returnValueException[12] = 40;
        messageTextException[13] = "Error: unble to initiliaze text score";
        returnValueException[13] = 40;
        messageTextException[14] = "Failed opening file";
        returnValueException[14] = 40;
        messageTextException[15] = "Failed reading from";
        returnValueException[15] = 40;
        messageTextException[16] = "Failed reading properties from";
        returnValueException[16] = 40;
        messageTextException[17] = "I/O error occurred while parsing log";
        returnValueException[17] = 40;
        messageTextException[18] = "I/O error";
        returnValueException[18] = 40;
        messageTextException[19] = "IO problem with file ";
        returnValueException[19] = 40;
        messageTextException[20] = "Missing property:";
        returnValueException[20] = 40;
        messageTextException[21] = "Database access error";
        returnValueException[21] = 41;
        messageTextException[22] = "Db access error";
        returnValueException[22] = 41;
        messageTextException[23] = "Db access was disabled";
        returnValueException[23] = 41;
        messageTextException[24] = "Db access was disabled";
        returnValueException[24] = 41;
        messageTextException[25] = "Db error";
        returnValueException[25] = 41;
        messageTextException[25] = "Failed closing connection ";
        returnValueException[25] = 41;
        messageTextException[26] = "Failed obtaining connection";
        returnValueException[26] = 41;
        messageTextException[27] = "failed querying db";
        returnValueException[27] = 41;
        messageTextException[28] = "Failed starting transaction";
        returnValueException[28] = 41;
        messageTextException[29] = "Loading driver failed";
        returnValueException[29] = 41;
        messageTextException[30] = "Cannot close";
        returnValueException[30] = 60;
        messageTextException[31] = "Copy file utility error";
        returnValueException[31] = 60;
        messageTextException[32] = "Could not close";
        returnValueException[32] = 60;
        messageTextException[33] = "Could not write interval in";
        returnValueException[33] = 60;
        messageTextException[34] = "Error closing log";
        returnValueException[34] = 60;
        messageTextException[35] = "Error creating DOM object";
        returnValueException[35] = 60;
        messageTextException[36] = "Error in closing";
        returnValueException[36] = 60;
        messageTextException[37] = "Error in closing file";
        returnValueException[37] = 60;
        messageTextException[38] = "Error parsing log";
        returnValueException[38] = 60;
        messageTextException[39] = "Error printing DOM";
        returnValueException[39] = 60;
        messageTextException[40] = "Error writing DOM to file ";
        returnValueException[40] = 60;
        messageTextException[41] = "Error: closing text score file";
        returnValueException[41] = 60;
        messageTextException[42] = "Error: unable to open complete file";
        returnValueException[42] = 60;
        messageTextException[43] = "Failed creating directory";
        returnValueException[43] = 60;
        messageTextException[44] = "Failed creating directory";
        returnValueException[44] = 60;
        messageTextException[45] = "Failed locking";
        returnValueException[45] = 60;
        messageTextException[46] = "Failed to open output fil";
        returnValueException[46] = 60;
        messageTextException[47] = "Failed to rename index.xml";
        returnValueException[47] = 60;
        messageTextException[48] = "Failed unlocking";
        returnValueException[48] = 60;
        messageTextException[49] = "Failed writing to ";
        returnValueException[49] = 60;
        messageTextException[50] = "Failed closing db";
        returnValueException[50] = 61;
        messageTextException[51] = "Failed committing";
        returnValueException[51] = 61;
        messageTextException[52] = "Failed ending transaction";
        returnValueException[52] = 61;
        messageTextException[53] = "failed executing sql statement:";
        returnValueException[53] = 61;
        messageTextException[55] = "No data for";
        returnValueException[55] = 11;
    }
}
