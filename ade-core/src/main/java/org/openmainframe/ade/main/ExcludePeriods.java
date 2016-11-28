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

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.xml.transform.Source;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.dataStore.IDataStoreSources;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.main.Train;
import org.openmainframe.ade.main.ControlProgram;
import org.openmainframe.ade.main.TrainLogs;
import org.slf4j.LoggerFactory;

public class ExcludePeriods extends ControlProgram {
	
	private static final String INCLUDE_OPT = "include-periods";
	private static final String ALL_SOURCES_OPT = "all-sources";
	private static final String SELECT_SOURCES_OPT = "select-sources";
	private static final String UNSELECT_SOURCES_OPT = "unselect-sources";
	private static final String DURATION_OPT = "duration";
	private static final String START_DATE_OPT = "start-date";
	private static final String END_DATE_OPT = "end-date";
	   /**
     * non-static logger, useful also for extending classes
     */
    protected org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * static logger useful for static methods
     */
    protected static org.slf4j.Logger s_logger = LoggerFactory.getLogger(Train.class);

	private final String HELP = getClass()+": Operate on data belonging to selected " +
			"sources from a specified time span. Periods are selected if fully " +
			"contained in the date range (inclusive).\n" +
			"Examples:\n" +
			"\t"+getClass().getSimpleName()+" --"+ALL_SOURCES_OPT+" (select all possible data)\n" +
			"\t"+getClass().getSimpleName()+" --"+ALL_SOURCES_OPT+" --"+UNSELECT_SOURCES_OPT+" src1 src3 (select all sources except 'src1' and 'src3')\n" +
			"\t"+getClass().getSimpleName()+" --"+SELECT_SOURCES_OPT+" src1 src2 (select sources 'src1' and 'src2')\n" +
			"\t"+getClass().getSimpleName()+" --"+ALL_SOURCES_OPT+" --"+START_DATE_OPT+" 02/01/2013 (select data since the beginning of February 2013)\n" +
			"\t"+getClass().getSimpleName()+" --"+ALL_SOURCES_OPT+" --"+END_DATE_OPT+" 03/01/2013 (select data until the end of February 2013)\n" +
			"\t"+getClass().getSimpleName()+" --"+ALL_SOURCES_OPT+" --"+START_DATE_OPT+" 02/01/2013 --"+END_DATE_OPT+" 03/01/2013 (select data from February 2013)\n" +
			"\t"+getClass().getSimpleName()+" --"+ALL_SOURCES_OPT+" --"+START_DATE_OPT+" 02/01/2013 --"+DURATION_OPT+" P1M (select data from February 2013)\n" +
			"\t"+getClass().getSimpleName()+" --"+ALL_SOURCES_OPT+" --"+END_DATE_OPT+"  03/01/2013 --"+DURATION_OPT+" P1M (select data from February 2013)\n" +
			"\t"+getClass().getSimpleName()+" --"+ALL_SOURCES_OPT+" --"+START_DATE_OPT+" 02/01/2013 --"+DURATION_OPT+" PT1000H (select date from 1000 hours since beginning of February 2013)\n" +
			"\t"+getClass().getSimpleName()+" --"+ALL_SOURCES_OPT+" --"+START_DATE_OPT+" \"02/01/2013 12:00\" --"+DURATION_OPT+" PT12H (select data from noon till midnight of February 1st 2013)\n" +
			"\t"+getClass().getSimpleName()+" --"+ALL_SOURCES_OPT+" --"+START_DATE_OPT+" \"02/01/2013 Asia/Jerusalem\" --"+DURATION_OPT+" P1M (select data from February 2013, Jerusalem time)\n" +
			"\t"+getClass().getSimpleName()+" --"+ALL_SOURCES_OPT+" --"+START_DATE_OPT+" \"02/01/2013 -05:00\" --"+DURATION_OPT+" P1M (select data from February 2013, UTC-5 time)\n\n";
	
	protected boolean m_exclude;
	protected Period m_period = null;
	protected DateTime m_endDate = null;
	protected DateTime m_startDate = null;

	protected Collection<ISource> m_sources;

	@Override
	protected boolean doControlLogic() throws AdeException {
		excludePeriods(m_sources, m_startDate, m_endDate, m_exclude);
		return true;
	}
	
	protected void excludePeriods(Collection<ISource> m_sources2, 
			DateTime startDate, DateTime endDate, boolean exclude) 
					throws AdeException {
		// for all sources
		for (ISource source: m_sources) {
			String msg = "Beginning to operate on source: "+source;
			System.out.println(msg);
			logger.info(msg);
			
			for (IPeriod period: 
				TrainLogs.getSourcePeriods((ISource) source, m_startDate, m_endDate)) {
				period.setExcludeFromTraining(m_exclude);
				Ade.getAde().getDataStore().periods().updatePeriodMetaData(period);
			}
		}
	}

