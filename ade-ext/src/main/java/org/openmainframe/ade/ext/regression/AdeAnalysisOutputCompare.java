/*
 
    Copyright IBM Corp. 2016
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
package org.openmainframe.ade.ext.regression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/*
 * Room for improvement:
 * 1 - write directly to log file instead of piping via shell
 * 2 - pass in logfile as parm or designate one by default
 *     if file passed in then use it; else check env var; else just use default /tmp file
 * 3 - ignored_nodes items defined in properties file
 */

public class AdeAnalysisOutputCompare {
    private static final Logger logger = LoggerFactory.getLogger(AdeAnalysisOutputCompare.class);
    private static final String DEFAULT_PROPFILE_PATH = "conf/setup.props";
    private static final String ANALYSIS_OUTPUT_PATH_PROPNAME = "ade.analysisOutputPath";
    private static final String ANALYSIS_RESULTS_XML_FILE_EXTENSION = "xml";
    private static final String[] ANALYSIS_RESULTS_TO_SKIP = new String[] {"_debug.xml"};
    private String baselinePath;
    private String analysisOutputPath;
    
    /**
     * Entry into the compare program.
     * 
     * @param args - list of arguments for program. Refer to getCmdLineOptions() to see
     *               currently supported options. 
     */
    public static void main(String[] args) {
        final AdeAnalysisOutputCompare xmltester = new AdeAnalysisOutputCompare();
        int rc = xmltester.run(args);
        
        System.out.println("-------------------------------------");
        System.out.println("AdeAnalysisOutputCompare RC="+rc);
        System.out.println("-------------------------------------");
        System.exit(rc);
    }
    
    /**
     * 
     * @param args
     * @throws Exception 
     */
    private int run(String[] args) {
        int rc = 0;
        
        try {
            parseArgs(args);
            retrieveOutputPath();
            
            System.out.println("-------------------------------------------------");
            System.out.println("Baseline Path: "+getBaselinePath());
            System.out.println("Analysis Output Path: "+getAnalysisOutputPath());
            System.out.println("-------------------------------------------------");
        } catch (ParseException | IOException e) {
            logger.error("Error encountered during setup.", e);
            return 101;
        } catch (Exception e) {
            logger.error("An unexpected error was encountered during setup.", e);
            return 102;
        }
        
        final List<File> baselineOutputFiles = locateBaselineOutputFiles(getBaselinePath(),
                                                                   ANALYSIS_RESULTS_XML_FILE_EXTENSION);
        
        if (baselineOutputFiles.isEmpty()) {
            // something is wrong so no reason to continue
            System.err.println("Unable to find any analysis output in "+getBaselinePath());
            return 1;
        }
        
        System.out.println("Located "+baselineOutputFiles.size()+" output files in "+getBaselinePath());

        for (File file : baselineOutputFiles) {
            final int comp_rc = doAnalysisResultsCompare(file);
            if (comp_rc>rc) {
                rc = comp_rc;
            }
        }
        
        return rc;
    }

    /**
     * Build and return Options object.
     * 
     * @return Options command line options
     */
    private static Options getCmdLineOptions() {
        final Options options = new Options();
        options.addOption("b",true,"path to baseline output");
        return options;
    }

    /**
     * Parses the arguments passed into the program.
     * 
     * @param args   an array of String that contains all values passed into 
     *               the program
     * @throws ParseException if there is an error parsing arguments
     */
    private void parseArgs(String[] args) throws ParseException {
        final Options options = getCmdLineOptions();
        final CommandLineParser parser = new BasicParser();
        CommandLine cmd;

        /*
         * Parse the args according to the options definition. Will throw
         * MissingOptionException when a required option is not specified or
         * the more generic ParseException if there is any other error parsing
         * the arguments.
         */
        cmd = parser.parse(options, args); 
        
        if(cmd.hasOption("b")) {
            setBaselinePath(cmd.getOptionValue("b"));
        }
    }

