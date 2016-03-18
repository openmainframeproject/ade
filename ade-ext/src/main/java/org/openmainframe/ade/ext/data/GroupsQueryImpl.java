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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.dbUtils.DriverType;
import org.openmainframe.ade.data.DataType;
import org.openmainframe.ade.data.GroupType;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.ext.utils.AtomicTransaction;
import org.openmainframe.ade.ext.utils.ExtDataStoreUtils;
import org.openmainframe.ade.ext.utils.EXT_TABLES_SQL;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the main logic for inserting groups into the GROUPS table in the database.
 * It also updates the MODELS, SOURCES and RULES table based on the groups that were added.
 */
public final class GroupsQueryImpl {

    /**
     * The default unassigned analysis group id. Note, this is NOT the internal id in the GROUPS table.
     */
    private static final int UNASSIGNED_ANALYSIS_GROUP_ID = -1;
    /**
     * Default name for sources not assigned to a specific group.
     */
    private static final String UNASSIGNED_GROUP = "UNASSIGNED";

    /**
     * By default, unassigned groups are of type "MODEL."
     */
    private final static int UNASSIGNED_GROUP_TYPE = GroupType.MODELGROUPS.getValue();
 
    /**
     * By default, unassigned groups have SYSLOG data.
     */
    private final static int UNASSIGNED_DATA_TYPE = DataType.SYSLOG.getValue();

    /**
     * The default unassigned rule name.
     */
    private final static String UNASSIGNED_RULE_NAME = "UNASSIGNED_RULE";

    /**
     * The unassigned rule description.
     */
    private final static String UNASSIGNED_RULE_DESCRIPTION = "Includes all sources.";

    /**
     * The membership rule for the unassigned group.
     */
    private final static String UNASSIGNED_RULE = "*";

    /**
     * Logger for GroupsQueryImpl class.
     */
    private static final Logger logger = LoggerFactory.getLogger(GroupsQueryImpl.class);
    
    /**
     * Set of database tables used when updating analysis groups.
     */
    static final String SOURCES_TABLE = SQL.SOURCES.name();
    static final String GROUPS_TABLE = SQL.GROUPS.name();
    static final String MODELS_TABLE = SQL.MODELS.name();
    static final String RULES_TABLE = SQL.RULES.name();
    static final String MANAGED_SYSTEMS_TABLE = EXT_TABLES_SQL.MANAGED_SYSTEMS.name();
    static final String ANALYSIS_GROUPS_TIME_TABLE = EXT_TABLES_SQL.GROUP_TIMESTAMPS.name();
    
    /**
     * Private constructor.
     */
    private GroupsQueryImpl() {}

    /**
     * Updates the provided source's analysis group in the database
     * with the group that matches this source's name. If no group
     * can be matched to this source then a value of "UNASSIGNED" will
     * be returned as the group.
     *
     * @param sourceName The name of the source whose analysis group should be updated.
     * @return the name of the group that was assigned to the given source.
     */
    public static String updateSourcesAnalysisGroup(String sourceName) {
        final DetermineSourceGroupAtomic atomicAction = new DetermineSourceGroupAtomic(sourceName);
        ExtDataStoreUtils.executeAtomicTransaction(atomicAction);
        return atomicAction.getAssignedGroupName();
    }
    
    /**
     * Updates/Adds/Deletes the groups in the database according to the new set of groups given 
     * in the JSON file. Also updates the SOURCES table to reference the group that each source now
     * belongs to, the GROUP_TIMESTAMP table to keep track of when the GROUPS table was updated, and 
     * the MODELS table to keep it up to date with the groups. 
     * @param groups a list of groups that will replace what is in the database. 
     * @throws AdeException if an error occurred while interacting with the database.
     */
    public static void modifyGroups(List<Group> groups) throws AdeException {
        ModifyModelGroupAtomic atomicUpdate = new ModifyModelGroupAtomic(groups);
        boolean success = ExtDataStoreUtils.executeAtomicTransaction(atomicUpdate);
        if (!success){
            if (atomicUpdate.getIllegalArgEx() != null){
                throw atomicUpdate.getIllegalArgEx();
            }
            else{
                logger.error("An error occurred while trying to modify " + GROUPS_TABLE + " table.");
                throw new AdeInternalException("An error occurred while trying to modify" + GROUPS_TABLE + " table.");
            }
        }
    }        

