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
package org.openmainframe.ade.impl.resultBrowser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.impl.dbUtils.QueryPreparedStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.TableGeneralUtils;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.DateTimeUtils;

public final class PeriodIdFinder {
    
    private PeriodIdFinder() {
        // Private constructor to hide the implicit public one.
    }

    static int getPeriodInternalIdByStartTime(int sourceInternalId, Date startTime, FramingFlowType framingFlowType, Connection con) throws AdeException {
        final FindByStart f = new FindByStart(sourceInternalId, startTime, framingFlowType, con);
        f.executeQuery();
        return f.m_periodInternalId;
    }

    private static class FindByStart extends QueryPreparedStatementExecuter {

        private Date m_start;
        int m_sourceInternalId;
        FramingFlowType m_framingFlowType;
        int m_periodInternalId = -1;

        FindByStart(int sourceInternalId, Date start, FramingFlowType framingFlowType, Connection con) {
            super("select " + SQL.PERIODS + ".PERIOD_INTERNAL_ID from "
                    + SQL.PERIODS + " join " + SQL.PERIOD_SUMMARIES + " on "
                    + SQL.PERIODS + ".PERIOD_INTERNAL_ID=" + SQL.PERIOD_SUMMARIES
                    + ".PERIOD_INTERNAL_ID where SOURCE_INTERNAL_ID=? and "
                    + "START_TIME=? and " + SQL.PERIOD_SUMMARIES + ".SUMMARY_TYPE_INTERNAL_ID=?", con);
            m_sourceInternalId = sourceInternalId;
            m_start = start;
            m_framingFlowType = framingFlowType;
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            int pos = 1;
            stmt.setInt(pos++, m_sourceInternalId);
            TableGeneralUtils.setPreparedStatementTimestamp(stmt, pos++, m_start);
            stmt.setInt(pos++, m_framingFlowType.getDatabaseId());
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            int pos = 1;
            if (m_periodInternalId >= 0) {
                throw new AdeInternalException("More than one period with the same start date " + DateTimeUtils.timestampToHumanDateAndTimeAde(m_start.getTime()));
            }
            m_periodInternalId = rs.getInt(pos++);
        }

    }

}
