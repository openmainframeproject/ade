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
package org.openmainframe.ade.ext.os.parser.helper;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

/**
 * Utility class for creating rules and groups JSON objects.
 * Yunli Tang
 */
public final class GroupsAndRulesJSONUtil {
    private static final String JSON_FIELD_NAME = "name";
    private static final String JSON_FIELD_DATATYPE = "dataType";
    private static final String JSON_FIELD_EVAL_ORDER = "evaluationOrder";
    private static final String JSON_FIELD_RULE_NAME = "ruleName";
    private static final String JSON_FIELD_DESCRIPTION = "description";
    private static final String JSON_FIELD_RULE = "membershipRule";
    
    private GroupsAndRulesJSONUtil(){}
    
    /**
     * Creates a JSONObject that contains the group fields.
     * @param name the analysis group name
     * @param dataType the type of the data.
     * @param evalOrder the order the group will be evaluated.
     * @param ruleName the name of the rule this group is referencing.
     * @return JSONObject that contains all the group fields.
     * @throws JSONException
     */
    public static JSONObject createGroup(String name, String dataType, int evalOrder, String ruleName) throws JSONException{
        JSONObject modelGroup = new JSONObject();
        modelGroup.put(JSON_FIELD_NAME, name);
        modelGroup.put(JSON_FIELD_DATATYPE , dataType);
        modelGroup.put(JSON_FIELD_EVAL_ORDER, evalOrder);
        modelGroup.put(JSON_FIELD_RULE_NAME,ruleName);
        return modelGroup;
    }
    
    /**
     * Creates a JSONObject that contains the rules fields.
     * @param name the rule name.
     * @param description a short description of the systems accepted into this group.
     * @param membershipRule the rule that determines system membership.
     * @return JSONObject that contains all the rule fields.
     * @throws JSONException
     */
    public static JSONObject createRule(String name, String description, String membershipRule) throws JSONException{
        JSONObject rule = new JSONObject();
        rule.put(JSON_FIELD_NAME, name);
        rule.put(JSON_FIELD_DESCRIPTION, description);
        rule.put(JSON_FIELD_RULE, membershipRule);
        return rule;
    }
}
