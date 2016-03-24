/*
 
    Copyright IBM Corp. 2010, 2016
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

package org.openmainframe.ade.ext.main.helper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.main.ExtControlProgram;
import org.openmainframe.ade.ext.os.AdeExtProperties;
import org.openmainframe.ade.ext.os.AdeExtPropertiesFactory;
import org.openmainframe.ade.ext.os.parser.RuntimeModelDataManager;
import org.openmainframe.ade.ext.utils.ArgumentConstants;
import org.openmainframe.ade.ext.utils.ArgumentConstants.INPUT_SOURCE;
import org.openmainframe.ade.flow.IMessageInstanceTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for parsing options and sending log messages to stream handler 
 * when running an UPLOAD or ANALYZE. 
 */
abstract public class UploadOrAnalyze extends ExtControlProgram {
    /**
     * A reference to this object, which should be singleton
     */
    private static UploadOrAnalyze s_uploadOrAnalyzeObject;

    public static AdeExtOperatingSystemType getAdeOSType() {
        return s_uploadOrAnalyzeObject.getAdeExtOperatingSystemType();
    }

    public static boolean isInputSourceSTDIN() {
        if (s_uploadOrAnalyzeObject == null) {
            return false;
        }

        return s_uploadOrAnalyzeObject.getInputSource() == INPUT_SOURCE.STDIN;
    }

    public static AdeExtRequestType getAdeRequestType() {
        if (s_uploadOrAnalyzeObject == null) {
            return null;
        }

        return s_uploadOrAnalyzeObject.getRequestType();
    }

    /**
     * The input options
     */
    private Options options = new Options();

    /**
     * The Logger
     */
    protected Logger logger;

    /**
     * Input file
     */
    private File m_inputFile;

    /**
     * Input directory
     */
    private File m_inputDir;

    /**
     * Sources for which analysis will be performed. 
     */
    private Collection<ISource> m_sources;
    /**
     * The format (file, directory or stdin) of logMessages.
     */
    private ArgumentConstants.INPUT_SOURCE m_inputSource;

    /**
     * The operating System type
     */
    private AdeExtOperatingSystemType m_osType;

    /**
     * Encapsulated properties that will be passed between different class
     * for processing. 
     */
    protected AdeExtProperties m_adeExtProperties;

    /**
     * Constructor
     */
    protected UploadOrAnalyze(AdeExtRequestType requestType) {
        super(requestType);
        logger = LoggerFactory.getLogger(this.getClass().getName());
        s_uploadOrAnalyzeObject = this;
    }

    /**
     * Parse the Arguments.  This method accept Options defined in a subclass, and
     * parse all the arguement based on the options defined in this method and subclass.
     * 
     * @param subClassOptions
     * @param args
     * @return
     * @throws AdeException
     */
    protected CommandLine parseArgs(Options subClassOptions, String[] args) throws AdeException {
        CommandLineParser parser = new GnuParser();
        OptionsFactory optionsFactory = new OptionsFactory();
        AdeExtPropertiesFactory adeExtPropertiesFactory = new AdeExtPropertiesFactory();
        AdeExtOptions adeExtOptions;
        AdeExtProperties adeExtProperties;

        options = AdeExtOptions.buildOptions(subClassOptions);

        for (AdeExtOperatingSystemType ostype : AdeExtOperatingSystemType.values()) {
            adeExtOptions = optionsFactory.getOptions(ostype);
            if (adeExtOptions != null) {
                adeExtOptions.addPlatformSpecificOptions(options);
            }
        }

        CommandLine line = parseLine(parser, args);

        /**
         * Process and validate the input parameters
         */
        if (line.hasOption('h')) {
            new HelpFormatter().printHelp(this.getClass().getSimpleName(), options);
            closeAll();
            System.exit(0);
        }

        /**
         * Process the file and directory options. Note that they are mutually
         * exclusive.
         */
        if (line.hasOption(AdeExtOptions.OPTION_INPUT_FILE)) {
            if (line.hasOption(AdeExtOptions.OPTION_INPUT_DIR)) {
                usageError("Input parameters " + AdeExtOptions.OPTION_INPUT_DIR + " (-d) and " + AdeExtOptions.OPTION_INPUT_FILE + " (-f) cannot be specified together.");
            }

            /* STDIN */
            if (line.getOptionValue(AdeExtOptions.OPTION_INPUT_FILE).equalsIgnoreCase(ArgumentConstants.STDIN)
                    || line.getOptionValue(AdeExtOptions.OPTION_INPUT_FILE).equals("-")) {
                m_inputSource = ArgumentConstants.INPUT_SOURCE.STDIN;
            } else {
                m_inputSource = ArgumentConstants.INPUT_SOURCE.LOGFILE;

                /* Verify if the file exist. */
                m_inputFile = new File(line.getOptionValue(AdeExtOptions.OPTION_INPUT_FILE));
                if (!m_inputFile.isFile()) {
                    usageError("Input is not a file: " + m_inputFile.getPath());
                } else if (!m_inputFile.canRead()) {
                    usageError("Input file is not readable: " + m_inputFile.getPath());
                }

            }
        } else if (line.hasOption(AdeExtOptions.OPTION_INPUT_DIR)) {
            if (line.hasOption(AdeExtOptions.OPTION_INPUT_FILE)) {
                usageError("Input parameters " + AdeExtOptions.OPTION_INPUT_DIR + " (-d) and " + AdeExtOptions.OPTION_INPUT_FILE + " (-f) cannot be specified together.");
            }

            m_inputSource = ArgumentConstants.INPUT_SOURCE.LOGDIR;
            m_inputDir = new File(line.getOptionValue(AdeExtOptions.OPTION_INPUT_DIR));

            if (!m_inputDir.isDirectory()) {
                usageError("Invalid directory: " + m_inputDir.getAbsolutePath());
            }
        } else {
            usageError("At least a directory or a file must be specified.");
        }

        /* Determine the OS Type, where the log messages come from.
         * Set the default to Linux
         */
        m_osType = AdeExtOperatingSystemType.LINUX;
        if (line.hasOption(AdeExtOptions.OPTION_OS_TYPE)) {
            String osType = line.getOptionValue(AdeExtOptions.OPTION_OS_TYPE);
            m_osType = AdeExtOperatingSystemType.getAdeExtOperatingSystemType(osType);

            if (m_osType == null) {
                usageError("Invalid OS Type defined: " + osType + ". Supported OS Types are: " +
                        AdeExtOperatingSystemType.getDefinedAdeExtOperatingSystemTypes());
            }
        }

        /* Read the parameters for the requested OS.  If parameters are defined
         * for non-requested OS, they will be ignored.
         */

        adeExtProperties = adeExtPropertiesFactory.getAdeExtProperties(m_osType);
        if (adeExtProperties == null) {
            throw new AdeUsageException("adeExtProperties do not exist for operating system type " + m_osType);
        }
        adeExtOptions = optionsFactory.getOptions(m_osType);
        boolean readOptionsSuccessful = adeExtOptions.readOptions(line, adeExtProperties);
        if (!readOptionsSuccessful) {
            usageError("Unknown required parameter");
        }
        m_adeExtProperties = adeExtProperties;
        setAdeExtProperties();

        String sourcesGiven = "all";
        if (line.hasOption(AdeExtOptions.OPTION_SOURCES)) {
            sourcesGiven = line.getOptionValue(AdeExtOptions.OPTION_SOURCES);
            m_adeExtProperties.setSourceOptionProvided(true);
        }
        m_sources = ArgumentConstants.getSourcesFromArgument(sourcesGiven);
        m_adeExtProperties.setSources(m_sources);

        return line;
    }

