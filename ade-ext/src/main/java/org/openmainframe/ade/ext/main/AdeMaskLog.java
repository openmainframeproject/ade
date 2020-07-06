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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.main.helper.AdeExtOperatingSystemType;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;
import org.openmainframe.ade.ext.AdeExt;
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;
import org.openmainframe.ade.ext.os.parser.LinuxSyslog3164ParserBase;
import org.openmainframe.ade.ext.os.parser.LinuxSyslog3164ParserFreeForm;
import org.openmainframe.ade.ext.os.parser.LinuxSyslog3164ParserWithCompAndPid;
import org.openmainframe.ade.ext.os.parser.LinuxSyslog3164ParserWithMark;
import org.openmainframe.ade.ext.os.parser.SparklogParser;
import org.openmainframe.ade.ext.os.parser.LinuxSyslog5424ParserBase;
import org.openmainframe.ade.ext.os.parser.LinuxSyslogLineParser;
import org.openmainframe.ade.ext.os.parser.SparklogLineParser;
import org.openmainframe.ade.ext.service.AdeExtMessageHandler;
import org.openmainframe.ade.ext.os.AdeExtPropertiesFactory;
import org.openmainframe.ade.ext.os.AdeExtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main to mask private information in a log */
public class AdeMaskLog extends ExtControlProgram {

	/**
	 * Variable pointing to the files
	 */
	private File mOutputFile;
	private File mInputFile;
	/**
	 * The parser for a line of Linux syslogs.
	 */
	private static LinuxSyslogLineParser[] mLineParsers;

	private static SparklogLineParser[] mSparkLineParsers;

	private static Pattern validIPV4Pattern;
	private static Pattern validIPV6Pattern;
	private static Pattern validEmailPattern;
	private static final String IPV4PATTERN = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
	private static final String IPV6PATTERN = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

	// pattern source: <a href="http://regxlib.com/REDetails.aspx?regexp_id=26"
	// target="_blank"
	// rel="nofollow">http://regxlib.com/REDetails.aspx?regexp_id=26</a>
	private static final String EMAILlPATTERN = "^([a-zA-Z0-9_\\-\\.])+@(([0-2]?[0-5]?[0-5]\\.[0-2]?[0-5]?[0-5]"
			+ "\\.[0-2]?[0-5]?[0-5]\\.[0-2]?[0-5]?[0-5])|((([a-zA-Z0-9\\-])+\\.)"
			+ "+([a-zA-Z\\-])+))$";

	private String mSystemName = null;
	private String mCompanyName = null;
	private String mCompanyNameNew = null;
	private Boolean mMaskTCPIPAddress = true;
	private Boolean mMaskEmailAddress = true;
	private String localHost = "127.0.0.1";

	Options options = new Options();
	/**
	 * The logger for this class.
	 */
	private static final Logger logger = LoggerFactory
			.getLogger(AdeMaskLog.class);

	/**
	 * Constructor to pass in the requestType to the super class.
	 * 
	 * @param requestType
	 *            The request type of this class.
	 * @return
	 */
	protected AdeMaskLog(AdeExtRequestType requestType) {
		super(requestType);
	}

