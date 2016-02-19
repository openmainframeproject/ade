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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.openmainframe.ade.data.DataType;
import org.openmainframe.ade.data.GroupType;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.data.Group;
import org.openmainframe.ade.ext.data.Rule;
import org.openmainframe.ade.ext.os.parser.JSONGroupParser;
import org.openmainframe.ade.ext.os.parser.helper.GroupsAndRulesJSONUtil;

import static org.junit.Assert.*;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

/**
 * JUnit test to test parsing of the GROUPS and RULES fields in JSON file.
 * Yunli Tang
 */
public class TestJSONGroupParser {  
    
    /**
     * The file path where the JSON will be located.
     */
    private static final String JSON_FILE_PATH = "analysisgroups.json";
    /**
     * The parser for parsing groups and rules in JSON file.
     */
    private JSONGroupParser jsonGroupParser;
    /**
     * The main JSON object that holds the rules and models JSON array.
     */
    private JSONObject jsonGroupObject;
    /**
     * Stores the model information.
     */
    private JSONArray modelArray;
    /**
     * Stores the rule information.
     */
    private JSONArray rulesArray;
    /**
     * The file where the JSON object gets written to.
     */
    private File jsonFile;
    /**
     * FileWriter for writing the JSON object to file.
     */
    private FileWriter fileWriter;

    /**
     * Preliminary initialization. Creates the skeleton of the JSON file.
     * @throws JSONException
     * @throws IOException
     */
    @Before
    public void initialize() throws JSONException, IOException{
        jsonGroupParser = new JSONGroupParser();
        jsonGroupObject = new JSONObject();
        initializeJSON();
        jsonFile = new File(JSON_FILE_PATH);
        fileWriter = new FileWriter(jsonFile);
    }
    
    private void initializeJSON() throws JSONException{
        rulesArray = new JSONArray();
        jsonGroupObject.put("rules", rulesArray);
        JSONObject modelInfo = new JSONObject();
        modelArray = new JSONArray();
        modelInfo.put("modelgroups", modelArray);
        jsonGroupObject.put("groups", modelInfo);
    }

    /**
     * Test to make sure an empty JSON file throws a AdeUsageException.
     * @throws IOException
     * @throws AdeException
     */
    @Test(expected=AdeUsageException.class)
    public void testEmptyJSON() throws IOException, AdeException{
        writeToFileAndParseJSON();
    }
    
    /**
     * Validates verifying the analysis group name. It is expected that a AdeUsageException is 
     * thrown because of the @ sign on the analysis group name.
     * @throws JSONException
     * @throws IOException
     * @throws AdeException
     */
    @Test(expected=AdeUsageException.class)
    public void testAnalysisGroupName() throws JSONException, IOException, AdeException {
        JSONObject modelGroup = GroupsAndRulesJSONUtil.createGroup("ModelGN@me1", "syslog", 1, "modelRule");
        modelArray.put(modelGroup);
        writeToFileAndParseJSON();
    }
    
    /**
     * Makes sure a AdeUsageException is thrown when the evaluation order is incorrect. The evaluation
     * order must be in increasing consecutive order starting with 1.
     * @throws JSONException
     * @throws IOException
     * @throws AdeException
     */
    @Test(expected=AdeUsageException.class)
    public void testEvaluationOrder() throws JSONException, IOException, AdeException{
        JSONObject modelGroup1 = GroupsAndRulesJSONUtil.createGroup("ModelGName1", "syslog", 2, "modelRule1");
        JSONObject modelGroup2 = GroupsAndRulesJSONUtil.createGroup("ModelGName2", "syslog", 3, "modelRule2");
        modelArray.put(modelGroup1);
        modelArray.put(modelGroup2);
        writeToFileAndParseJSON();
    }
    
    /**
     * Makes sure a AdeUsageException is thrown when we do not have a 1-1 mapping for groups to rules.
     * @throws JSONException
     * @throws IOException
     * @throws AdeException
     */
    @Test(expected=AdeUsageException.class)
    public void testvalidate1to1GroupRules() throws JSONException, IOException, AdeException {
        JSONObject modelGroup1 = GroupsAndRulesJSONUtil.createGroup("ModelGName1", "syslog", 1, "modelRule1");
        JSONObject modelGroup2 = GroupsAndRulesJSONUtil.createGroup("ModelGName2", "syslog", 2, "modelRule2");
        JSONObject rule1 = GroupsAndRulesJSONUtil.createRule("modelRule1", "Description for model rule 1", "ModelGName1");
        modelArray.put(modelGroup1);
        modelArray.put(modelGroup2);
        rulesArray.put(rule1);
        writeToFileAndParseJSON();
    }
    