    /**
     * Abstract class that contains methods to atomic read/update the GROUPS tables, read/update associated 
     * values in the SOURCES table.
     */
    private static abstract class AbstractGroupsUpdateAtomic extends AtomicTransaction {
        /**
         * Either contains the default unassigned analysis group id OR the unassigned analysis group
         * internal id that is in the database.
         */
        protected int unassignedGroupId = UNASSIGNED_ANALYSIS_GROUP_ID;
        /**
         *  Get the list of current groups to see which groups need to be updated, added, or deleted. 
         *  It will also be used to place sources in the correct group. 
         * @return The list of current groups in the database.
         * @throws SQLException
         * @throws AdeInternalException
         */
        protected List<Group> getGroupListAtomic() throws SQLException, AdeInternalException {
            final PreparedStatement groupListStatement = prepareStatement("SELECT * FROM " + GROUPS_TABLE);
            final ResultSet groupListResultSet = groupListStatement.executeQuery();
            final List<Group> currentModelGroups = new ArrayList<Group>();

            // For each of the database rows; parse the ModelGroup from it
            if (groupListResultSet != null) {
                while (groupListResultSet.next()) {
                    currentModelGroups.add(parseGroupResult(groupListResultSet));
                }
                groupListResultSet.close();
            }
            // Clean up the result set and statement as we no longer need them
            groupListStatement.close();
            return currentModelGroups;
        }
        
        /**
         * Helper method for parsing the group results from a ResultSet object. 
         * @param resultSet contains the group information from a SQL Query.
         * @return Group object with the results from the resultSet.
         * @throws SQLException
         * @throws AdeInternalException
         */
        private Group parseGroupResult(ResultSet resultSet) throws SQLException, AdeInternalException {
            int uid = resultSet.getInt("GROUP_INTERNAL_ID");
            String name = resultSet.getString("GROUP_NAME");           
            Short groupTypeVal = resultSet.getShort("GROUP_TYPE");
            GroupType groupType = GroupType.getGroupType(groupTypeVal);
            Short dataTypeVal = resultSet.getShort("DATA_TYPE");
            DataType dataType = DataType.getDataType(dataTypeVal);
            short evaluationOrder = resultSet.getShort("EVALUATION_ORDER");
            int rid = resultSet.getInt("RULE_INTERNAL_ID");
            String ruleName = getRuleName(rid);
            return (new Group(uid,name,groupType,dataType,evaluationOrder,ruleName));            
        }
        /**
         * Retrieves the rule name by selecting the row with the given unique rule id. 
         * Note: this value can be null if a rule has been deleted from the RULES table.
         * However, after updating the GROUPS table, the null value will also be updated.
         * @param rid unique rule id. 
         * @return String value containing the rule name.
         * @throws SQLException
         */
        protected String getRuleName(int rid) throws SQLException{
            String ruleName = null;
            PreparedStatement ruleStatement = prepareStatement("SELECT RULE_NAME FROM " + 
                                                        RULES_TABLE + " WHERE RULE_INTERNAL_ID=" + rid);
            ResultSet ruleResultSet = ruleStatement.executeQuery();
            if (ruleResultSet.next()){
                ruleName = ruleResultSet.getString("RULE_NAME");
            }
            ruleResultSet.close();
            ruleStatement.close();
            return ruleName;
        }
        /**
         * Retrieves the unique rule id from the rule name passed in. 
         * @param ruleName the unique name of the rule associated with the rule id. 
         * @return the unique rule id. 
         * @throws SQLException
         */
        protected int getRid(String ruleName) throws SQLException{
            int uid = -1;
            PreparedStatement ruleStatement = prepareStatement("SELECT RULE_INTERNAL_ID FROM " + 
                                                        RULES_TABLE + " WHERE RULE_NAME='" + ruleName + "'");
            ResultSet ruleResultSet = ruleStatement.executeQuery();
            if (ruleResultSet.next()){
                uid = ruleResultSet.getInt("RULE_INTERNAL_ID");
            }
            ruleResultSet.close();
            ruleStatement.close();
            return uid;
        }

