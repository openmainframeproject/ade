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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.dataStore.GroupRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISOPeriodFormat;

/**
 * Abstract class for training. Message logs training is performed by default,
 * but configuration training  is optional, and can be done by implementing
 * the {@link #trainConfig(List)} method.
 */
abstract class Train extends ControlProgram {

    /**
     * non-static logger, useful also for extending classes
     */
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * static logger useful for static methods
     */
    protected static Logger s_logger = LoggerFactory.getLogger(Train.class);

    private static final String ALL_OPT = "all";
    private static final String GROUPS_OPT = "groups";
    private static final String UNSELECT_OPT = "unselect";
    private static final String NUM_DAYS_OPT = "num-days";
    private static final String DURATION_OPT = "duration";
    private static final String START_DATE_OPT = "start-date";
    private static final String END_DATE_OPT = "end-date";

    private final String HELP = getClass() + ": Operate on data belonging to selected "
            + "analysis groups from a specified time span. Periods are selected if fully "
            + "contained in the date range (inclusive).\n"
            + "Examples:\n"
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " (select all possible data)\n"
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " --" + UNSELECT_OPT + " ag1 ag3 (select all analysis groups except 'ag1' and 'ag3')\n"
            + "\t" + getClass().getSimpleName() + " --" + GROUPS_OPT + " ag1 ag2 (select analysis groups 'ag1' and 'ag2')\n"
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " --" + START_DATE_OPT + " 02/01/2013 (select data since the beginning of February 2013)\n"
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " --" + END_DATE_OPT + " 03/01/2013 (select data until the end of February 2013)\n"
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " --" + START_DATE_OPT + " 02/01/2013 --" + END_DATE_OPT + " 03/01/2013 (select data from February 2013)\n"
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " --" + START_DATE_OPT + " 02/01/2013 --" + DURATION_OPT + " P1M (select data from February 2013)\n"
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " --" + END_DATE_OPT + " 03/01/2013 --" + DURATION_OPT + " P1M (select data from February 2013)\n"
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " --" + START_DATE_OPT + " 02/01/2013 --" + DURATION_OPT + " PT1000H (select date from 1000 hours since beginning of February 2013)\n" 
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " --" + START_DATE_OPT + " 02/01/2013 --" + NUM_DAYS_OPT + " 10 (select data from the first 10 days of February 2013)\n" 
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " --" + START_DATE_OPT + " \"02/01/2013 12:00\" --" + DURATION_OPT + " PT12H (select data from noon till midnight of February 1st 2013)\n"
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " --" + START_DATE_OPT + " \"02/01/2013 Asia/Jerusalem\" --" + DURATION_OPT + " P1M (select data from February 2013, Jerusalem time)\n"
            + "\t" + getClass().getSimpleName() + " --" + ALL_OPT + " --" + START_DATE_OPT + " \"02/01/2013 -05:00\" --" + DURATION_OPT + " P1M (select data from February 2013, UTC-5 time)\n\n";

    protected Period m_period = null;
    protected DateTime m_endDate = null;
    protected DateTime m_startDate = null;

    protected Set<Integer> m_analysisGroups;

    @Override
    protected boolean doControlLogic() throws AdeException {
        if (m_period != null) {
            if (m_startDate == null && m_endDate != null) {
                m_startDate = m_endDate.minus(m_period);
            } else if (m_startDate != null && m_endDate == null) {
                m_endDate = m_startDate.plus(m_period);
            }
        }

        // for all source groups
        for (int analysisGroup : m_analysisGroups) {
            final String msg = "Beginning to operate on analysis group: " + analysisGroup;
            System.out.println(msg);
            logger.info(msg);

            trainAnalysisGroup(analysisGroup, m_startDate, m_endDate);
        }
        return true;
    }

    abstract protected void trainAnalysisGroup(int analysisGroup, DateTime startDate, DateTime endDate) throws AdeException;

