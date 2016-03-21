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

import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;
import org.openmainframe.ade.ext.service.notifications.TrainingPredconditionException;
import org.openmainframe.ade.ext.utils.ArgumentConstants;
import org.openmainframe.ade.ext.utils.ExtDateTimeUtils;
import org.openmainframe.ade.flow.IAdeIterator;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.FileUtils;

/**
 * Main of the Verify Linux Training process.
 *
 *   Verify Linux Training
 *       Function
 *           Parses / checks input parameters
 *           Extracts collection of message ids by interval within requested periods
 *           Determines if number of message ids is sufficient for pattern matching
 *           Outputs return code
 *
 */
public class VerifyLinuxTraining extends ExtControlProgram {
    private static class MessageMetrics {
        /**
         * Number of unique messages in all intervals
         */
        private final int numUniqueMessageIds;

        /**
         * number of intervals containing message ids
         */
        private final int numIntervalsWithMessages;

        /**
         * Number of intervals
         */
        private final int numIntervals;

        public MessageMetrics(int numUniqueMessageIds,
                int numIntervalsWithMessages, int numIntervals) {
            super();
            this.numUniqueMessageIds = numUniqueMessageIds;
            this.numIntervalsWithMessages = numIntervalsWithMessages;
            this.numIntervals = numIntervals;
        }

        public int getNumUniqueMessageIds() {
            return numUniqueMessageIds;
        }

        public int getNumIntervalsWithMessages() {
            return numIntervalsWithMessages;
        }

        public int getNumIntervals() {
            return numIntervals;
        }
    }

    /**
     * Any value below this is considered to be a 'short time'.
     */
    private static final int LONG_TIME_THRESHOLD_IN_INTERVALS = 1000;

    /**
     * Any value below this is considered an insufficient number of
     * messages in the 'short time' case.
     */
    private static final int MANY_MESSAGES_THRESHOLD = 180;

    /**
     * If we have a long period of time, any number of messages below this is
     * a trival number and is insufficent to allow training to continue.
     */
    private static final int NONTRIVIAL_NUM_MESSAGES_THRESHOLD = 20;

    /**
     * Linux analysis group
     */
    private String m_analysisGroupId;

    /**
     * Start date for intervals (or null)
     */
    private Date m_startDate;

    /**
     * End date for intervals (or null)
     */
    private Date m_endDate;

    /**
     * trace active
     */
    private boolean m_traceOn;

    /**
     * path to trace files
     */
    private File m_traceOutputPath;
    
    public VerifyLinuxTraining() {
        super(AdeExtRequestType.CHECK_LINUX_MESSAGES);
    }

    /**
     * Entry point of verify linux training
     *
     * @param args
     */
    public static void main(String[] args) {
        final VerifyLinuxTraining instance = new VerifyLinuxTraining();
        instance.verifyTraining(args);
    }

    protected final void verifyTraining(String[] args) {
        // Exception handlers expected to invoke System.exit
        try {
            run(args);
        } catch (TrainingPredconditionException tpe) {
            getMessageHandler().handleTrainingPredconditionException(tpe);
        } catch (AdeUsageException e) {
            getMessageHandler().handleUserException(e);
        } catch (AdeInternalException e) {
            getMessageHandler().handleAdeInternalException(e);
        } catch (AdeException e) {
            getMessageHandler().handleAdeException(e);
        } catch (Throwable e) {
            getMessageHandler().handleUnexpectedException(e);
        } finally {
            // Won't get called in error cases due to use of System.ext.
            // This apppears to be a no-op anyway.
            quietCleanup();
        }

    }


    /**
     * The main logic for VerifyLinuxTraining
     * @throws AdeException
     * @throws TrainingPredconditionException
     */
    @Override
    protected final boolean doControlLogic() throws AdeException, TrainingPredconditionException {
        // Get all the Linux systems that belong to the model group.
        final Set<ISource> sources = ArgumentConstants.getAnalysisGroupSourcesFromArgument(m_analysisGroupId);

        /* set trace on */
        m_traceOn = true;

        /*  process requested sources */
        System.out.printf("Start VerifyLinuxTraining for analysis group %s \n", m_analysisGroupId);
        
        System.out.println("\tSources:");
        for (ISource source : sources) {
            System.out.println("\t " + source.getSourceId());
        }
        
        /* Extract data from the database */
        final MessageMetrics metrics = computeMessageMetrics(a_ade, sources, m_startDate, m_endDate);

        /* Check for sufficient message ids */
        // Throws if the message traffic is not sufficient for training.
        checkMessageDensity(metrics);

        System.out.println("\nMessage traffic is sufficient for training.\n");
        return true;
    }

