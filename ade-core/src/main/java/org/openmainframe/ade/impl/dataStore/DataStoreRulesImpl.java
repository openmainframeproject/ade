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

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.dataStore.IDataStoreRules;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.dbUtils.PreparedStatementWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStoreRulesImpl implements IDataStoreRules {

    private static final Logger logger = LoggerFactory.getLogger(DataStoreRulesImpl.class);
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
     * Inserts a new unassigned rule in the database. Note, we do not have to check and see if there is an 
     * unassigned rule in the RULES table already since we only call this method if we haven't created 
     * an unassigned group yet.
     * @return The rule internal id for the unassigned group rule.
     * @throws AdeException
     */
    @Override
    public int insertUnassignedRule() throws AdeException {
        final ConnectionWrapper cw = new ConnectionWrapper(AdeInternal.getDefaultConnection());
        ResultSet generatedKey = null;
        PreparedStatement ps = null;
        int ruleId = 0;
        try{
            PreparedStatementWrapper psw = cw.preparedStatement("INSERT INTO " + SQL.RULES +
                    " (RULE_NAME, DESCRIPTION, RULE) VALUES (?,?,?)", new String[]{"RULE_INTERNAL_ID"});
            ps = psw.getPreparedStatement();
            int pos = 1;
            ps.setString(pos++, UNASSIGNED_RULE_NAME);
            ps.setString(pos++, UNASSIGNED_RULE_DESCRIPTION);
            ps.setString(pos++, UNASSIGNED_RULE);
            ps.execute();
            generatedKey = ps.getGeneratedKeys();
            if (generatedKey.next()) {
                ruleId = (int) generatedKey.getLong(1);
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
        return ruleId;
    }
    
}
