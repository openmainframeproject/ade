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
package org.openmainframe.ade.ext.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.ext.utils.AtomicTransaction;
import org.openmainframe.ade.ext.utils.ExtDataStoreUtils;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains the main logic for updating and interacting with the RULES table in the database.
 * The RULES table is associated with the GROUPS table where a RULE determines membership into a 
 * group.
 */
public final class RulesQueryImpl { 

    private static final Logger logger = LoggerFactory.getLogger(RulesQueryImpl.class);
    private static final String RULES_TABLE = SQL.RULES.name();

    /**
     * Private constructor.
     */
    private RulesQueryImpl() {}

    /**
     * Updates/Adds/Deletes the rules in the database according to the new set of rules given 
     * in the JSON file. 
     * @param rules a list of rules that will replace what is in the database. 
     * @throws AdeInternalException
     */
    public static void modifyRules(List<Rule> rules) throws AdeInternalException{
        ModifyRulesAtomic atomicUpdate = new ModifyRulesAtomic(rules);
        boolean success = ExtDataStoreUtils.executeAtomicTransaction(atomicUpdate);
        if (!success){
            logger.error("An error occurred while trying to modify " + RULES_TABLE + " table.");
            throw new AdeInternalException("An error occurred while trying to modify " + RULES_TABLE + " table.");
        }
    }  

    /**
     * Abstract class that contains methods to atomic read/update the RULES table.
     */
    private static abstract class AbstractRulesUpdateAtomic extends AtomicTransaction {
       
        /**
         * Get the list of current rules to see which rules need to be added/deleted/updated. 
         * @return The list of current rules in the database. 
         * @throws SQLException
         */
        protected List<Rule> getRuleListAtomic() throws SQLException {
            final PreparedStatement ruleListStatement = prepareStatement("SELECT * FROM " + RULES_TABLE);
            final ResultSet ruleListResultSet = ruleListStatement.executeQuery();
            final List<Rule> currentRules = new ArrayList<Rule>();
            
            if (ruleListResultSet != null) {
                while (ruleListResultSet.next()){
                    currentRules.add(parseRuleResult(ruleListResultSet));
                }
                ruleListResultSet.close();
            }
            ruleListStatement.close();
            return currentRules;            
        }
        
        /**
         * A helper method for parsing the rules results from a ResultSet object. 
         * @param ruleListResultSet contains the rules results from the SQL Query.
         * @return A Rule object with the parsed information from the ResultSet.
         * @throws SQLException
         */
        protected Rule parseRuleResult(ResultSet ruleListResultSet) throws SQLException{
            int uid = ruleListResultSet.getInt("RULE_INTERNAL_ID");
            String name = ruleListResultSet.getString("RULE_NAME");
            String description = ruleListResultSet.getString("DESCRIPTION");
            String rule = ruleListResultSet.getString("RULE");
            
            return new Rule(uid, name, rule, description);
        }    
    }

    /**
     * Class for modifying the RULES table. 
     */
    private static class ModifyRulesAtomic extends AbstractRulesUpdateAtomic{
        
        private final List<Rule> rules;
        
        /**
         * Constructor for getting a new set of rules.
         * @param rules the new list of rules.
         */
        public ModifyRulesAtomic(List<Rule> rules){
            super();
            this.rules = rules;
        }
        /**
         * Main part of the logic. Get the list of current rules in the database and essentially
         * does a replacement of whatever is in the database with the new list of rules from the 
         * JSON file. 
         */
        @Override
        public boolean performAtomicTransaction() throws AdeException {
            
            try {
                List<Rule> currentRules = getRuleListAtomic();
                List<Rule> rulesToAdd = getRulesToAdd(currentRules);
                List<Rule> rulesToDelete = getRulesToDelete(currentRules);
                List<Rule> rulesToUpdate = getRulesToUpdate(currentRules, rulesToDelete);
                
                List<String> batchList = new ArrayList<String>();
                deleteRules(batchList, rulesToDelete);
                addRules(batchList, rulesToAdd);
                updateRules(batchList, rulesToUpdate);
                executeBatch(batchList);               
                
            } catch (SQLException e) {
                throw new AdeInternalException("An error occurred while trying to update " + RULES_TABLE, e);
            }
            return true;
        }      
        
