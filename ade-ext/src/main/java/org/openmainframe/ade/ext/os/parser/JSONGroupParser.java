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
package org.openmainframe.ade.ext.os.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.openmainframe.ade.data.DataType;
import org.openmainframe.ade.data.GroupType;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.data.Group;
import org.openmainframe.ade.ext.data.Rule;

/**
 * Class for parsing the JSON file that contains the groups and associated rules. 
 */
public class JSONGroupParser {
    
    /**
     * The groups parsed from the JSON file.
     */
    private HashMap<Integer, List<Group>> parsedGroupsByType;
    /**
     * The rules parsed from the JSON file.
     */
    private List<Rule> parsedRules;
    
    /**
     * Constructor for initializing the rules and groups member variables.
     */
    public JSONGroupParser(){
        parsedGroupsByType = new HashMap<Integer, List<Group>>();
        parsedRules = new ArrayList<Rule>();
    }
    
    /**
     * Main logic for parsing the JSON file. First it parses the supported group types and then
     * it parses the rules. Validation of groups and rules is also being performed.
     * @param jsonFile The JSON file.
     * @throws AdeInternalException
     * @throws AdeUsageException
     */
    public void parseJSON(File jsonFile) throws AdeInternalException, AdeUsageException{
        InputStream jsonInputStream = null;
        try {
            jsonInputStream = new FileInputStream(jsonFile);
            JSONObject jsonData = new JSONObject(jsonInputStream);
            JSONObject groups = jsonData.getJSONObject("groups");
            for (GroupType group : GroupType.values()){
                List<Group> parsedGroups;
                JSONArray groupsArray = groups.getJSONArray(group.name().toLowerCase());
                parsedGroups = parseGroups(groupsArray, group.name());    
                parsedGroupsByType.put(group.getValue(),parsedGroups);
            }
            JSONArray rules = jsonData.getJSONArray("rules");
            parsedRules = parseRules(rules);
            validateGroupRules(parsedGroupsByType, parsedRules);
        } catch (IOException e) {
            throw new AdeInternalException("Cannot read file into a string", e);
        } catch (JSONException e) {
            throw new AdeInternalException("Cannot convert to JSONObject", e);
        } finally {
            if (jsonInputStream != null){
                try { jsonInputStream.close(); } catch (IOException e) {}
            }
        }
    }
    

    /**
     * Main logic for parsing the groups from the JSON file. Also does validation of the groups attributes.
     * @param groups JSONArray object that contains the groups parsed from the JSON file.
     * @param groupType the type of the groups in the JSONArray Object.
     * @return An ArrayList of Group Objects that contain the parsed information.
     * @throws JSONException
     * @throws AdeUsageException
     */
    private List<Group> parseGroups(JSONArray groups, String groupType) throws JSONException, AdeUsageException{
        if (groups.length() == 0) throw new AdeUsageException("No groups specified for group of type " + groupType);
        List<Group> currentGroups = new ArrayList<Group>();
        for (int i = 0; i < groups.length(); i++){
            JSONObject group = groups.getJSONObject(i);
            String name = group.getString("name");
            String dataType = group.getString("dataType");
            short evalOrder = group.getShort("evaluationOrder");
            String ruleName = group.getString("ruleName");
         
            if (!verifyStringParam(name, 200, "[a-zA-Z0-9_ ]*") || name.equalsIgnoreCase("unassigned")
                    || !validateDataType(dataType) || evalOrder < 1 || !verifyStringParam(ruleName, 200, "[a-zA-Z0-9_ ]*")){
                throw new AdeUsageException("Invalid parameters for a group of type " + groupType + " was specified");
            }
            currentGroups.add(new Group(name, GroupType.valueOf(groupType), DataType.valueOf(dataType.toUpperCase()), evalOrder, ruleName));
        }
        
        validateEvaluationOrderAndName(currentGroups);
        
        return currentGroups;        
    }
    

