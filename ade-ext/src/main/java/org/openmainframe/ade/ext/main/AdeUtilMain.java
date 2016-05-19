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
import org.openmainframe.ade.dataStore.IDataStore;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;
import org.openmainframe.ade.ext.service.AdeExtMessageHandler;
import org.openmainframe.ade.main.ControlDB;

/** Main for a utility that prints out model information */
public class AdeUtilMain extends org.openmainframe.ade.main.AdeUtilMain {


    /**
     * Variable pointing to the DataStore
     */
    @SuppressWarnings("unused")
	private IDataStore m_dataStore;
    
    private File m_outputFile;
    private File m_inputFile;
    private String m_inputFilename;
    private String m_cmd;

    /**
     * The entry point of AdeUtilMain
     * 
     * @param args
     * @throws AdeException
     */
    public static void main(String[] args) throws AdeException {
        final AdeExtRequestType requestType = AdeExtRequestType.UTILITIES;
        System.err.println("Running Ade: " + requestType);

		final AdeExtMessageHandler messageHandler = new AdeExtMessageHandler();

        final AdeUtilMain adeutilmain = new AdeUtilMain();
        try {
            adeutilmain.run(args);
        } catch (AdeUsageException e) {
            messageHandler.handleUserException(e);
        } catch (AdeInternalException e) {
            messageHandler.handleAdeInternalException(e);
        } catch (AdeException e) {
            messageHandler.handleAdeException(e);
        } catch (Throwable e) {
            messageHandler.handleUnexpectedException(e);
        } finally {
            adeutilmain.quietCleanup();
        }

    }

    /**
     * Parse the input arguments
     */
	@SuppressWarnings("static-access")
	@Override
    protected void parseArgs(String[] args) throws AdeUsageException {
		Options options = new Options();		
		
		Option helpOpt = new Option("h", "help", false, "Print help message and exit");
		options.addOption(helpOpt);

		Option versionOpt = OptionBuilder
		.withLongOpt("version")
		.hasArg(false)
		.isRequired(false)
		.withDescription("Print current Ade version (JAR) and exit")
		.create('v');
		options.addOption(versionOpt);
		
		Option dbVersionOpt = OptionBuilder
		.withLongOpt("db-version")
		.withDescription("Print current Ade DB version and exit")
		.create('b');
		options.addOption(dbVersionOpt);

		Option outputFileOpt = OptionBuilder
		.withLongOpt("output")
		.hasArg(true)
		.withArgName("FILE")
		.isRequired(false)
		.withDescription("Output file name (where relevant)")
		.create('o');
		options.addOption(outputFileOpt);

		Option inputFileOpt = OptionBuilder
		.withLongOpt("input")
		.hasArg(true)
		.withArgName("MODEL FILE / FLOW FILE")
		.isRequired(false)
		.withDescription("Input file name if needed (MODEL FILE or FLOW FILE)")
		.create('i');
	//	options.addOption(inputFileOpt);
		
		OptionGroup optGroup = new OptionGroup();
		optGroup.setRequired(false);

		Option DumpModelOpt = OptionBuilder
		.withLongOpt("model")
		.hasArg(true)
		.withArgName("MODEL FILE")
		.isRequired(false)
		.withDescription("Extract a text version of a model (csv) and exit")
		.create('m');
		optGroup.addOption(DumpModelOpt);

		Option DumpModelDebugOpt = OptionBuilder
		.withLongOpt("debugPrint")
		.hasArg(true)
		.withArgName("MODEL FILE")
		.isRequired(false)
		.withDescription("Extract a text version of a model debug information and exit")
		.create('d');
		optGroup.addOption(DumpModelDebugOpt);

		Option verifyFlowOpt = OptionBuilder
		.withLongOpt("verifyFlow")
		.hasArg(true)
		.withArgName("FLOW FILE")
		.isRequired(false)
		.withDescription("Verify the flow file matches the XSD standard and exit")
		.create('f');
		optGroup.addOption(verifyFlowOpt);
			
		options.addOptionGroup(optGroup);		
		
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;

		try {
			// parse the command line arguments
			line = parser.parse(options, args);
		} catch (MissingOptionException exp) {
			System.out.println( "Command line parsing failed.  Reason: " + exp.getMessage() );
			System.out.println();
			new HelpFormatter().printHelp(ControlDB.class.getName(), options );
			System.exit(0);
		} catch( ParseException exp ) {
			// oops, something went wrong
			System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
			throw new AdeUsageException("Argument Parsing failed", exp);
		}
		
		if (line.hasOption('h')) {
            new HelpFormatter().printHelp(this.getClass().getSimpleName(), options);
            System.exit(0);
        }
		if (line.hasOption(helpOpt.getLongOpt())) {
			new HelpFormatter().printHelp(getClass().getSimpleName(), options );
		}
		
		if (line.hasOption(outputFileOpt.getLongOpt())) {
			m_outputFile = new File(line.getOptionValue(outputFileOpt.getLongOpt()));
		}
		
		m_inputFile = null;

	    m_cmd = null;
	    
		if (line.hasOption('v')) {
          m_cmd = "version";
        }
		
		if (line.hasOption('b')) {
	       m_cmd = "db-version";
	        }
	    
		if (line.hasOption('m')) {
		   m_inputFile = new File(line.getOptionValue(DumpModelOpt.getLongOpt()));
           m_cmd = "model";
        }
		
		if (line.hasOption('d')) {
			m_inputFile = new File(line.getOptionValue(DumpModelDebugOpt.getLongOpt()));
	        m_cmd = "debugPrint";
	        }
	    
		if (line.hasOption('f')) {
			m_inputFilename = line.getOptionValue(verifyFlowOpt.getLongOpt());
            m_cmd = "verifyFlow";
        }
		
	}
    

