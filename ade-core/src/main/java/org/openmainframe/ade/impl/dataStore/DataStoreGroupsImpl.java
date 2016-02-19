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
package org.openmainframe.ade.impl.dataStore;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.DataType;
import org.openmainframe.ade.data.GroupType;
import org.openmainframe.ade.dataStore.IDataStoreGroups;
import org.openmainframe.ade.dataStore.IDataStoreRules;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.dbUtils.PreparedStatementWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStoreGroupsImpl implements IDataStoreGroups {
    
    private static final Logger logger = LoggerFactory.getLogger(DataStoreGroupsImpl.class);
    
    /**
     * The default unassigned group id; NOT the internal id. This is just used to differentiate
     * from group internal ids.
     */
    private final static int UNASSIGNED_ANALYSIS_GROUP_ID = -1;
    
    /**
     * The default group name for unassigned groups.
     */
    private final static String UNASSIGNED_GROUP_NAME = "UNASSIGNED";
    
    /**
     * By default, unassigned groups are of type "MODELGROUPS."
     */
    private final static int UNASSIGNED_GROUP_TYPE = GroupType.MODELGROUPS.getValue();
    
    /**
     * By default, unassigned groups have SYSLOG data.
     */
    private final static int UNASSIGNED_DATA_TYPE = DataType.SYSLOG.getValue();
    
    /**
     * This method returns the group internal id of the unassigned group. First it checks to see if the
     * unassigned group is already in the database. If it is, it returns the internal id otherwise, we add
     * the unassigned rule into the RULES table, retrieve the rule internal id, and uses this id as one of the
     * column values when inserting the analysis group.
     * @param analysisGroup this is the group id. For this particular method it should be the unofficial group id 
     * for unassigned groups.
     * @return the group internal id of the unassigned group.
     */
    @Override
    public int insertUnassignedGroup(int analysisGroup) throws AdeException{
        if (analysisGroup != UNASSIGNED_ANALYSIS_GROUP_ID){
            throw new AdeInternalException("This update is for unassigned analysis groups ONLY");
        }
        int analysisGroupInternalId;
        analysisGroupInternalId = getUnassignedGroupInternalId();
        if (analysisGroupInternalId != 0) return analysisGroupInternalId;
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        ResultSet generatedKey = null;
        PreparedStatement ps = null;
        try{       
            final int ruleId = insertUnassignedRules();
            final int evaluationOrder = getNumOfGroups(cw) + 1;
            PreparedStatementWrapper psw = cw.preparedStatement("INSERT INTO " + SQL.GROUPS + " (GROUP_NAME, "
                    + "GROUP_TYPE, DATA_TYPE, RULE_INTERNAL_ID, EVALUATION_ORDER) "
                    + "VALUES (?,?,?,?,?)", new String[]{"GROUP_INTERNAL_ID"});
            ps = psw.getPreparedStatement();
            int pos = 1;
            ps.setString(pos++, UNASSIGNED_GROUP_NAME);
            ps.setInt(pos++, UNASSIGNED_GROUP_TYPE);
            ps.setInt(pos++, UNASSIGNED_DATA_TYPE);
            ps.setInt(pos++, ruleId);
            ps.setInt(pos++, evaluationOrder);
            ps.execute();
            generatedKey = ps.getGeneratedKeys();
            if (generatedKey.next()) {
                analysisGroupInternalId = generatedKey.getInt(1);
            }
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
            try {
                if (generatedKey != null)
                    generatedKey.close();
            } catch (SQLException e) {
                logger.error("Error encountered closing the ResultSet.", e);
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                logger.error("Error encountered closing the PreparedStatement.", e);
            }
        }
        return analysisGroupInternalId;
    }
    
    /**
     * Retrieves the group internal id for the unassigned group.
     * @return returns the group internal id of the unassigned group. If it hasn't been added yet,
     * then the id column is NULL in the database and it will return a value of 0.
     * @throws AdeException
     */
    private int getUnassignedGroupInternalId() throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        ResultSet rs = null;
        PreparedStatement ps = null;
        int groupId = 0;
        try {
            PreparedStatementWrapper psw = cw.preparedStatement("SELECT GROUP_INTERNAL_ID FROM " + SQL.GROUPS + 
                    " WHERE GROUP_NAME =?");
            ps = psw.getPreparedStatement();
            ps.setString(1,UNASSIGNED_GROUP_NAME);
            rs = psw.executeQuery();
            if (rs.next()){
                groupId = rs.getInt(1);
            }
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                logger.error("Error encountered closing the ResultSet.", e);
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                logger.error("Error encountered closing the PreparedStatement.", e);
            }
        }
        return groupId;
    }
    
    /**
     * This creates a "DataStoreRules" object to manipulate the RULES table in the database.
     * @return Returns the rule internal id of the inserted unassigned rule.
     * @throws AdeException
     */
    private int insertUnassignedRules() throws AdeException{
        final IDataStoreRules dataStoreRules = Ade.getAde().getDataStore().rules();
        return dataStoreRules.insertUnassignedRule();
    }
    
    /**
     * Retrieves the number of groups in the groups table. This number is for calculating
     * the evaluation order of the unassigned group. The unassigned group should always
     * be the last group in the evaluation order.
     * @param cw the ConnectionWrapper to connect to the database.
     * @return the number of groups in the GROUPS table.
     * @throws AdeException
     */
    private int getNumOfGroups(ConnectionWrapper cw) throws AdeException{
        final String sqlStatement = "SELECT COUNT(*) FROM " + SQL.GROUPS;
        return cw.simpleQueries().executeIntQuery(sqlStatement, true);
    }
}
