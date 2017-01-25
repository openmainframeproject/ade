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


import java.text.ParseException;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;
import org.openmainframe.ade.ext.service.AdeExtMessageHandler;
import org.openmainframe.ade.main.TrainLogs;

/**
 * Main of the Train process
 */
public class Train extends TrainLogs {
    /**
     * Entry point to train.
     * 
     * @param args
     * @throws AdeException
     */
    public static void main(String[] args) throws AdeException {
        final AdeExtRequestType requestType = AdeExtRequestType.TRAIN;
        System.err.println("Running Ade: " + requestType);

        final AdeExtMessageHandler messageHandler = new AdeExtMessageHandler();

        final Train train = new Train();
        try {
            train.run(args);
        } catch (AdeUsageException e) {
            messageHandler.handleUserException(e);
        } catch (AdeInternalException e) {
            messageHandler.handleAdeInternalException(e);
        } catch (AdeException e) {
            messageHandler.handleAdeException(e);
        } catch (Throwable e) {
            messageHandler.handleUnexpectedException(e);
        } finally {
            train.quietCleanup();
        }
    }

    /**
     * The start / end date passed to Ade
     */
    private DateTime m_startDateTime;
    private DateTime m_endDateTime;
	private CharSequence duration_key = "-d";

    /**
     * Parse the AdeExt Argument.  This method is used to parse
     * the input argument in AdeCore.
     * 
     * @param args
     * @return 
     * @throws AdeException
     */
    public final ArrayList<String> parseAdeExtArgs(String[] args) throws AdeException {
        ArrayList<String> adeArgs;
        adeArgs = new ArrayList<String>();
        int m_duration = 0;

        if (args.length == 0) {
            usageError("Expecting at least one argument");
            return new ArrayList<String>(0);
        }
        if (args.length > 3) {
            usageError("Too many arguments");
        }

        /* Convert the Source argument to AnalysisGroup */
        if (args[0].equalsIgnoreCase("all")) {
            adeArgs.add("-a");
        } else {
            /* Add the source.  
             * Note that Ade expect analysisGroup instead of source. 
             *
             * In Upload and Analyze, the AnalysisGroup always use the same name as 
             * the sourceId.  Therefore, the sourceId input here can be used as 
             * AnalysisGroup name.
             */
            adeArgs.add("-s");
            adeArgs.add(args[0]);
        }

        //"MM/dd/yyyy HH:mm ZZZ"
        final DateTimeFormatter outFormatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm ZZZ").withOffsetParsed()
                .withZoneUTC();
        /* check for duration indicator instead of start or end date */
        if (args[1].contains(duration_key )){
        	/* extract duration of training */
        	String[] values = args[1].split((String) duration_key);
        	if (values[1] == null) {
       			System.out.println("Duration requires number of day- "
    					+ "value provided: " + values[1]);
       		 usageError("Incorrect training duration specified");
        	}
        	else {
        		try {
        		   m_duration = Integer.parseInt(values[1]);
        		}
        		catch(NumberFormatException e){
        			System.out.println("Duration requires number of day- "
        					+ "value provided: " + values[1]);
              		 usageError("Incorrect training duration specified");
        		}
        	}
    		m_endDateTime = new DateTime();
           	adeArgs.add("-start-date");
           	m_startDateTime = m_endDateTime.minusDays(m_duration);
           	adeArgs.add(m_startDateTime.toString(outFormatter));
        	adeArgs.add("-end-date");
    		adeArgs.add(m_endDateTime.toString(outFormatter));
        }
        else {
        /* Handle the end date if exist */

        	if (args.length > 2) {
        		final DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yyyy").withOffsetParsed().withZoneUTC();
        		try {
        			m_endDateTime = formatter.parseDateTime(args[2]);
        		}
        		catch(IllegalArgumentException parseEx){
        			usageError("Incorrect end date specifed");;
        	    }
            /* Move the endDate's time to the end of date.  AdeCore requires the end date
             * to be the start day of the next day. 
             * 
             * For example, if the endDate specified is 10/10.  The endDate will be set to
             * 10/11 00:00:00.  This will include the entire 10/10 in the Model.  */
        		adeArgs.add("-end-date");
        		m_endDateTime = m_endDateTime.withDurationAdded(Duration.standardDays(1), 1);
        		adeArgs.add(m_endDateTime.toString(outFormatter));
        	} else {
            /* If endDate wasn't specified, don't specify it. */
        		m_endDateTime = null;
        	}

        /* Handle the start date if exist */
        	if (args.length > 1) {
        		final DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yyyy").withOffsetParsed().withZoneUTC();
        		try{
        			m_startDateTime = formatter.parseDateTime(args[1]);
        		}
        		catch(IllegalArgumentException parseEx){
        			usageError("Incorrect start date specifed");;
        	    }
        		adeArgs.add("-start-date");
        		adeArgs.add(m_startDateTime.toString(outFormatter));
        	} else {
        		m_startDateTime = null;
        	}
        }	
        /* call the super class with the converted arguments */
        StringBuilder bldadeArgsString = new StringBuilder("");
        for (String arg : adeArgs) {
            bldadeArgsString.append(arg + " ");
        }
        logger.trace("Arguments used to call TrainLog: " + bldadeArgsString.toString());

        return adeArgs;
    }

    /**
     * Parse the input parameters defined by Anomaly Detection Engine, and map it to 
     * parameters defined by Ade.
     */
    protected final void parseArgs(String[] args) throws AdeException {
        final ArrayList<String> adeArgs = parseAdeExtArgs(args);
        super.parseArgs(adeArgs.toArray(new String[adeArgs.size()]));
    }

    /**
     * Return the start DateTime
     * @return
     */
    public final DateTime getStartDateTime() {
        return m_startDateTime;
    }

    /**
     * Return the end DateTime
     * @return
     */
    public final DateTime getEndDateTime() {
        return m_endDateTime;
    }

    /**
     * Output error message together with the syntax for this class.
     * 
     * @param errorMsg
     * @throws AdeUsageException
     */
    private void usageError(String errorMsg) throws AdeUsageException {
        System.out.flush();
        System.err.println("Usage:");
        System.err.println("\ttrain <analysis_group_name> [<start date> | <start date> <end date>] ");
        System.err.println("\ttrain <analysis_group_name> -d<training period> ");
        System.err.println("\ttrain all [<start date> | <start date> <end date>");
        System.err.println("\ttrain all [<start date> | -d<training period>");
        System.err.println();
        System.err.println("Reads summary of all messages within the specified dates and system id");
        System.err.println("and updates the default model for this system.");
        System.err
                .println("Specifying 'all' instead of analysis_group_name creates a model for each system in the database");
        System.err.println("For Linux, analysis group name is the name of the group for a set of Linux Systems.");
        System.err.println();
        System.err.flush();
        throw new AdeUsageException(errorMsg);
    }

}