    /**
     * Checks to make sure that every rule name in a group is referenced by a rule otherwise, a 
     * AdeUsageException is thrown.
     * @throws JSONException
     * @throws IOException
     * @throws AdeException
     */
    @Test(expected=AdeUsageException.class)
    public void testvalidateGroupRulesNames() throws JSONException, IOException, AdeException {
        JSONObject modelGroup1 = GroupsAndRulesJSONUtil.createGroup("ModelGName1", "syslog", 1, "modelRule1");
        JSONObject modelGroup2 = GroupsAndRulesJSONUtil.createGroup("ModelGName2", "syslog", 2, "modelRule2");
        JSONObject rule1 = GroupsAndRulesJSONUtil.createRule("modelRule1", "Description for model rule 1", "ModelGName1");
        JSONObject rule2 = GroupsAndRulesJSONUtil.createRule("modelRule3", "Description for model rule 2", "ModelGName2");
        modelArray.put(modelGroup1);
        modelArray.put(modelGroup2);
        rulesArray.put(rule1);
        rulesArray.put(rule2);
        writeToFileAndParseJSON();
    }
    
    /**
     * Test to see if the groups and rules are parsed correctly. i.e. all the fields for both groups and rules
     * are the same before parsing the JSON and after parsing the JSON.
     * @throws JSONException
     * @throws IOException
     * @throws AdeException
     */
    @Test
    public void testGroupsAndRulesCorrectlyParsed() throws JSONException, IOException, AdeException {
        List<Group> modelGroups = new ArrayList<Group>();
        modelGroups.add(new Group("ModelGName1", GroupType.getGroupType(1), DataType.getDataType((short)1), 3, "prefixRule"));
        modelGroups.add(new Group("ModelGName2", GroupType.getGroupType(1), DataType.getDataType((short)1), 1, "postfixRule"));
        modelGroups.add(new Group("ModelGName3", GroupType.getGroupType(1), DataType.getDataType((short)1), 2, "SYS1Rule"));
        JSONObject modelGroup1 = createGroupFromList(modelGroups.get(0));
        JSONObject modelGroup2 = createGroupFromList(modelGroups.get(1));
        JSONObject modelGroup3 = createGroupFromList(modelGroups.get(2));
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule("prefixRule", "PREFIX*", "Matches systems that start with PREFIX"));
        rules.add(new Rule("postfixRule", "*POSTFIX", "Matches systems that end with POSTFIX"));
        rules.add(new Rule("SYS1Rule", "SYS1", "Matches systems named SYS1"));
        JSONObject rule1 = createRuleFromList(rules.get(0));
        JSONObject rule2 = createRuleFromList(rules.get(1));
        JSONObject rule3 = createRuleFromList(rules.get(2));
        modelArray.put(modelGroup1);
        modelArray.put(modelGroup2);
        modelArray.put(modelGroup3);
        rulesArray.put(rule1);
        rulesArray.put(rule2);
        rulesArray.put(rule3);
        writeToFileAndParseJSON();
        HashMap<Integer, List<Group>> parsedGroupsByType = jsonGroupParser.getParsedGroupsByType();
        List<Rule> parsedRules = jsonGroupParser.getParsedRules();
        for (int i = 0 ; i < parsedRules.size(); i++){
            assertRulesEqual(rules.get(i),parsedRules.get(i));
        }
        for (GroupType group : GroupType.values()){
            List<Group> parsedGroups = parsedGroupsByType.get(group.getValue());
            for (int i = 0 ; i < parsedGroups.size(); i++){
                assertGroupsEqual(modelGroups.get(i), parsedGroups.get(i));
            }
        }
    }
    
    private JSONObject createGroupFromList(Group modelGroup) throws JSONException{
        return GroupsAndRulesJSONUtil.createGroup(modelGroup.getName(),modelGroup.getDataType().name(),
                modelGroup.getEvaluationOrder(),modelGroup.getRuleName());
    }
    
    private JSONObject createRuleFromList(Rule rule) throws JSONException{
        return GroupsAndRulesJSONUtil.createRule(rule.getName(), rule.getDescription(), rule.getMembershipRule());
    }
    
    private void assertRulesEqual(Rule origRule, Rule parsedRule){
        assertEquals("The rule names do not match.",origRule.getName(), parsedRule.getName());
        assertEquals("The descriptions do not match.",origRule.getDescription(), parsedRule.getDescription());
        assertEquals("The membership rules do not match.",origRule.getMembershipRule(), parsedRule.getMembershipRule());
    }
    
    private void assertGroupsEqual(Group origGroup, Group parsedGroup){
        assertEquals("The group names do not match.",origGroup.getName(), parsedGroup.getName());
        assertEquals("The group types do not match.",origGroup.getGroupType(), parsedGroup.getGroupType());
        assertEquals("The data types do not match.",origGroup.getDataType(), parsedGroup.getDataType());
        assertEquals("The evaluation orders do not match.",origGroup.getEvaluationOrder(), parsedGroup.getEvaluationOrder());
        assertEquals("The rule names do not match.",origGroup.getRuleName(), parsedGroup.getRuleName());
    }
    
    /**
     * Writes the JSON object to file and calls the parseJSON method in the jsonGroupParser object for testing.
     * @throws IOException
     * @throws AdeException
     */
    public void writeToFileAndParseJSON() throws IOException, AdeException{
        fileWriter.write(jsonGroupObject.toString());
        fileWriter.flush();
        jsonGroupParser.parseJSON(jsonFile);
    }
    
    /**
     * Clean-up code.
     * @throws IOException 
     */
    @After
    public void cleanUp() throws IOException{
        fileWriter.close();
        jsonFile.delete();
    }
}