    /**
     * Process the input arguments
     */
    @Override
    protected final void parseArgs(String[] args) throws AdeException {
        if (args.length == 0) {
            usageError("Expecting at least one argument");
            return;
        }
        if (args.length > 3) {
            usageError("Too many arguments");
        }
        m_analysisGroupId = args[0];
        if (args.length > 1) {
            m_startDate = ExtDateTimeUtils.startOfDay(ArgumentConstants.parseDate(args[1]));
        } else {
            // This has to be set or the period queries will fail
            usageError("Expecting start date argument");
        }
        if (args.length > 2) {
            m_endDate = ArgumentConstants.parseDate(args[2]);

            // Move to end of day.
            m_endDate = ExtDateTimeUtils.daysAfter(ExtDateTimeUtils.startOfDay(m_endDate), 1);
        } else {
            // This has to be set for queries to work.
            usageError("Expecting end date argument");
        }
    }

    /**
     * Output error related to the invocation of VerifyLinuxTraining
     *
     * @param errorMsg
     * @throws AdeUsageException
     */
    private void usageError(String errorMsg) throws AdeUsageException {

        System.out.flush();
        System.err.println("Usage:");
        System.err.println("\tVerifyLinuxTraining <analysis_group> <start date> <end date> ");
        System.err.println();
        System.err.println("Determines if the date range includes sufficient methods to allow training for the analysis group");
        System.err.println();
        System.err.flush();
        throw new AdeUsageException(errorMsg);

    }