    @Override
    protected boolean doControlLogic() throws AdeException {

        // Convert first argument to an enum
        final AdeUtilMainOperator operator = AdeUtilMainOperator.getOperatorType(m_cmd);
        
        m_dataStore = a_ade.getDataStore();

        // Handle specific operations
        switch (operator) {
            case Version:
    			System.out.println("Current Ade version (JAR): "+Ade.getAde().getVersion());
                return(true);
            case VersionDB:
            	System.out.println("Current Ade DB version: "+Ade.getAde().getDbVersion());
                return(true);
            case CSV:
                return doCSV();
            case DebugModel:
				return doDebugModel();
            case VerifyFlow:
				return VerifyFlow(m_inputFilename);
            case HELP:
            	usageError("");
            	return(true) ;
            default:
                throw new AdeInternalException("Cannot handle " + operator);
            }

       
    }

    private boolean VerifyFlow(String flowFilename) throws AdeUsageException {

			File flowFile=new File(flowFilename);			
			try {
				validateGood(flowFile);
			} catch (Exception e) {
				throw new AdeUsageException("Failed when verifiying "+flowFile.getName(),e);
			}		

		return true;
	}

	private boolean doCSV() {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean doDebugModel() throws AdeException, AdeInternalException, AdeException {
		dumpModelDebug(m_inputFile, m_outputFile);
		return false;
	}

	/**
     * Enum with all the supported (including the previously supported)
     * operators.
     */
    public enum AdeUtilMainOperator {
        //Print version of ADE
        Version("version"),
        //Print version of ADE database tables
        VersionDB("db-version"),
        //Print model (csv)
        CSV("model"), 
        //Print model text
        DebugModel("debugPrint"), 
		//check flowlayout 
        VerifyFlow("verifyFlow"), 
        // print out list of options
        HELP("help"),
        // Unknown operator
        Unknown("unknown");

        private String m_operatorName;

        AdeUtilMainOperator(String operatorName) {
            m_operatorName = operatorName;
        }

        public String getOperatorName() {
            return m_operatorName;
        }

        /** Find enum value matching given string */
        public static AdeUtilMainOperator getOperatorType(String operatorName) throws AdeException {
            for (AdeUtilMainOperator val : values()) {
                if (val.getOperatorName().equalsIgnoreCase(operatorName)) {
                    return val;
                }
            }
            usageError("Request not supported");
            return null;
        }
    }
    /**
     * Output error related to the invocation of VerifyLinuxTraining
     *
     * @param errorMsg
     * @throws AdeUsageException
     */
    private static void usageError(String errorMsg) throws AdeUsageException {

        System.out.flush();
        System.err.println("Usage:");
        System.err.println("\tAdeUtilMain ");
        System.err.println();
        System.err.println("version returns the version of the ADE code");
        System.err.println();
        System.err.println("db-version returns the version of the database schema in the database pointeed to by ADE in flowlayout.xml");
        System.err.println();
        System.err.println("debugPrint prints a text summary of the model stored in the file specified");
        System.err.println();
        System.err.println("verfiyFlow checks the flowlayout.xml file specified against the xsd file");
        System.err.flush();
        
        if (errorMsg != "")
        throw new AdeUsageException(errorMsg);

    }
}