    @Override
    protected void parseArgs(String[] args) throws AdeException {
        final Option helpOpt = new Option("h", "help", false, "Print help message and exit");

        OptionBuilder.withLongOpt(ALL_OPT);
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("All analysis groups");
        final Option allAnalysisGroupsOpt = OptionBuilder.create('a');

        OptionBuilder.withLongOpt(GROUPS_OPT);
        OptionBuilder.withArgName("ANALYSIs GROUPS");
        OptionBuilder.hasArgs();
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Selected analysis groups");
        final Option selectAnalysisGroupsOpt = OptionBuilder.create('s');

        final OptionGroup inputAnalysisGroupsOptGroup = new OptionGroup()
                .addOption(allAnalysisGroupsOpt)
                .addOption(selectAnalysisGroupsOpt);
        inputAnalysisGroupsOptGroup.setRequired(true);

        OptionBuilder.withLongOpt(UNSELECT_OPT);
        OptionBuilder.withArgName("ANALYSIS GROUPS");
        OptionBuilder.hasArgs();
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Unselect analysis groups. Used only with '" + ALL_OPT + "'");
        final Option unselectAnalysisGroupsOpt = OptionBuilder.create('u');

        OptionBuilder.withLongOpt(DURATION_OPT);
        OptionBuilder.withArgName("DURATION (ISO 8601)");
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg();
        OptionBuilder.withDescription(
                "Duration from/to start/end date. Defaults to infinity. Replaces either 'start-date' or 'end-date'");
        final Option periodOpt = OptionBuilder.create();


        OptionBuilder.withLongOpt(NUM_DAYS_OPT);
        OptionBuilder.withArgName("INT");
        OptionBuilder.hasArg();
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Number of days. same as '" + DURATION_OPT + "'");
        final Option numDaysOpt = OptionBuilder.create('n');

        final OptionGroup periodOptGroup = new OptionGroup()
                .addOption(periodOpt)
                .addOption(numDaysOpt);

        OptionBuilder.withLongOpt(START_DATE_OPT);
        OptionBuilder.withArgName("MM/dd/yyyy[ HH:mm][ Z]");
        OptionBuilder.hasArg();
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription(
                "Start of date range. Optional. Replaces 'duration'/'num-days' when used along with 'end-date'");
        final Option startDateOpt = OptionBuilder.create();

        OptionBuilder.withLongOpt(END_DATE_OPT);
        OptionBuilder.withArgName("MM/dd/yyyy[ HH:mm][ Z]");
        OptionBuilder.hasArg();
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription(
                "End of date range. Defaults to this moment. Replaces 'duration'/'num-days' when"
                + " used along with 'start-date'");
        final Option endDateOpt = OptionBuilder.create();

        final Options options = new Options();
        options.addOption(helpOpt);
        options.addOptionGroup(inputAnalysisGroupsOptGroup);
        options.addOption(unselectAnalysisGroupsOpt);
        options.addOptionGroup(periodOptGroup);
        options.addOption(endDateOpt);
        options.addOption(startDateOpt);

        final CommandLineParser parser = new GnuParser();
        CommandLine line = null;

        try {
            // parse the command line arguments
            line = parser.parse(options, args);
        } catch (MissingOptionException exp) {
            new HelpFormatter().printHelp(HELP + "\nOptions:", options);
            throw new AdeUsageException("Command line parsing failed", exp);
        } catch (ParseException exp) {
            // oops, something went wrong
            throw new AdeUsageException("Argument Parsing failed", exp);
        }

        if (line.hasOption(helpOpt.getLongOpt())) {
            new HelpFormatter().printHelp(HELP, options);
            closeAll();
            System.exit(0);
        }

        if (line.hasOption(UNSELECT_OPT) && !line.hasOption(ALL_OPT)) {
            throw new AdeUsageException("'" + UNSELECT_OPT + "' cannot be used without '" + ALL_OPT + "'");
        }

        final Set<Integer> allAnalysisGroups = Ade.getAde().getDataStore().sources().getAllAnalysisGroups();
        if (line.hasOption(ALL_OPT)) {
            System.out.println("Operating on all available analysis groups");
            if (!line.hasOption(UNSELECT_OPT)) {
                m_analysisGroups = allAnalysisGroups;
            } else {
                final Set<Integer> unselectedAnalysisGroups = parseAnalysisGroups(allAnalysisGroups, line.getOptionValues(UNSELECT_OPT));
                final Set<String> unselectedGroupNames = getGroupNames(unselectedAnalysisGroups);
                System.out.println("Omitting analysis groups: " + unselectedGroupNames.toString());
                m_analysisGroups = new TreeSet<Integer>(allAnalysisGroups);
                m_analysisGroups.removeAll(unselectedAnalysisGroups);
            }
        } else if (line.hasOption(GROUPS_OPT)) {
            m_analysisGroups = parseAnalysisGroups(allAnalysisGroups, line.getOptionValues(GROUPS_OPT));
            final Set<String> operatingAnalysisGroups = getGroupNames(m_analysisGroups);
            System.out.println("Operating on analysis groups: " + operatingAnalysisGroups.toString());
        }

        if ((line.hasOption(NUM_DAYS_OPT) || line.hasOption(DURATION_OPT))
                && line.hasOption(START_DATE_OPT) && line.hasOption(END_DATE_OPT)) {
            throw new AdeUsageException("Cannot use '" + DURATION_OPT + "'/'" + NUM_DAYS_OPT
                    + "', '" + START_DATE_OPT + "' and '" + END_DATE_OPT + "' together");
        }
        if (line.hasOption(NUM_DAYS_OPT)) {
            final String numDaysStr = line.getOptionValue(NUM_DAYS_OPT);
            final int numDays = Integer.parseInt(numDaysStr);
            this.m_period = Period.days(numDays);
        }
        if (line.hasOption(DURATION_OPT)) {
            final String periodStr = line.getOptionValue(DURATION_OPT);
            this.m_period = ISOPeriodFormat.standard().parsePeriod(periodStr);
        }
        if (line.hasOption(START_DATE_OPT)) {
            m_startDate = parseDate(line.getOptionValue(START_DATE_OPT));
        }
        if (line.hasOption(END_DATE_OPT)) {
            m_endDate = parseDate(line.getOptionValue(END_DATE_OPT));
        }
    }