	/**
	 * Parse the input arguments
	 */
	@SuppressWarnings("static-access")
	protected void parseArgs(String[] args) throws AdeUsageException {

		Option helpOpt = new Option("h", "help", false,
				"Mask potentially sensitive information in Linux Log RFC 3164 format");
		options.addOption(helpOpt);

		Option outputFileOpt = OptionBuilder.withLongOpt("output").hasArg(true)
				.withArgName("FILE").isRequired(false)
				.withDescription("Output file name ").create('o');
		options.addOption(outputFileOpt);

		Option inputFileOpt = OptionBuilder.withLongOpt("input").hasArg(true)
				.withArgName("FILE").isRequired(false)
				.withDescription("Input file name").create('f');
		options.addOption(inputFileOpt);

		Option systemNameOpt = OptionBuilder.withLongOpt("systemname")
				.hasArg(true).withArgName("SYSTEM_NAME").isRequired(false)
				.withDescription("String to replace system name").create('s');
		options.addOption(systemNameOpt);

		Option companyNameOpt = OptionBuilder
				.withLongOpt("companyname")
				.hasArgs(2)
				.withArgName("COMPANY_NAME> <REPLACEMENT_COMPANY_NAME")
				.isRequired(false)
				.withDescription(
						"String to find company name and replacement company name")
				.withValueSeparator(' ').create('c');
		options.addOption(companyNameOpt);

		Option ipAddressMaskOpt = OptionBuilder
				.withLongOpt("maskIPAddress")
				.withDescription(
						"Do not mask IP address with local host IP address")
				.create('t');
		options.addOption(ipAddressMaskOpt);

		Option emailAddressMaskOpt = OptionBuilder
				.withLongOpt("maskEmailAddress")
				.withDescription(
						"Do not Mask email address with gmail.com address")
				.create('e');
		options.addOption(emailAddressMaskOpt);

		CommandLineParser parser = new GnuParser();
		CommandLine line = null;

		try {
			// parse the command line arguments
			line = parser.parse(options, args);
		} catch (MissingOptionException exp) {

			new HelpFormatter().printHelp(AdeMaskLog.class.getName(), options);
			throw new AdeUsageException("Command Line parsing failed", exp);

		} catch (ParseException exp) {
			// oops, something went wrong
			logger.error("Parsing failed", exp);
			throw new AdeUsageException("Argument Parsing failed", exp);
		}

		if (line.hasOption('h')) {
			new HelpFormatter().printHelp(this.getClass().getSimpleName(),
					options);
			System.exit(0);
		}
		if (line.hasOption(helpOpt.getLongOpt())) {
			new HelpFormatter().printHelp(getClass().getSimpleName(), options);
		}

		if (line.hasOption(outputFileOpt.getLongOpt())) {
			mOutputFile = new File(line.getOptionValue(outputFileOpt
					.getLongOpt()));
		} else {
			throw new AdeUsageException(
					"Command Line parsing failed missing output file name");
		}

		if (line.hasOption(inputFileOpt.getLongOpt())) {
			mInputFile = new File(
					line.getOptionValue(inputFileOpt.getLongOpt()));
		} else {
			throw new AdeUsageException(
					"Command Line parsing failed missing input file name");
		}

		if (line.hasOption(systemNameOpt.getLongOpt())) {
			mSystemName = line.getOptionValue(systemNameOpt.getLongOpt());
		}

		if (line.hasOption(companyNameOpt.getLongOpt())) {
			String[] workArg = line
					.getOptionValues(companyNameOpt.getLongOpt());
			mCompanyName = workArg[0];
			mCompanyNameNew = workArg[1];
		}

		if (line.hasOption(ipAddressMaskOpt.getLongOpt())) {
			mMaskTCPIPAddress = false;
		}

		if (line.hasOption(emailAddressMaskOpt.getLongOpt())) {
			mMaskEmailAddress = false;
		}
	}

	/**
	 * Check if we're using Spark logs
	 */
	private static boolean isSpark() throws AdeException{
		return AdeExt.getAdeExt().getConfigProperties().isSparkLog();
	}
	

	/**
	 * Read and write file specified by input and output file name mask system
	 * name, ip addresses, email
	 * 
	 * * @see org.openmainframe.ade.main.ControlProgram#doControlLogic()
	 */