        /**
         * Adds a source to a group based on the membership rule. The groups are
         * sorted according to evaluation order first. Then, create a pattern for each of
         * their membership rules so it can be easily iterated in order of importance. Then,
         * the rules are checked against each sources name. If the name matches the rule, 
         * it will update the sources table to refer to that group otherwise it will refer to 
         * the "UNASSIGNED" group.
         * @param groups the current groups used for extracting membership rules.
         * @param sourcesResult The list of sources to be matched to a group. 
         * @param batchList the list of sql batch commands.
         * @throws AdeException 
         */
        protected void updateGroupSources(List<Group> groups,
                ResultSet sourcesResult, List<String> batchList) throws SQLException, AdeException {
            // Sort the model groups by evaluation order and then create a pattern for
            // each of there "rules" so that we can easily iterate through the rules in
            // order of importance
            if (!sourcesResult.isBeforeFirst()) return;
            Collections.sort(groups, new GroupComparator());

            final List<Pattern> patternList = getPatternList(groups);
            List<Short> unassignedSourcesId = new ArrayList<Short>();
          
            while (sourcesResult.next()) {
                boolean foundMatch = false;
                final String sourceName = sourcesResult.getString("SOURCE_ID");
                final String sourceNameUpper = sourceName.toUpperCase();
                final short sourceInternalId = sourcesResult.getShort("SOURCE_INTERNAL_ID");

                // Go through all of the patterns in order looking for a match
                for (int i = 0; i < patternList.size(); i++) {
                    
                    // A match was found, add a batch statement to update the source
                    if (patternList.get(i).matcher(sourceNameUpper).matches()) {
                        foundMatch = true;
                        batchList.add("UPDATE " + SOURCES_TABLE + " SET " +
                                "ANALYSIS_GROUP=" + groups.get(i).getUid() +
                                " WHERE SOURCE_INTERNAL_ID=" + sourceInternalId);
                        setSourcesGroup(sourceName, groups.get(i).getName());
                        break;
                    }
                }   
                
                //A match was not found, assign the source group name to UNASSIGNED.
                if (!foundMatch) {
                    unassignedSourcesId.add(sourceInternalId);
                    setSourcesGroup(sourceName, UNASSIGNED_GROUP);
                }
            }

            if (!unassignedSourcesId.isEmpty()){
                unassignedGroupId = insertUnassignedGroup();
                for (short sourceId : unassignedSourcesId){
                    batchList.add("UPDATE " + SOURCES_TABLE + " SET " + "ANALYSIS_GROUP=" + unassignedGroupId +
                            " WHERE SOURCE_INTERNAL_ID=" +sourceId);
                }
            }
        }

        /**
         * This method returns the group internal id of the unassigned group. First, we add the unassigned rule into
         * the RULES table, retrieve the rule internal id, and uses this id as one of the column values when inserting
         * the analysis group. After the execution is done, we get the generated keys which returns the internal id of
         * the newly inserted unassigned analysis group.
         * @return the group internal id of the unassigned group.
         */
        private int insertUnassignedGroup() throws SQLException{
            PreparedStatement groupStatement = null;
            final int ruleid = insertUnassignedRule();
            final int evaluationOrder = getNumOfGroups() + 1;
            String unassignedGroupSqlStatement = "INSERT INTO " + GROUPS_TABLE + " (GROUP_NAME, "
                    + "GROUP_TYPE, DATA_TYPE, RULE_INTERNAL_ID, EVALUATION_ORDER) "
                    + "VALUES ('%s', %d, %d, %d, %d)";
            unassignedGroupSqlStatement = String.format(unassignedGroupSqlStatement, UNASSIGNED_GROUP, UNASSIGNED_GROUP_TYPE,
                    UNASSIGNED_DATA_TYPE, ruleid , evaluationOrder);
            groupStatement = prepareStatement(unassignedGroupSqlStatement, new String[]{"GROUP_INTERNAL_ID"});
            groupStatement.execute();
            final int unassignedGroupId = getInternalId(groupStatement);
            return unassignedGroupId;
        }

        /**
         * Inserts a new unassigned rule in the database. Note, we do not have to check and see if there is an 
         * unassigned rule in the RULES table already since we only call this method if we haven't created 
         * an unassigned group yet.
         * @return The rule internal id for the unassigned group rule.
         * @throws SQLException
         */
        private int insertUnassignedRule() throws SQLException{
            PreparedStatement ruleStatement = null;
            String unassignedRuleSqlStatement = "INSERT INTO " + SQL.RULES +
                    " (RULE_NAME, DESCRIPTION, RULE) VALUES ('%s','%s','%s')";
            unassignedRuleSqlStatement = String.format(unassignedRuleSqlStatement, UNASSIGNED_RULE_NAME, 
                    UNASSIGNED_RULE_DESCRIPTION, UNASSIGNED_RULE);
            ruleStatement = prepareStatement(unassignedRuleSqlStatement, new String[]{"RULE_INTERNAL_ID"});
            ruleStatement.execute();
            final int ruleid = getInternalId(ruleStatement);
            return ruleid;
        }

