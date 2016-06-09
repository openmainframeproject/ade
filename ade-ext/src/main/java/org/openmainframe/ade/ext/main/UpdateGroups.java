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
package org.openmainframe.ade.ext.main;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openmainframe.ade.data.GroupType;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.data.Group;
import org.openmainframe.ade.ext.data.GroupsQueryImpl;
import org.openmainframe.ade.ext.data.Rule;
import org.openmainframe.ade.ext.data.RulesQueryImpl;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;
import org.openmainframe.ade.ext.os.parser.JSONGroupParser;
import org.openmainframe.ade.ext.service.AdeExtMessageHandler;


/**
 * Main class for updating groups and the rules associated with them.
 */
public class UpdateGroups extends ExtControlProgram{
    
    private static final String OPTION_INPUT_JSON = "json";
    private static final String OPTION_HELP = "help";
   
    private File inputJSONFile;
    
    /**
     * Constructor to pass in the requestType to the super class.
     * @param requestType The request type of this class.
     */
    protected UpdateGroups(AdeExtRequestType requestType) {
        super(requestType);
    }
    
    /**
     * Parses the arguments. The valid arguments are:
     *  --help or -h : prints out help message for this main class.
     *  --json or -j : the JSON file.
     * @param args the arguments passed in as options.
     */
    @Override
    public void parseArgs(String [] args) throws AdeUsageException{
        Options options = new Options();
        buildOptions(options);        
        CommandLineParser parser = new GnuParser();
        CommandLine line = parseLine(parser,options,args);
        
        if (line.hasOption('h')) {
            new HelpFormatter().printHelp(this.getClass().getSimpleName(), options);
            System.exit(0);
        }
        
        if (line.hasOption('j')){
            String jsonFile = line.getOptionValue("j");
            inputJSONFile = new File(jsonFile);
            validateFile(inputJSONFile);
        } else{
            new HelpFormatter().printHelp(this.getClass().getSimpleName(), options);
            throw new AdeUsageException("Must specify a JSON file path using the -j option.");
        }
    }
    
    /**
     * Method for building the allowed options.
     * @param options Options object for adding the options created with OptionBuilder.
     */
    private void buildOptions(Options options){        
        OptionBuilder.withArgName(OPTION_HELP);
        OptionBuilder.withLongOpt(OPTION_HELP);
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Print help message and exit.");
        options.addOption(OptionBuilder.create('h'));
        
        OptionBuilder.withArgName(OPTION_INPUT_JSON);
        OptionBuilder.withLongOpt(OPTION_INPUT_JSON);
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Specify the JSON input file.");
        options.addOption(OptionBuilder.create("j"));
    }
    
    /**
     * Used for parsing and validating the options.
     * @param parser Parser for parsing out the options.
     * @param options set of acceptable options.
     * @param args the arguments passed in by the user as options.
     * @return CommandLine object to get the option values.
     * @throws AdeUsageException
     */
    private CommandLine parseLine(CommandLineParser parser, Options options, String[] args) throws AdeUsageException {
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
     * Error checking to make sure the JSON file passed in is valid.
     * @param jsonFile the JSON file.
     * @throws AdeUsageException
     */
    private void validateFile(File jsonFile) throws AdeUsageException{
        if (!jsonFile.isFile()) {
            throw new AdeUsageException("Input is not a file: " + jsonFile.getPath());
        } else if (!jsonFile.canRead()) {
            throw new AdeUsageException("Input file is not readable: " + jsonFile.getPath());
        }
    }
   
    /**
     * The main control logic. Parses the JSON file and updates the database with the contents 
     * parsed from the JSON.
     */
    @Override
    protected boolean doControlLogic() throws AdeException {
        JSONGroupParser jsonParser = parseJSON();
        HashMap<Integer, List<Group>> parsedGroupsByType = jsonParser.getParsedGroupsByType();
        List<Rule> parsedRules = jsonParser.getParsedRules();         
        updateDB(parsedGroupsByType, parsedRules);
        return true;
    }
   
    /**
     * Method for parsing the JSON file. Calls the parseJSON method in the JSONGroupParser class.
     * @return The parser that holds the contents of the JSON file.
     * @throws AdeInternalException
     * @throws AdeUsageException
     */
    private JSONGroupParser parseJSON() throws AdeInternalException, AdeUsageException{
        JSONGroupParser jsonParser = new JSONGroupParser();
        jsonParser.parseJSON(inputJSONFile);
        return jsonParser;
    }
   

    /**
     * Method for updating the database. Makes calls to methods in RulesQueryImpl for modifying the
     * RULES table and the GROUPS table.
     * @param parsedGroupsByType the groups extracted from the JSON file.
     * @param parsedRules the rules extracted from the JSON file.
     * @throws AdeException
     */
    private void updateDB(Map<Integer, List<Group>> parsedGroupsByType, List<Rule> parsedRules) throws AdeException {
        RulesQueryImpl.modifyRules(parsedRules);
        for (GroupType groupType : GroupType.values()){
            int groupTypeVal = groupType.getValue();
            List<Group> groups= parsedGroupsByType.get(groupTypeVal);
            GroupsQueryImpl.modifyGroups(groups);
        }
    }

   
    /**
     * Main class. Creates the UpdatesGroups class and passes in the arguments for processing. 
     * @param args the string arguments passed in from the user.
     * @throws AdeException
     */
    public static void main(String [] args) throws AdeException {
        AdeExtRequestType requestType = AdeExtRequestType.UPDATE_GROUPS;
        UpdateGroups groups = new UpdateGroups(requestType);
        AdeExtMessageHandler messageHandler = new AdeExtMessageHandler();
        try {
            groups.run(args);
        } catch (AdeUsageException e) {
            messageHandler.handleUserException(e);
        } catch (AdeInternalException e) {
            messageHandler.handleAdeInternalException(e);
        } catch (AdeException e) {
            messageHandler.handleAdeException(e);
        }
    }

}