        /**
         * Any rules that are in the JSON file and NOT in the database will be added. 
         * Unique rules are identified their name. 
         * @param currentRules the current rules in the database. 
         * @return the list of rules to be added. 
         */
        private List<Rule> getRulesToAdd(List<Rule> currentRules){
            List<Rule> rulesToAdd = new ArrayList<Rule>(rules);
            rulesToAdd.removeAll(currentRules);
            return rulesToAdd;
        }
        
        /**
         * Any rules that are not in the JSON file but are in the database will be deleted.
         * @param currentRules the current rules in the database.
         * @return the list of rules to be deleted.
         */
        private List<Rule> getRulesToDelete(List<Rule> currentRules){
            List<Rule> rulesToDelete = new ArrayList<Rule>(currentRules);            
            rulesToDelete.removeAll(rules); 
            return rulesToDelete;
        }
        
        /**
         * Any rules that are both in the JSON file and the database that have matching names
         * but differ in membership rules and/or description will be updated. 
         * @param currentRules the current rules in the database
         * @param rulesToAdd the rules to be added to the database
         * @return the list of rules to be updated.
         */
        private List<Rule> getRulesToUpdate(List<Rule> currentRules, List<Rule> rulesToAdd){
            List<Rule> rulesToUpdate = new ArrayList<Rule>();
            for (Rule rule : rules){
                Rule copiedRule = new Rule(rule);
                rulesToUpdate.add(copiedRule);
            }
            rulesToUpdate.removeAll(rulesToAdd);
            Iterator<Rule> it = rulesToUpdate.iterator();
            while (it.hasNext()){
                Rule rule = it.next();
                if (exactMatch(currentRules, rule)){
                    it.remove();
                }
            }
            return rulesToUpdate;
        }
        
        /**
         * If there is an exact match between the rule and the rules in the database, then there is 
         * no need to update, otherwise update the rule. (A unique rule is identified by its name)
         * @param currentRules the current rules in the database
         * @param rule the rule being checked against the rules in the database to see if there is a match.
         * @return true if the rule is an exact match otherwise, return false. 
         */
        private boolean exactMatch(List<Rule> currentRules, Rule rule){
            if (!currentRules.contains(rule)) return false;
            for (Rule r : currentRules){
                if  ((r.getName().equalsIgnoreCase(rule.getName())) && 
                    (r.getMembershipRule().equalsIgnoreCase(rule.getMembershipRule())) &&
                    (r.getDescription().equalsIgnoreCase(rule.getDescription()))) return true;
                else if (r.getName().equalsIgnoreCase(rule.getName())){
                    rule.setUid(r.getUid());
                }
            }            
            return false;
        }

        /**
         * the sql commands to be added to the batchList for deleting the rules in rulesToDelete.
         * @param batchList list of sql batch commands.
         * @param rulesToDelete list of rules to be deleted in the database.
         */
        private static void deleteRules(List<String> batchList, List<Rule> rulesToDelete){
            String deleteStatement = "DELETE FROM " + RULES_TABLE + " WHERE RULE_INTERNAL_ID=%d";
            for (Rule rule : rulesToDelete){
                batchList.add(String.format(deleteStatement, rule.getUid()));
            }
        }
        
        /**
         * The SQL commands to be added to the batchList for adding the rules in rulesToAdd. 
         * @param batchList list of sql batch commands.
         * @param rulesToAdd list of rules to be added in the database.
         */
        private static void addRules(List<String> batchList, List<Rule> rulesToAdd){
            String addStatement = "INSERT INTO " + RULES_TABLE + " (" + 
                    "RULE_NAME, " +
                    "DESCRIPTION, " +
                    "RULE) " +
                    "VALUES ('%s', '%s', '%s')";
            for (Rule rule : rulesToAdd){
                batchList.add(String.format(addStatement, rule.getName(), rule.getDescription(), rule.getMembershipRule()));
            }
        }
        
        /**
         * The SQL commands to be added to the batchlist for updating the rules in rulesToUpdate. 
         * @param batchList list of sql batch commands.
         * @param rulesToUpdate list of rules to be updated in the database.
         */
        private static void updateRules(List<String> batchList, List<Rule> rulesToUpdate){
            String updateStatement = "UPDATE " + RULES_TABLE + " SET " +
                    "RULE_NAME='%s', " +
                    "DESCRIPTION='%s', " +
                    "RULE='%s' " +
                    "WHERE RULE_INTERNAL_ID=%d";
            for (Rule rule : rulesToUpdate){
                batchList.add(String.format(updateStatement, rule.getName(), rule.getDescription(),
                        rule.getMembershipRule(), rule.getUid()));
            }
        }       
        
    }

}