	protected boolean doControlLogic() throws AdeException {

		createParsers();
		createPattern();

		FileInputStream fis = null;
		FileWriter fos = null;
		try {
			// open input file
			// open output file
			fis = new FileInputStream(mInputFile);
			fos = new FileWriter(mOutputFile, false);

			// Construct BufferedReader from InputStreamReader
			// Construct BufferedWriter from OutputStreamWriter
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			BufferedWriter bw = new BufferedWriter(fos);

			// read first record of file
			String line = null;
			line = br.readLine();

			// process all records in file
			while (line != null) {
				String writeLine = generateMaskedLine(line);
				bw.write(writeLine);
				bw.write(System.lineSeparator());
				line = br.readLine();

			}
			
			// close 
			br.close();
			bw.close();
		} catch (FileNotFoundException e) {
			logger.error("File not found exception", e);
			throw new AdeUsageException(e.getMessage());
		} catch (IOException e) {
			logger.error("IO exception", e);
			throw new AdeInternalException(e.getMessage());
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				logger.error("IO exception during close", e);
			}

		}
		return true;

	}

	/**
	 * Create Pattern to find email and IP address
	 */
	private static void createPattern() {

		validIPV4Pattern = Pattern.compile(IPV4PATTERN,
				Pattern.CASE_INSENSITIVE);
		validIPV6Pattern = Pattern.compile(IPV6PATTERN,
				Pattern.CASE_INSENSITIVE);
		validEmailPattern = Pattern.compile(EMAILlPATTERN);
		// pattern source: <a
		// href="http://regxlib.com/REDetails.aspx?regexp_id=26" target="_blank"
		// rel="nofollow">http://regxlib.com/REDetails.aspx?regexp_id=26</a>

	}

	/**
	 * Create Parser for Linux log
	 * 
	 * @throws AdeInternalException
	 * 
	 */
	private static void createParsers() throws AdeInternalException {

		AdeExtOperatingSystemType m_osType;
		AdeExtProperties linuxProperties;
		AdeExtPropertiesFactory adeExtPropertiesFactory = new AdeExtPropertiesFactory();
		m_osType = AdeExtOperatingSystemType.LINUX;
		linuxProperties = adeExtPropertiesFactory.getAdeExtProperties(m_osType);

		// create parser

		try {

			if (isSpark()){
				mSparkLineParsers = new SparklogLineParser[] {
					new SparklogParser(),
				};
				SparklogParser.setAdeExtProperties((LinuxAdeExtProperties) linuxProperties);
			}
			else{
				mLineParsers = new LinuxSyslogLineParser[] {
					new LinuxSyslog5424ParserBase(),
					new LinuxSyslog3164ParserWithMark(),
					new LinuxSyslog3164ParserWithCompAndPid(),
					new LinuxSyslog3164ParserFreeForm(), };
					LinuxSyslog3164ParserBase
					.setAdeExtProperties((LinuxAdeExtProperties) linuxProperties);
					((LinuxAdeExtProperties) linuxProperties).setYear(java.util.Calendar
					.getInstance().get(Calendar.YEAR));
			}
			
		} catch (AdeException e) {
			logger.error("Failure during parser construction", e);
			throw new AdeInternalException("Parser construction failure");
		}
	}

	/**
	 * parse line then invoke routines to mask data within the line
	 * 
	 * @param line
	 * @return
	 */
	private String generateMaskedLine(String currentLine) throws AdeException{
		boolean gotLine;
		String outline;

		// Spark logs
		if (isSpark()){
			for (SparklogLineParser lineParser : mSparkLineParsers) {
				gotLine = lineParser.parseLine(currentLine);
				if (gotLine) {
					String oldSystemName = lineParser.getSource();
					String oldText = lineParser.getMessageBody();
					return createNewLine(currentLine, oldSystemName, oldText);
				}
			}
			outline = currentLine;
			return outline;
		}

		// Linux Syslogs
		for (LinuxSyslogLineParser lineParser : mLineParsers) {
			gotLine = lineParser.parseLine(currentLine);
			if (gotLine) {
				String oldSystemName = lineParser.getSource();
				String oldText = lineParser.getMessageBody();
				return createNewLine(currentLine, oldSystemName, oldText);
			}
		}
		outline = currentLine;
		return outline;
	}

	/**
	 * Create new line by replacing system name, email, IP address
	 * 
	 * @param currentLine
	 * @param oldSystemName
	 * @param oldText
	 * @return
	 */
	private String createNewLine(String currentLine, String oldSystemName,
			String oldText) {

		String newText = currentLine;

		if (mMaskTCPIPAddress) {
			newText = maskIPAddress(oldText);
		}
		if (mMaskEmailAddress) {
			currentLine = maskEmail(currentLine, oldText, newText);
		}
		String updatedLine = maskCompanyName(currentLine);
		String finishedLine = maskSystemName(updatedLine, oldSystemName);

		return finishedLine;
	}
	/**
	 * Overlay company name with masked value
	 * @param currentLine
	 * @return
	 */
	private String maskCompanyName(String currentLine) {
		return currentLine.replace(mCompanyName, mCompanyNameNew);
	}
	/**
	 * Replace IP address with standard well known value for local host
	 * @param oldText
	 * @return
	 */
	private String maskIPAddress(String oldText) {
		String[] textTokens = oldText.split("\\s+");
		int tokenCount = textTokens.length;
		String newText = oldText;
		String delims = "[:|=|<|>|(|)|\\[|\\]]";
		for (int j = 0; j < tokenCount; j++) {
			String[] strippedToken = textTokens[j].split(delims);
			int strippedTokenCount = strippedToken.length;
			for (int k = 0; k < strippedTokenCount; k++) {
				if (isIpAddress(strippedToken[k])) {
					newText = newText.replace(strippedToken[k], localHost);
				}
			}
		}
		return newText;
	}

	/**
	 * change email address to myEmail@gmail.com
	 * 
	 * @param current
	 *            line
	 * @param text
	 *            to be processed
	 * @param text
	 *            after processing
	 */
	private String maskEmail(String currentLine, String oldText, String newText) {
		String[] textTokens = oldText.split("\\s+");
		int tokenCount = textTokens.length;
		String newLine;
		String localEmail = "myEmail@gmail.com";
		String delims = "[:|=|<|>|(|)|\\[|\\]]";
		for (int j = 0; j < tokenCount; j++) {
			String[] strippedToken = textTokens[j].split(delims);
			int strippedTokenCount = strippedToken.length;
			for (int k = 0; k < strippedTokenCount; k++) {
				if (isValidEmail(strippedToken[k])) {
					newText = newText.replace(strippedToken[k], localEmail);
				}
			}
		}
		newLine = currentLine.replace(oldText, newText);
		return newLine;
	}

	private String maskSystemName(String currentLine, String oldSystemName) {

		return currentLine.replace(oldSystemName, mSystemName);
	}

	/**
	 * Check if word contains an email address
	 * 
	 * @param email token to be analyzed
	 * @return
	 */
	public static boolean isValidEmail(String email) {
		Matcher matcher = validEmailPattern.matcher(email);
		return matcher.find();
	}

	/**
	 * Determine if the given string is a valid IPv4 or IPv6 address. This
	 * method uses pattern matching to see if the given string could be a valid
	 * IP address.
	 *
	 * @param ipAddress
	 *            A string that is to be examined to verify whether or not it
	 *            could be a valid IP address.
	 * @return <code>true</code> if the string is a value that is a valid IP
	 *         address, <code>false</code> otherwise.
	 */
	public static boolean isIpAddress(String ipAddress) {

		Matcher m1 = validIPV4Pattern.matcher(ipAddress);
		if (m1.matches()) {
			return true;
		}
		Matcher m2 = validIPV6Pattern.matcher(ipAddress);
		return m2.matches();
	}

	/**
	 * The entry point of AdeMaskLog
	 * 
	 * @param args
	 * @throws AdeException
	 */

	public static void main(String[] args) throws AdeException {

		AdeExtRequestType requestType = AdeExtRequestType.MASK_LOG;
		new AdeExtMessageHandler();
		System.err.println("Running Ade: " + requestType);
		final AdeMaskLog instance = new AdeMaskLog(requestType);
		(instance).AdeMaskLog(args);

	}

	protected final void AdeMaskLog(String[] args) {
		// Exception handlers expected to invoke System.exit
		try {
			run(args);

		} catch (AdeUsageException e) {
			getMessageHandler().handleUserException(e);
		} catch (AdeInternalException e) {
			getMessageHandler().handleAdeInternalException(e);
		} catch (AdeException e) {
			getMessageHandler().handleAdeException(e);
		} catch (Exception e) {
			getMessageHandler().handleUnexpectedException(e);
		} finally {
			// Won't get called in error cases due to use of System.ext.
			// This apppears to be a no-op anyway.
			quietCleanup();
		}

	}

}