    /**
     * Main logic for parsing the rules from the JSON file. Also does validation of the rules attributes.
     * @param groups JSONArray object that contains the groups parsed from the JSON file.
     * @param rules JSONArray object that contains the rules parsed from the JSON file.
     * @return An ArrayList of Rule Objects that contain the parsed information.
     * @throws AdeUsageException
     * @throws JSONException
     */
    private List<Rule> parseRules(JSONArray rules) throws AdeUsageException, JSONException{
        if (rules.length() == 0) throw new AdeUsageException("No rules specified");
        List<Rule> currentRules = new ArrayList<Rule>();
        for (int i = 0; i < rules.length(); i++){
            JSONObject rule = rules.getJSONObject(i);
            String name = rule.getString("name");
            String description = rule.getString("description");
            String membershipRule = rule.getString("membershipRule");
            
            if (!verifyStringParam(name, 200, "[a-zA-Z0-9_ ]*") || (description != null && description.length() > 1000)
                    || name.equalsIgnoreCase("unassigned_rule") || !verifyStringParam(membershipRule,256,"[a-zA-Z0-9.:?/*-]*")){
                throw new AdeUsageException("Invalid parameters for rules was specified");
            }
            
            currentRules.add(new Rule(name, membershipRule, description));
            
            validateRuleNames(currentRules);
        }
        return currentRules;
    }
    
    
    /**
     * Validation checking to make sure the evaluation orders do not contain duplicates and is in consecutive
     * order. Also makes sure that the names are unique. Note: names must be unique ONLY within group type. 
     * @param groups groups of the same type parsed from the JSON file to be validated.
     * @throws AdeUsageException
     */
    private void validateEvaluationOrderAndName(List<Group> groups) throws AdeUsageException{
        int maxEvalOrder = 0;
        HashSet<Integer> evalOrders = new HashSet<Integer>();
        HashSet<String> usedNames = new HashSet<String>();
        for (Group group : groups){
            if (group.getEvaluationOrder() > maxEvalOrder){
                maxEvalOrder = group.getEvaluationOrder();
            }
            if (!evalOrders.add(group.getEvaluationOrder())){
                throw new AdeUsageException("Attempted to add/update groups to have duplicate evaluation orders");
            }
            if (!usedNames.add(group.getName().toUpperCase())){
                throw new AdeUsageException("Attempted to add/update group of type " + group.getGroupType().name() + " to have duplicate names");
            }            
        }        
        if (evalOrders.size() != maxEvalOrder){
            throw new AdeUsageException("Attempted to add/update groups that have non-consecutive evaluation orders");
        }
    }
    

    /**
     * Method to check there are no duplicated names.
     * @param rules parsed rules from JSON file to be validated.
     * @throws AdeUsageException
     */
    private void validateRuleNames(List<Rule> rules) throws AdeUsageException{
        HashSet<String> ruleNames = new HashSet<String>();
        for (Rule rule : rules){
            if (!ruleNames.add(rule.getName().toUpperCase())){
                throw new AdeUsageException("Attempted to add/update rule table with duplicate names");
            }   
        }
    }
    

    /**
     * Method to make sure that every "ruleName" referenced by a group is provided in the
     * JSON file. Also makes sure that every rule in the rules section of the JSON file is referenced
     * by at least one group.
     * @param groupsByType groups of the same type parsed from the JSON file.
     * @param rules rules parsed from the JSON file.
     * @throws AdeUsageException
     */
    private void validateGroupRules(HashMap<Integer, List<Group>> groupsByType, List<Rule> rules) throws AdeUsageException{
        HashSet<String> ruleNames = new HashSet<String>();
        HashSet<String> usedRules = new HashSet<String>();
        for (Rule rule: rules){
            ruleNames.add(rule.getName().toUpperCase());
        }      
        for (GroupType groupType : GroupType.values()){
            int groupVal = groupType.getValue();
            List<Group> groups = groupsByType.get(groupVal);
            for (Group group : groups){
                if (!ruleNames.contains(group.getRuleName().toUpperCase())){
                    throw new AdeUsageException("The rule name " + group.getRuleName() + " must be provided in the JSON file.");
                }
                usedRules.add(group.getRuleName().toUpperCase());
            }
        }
        if (usedRules.size() != ruleNames.size()){
            throw new AdeUsageException("Every rule in the JSON file must be referenced by a group.");
        }
    }
                 
    /**
     * Returns true if the specified String is non-null, greater then 0 in length, less than
     * the specified length and matches the regex provided.
     * @param str the string to verify.
     * @param maxLength the maximum length of the string.
     * @param acceptableCharacters a regex that should match the string.
     * @return true if all conditions satisfy the requirements.
     */
    private static boolean verifyStringParam(String str, int maxLength, String acceptableCharacters)
    {
        return !(str == null || str.length() == 0 || str.length() > maxLength || !Pattern.matches(acceptableCharacters, str));
    }
    
    private static boolean validateDataType(String dataType){
        for (DataType type : DataType.values()){
            if (type.name().equalsIgnoreCase(dataType)){
                return true;
            }
        }
        return false;
    }
    

    /**
     * Method for getting the parsed groups from the JSON file.
     * @return parsedGroupsByType contains the groups of a specific type.
     */
    public HashMap<Integer,List<Group>> getParsedGroupsByType(){
        return parsedGroupsByType;
    }
    
    /**
     * Method for getting the parsed rules from the JSON file.
     * @return parsedRules contains the rules.
     */
    public List<Rule> getParsedRules(){
        return parsedRules;
    }    

}