    /**
     * Use setup file (setup.props) to locate the output path of the analysis 
     * results (default to output/continuous.)
     * 
     * @throws IOException in the form of FileNotFoundException or general IOException
     */
    private void retrieveOutputPath() throws IOException, Exception {
        String outputPath = null;
        
        /*
         * Retrieve the system property ade.setUpFilePath and if that is
         * not defined use the default and make best effort to proceed.
         */
        String pathToSetupFile = System.getProperty("ade.setUpFilePath");
        if (pathToSetupFile == null) {
            pathToSetupFile = DEFAULT_PROPFILE_PATH;
        }

        Properties props = new Properties();
        
        /*
         * Open properties file, load properties and close file. Can result in 
         * FileNotFoundException when properties file does not exist or IOException
         * when problem reading from input stream.
         */
        final FileInputStream in = new FileInputStream(pathToSetupFile);
        props.load(in);
        in.close();
        
        if (props.get("ade.analysisOutputPath") != null) {
            outputPath = props.get("ade.analysisOutputPath").toString();
        }
        
        // Verify that outputPath points to a directory otherwise throw exception.
        if (outputPath != null) {
            final File dir = new File(outputPath);
            if (dir.exists() && dir.isDirectory()) {
                setAnalysisOutputPath(outputPath);
            } else {
                // still thinking through best way to handle these errors
                throw new Exception("Unable to find "+outputPath+" or it is not a directory");
            }
        } else {
            throw new Exception("Unable to determine "+ANALYSIS_OUTPUT_PATH_PROPNAME+" in "+pathToSetupFile);
        }
    }
    
    /**
     * For baseline file (control) that is passed in find the corresponding analysis
     * output file (test) and initiate a compare.
     * 
     * @param baselineFile File representing a baseline output file
     */
    private int doAnalysisResultsCompare(File baselineFile) {
        int rc;
        
        // determine relative path of baseline file
        final String baselineDirPath = getBaselinePath();
        final String baselineFileRelPath = findRelativePath(baselineFile.getPath(),baselineDirPath);

        // using baseline file relative path determine analysis output file path
        String tmpAnalysisOutputDir = getAnalysisOutputPath();
        if (!tmpAnalysisOutputDir.endsWith(File.separator)) {
            tmpAnalysisOutputDir = tmpAnalysisOutputDir + File.separator;
        }
        
        /*
         * Construct path to analysis output file and create File object. If file does not
         * exist return non-zero RC otherwise call method to do appropriate comparison.
         */
        final File analysisOutputFile = new File(tmpAnalysisOutputDir + baselineFileRelPath);
        if (!analysisOutputFile.exists()) {
            System.err.println("  "+baselineFileRelPath+" --> "+analysisOutputFile.getPath()+" does not exist");
            rc = 98;
        } else {
            rc = compareXmlFiles(baselineFile,analysisOutputFile); 
        }
        return rc;
    }
    
    /**
     * Method to compare two xml files (in UTF-8 format) using XmlUnit library.  
     * 
     * @param controlFile File object representing the control (baseline)
     * @param testFile    File object representing the test (analysis output)
     * @return rc         Return code of comparison
     */
    private int compareXmlFiles(File controlFile,File testFile) {
        int rc=99;
        
        System.out.println("Comparing "+controlFile.getPath()+" to "+testFile.getPath());
        DifferenceListener listener = null;
        
        if (controlFile.getName().startsWith("index")) {
            listener = new AdeXmlDifferenceListener(IgnoreXpaths.INDEX);
        } else if (controlFile.getName().startsWith("interval") && testFile.getName().endsWith("_debug.xml")) {
            listener = new AdeXmlDifferenceListener(IgnoreXpaths.INTERVAL_DEBUG);
        } else if (controlFile.getName().startsWith("interval")) {
            listener = new AdeXmlDifferenceListener(IgnoreXpaths.INTERVAL);
        }
            
        try {
            final Diff diff = new Diff(new InputStreamReader(new FileInputStream(controlFile), StandardCharsets.UTF_8),
                    new InputStreamReader(new FileInputStream(testFile), StandardCharsets.UTF_8));
            diff.overrideDifferenceListener(listener);
            if (diff.similar()) {
                System.out.println("XML files are similar");
                rc = 0;
            } else {
                System.out.println("XML files are not similar");
            }
        } catch (FileNotFoundException e) {
            // should never get here since file existence is check prior to being here
            logger.error("Error encountered with the file.", e);
        } catch (SAXException | IOException e) {
            logger.error("Error encountered with the Diff.", e);
        }
        
        return rc;
    }

    /**
     * Determine the path to the file relative to the given directory path.
     * 
     * @param filePath  Path to the file
     * @param dirPath   Fully qualified path of directory (/..../baseline/)
     * @return
     */
    private static String findRelativePath(String filePath, String dirPath) {
        String relPath;
        
        if (!dirPath.endsWith(File.separator)) {
            dirPath = dirPath+File.separator;
        }
        
        if (filePath.startsWith(dirPath)) {
            relPath = filePath.substring(dirPath.length());
        } else {
            relPath = filePath;
        }
        
        return relPath;
    }

