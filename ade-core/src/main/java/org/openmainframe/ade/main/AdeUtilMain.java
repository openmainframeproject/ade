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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.scoringApi.IMainScorer;

public class AdeUtilMain extends ControlProgram {

	public static void main(String[] args) throws AdeException {
		new AdeUtilMain().run(args);
	}

	public static final String FLOW_LAYOUT_XSD_File_Name = File.separator + "FlowLayout.xsd";

	@SuppressWarnings("static-access")
	@Override
	protected void parseArgs(String[] args) throws AdeException {
		Options options = new Options();

		Option helpOpt = new Option("h", "help", false,
				"Print help message and exit");
		options.addOption(helpOpt);

		Option versionOpt = OptionBuilder.withLongOpt("version").hasArg(false)
				.isRequired(false)
				.withDescription("Print current Ade version (JAR) and exit")
				.create('V');
		options.addOption(versionOpt);

		Option dbVersionOpt = OptionBuilder.withLongOpt("db-version")
				.withDescription("Print current Ade DB version and exit")
				.create();
		options.addOption(dbVersionOpt);

		Option outputFileOpt = OptionBuilder.withLongOpt("output").hasArg(true)
				.withArgName("FILE").isRequired(false)
				.withDescription("Output file name (where relevant)")
				.create('o');

		options.addOption(outputFileOpt);

		OptionGroup optGroup = new OptionGroup();
		optGroup.setRequired(false);

		Option DumpModelOpt = OptionBuilder
				.withLongOpt("model")
				.hasArg(true)
				.withArgName("MODEL FILE")
				.isRequired(false)
				.withDescription(
						"Extract a text version of a model (csv) and exit")
				.create('m');
		optGroup.addOption(DumpModelOpt);

		Option DumpModelDebugOpt = OptionBuilder
				.withLongOpt("debugPrint")
				.hasArg(true)
				.withArgName("MODEL FILE")
				.isRequired(false)
				.withDescription(
						"Extract a text version of a model debug information and exit")
				.create('d');
		optGroup.addOption(DumpModelDebugOpt);

		Option verifyFlowOpt = OptionBuilder
				.withLongOpt("verifyFlow")
				.hasArg(true)
				.withArgName("FLOW FILE")
				.isRequired(false)
				.withDescription(
						"Verify the flow file matches the XSD standard and exit")
				.create('f');
		optGroup.addOption(verifyFlowOpt);

		options.addOptionGroup(optGroup);

		CommandLineParser parser = new GnuParser();
		CommandLine line = null;

		try {
			// parse the command line arguments
			line = parser.parse(options, args);
		} catch (MissingOptionException exp) {
			System.out.println("Command line parsing failed.  Reason: "
					+ exp.getMessage());
			System.out.println();
			new HelpFormatter().printHelp(ControlDB.class.getName(), options);
			System.exit(0);
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			throw new AdeUsageException("Argument Parsing failed", exp);
		}

		if (line.hasOption(helpOpt.getLongOpt())) {
			new HelpFormatter().printHelp(getClass().getSimpleName(), options);
			closeAll();
			System.exit(0);
		}
		if (line.hasOption(versionOpt.getLongOpt())) {
			System.out.println("Current Ade version (JAR): "
					+ Ade.getAde().getVersion());
			closeAll();
			System.exit(0);
		}
		if (line.hasOption(dbVersionOpt.getLongOpt())) {
			System.out.println("Current Ade DB version: "
					+ Ade.getAde().getDbVersion());
			closeAll();
			System.exit(0);
		}

		File outputFile = null;
		if (line.hasOption(outputFileOpt.getLongOpt())) {
			outputFile = new File(line.getOptionValue(outputFileOpt
					.getLongOpt()));
		}

		if (line.hasOption(DumpModelDebugOpt.getLongOpt())) {
			File modelFile = new File(line.getOptionValue(DumpModelDebugOpt
					.getLongOpt()));
			dumpModelDebug(modelFile, outputFile);
		}
		if (line.hasOption(verifyFlowOpt.getLongOpt())) {
			String flowFilename = line.getOptionValue(verifyFlowOpt
					.getLongOpt());
			File flowFile = new File(flowFilename);
			try {
				validateGood(flowFile);
			} catch (Exception e) {
				throw new AdeUsageException("Failed when verifiying "
						+ flowFile.getName(), e);
			}
		}

	}

	protected void dumpModelDebug(File modelFile, File outputFile)
			throws AdeException, AdeUsageException, AdeInternalException {
		IMainScorer model = Ade.getAde().getDataStore().models()
				.readModelFromFile(modelFile);
		PrintStream outStream;
		if (outputFile == null) {
			outputFile = new File(Ade.getAde().getDirectoryManager()
					.getOutputHome(), "model_debug.txt");
		}
		try {
			System.out.println("Writing model debug information to "
					+ outputFile.getAbsolutePath());
			outStream = new PrintStream(outputFile);
		} catch (IOException e) {
			throw new AdeUsageException(
					"Failed writing debug information to file", e);
		}

		try {
			model.debugPrint(outStream);
		} catch (Exception e) {
			throw new AdeInternalException(
					"Failed generating debug information of model", e);
		}
		outStream.close();
	}

	@Override
	protected boolean doControlLogic() throws AdeException {
		return true;
	}

	protected void validateGood(File file) throws IOException, SAXException,
			AdeException {
		System.out.println("Starting");

		String fileName_Flowlayout_xsd = Ade.getAde().getConfigProperties()
				.getXsltDir()
				+ FLOW_LAYOUT_XSD_File_Name;
		Source schemaFile = new StreamSource(fileName_Flowlayout_xsd);
		SchemaFactory sf = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema mSchema = sf.newSchema(schemaFile);

		System.out.println("Validating " + file.getPath());
		Validator val = mSchema.newValidator();
		FileInputStream fis = new FileInputStream(file);
		StreamSource streamSource = new StreamSource(fis);

		try {
			val.validate(streamSource);
		} catch (SAXParseException e) {
			System.out.println(e);
			throw e;
		}
		System.out.println("SUCCESS!");
	}

}