        /**
         * Retrieves the internal id generated in the database by using the getGeneratedKeys method.
         * @param preparedStatement The precompiled SQL Statement
         * @return the internal id generated in the database.
         * @throws SQLException
         */
        public int getInternalId(PreparedStatement preparedStatement) throws SQLException {
            ResultSet generatedKey = null;
            int internalId = 0;
            generatedKey = preparedStatement.getGeneratedKeys();
            if (generatedKey.next()) {
                internalId = generatedKey.getInt(1);
            }
            preparedStatement.close();
            generatedKey.close();
            
            return internalId;
        }

        /**
         * Retrieves the number of groups in the groups table. This number is for calculating
         * the evaluation order of the unassigned group. The unassigned group should always
         * be the last group in the evaluation order.
         * @return the number of groups in the GROUPS table.
         * @throws SQLException
         */
        private int getNumOfGroups() throws SQLException {
            PreparedStatement numOfGroupsStatement = null;
            ResultSet numOfGroupsResult = null;
            numOfGroupsStatement = prepareStatement("SELECT COUNT(*) AS COUNT_TOTAL FROM " + SQL.GROUPS);
            numOfGroupsResult = numOfGroupsStatement.executeQuery();
            numOfGroupsResult.next();
            int numOfGroups = numOfGroupsResult.getInt("COUNT_TOTAL");
            numOfGroupsStatement.close();
            numOfGroupsResult.close();
                
            return numOfGroups;
        }

        /**
         * Method to alert any extenders of this class that a source has been assigned to a group.
         * @param sourceName the name of the source that was assigned to a group
         * @param modelName the name of the group that was assigned to the source
         */
        protected void setSourcesGroup(String sourceName, String groupName) {}

        /**
         * Comparator to order groups by evaluation order. 
         */
        protected static class GroupComparator implements Comparator<Group> {
            @Override
            public int compare(Group object1, Group object2) {
                return object1.getEvaluationOrder() - object2.getEvaluationOrder();
            }
        }
        
        /**
         * Returns an arraylist of patterns for each group rule. 
         * @param groups list of groups to extract the membership rules from.
         * @return patternList a list of patterns for each group rule.
         * @throws SQLException
         */
        private ArrayList<Pattern> getPatternList(List<Group> groups) throws SQLException{
            final ArrayList<Pattern> patternList = new ArrayList<Pattern>();
            PreparedStatement ruleStatement = null;
            ResultSet ruleResult = null;
            for (Group group : groups) { 
                ruleStatement = prepareStatement("SELECT RULE FROM " + RULES_TABLE + 
                        " WHERE RULE_NAME='" + group.getRuleName() + "'");               
                ruleResult = ruleStatement.executeQuery();
                ruleResult.next();
                // Anything between the \Q and \E tags will automatically be escaped.
                // Intentional wildcards should be omitted from these tags. 
                String pString = "\\Q" + ruleResult.getString("RULE").toUpperCase() + "\\E";
                pString = pString.replaceAll("\\*", "\\\\E.*\\\\Q");
                pString = pString.replaceAll("\\?", "\\\\E.?\\\\Q");
                patternList.add(Pattern.compile(pString));    
            }
            if (ruleStatement != null && ruleResult != null){
                ruleStatement.close();
                ruleResult.close();
            }
            return patternList;
        }
    }

    /**
     * Class to atomically update a given sources' group in the SOURCES table.
     */
    private static class DetermineSourceGroupAtomic extends AbstractGroupsUpdateAtomic {
        private final String sourceName;

        /**
         * The name of the group that was assigned to this source, this will be null if the
         * atomic action has not yet been performed.
         */
        private String assignedGroupName;       

	private boolean mySQL;

        /**
         * The explicit value constructor for calling the parent constructor
         * and initializing the source name.
         * @param sourceName the source name.
         */
        public DetermineSourceGroupAtomic(String sourceName) {
            super();
            this.sourceName = sourceName;
	    try {
	        final String driver = Ade.getAde().getConfigProperties().database().getDatabaseDriver();

	        if ((DriverType.parseDriverType(driver) == DriverType.MY_SQL) ||
		    (DriverType.parseDriverType(driver) == DriverType.MARIADB))
		    mySQL = true;
	        else
    	            mySQL = false;
            } catch (AdeException e) {
                logger.error("An error occurred while trying to retrieve driver type");
            }	
        }