    public static DateTime parseDate(String dateStr) throws AdeUsageException {
        // the patterns and formatters are separated into 2 arrays to allow 
        // logging which pattern matched/failed in parsing the string (unable
        // to obtain pattern from formatter)
        final String[] patterns = new String[] { "MM/dd/yyyy", "MM/dd/yyyy HH:mm",
                "MM/dd/yyyy Z", "MM/dd/yyyy HH:mm Z", "MM/dd/yyyy ZZZ",
                "MM/dd/yyyy HH:mm ZZZ"
        };
        final DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormat.forPattern(patterns[0]).withOffsetParsed().withZoneUTC(),
                DateTimeFormat.forPattern(patterns[1]).withOffsetParsed().withZoneUTC(),
                DateTimeFormat.forPattern(patterns[2]).withOffsetParsed(),
                DateTimeFormat.forPattern(patterns[3]).withOffsetParsed(),
                DateTimeFormat.forPattern(patterns[4]).withOffsetParsed(),
                DateTimeFormat.forPattern(patterns[5]).withOffsetParsed()
        };
        for (int i = 0; i < formatters.length; i++) {
            try {
                final DateTime res = formatters[i].parseDateTime(dateStr);
                s_logger.info(String.format("Succesfully parsed date string '%s'"
                        + " with pattern '%s", dateStr, patterns[i]));
                return res;
            } catch (IllegalArgumentException e) {
                // ignore exception and continue iterating through formatters
                continue;
            }
        }
        throw new AdeUsageException("Failed parsing date string " + dateStr);
    }

    private static Set<Integer> parseAnalysisGroups(final Set<Integer> allAnalysisGroups, String[] rawAnalysisGroups) throws AdeException {
        final Map<Integer, String> groupIdToNameMap = new HashMap<Integer,String>();
        for (int groupId : allAnalysisGroups){
            String groupName = GroupRead.getAnalysisGroupName(groupId);
            groupIdToNameMap.put(groupId, groupName);
        }
        final Set<Integer> analysisGroupsId = new TreeSet<Integer>();
        for (String analysisGroup : rawAnalysisGroups){
            boolean containsAnalysisGroup = false;
            for (Map.Entry<Integer,String> IdAndName : groupIdToNameMap.entrySet()){
                if (IdAndName.getValue().equals(analysisGroup)){
                    analysisGroupsId.add(IdAndName.getKey());
                    containsAnalysisGroup = true;
                    break;
                }
            }
            if (!containsAnalysisGroup){
                throw new AdeUsageException("Unknown analysis group: " + analysisGroup + ".\nAvailable analysis groups: " + groupIdToNameMap.values().toString());
            }
        }
        return analysisGroupsId;
    }
    
    /**
     * Retrieves the analysis group names by calling a GroupRead static class method.
     * @param groupIds Set of group internal ids.
     * @return The analysis group names for the internal ids passed in.
     * @throws AdeException
     */
    private Set<String> getGroupNames(Set<Integer> groupIds) throws AdeException{
        final Set<String> groupNames = new TreeSet<String>();
        for (int groupId : groupIds){
            String groupName = GroupRead.getAnalysisGroupName(groupId);
            groupNames.add(groupName);
        }
        return groupNames;
    }
}
