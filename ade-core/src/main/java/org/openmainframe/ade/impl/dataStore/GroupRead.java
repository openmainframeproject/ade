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

import java.sql.SQLException;
import java.util.List;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.exceptions.AdeException;

/**
 * Class for retrieving and accessing group data from the GROUPS table.
 */
public final class GroupRead {
    
    /**
     * Default value for unassigned groups. This is NOT the internal id.
     */
    private static final int UNASSIGNED_GROUP_ID = -1;
    
    /**
     * Default name for unassigned groups.
     */
    private static final String UNASSIGNED_GROUP_NAME = "UNASSIGNED";

    private GroupRead() {
    	//private constructor
    }
    /**
     * This method returns the analysis group name based on the group internal id. It does
     * a simple query into the database to extract the GROUP_NAME column which contains the
     * name.
     * @param analysisGroupId the group internal id in all cases except for the unassigned case. For the unassigned
     * case we designate it its own id.
     * @return
     * @throws AdeException
     */
    public static String getAnalysisGroupName(int analysisGroupId) throws AdeException{
        if (analysisGroupId == UNASSIGNED_GROUP_ID){
            return UNASSIGNED_GROUP_NAME;
        }
        String res = null;
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        try {
            final String SQLStatement = "SELECT GROUP_NAME FROM " + SQL.GROUPS + 
                " WHERE GROUP_INTERNAL_ID = " + analysisGroupId;
            res = cw.simpleQueries().executeStringQuery(SQLStatement, true);
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        return res;
    }
    
    /**
     * This method returns the internal id based on the group name given by executing
     * a SELECT statement into the GROUPS table.
     * @param groupName the name of the group.
     * @return the group internal id in the groups database table.
     * @throws AdeException
     */
    public static int getAnalysisGroupId(String groupName) throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        List<Integer> res = null;
        try {
            final String SQLStatement = "SELECT GROUP_INTERNAL_ID FROM " + SQL.GROUPS + 
                    " WHERE GROUP_NAME = '" + groupName + "'";
            res = cw.simpleQueries().executeIntListQuery(SQLStatement);
            cw.close();
        } catch (SQLException e) {
            cw.failed(e);
        } finally {
            cw.quietCleanup();
        }
        return res.get(0);
    }

}