        @Override
        public boolean performAtomicTransaction() throws AdeException {
            try {
                // Lock the sources table so that we can update it without worrying if
                // analysis groups are currently being changed. This will be
                // released at the end of this transaction.
		if (mySQL) 
                    this.execute("LOCK TABLES " + SOURCES_TABLE + " WRITE, " +
                                 GROUPS_TABLE + " WRITE, " + 
                                 MANAGED_SYSTEMS_TABLE + " WRITE, " +
                                 MODELS_TABLE + " WRITE, " +
                                 ANALYSIS_GROUPS_TIME_TABLE + " WRITE, " +
                                 RULES_TABLE + " WRITE");
		else
                    this.execute("LOCK TABLE " + SOURCES_TABLE + " IN EXCLUSIVE MODE");

                final List<Group> groups = getGroupListAtomic();

                final PreparedStatement sourceListStatement = prepareStatement(
                        "SELECT SOURCES.SOURCE_INTERNAL_ID, SOURCES.SOURCE_ID FROM "
                                + SOURCES_TABLE + " INNER JOIN " + MANAGED_SYSTEMS_TABLE
                                + " ON SOURCES.SOURCE_INTERNAL_ID=MANAGED_SYSTEMS.SOURCE_INTERNAL_ID WHERE "
                                + "UPPER(" + MANAGED_SYSTEMS_TABLE + ".OPERATING_SYSTEM)='LINUX'"
                                + " AND SOURCES.SOURCE_ID='" + sourceName + "'", 
                                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                final ResultSet sourcesResult = sourceListStatement.executeQuery();

                final List<String> batchList = new ArrayList<String>();
                this.updateGroupSources(groups, sourcesResult, batchList);
                this.executeBatch(batchList);
                sourceListStatement.close();

            } catch (SQLException e) {
                logger.error("An error occurred while trying to update " + SOURCES_TABLE + " table.");
                throw new AdeInternalException("An error occurred while "
                        + "trying to update " + SOURCES_TABLE + " table.", e);
            }
            if (mySQL) {
                try {
                    this.execute("UNLOCK TABLES");
                } catch (SQLException e) {
                    logger.error("An error occurred while trying to unlock " + SOURCES_TABLE + " table.");
                    throw new AdeInternalException("An error occurred while "
                            + "trying to unlock " + SOURCES_TABLE + " table.", e);
                }
            }
            return true;
        }

        @Override
        protected void setSourcesGroup(String sourceName, String modelName) {
            this.assignedGroupName = modelName;
        }

        public String getAssignedGroupName() {
            return assignedGroupName;
        }
    }
    
    /**
     * Class to implement the atomic transaction that must take place in order to modify 
     * the groups and the groups associated with each source. 
     */
    private static class ModifyModelGroupAtomic extends AbstractGroupsUpdateAtomic
    {
        List<Group> groups;
        private IllegalArgumentException illegalArgEx;
	private boolean mySQL;

        /**
         * Constructor for initializing the new list of groups.
         * @param groups the list of new groups.
         */
        public ModifyModelGroupAtomic(List<Group> groups) {
            super();
            this.groups = groups;
	    try {
	        final String driver = Ade.getAde().getConfigProperties().database().getDatabaseDriver();

	        if ((DriverType.parseDriverType(driver) == DriverType.MY_SQL) ||
		    (DriverType.parseDriverType(driver) == DriverType.MARIADB))
		    mySQL = true;
	        else
    	            mySQL = false;
            } catch (AdeException e) {
                logger.error("An error occurred while trying to retrieve driver type");
            }	
        }