    /**
     * Parse the input options
     * @throws AdeException 
     */
    private CommandLine parseLine(CommandLineParser parser, String[] args) throws AdeException {
        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        } catch (ParseException exp) {
            System.err.println("Parsing failed. Reason: " + exp.getMessage());
            throw new AdeUsageException("Argument Parsing failed", exp);
        }
        return line;
    }

    /**
     * Set the value in the AdeExtProperties object
     */
    protected void setAdeExtProperties() {
        m_adeExtProperties.setRequestType(requestType());
    }

    /**
     * Return the AdeExtProperties  
     * @return
     */
    protected AdeExtProperties getAdeExtProperties() {
        return m_adeExtProperties;
    }

    /**
     * Return the operating system type
     * @return
     */
    protected AdeExtOperatingSystemType getAdeExtOperatingSystemType() {
        return m_osType;
    }

    /**
     * Return the operating system type
     * @return
     */
    protected INPUT_SOURCE getInputSource() {
        return m_inputSource;
    }

    /**
     * Return the operating system type
     * @return
     */
    protected AdeExtRequestType getRequestType() {
        return m_requestType;
    }

    /**
     * Handle the logMessages, and send them to the AdeInputStreamHandlerExt. 
     */
    @Override
    protected boolean doControlLogic() throws AdeException {
        /* Instantiate the target that will receive messages */
        IMessageInstanceTarget miTarget = getMITarget();

        /* Create and initialize the handler */
        AdeInputStreamHandlerExt streamHandler;
        StreamHandlerExtFactory streamHandlerExtFactory = new StreamHandlerExtFactory();
        streamHandler = streamHandlerExtFactory.getStreamHandler(m_osType, m_adeExtProperties);
        if (streamHandler == null) {
            throw new AdeInternalException("Stream Handler does not exist for operating system type " + m_osType);
        }
        streamHandler.addTarget(miTarget);
        streamHandler.beginOfStream();
        streamHandler.sendDiscontinuitySeperator();

        /* Process the input log messages */
        if (m_inputSource == INPUT_SOURCE.LOGDIR) {
            for (File file : m_inputDir.listFiles(getFilenameFilter())) {
                streamHandler.incomingStreamFromFile(file);
            }
        } else if (m_inputSource == INPUT_SOURCE.LOGFILE) {
            streamHandler.incomingStreamFromFile(m_inputFile);
        } else {
            /* the only option left is STDIN */
            streamHandler.incomingStreamFromSTDIN();
        }

        streamHandler.endOfStream();
        String lastNewlySeenSourceId = m_adeExtProperties.getLastNewlySeenSourceId();
        
        /* Write the runtimeModelData to a file, only if we have identified a source */
        if( lastNewlySeenSourceId != null )
        {
            RuntimeModelDataManager runtimeModelDataManager = new RuntimeModelDataManager();
            runtimeModelDataManager.writeModelDataToFile(lastNewlySeenSourceId);
        }        
        return true;
    }

    /**
     * Output an usageError together with a Help Message
     * @param errorMsg
     * @throws AdeUsageException
     */
    protected void usageError(String errorMsg) throws AdeUsageException {
        printHelp();
        throw new AdeUsageException(errorMsg);
    }

    /**
     * Print a Syntax Help 
     */
    protected void printHelp() {
        System.out.flush();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(this.getClass().getName(), options);
    }

    /**
     * Return the filename file 
     * @return
     */
    private FilenameFilter getFilenameFilter() {
        FilenameFilter filter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".gz");
            }
        };

        return filter;
    }

    /**
     * The Subclass will provide a TargetStream that will use to process
     * the incoming messages.
     * @return
     * @throws AdeException
     */
    protected abstract IMessageInstanceTarget getMITarget() throws AdeException;
}