    /**
     * Count the number of occurences of unique messages IDs in the given time range
     * for all of the sources, assumed to be from a single model group.
     * Also sets the member variables for the number of intervals and the number
     * of intervals containing messages.
     *
     * @param sourceSet
     * @param start
     * @param end
     *
     * @return MessageMetrics - interesting data to use in computing whether the training can proceed.
     *
     * @throws AdeException
     */
    private static MessageMetrics computeMessageMetrics(Ade ade, Set<ISource> sourceSet, Date start, Date end) throws AdeException {
        int numIntervals = 0;
        int numIntervalsWithMessages = 0;
        final Map<Integer, Integer> occurrence = new HashMap<Integer, Integer>();

        System.out.println("Start computeMessageMetrics ");

        /*
         * Get data from the database
         *
         * get list of periods from start date to end date for each source
         */
        final Collection<IPeriod> periods = new ArrayList<IPeriod>();
        for (ISource source : sourceSet) {
            periods.addAll(Ade.getAde().getDataStore().periods().getAllPeriods(source, start, end));
        }

        /* return if no periods */
        if (periods.isEmpty()) {
            return new MessageMetrics(0, 0, 0);
        }

        /* extract data from the database for each period */
        final List<IInterval> curIntervals = new ArrayList<IInterval>();
        final Comparator<IInterval> intervalComparator = new Comparator<IInterval>() {
            @Override
            public int compare(IInterval int1, IInterval int2) {
                final long diff = int1.getIntervalStartTime() - int2.getIntervalStartTime();

                // Casting to an int isn't a shortcut here.
                if (diff < 0) {
                    return -1;
                } else if (diff > 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };

        // Periods are days. 120 with default training params for Linux.
        for (IPeriod period : periods) {
            final FramingFlowType framingFlowType = Ade.getAde().getFlowFactory().getFlowByName("LINUX").getMyFramingFlows().get("tenMinutesTrain");
            final IAdeIterator<IInterval> iterator = ade.getDataStore().periods().getPeriodIntervals(period, framingFlowType);

            try {
                /* For each interval update count of messages
                 *   fetch data
                 */
                curIntervals.clear();
                iterator.open();

                // Because intervals overlap (each includes 50 minutes of the previous interval),
                // we only want to count one interval with messages per hour so sort the intervals
                // by time and make sure that intervals in the same hour don't increases the amount
                // of messages multiple times
                IInterval curInt;
                while ((curInt = iterator.getNext()) != null) {
                    curIntervals.add(curInt);
                }

                Collections.sort(curIntervals, intervalComparator);

                // We need the total number of intervals for notification messages.
                numIntervals += curIntervals.size();

                DateTime lastIntervalTime = null;
                boolean addedMessage = false;

                for (IInterval curInterval : curIntervals) {
                    // Check to see if this interval is for the same hour as the previously processed one,
                    // if it's not set it as the new interval and allow the amount of intervals with messages
                    // to be incremented
                    if (lastIntervalTime == null) {
                        lastIntervalTime = new DateTime(curInterval.getIntervalStartTime());
                    } else {
                        DateTime currentIntervalTime = new DateTime(curInterval.getIntervalStartTime());
                        currentIntervalTime = currentIntervalTime.minusMinutes(currentIntervalTime.getMinuteOfHour()).minusSeconds(currentIntervalTime.getSecondOfMinute()).minusMillis(currentIntervalTime.getMillisOfSecond());

                        if (!lastIntervalTime.isEqual(currentIntervalTime)) {
                            lastIntervalTime = currentIntervalTime;
                            addedMessage = false;
                        }
                    }

                    final Collection<IMessageSummary> msgSummaries = curInterval.getMessageSummaries();
                    if (!msgSummaries.isEmpty() && !addedMessage) {
                        numIntervalsWithMessages++;
                        addedMessage = true;
                    }
                    for (IMessageSummary msgSummary : msgSummaries) {
                        final int msgID = msgSummary.getMessageInternalId();
                        if (occurrence.containsKey(msgID)) {
                            /* get number of occurrences for this msgID (internal msgID)
                             * increment it and put back again */
                            occurrence.put(msgID, occurrence.get(msgID) + 1);
                        } else {
                            /* this is first time we see this word, set value '1' */
                            occurrence.put(msgID, 1);
                        }
                    }
                }
                iterator.close();
            } finally {
                iterator.quietCleanup();
            }
        }

        return new MessageMetrics(occurrence.size(), numIntervalsWithMessages, numIntervals);
    }

    /**
     * Determine if there are enough unique messages in the given time-span to justify training.
     * If not, throw.
     *
     * @param metrics
     *
     * @throws AdeUsageException
     * @throws TrainingPredconditionException
     */
    private void checkMessageDensity(MessageMetrics metrics) throws AdeUsageException, TrainingPredconditionException {
        final int numIntervalsWithMsg = metrics.getNumIntervalsWithMessages();
        final int numUniqueMsgIds = metrics.getNumUniqueMessageIds();
        final int numIntervals = metrics.getNumIntervals();
        final int ANALYSIS_INTERVAL_LEN_MINUTES = 60;

        /* if tracing active write out results */
        // Notifications are erased at activation, stderr capture is erased when a new process of the same
        // type is started, this is erased at every train. So at times this might be our best source of
        // information.
        if (m_traceOn) {
            /* If tracing active then create directories for trace file */
            final File out = AdeInternal.getAdeImpl().getDirectoryManager().getTracePath();
            m_traceOutputPath = new File(out, m_analysisGroupId);
            FileUtils.createDir(m_traceOutputPath);

            final PrintWriter fout = FileUtils.openPrintWriterToFile(new File(m_traceOutputPath, "verifyLinuxTraining.summary"),
                    true);
            fout.printf("Linux Training Check \n");
            fout.printf("For analysis group: %s \n", this.m_analysisGroupId);
            if (m_startDate != null) {
                fout.printf("Start date: %s\n", DateFormat.getDateInstance().format(m_startDate));
            } else {
                fout.printf("Start date: (not specified)\n");
            }
            if (m_endDate != null) {
                fout.printf("End Date: %s\n", DateFormat.getDateInstance().format(m_endDate));
            } else {
                fout.printf("End Date: (not specified)\n");
            }
            fout.println();
            fout.printf("Number of intervals with message ids: %d \n", (int) numIntervalsWithMsg);
            fout.printf("Number of messages in sufficient intervals: %d\n", numUniqueMsgIds);
            fout.printf("Bad model algorithm: (%d < 180 && %d < 1000) || (%d < 20 && %d >= 1000)\n", numUniqueMsgIds,
                    numIntervalsWithMsg, numUniqueMsgIds, numIntervalsWithMsg);
            fout.println();
            fout.close();
        }

        boolean isSufficientData = true;

        /* Check if we have sufficient data */
        // These checks are based on what we've seen in customer Linux data.
        if (numIntervalsWithMsg < LONG_TIME_THRESHOLD_IN_INTERVALS) {
            // We have a short period of time, pass if we have a large amount of messages in that time.
            if (numUniqueMsgIds < MANY_MESSAGES_THRESHOLD) {
                // Check fails, throw, fail training and publish a notification.
                isSufficientData = false;
            }

        } else {
            // We have a long period of time, pass if we have a nontrivial amount of messages.
            if (numUniqueMsgIds < NONTRIVIAL_NUM_MESSAGES_THRESHOLD) {
                // Check fails, throw, fail training and publish a notification.
                isSufficientData = false;
            }
        }

        if (!isSufficientData) {
            throw new TrainingPredconditionException(m_analysisGroupId, m_requestType,
                    new DateTime(m_startDate.getTime()), new DateTime(m_endDate.getTime()),
                    numUniqueMsgIds, numIntervals, numIntervalsWithMsg,
                    ANALYSIS_INTERVAL_LEN_MINUTES);
        }
    }

}