        /**
         * Updates the GROUPS table with the groups passed in through the JSON file. 
         * Essentially, a replacement of the current groups in the database with the new
         * groups passed in. Then match each of the sources in the SOURCE table to one of the groups 
         * or UNASSIGNED if no matching group can be found.
         */
        @Override
        public boolean performAtomicTransaction() throws AdeException {
            try
            {
                // Lock the sources table so that we can update it without worrying if
                // analysis groups are currently being changed. This will be
                // released at the end of this transaction.
		if (mySQL)
                    this.execute("LOCK TABLES " + SOURCES_TABLE + " WRITE, " +
                                 GROUPS_TABLE + " WRITE, " + 
                                 RULES_TABLE + " WRITE, " + 
                                 MANAGED_SYSTEMS_TABLE + " WRITE, " + 
                                 ANALYSIS_GROUPS_TIME_TABLE + " WRITE, " + 
                                 MODELS_TABLE + " WRITE");
		else
                    this.execute("LOCK TABLE " + SOURCES_TABLE + " IN EXCLUSIVE MODE");

                List<Group> currentGroups = this.getGroupListAtomic();
                List<Group> groupsToAdd = getGroupsToAdd(currentGroups);
                List<Group> groupsToDelete = getGroupsToDelete(currentGroups);
                List<Group> groupsToUpdate = getGroupsToUpdate(currentGroups, groupsToAdd);

                List<String> batchList = new ArrayList<String>();
                deleteGroups(batchList, groupsToDelete);
                addGroups(batchList, groupsToAdd);
                updateGroups(batchList, groupsToUpdate);
                executeBatch(batchList);
                batchList.clear();
                currentGroups.clear();
                
                /**
                 * Now that the groups have been updated, we need to update the sources group entry. 
                 * We are only concerned about sources for linux systems. So for each linux source, 
                 * compare the name of the source to each groups "rule" in "evaluation order".
                 * If one of the rules match then set the analysis group in the source table
                 * to refer to that group by name and type.
                 */
                currentGroups = this.getGroupListAtomic();
                updateGroupSources(batchList, currentGroups);
                updateTimestamp(batchList);
                updateModelsTable(batchList, groupsToDelete);
                executeBatch(batchList); 
		if (mySQL)
                    this.execute("UNLOCK TABLES");
            }
            catch (IllegalArgumentException ex)
            {
                this.illegalArgEx = ex;
                return false;
            }
            catch (SQLException e)
            {
                logger.error("An error occurred while trying to update group table");
                throw new AdeInternalException("An error occurred while " +
                        "trying to update sources table", e);
            }

            return true;
        }
        

        /**
         * Any group that is in the JSON file and NOT in the database will be added. 
         * Unique groups are identified by group name and type. 
         * @param currentGroups The current groups in the database. 
         * @return The list of groups to add to the database. 
         */
        private List<Group> getGroupsToAdd(List<Group> currentGroups){
            List<Group> groupsToAdd = new ArrayList<Group>(groups);
            groupsToAdd.removeAll(currentGroups);
            return groupsToAdd;
        }
        
        /**
         * Any group that is NOT in the JSON file and is in the database will be deleted.
         * Unique groups are identified by group name and type.
         * @param currentGroups The current groups in the database. 
         * @return The list of groups to delete from the database. 
         */
        private List<Group> getGroupsToDelete(List<Group> currentGroups){
            List<Group> groupsToDelete = new ArrayList<Group>(currentGroups);
            groupsToDelete.removeAll(groups);
            return groupsToDelete;
        }

        /**
         * Groups that have the same name and type but have differing evaluation orders,
         * data types, and/or rule names will be updated. 
         * @param currentGroups The current groups in the database. 
         * @param groupsToAdd The groups to add in the database. 
         * @return the list of groups to be updated.
         */
        private List<Group> getGroupsToUpdate(List<Group> currentGroups, List<Group> groupsToAdd){
            List<Group> groupsToUpdate = new ArrayList<Group>();
            for (Group group : groups){
                Group copiedGroup = new Group(group);
                groupsToUpdate.add(copiedGroup);
            }
            groupsToUpdate.removeAll(groupsToAdd);
            Iterator<Group> it = groupsToUpdate.iterator();
            while (it.hasNext()){
                Group group = it.next();
                if (exactMatch(currentGroups, group)){
                    it.remove();
                }
            }
            return groupsToUpdate;
        }
        

        /**
         * If there is an exact match in the database then no need to update otherwise, update it.
         * @param currentGroups the current groups in the database
         * @param group the group to check if there is an exact match in the database. 
         * @return true if a group with the same field values already exists, false otherwise.
         */
        private boolean exactMatch(List<Group> currentGroups, Group group){
            if (!currentGroups.contains(group)) return false;
            for (Group g : currentGroups){
                if ((group.getName().equalsIgnoreCase(g.getName())) &&
                        (group.getGroupType().getValue() == g.getGroupType().getValue())){
                    
                    if ((group.getDataType().getValue() == g.getDataType().getValue()) &&
                            (group.getRuleName().equals(g.getRuleName())) &&
                            (group.getEvaluationOrder() == g.getEvaluationOrder())){
                        return true;
                    }
                    else{
                        group.setUid(g.getUid());
                    }
                } 
            }
            return false;
        }
 