    /**
     * Recursive method to locate all files in give directory with the specified file
     * extension.  
     * 
     * @param dirName       path to directory where to search from
     * @param fileExtension file extension of files to locate
     * @return ArrayList<File>  list of File objects
     */
    private static List<File> locateBaselineOutputFiles(String dirName,String fileExtension) {
        List<File> outputFiles = new ArrayList<File>();
        
        final File dir = new File(dirName);
        final File[] dirContent = dir.listFiles();
        for (File file : dirContent) {
            if (file.isDirectory()) {
                // recursive call to dive deeper into directory
                outputFiles.addAll(locateBaselineOutputFiles(file.getPath(),fileExtension));
            } else if (file.getPath().endsWith("."+fileExtension)) {
                boolean dontadd = false; // flag indicating whether to add this file to list
                for (String ext : ANALYSIS_RESULTS_TO_SKIP) {
                    if (file.getPath().endsWith(ext)) {
                        dontadd = true;
                    }
                }
                if (!dontadd) {
                    System.out.println("Adding to list "+file.getPath());
                    outputFiles.add(file);
                }
            }
        }
        
        return outputFiles;
    }

    public String getBaselinePath() {
        return baselinePath;
    }

    public void setBaselinePath(String baselinePath) {
        this.baselinePath = baselinePath;
    }

    public String getAnalysisOutputPath() {
        return analysisOutputPath;
    }

    public void setAnalysisOutputPath(String analysisOutputPath) {
        this.analysisOutputPath = analysisOutputPath;
    }

    private class AdeXmlDifferenceListener implements DifferenceListener {
        /*
         * IGNORED_NODES is a list of XPaths that point to XML nodes that should be ignored during
         * comparison. The list can include XPath for nodes that will be different every analysis 
         * run, such as dates, or can be things that are known differences that should not be
         * flagged.
         */
        private final List<String> IGNORED_NODES;
        
        /**
         * Constructor accepting an IgnoreXpaths type from the enum.
         * @param ignore_xpaths - IgnoreXpaths object
         */
        public AdeXmlDifferenceListener(IgnoreXpaths ignore_xpaths) {
            this.IGNORED_NODES = ignore_xpaths.getIgnoreXPaths();
        }

        /**
         * Constructor accepting a List<String> of xpaths for nodes to ignore.
         * @param ignore_xpaths - List<String> of xpaths for nodes to ignore.
         */
        @SuppressWarnings("unused")
        public AdeXmlDifferenceListener(List<String> ignore_xpaths) {
            this.IGNORED_NODES = ignore_xpaths;
        }

        /**
         * If a difference is found when comparing xml it will trigger a call to 
         * differenceFound. Determine if the difference can be ignored (part of the
         * IGNORED_NODES) or if it's a 'true' difference.
         */
        @Override
        public int differenceFound(Difference arg0) {
            String elemXpathVal = arg0.getControlNodeDetail().getXpathLocation();
            if (IGNORED_NODES.contains(elemXpathVal)) {
                return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;                
            }
            System.out.println(" ** Difference Found:");
            System.out.println("\t"+arg0.toString()+"\n");
            return RETURN_ACCEPT_DIFFERENCE;
        }

        /**
         * 
         */
        @Override
        public void skippedComparison(Node arg0, Node arg1) {
            System.out.println("*** Skipped comparison for |"+arg0.toString()+"| and |"+arg1.toString()+"|");       
        }
    }

    private enum IgnoreXpaths {
        INDEX(new ArrayList<String> (Arrays.asList(
                "/systems[1]/model_info[1]/@analysis_group",
                "/systems[1]/model_info[1]/@model_creation_date")
                )), 
        INTERVAL(new ArrayList<String> (Arrays.asList(
                "/interval[1]/model_internal_id[1]/text()[1]",
                "/interval[1]/model_info[1]/@model_creation_date",
                "/interval[1]/model_info[1]/@analysis_group")
                )), 
        INTERVAL_DEBUG(new ArrayList<String> (Arrays.asList(
                "/AnalyzedInterval[1]/ModelId[1]/text()[1]")
                ));
        
        private List<String> ignore_xpaths;
        
        private IgnoreXpaths(List<String> xpaths) {
            this.ignore_xpaths = xpaths;
        }
        
        public List<String> getIgnoreXPaths() {
            return ignore_xpaths;
        }
    }
}