	@SuppressWarnings("static-access")
	@Override
	protected void parseArgs(String[] args) throws AdeException {
		Option helpOpt = new Option("h", "help", false, "Print help message and exit");

		Option includeOpt = OptionBuilder
		.withLongOpt(INCLUDE_OPT)
		.withDescription("include selected data instead of excluding")
		.create();
		
		Option allSourcesOpt = OptionBuilder
		.withLongOpt(ALL_SOURCES_OPT)
		.isRequired(false)
		.withDescription("Use all sources")
		.create();

		Option selectSourcesOpt = OptionBuilder
		.withLongOpt(SELECT_SOURCES_OPT)
		.withArgName("SOURCES")
		.hasArgs()
		.isRequired(false)
		.withDescription("Use selected sources")
		.create();

		OptionGroup inputSourcesOptGroup = new OptionGroup()
		.addOption(allSourcesOpt)
		.addOption(selectSourcesOpt);
		inputSourcesOptGroup.setRequired(true);

		Option unselectSourcesOpt = OptionBuilder
		.withLongOpt(UNSELECT_SOURCES_OPT)
		.withArgName("SOURCES")
		.hasArgs()
		.isRequired(false)
		.withDescription("Unselect sources. Used only in conjunction with the 'all-sources' option.")
		.create();

		Option periodOpt = OptionBuilder.withArgName("duration (ISO 8601)")
		.withLongOpt(DURATION_OPT)
		.hasArg()
		.withDescription("Duration from/to start/end date. Defaults to infinity. Replaces either 'start-date' or 'end-date'")
		.create();

		Option startDateOpt = OptionBuilder.withArgName("MM/dd/yyyy[ HH:mm][ Z]")
		.withLongOpt(START_DATE_OPT)
		.hasArg()
		.isRequired(false)
		.withDescription("Start of date range. Optional. Replaces 'duration' when used along with 'end-date'")
		.create();
		
		Option endDateOpt = OptionBuilder.withArgName("MM/dd/yyyy[ HH:mm][ Z]")
		.withLongOpt(END_DATE_OPT)
		.hasArg()
		.isRequired(false)
		.withDescription("End of date range. Defaults to this moment. Replaces 'duration' when used along with 'start-date'")
		.create();


		Options options = new Options();
		options.addOption(helpOpt);
		options.addOption(includeOpt);
		options.addOptionGroup(inputSourcesOptGroup);
		options.addOption(unselectSourcesOpt);
		options.addOption(periodOpt);
		options.addOption(endDateOpt);
		options.addOption(startDateOpt);

		CommandLineParser parser = new GnuParser();
		CommandLine line = null;

		try {
			// parse the command line arguments
			line = parser.parse(options, args);
		} catch (ParseException exp) {
			new HelpFormatter().printHelp(HELP+"\nOptions:", options );
			throw new AdeUsageException("Command line parsing failed", exp);
		}

		if (line.hasOption(helpOpt.getOpt())) {
			new HelpFormatter().printHelp(HELP, options );
			closeAll();
			System.exit(0);
		}

		m_exclude = !line.hasOption(INCLUDE_OPT);
		
		if (line.hasOption(UNSELECT_SOURCES_OPT) && !line.hasOption(ALL_SOURCES_OPT)) {
			throw new AdeUsageException("'"+UNSELECT_SOURCES_OPT+"' cannot be used without '"+ALL_SOURCES_OPT+"'");
		}

		if (line.hasOption(ALL_SOURCES_OPT)) {
			Collection<ISource> allSources = Ade.getAde().getDataStore().sources().getAllSources();
			System.out.println("Operating on all available sources");
			if (!line.hasOption(UNSELECT_SOURCES_OPT)) {
				m_sources = allSources;
			} else {
				Collection<ISource> unselectedSources = parseSources(line.getOptionValues(UNSELECT_SOURCES_OPT));
				System.out.println("Omitting sources: "+unselectedSources.toString());
				m_sources = new TreeSet<ISource>(allSources);
				m_sources.removeAll(unselectedSources);
			}
		} else if (line.hasOption(SELECT_SOURCES_OPT)) {
			m_sources = parseSources(line.getOptionValues(SELECT_SOURCES_OPT));
			System.out.println("Operating on sources: "+m_sources);
		}
		
		if (line.hasOption(DURATION_OPT) && line.hasOption(START_DATE_OPT) && line.hasOption(END_DATE_OPT)) {
			throw new AdeUsageException("Cannot use '"+START_DATE_OPT+"', '"+END_DATE_OPT+"' and '"+DURATION_OPT+"' together");
		}
		if (line.hasOption(DURATION_OPT)) {
			this.m_period = ISOPeriodFormat.standard().parsePeriod(line.getOptionValue(DURATION_OPT));
		}
		if (line.hasOption(START_DATE_OPT)) {
			m_startDate = Train.parseDate(line.getOptionValue(START_DATE_OPT));
		}
		if (line.hasOption(END_DATE_OPT)) {
			m_endDate = Train.parseDate(line.getOptionValue(END_DATE_OPT));
		}
		
		if (m_period != null) {
			if (m_startDate==null && m_endDate!=null) {
				m_startDate = m_endDate.minus(m_period);
			} else if (m_startDate!=null && m_endDate==null) {
				m_endDate = m_startDate.plus(m_period);
			} else {
				throw new IllegalStateException();
			}
		}
	}

	private static Collection<ISource> parseSources(String[] rawSources) throws AdeException {
		IDataStoreSources sourcesStore = Ade.getAde().getDataStore().sources();
		Collection<ISource> sources = new TreeSet<ISource>();
		for (String rawSource: rawSources) {
			ISource source;
			try {
				source = sourcesStore.getSource(rawSource);
				if (source == null) {
					throw new AdeUsageException("Unknown source: "+rawSource);
				}
				sources.add(source);
			} catch (AdeException e) {
				new AdeUsageException("Failed parsing sources: "+rawSources, e);
			}
		}
		return sources;
	}
	
	public static void main(String[] args) throws AdeException {
		new ExcludePeriods().runMain(args);
	}

}