        /**
         * Adds a string representation of the sql batch command required to delete an analysis group
         * to the batchList for each group in groupsToDelete.
         * @param batchList a list of sql batch command strings to be added to
         * @param groupsToDelete the list of groups that should have batch commands added for them
         */
        private void deleteGroups(List<String> batchList, List<Group> groupsToDelete){
            String deleteStatement = "DELETE FROM " + GROUPS_TABLE + " WHERE GROUP_INTERNAL_ID=%d";
            for (Group group : groupsToDelete){
                batchList.add(String.format(deleteStatement, group.getUid()));
            }
        }
        
        /**
         * Adds a string representation of the sql batch command required to add an analysis group
         * to the batchList for each group in groupsToAdd.
         * @param batchList a list of sql batch command strings to be added to
         * @param groupsToAdd the list of groups that should have batch commands added for them
         */              
        private void addGroups(List<String> batchList, List<Group> groupsToAdd) throws SQLException{
            String addStatement = "INSERT INTO " + GROUPS_TABLE + " (" + 
                    "GROUP_NAME, " +
                    "GROUP_TYPE, " +
                    "DATA_TYPE, " +
                    "RULE_INTERNAL_ID, " +
                    "EVALUATION_ORDER) " +
                    "VALUES ('%s', %d, %d, %d, %d)";
            for (Group group : groupsToAdd){
                int ruid = getRid(group.getRuleName());
                batchList.add(String.format(addStatement, group.getName(), group.getGroupType().getValue()
                        , group.getDataType().getValue(), ruid, group.getEvaluationOrder()));
            }
        }
        
        /**
         * Adds a string representation of the sql batch command required to update an analysis group
         * to the batchList for each group in the groupsToUpdate.
         * @param batchList a list of sql batch command strings to be added to
         * @param groupsToUpdate the list of groups that should have batch commands added for them
         */
        private void updateGroups(List<String> batchList, List<Group> groupsToUpdate) throws SQLException{
            String updateStatement = "UPDATE " + GROUPS_TABLE + " SET " +
                    "GROUP_NAME='%s', " +
                    "GROUP_TYPE=%d, " +
                    "DATA_TYPE=%d, " +
                    "RULE_INTERNAL_ID=%d, " +
                    "EVALUATION_ORDER=%d " +
                    "WHERE GROUP_INTERNAL_ID=%d";
            for (Group group : groupsToUpdate){
                int ruid = getRid(group.getRuleName());
                batchList.add(String.format(updateStatement, group.getName(), group.getGroupType().getValue(),
                        group.getDataType().getValue(), ruid, group.getEvaluationOrder(), group.getUid()));
            }
        }
        
        /**
         * Updates the GROUPS_TIMESTAMP table with the current time.
         * @param batchList a list of sql batch command strings to add to.
         */
        private void updateTimestamp(List<String> batchList){
            // Update the last update and last user for this request
            Timestamp lastUpdate = new Timestamp(System.currentTimeMillis());

            batchList.add("INSERT INTO " + ANALYSIS_GROUPS_TIME_TABLE + " (" +
                    "LAST_TIMESTAMP) " +
                    "VALUES ('" + lastUpdate.toString() + "')");
        }
        
        /**
         * Update the models table so if a deleted group name is being referenced, it is no longer 
         * used as the default model. Also checks if one of the deleted groups is the UNASSIGNED group.
         * and if a new internal id has been given to the UNASSIGNED group. If so, we treat this as a 
         * RENAMED group and need to update the database accordingly.
         * @param batchList a list of sql batch command strings to add to.
         * @param groupsToDelete list of groups to be deleted from the database. 
         * @throws SQLException
         */
        private void updateModelsTable(List<String> batchList, List<Group> groupsToDelete) throws SQLException{
            if (groupsToDelete.size() > 0)
            {
                // Get the list of the models that are associated with a group.
                PreparedStatement modelListStatement =
                        prepareStatement("SELECT MODEL_INTERNAL_ID, ANALYSIS_GROUP, IS_DEFAULT "
                                + "FROM MODELS WHERE ANALYSIS_GROUP IS NOT NULL");

                ResultSet modelsResult = modelListStatement.executeQuery();

                // Lock the models table in exclusive mode. This may seem heavy handed, but
                // it's possible the a new model could be inserted after the "list" action
                // is performed here and as a result that model would reference a now
                // non-existent analysis group
                if (mySQL)
		    batchList.add("LOCK TABLES " + MODELS_TABLE + " WRITE, " +
                                  GROUPS_TABLE + " WRITE, " + 
                                  RULES_TABLE + " WRITE, " + 
                                  SOURCES_TABLE + " WRITE, " + 
                                  ANALYSIS_GROUPS_TIME_TABLE + " WRITE, " + 
                                  MANAGED_SYSTEMS_TABLE + " WRITE");
		else
		    batchList.add("LOCK TABLE " + MODELS_TABLE + " IN EXCLUSIVE MODE");

                // Iterate over the models to update analysis group names
                while (modelsResult.next())
                {
                    int groupId = modelsResult.getInt("ANALYSIS_GROUP");                      
                    short isDefault = modelsResult.getShort("IS_DEFAULT");
                    if (unassignedIsInGroups(groupsToDelete,groupId) && (unassignedGroupId != UNASSIGNED_ANALYSIS_GROUP_ID)){
                        batchList.add("UPDATE " + MODELS_TABLE + " SET ANALYSIS_GROUP=" + unassignedGroupId +
                                " WHERE MODEL_INTERNAL_ID=" + modelsResult.getInt("MODEL_INTERNAL_ID"));
                    }
                    else if (isDefault == 1 && isInGroups(groupsToDelete,groupId)){
                        batchList.add("UPDATE " + MODELS_TABLE + " SET IS_DEFAULT=0 WHERE "
                                + "MODEL_INTERNAL_ID=" + modelsResult.getInt("MODEL_INTERNAL_ID"));
                    }
                }
                // Clean up
                modelsResult.close();
                modelListStatement.close();
                if (mySQL)
                    this.execute("UNLOCK TABLES");
            }            
        }
        
        /**
         * Helper method for preparing the SQL Statement to get the list of sources for 
         * updating. The sources group references will be updated according to the new
         * set of groups.
         * @param batchList the list of sql batch commands to be added to. 
         * @throws SQLException
         * @throws AdeException 
         */
        private void updateGroupSources(List<String> batchList, List<Group> currentGroups) throws SQLException, AdeException{
            PreparedStatement sourceListStatement = prepareStatement(
                    "SELECT SOURCES.SOURCE_INTERNAL_ID, SOURCES.SOURCE_ID FROM " +
                    SOURCES_TABLE + " INNER JOIN " + MANAGED_SYSTEMS_TABLE +
                    " ON SOURCES.SOURCE_INTERNAL_ID=MANAGED_SYSTEMS.SOURCE_INTERNAL_ID WHERE " +
                    "UPPER(" + MANAGED_SYSTEMS_TABLE + ".OPERATING_SYSTEM)='LINUX' ", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet sourcesResult = sourceListStatement.executeQuery();
            updateGroupSources(currentGroups, sourcesResult, batchList);
            sourcesResult.close();
            sourceListStatement.close();
        }

        /**
         * Checks to see if the group id is contained in the list of groups.
         * @param groups the list of groups to be checked against.
         * @param groupId the internal id of the group.
         * @return true if the group is contained in the list of groups.
         */
        private boolean isInGroups(List<Group> groups, int groupId){
            for (Group group : groups){
                if (group != null && (group.getUid() == groupId)){
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks to see if the group id is contained in the list of groups and if the id belongs
         * to the UNASSIGNED group.
         * @param groups the list of groups to be checked against.
         * @param groupId the internal id of the group.
         * @return true if the group is contained in the list of groups and is the UNASSIGNED group.
         */
        private boolean unassignedIsInGroups(List<Group> groups, int groupId){
            for (Group group : groups){
                if (group != null && (group.getUid() == groupId) && 
                        (group.getName().equalsIgnoreCase(UNASSIGNED_GROUP))){
                    return true;
                }
            }
            return false;
        }


        /**
         * Accessor to retrieve the IllegalArgumentException if one was thrown. 
         * This can only be non-null if the atomic transaction returned false.
         * @return the IllegalArgumentException if one was thrown. This can only be non-null if 
         *      the atomic transaction returned false.
         */
        public IllegalArgumentException getIllegalArgEx() {
            return illegalArgEx;
        }            
    }    

}
