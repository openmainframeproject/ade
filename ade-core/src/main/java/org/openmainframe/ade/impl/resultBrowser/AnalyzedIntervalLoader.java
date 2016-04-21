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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IBasicInterval;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.actions.AnalyzedIntervalImpl;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.impl.dbUtils.QueryStatementExecuter;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.utils.patches.Version;

public final class AnalyzedIntervalLoader {
    
    private AnalyzedIntervalLoader() {
        // Private constructor to hide the implicit public one.        
    }

    public static List<IAnalyzedInterval> loadAnalyzedIntervalsByPeriod(int periodInternalId, FramingFlowType framingFlowType, Connection con) throws AdeException {
        final ByPeriod bp = new ByPeriod(periodInternalId, framingFlowType, con);
        bp.executeQuery();
        Collections.sort(bp.m_result, new IBasicInterval.ByStartTimeComparator());
        return bp.m_result;
    }

    private static class ByPeriod extends QueryStatementExecuter {

        private FramingFlowType m_framingFlowType;

        private ArrayList<IAnalyzedInterval> m_result = new ArrayList<IAnalyzedInterval>();

        public ByPeriod(int periodInternalId, FramingFlowType framingFlowType, Connection con) {
            super("select START_TIME, INTERVAL_SCORE, INTERVAL_SERIAL_NUM, NUM_UNIQUE_MESSAGE_IDS, "
                    + "ADE_VERSION, MODEL_INTERNAL_ID from " + SQL.ANALYSIS_RESULTS + " inner join "
                    + SQL.PERIOD_SUMMARIES + " on " + SQL.ANALYSIS_RESULTS + ".PERIOD_INTERNAL_ID="
                    + SQL.PERIOD_SUMMARIES + ".PERIOD_INTERNAL_ID where " + SQL.ANALYSIS_RESULTS
                    + ".PERIOD_INTERNAL_ID=" + periodInternalId + " and " + SQL.PERIOD_SUMMARIES
                    + ".SUMMARY_TYPE_INTERNAL_ID=" + framingFlowType.getDatabaseId(), con);
            m_framingFlowType = framingFlowType;
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            int pos = 1;
            final long startDate = rs.getLong(pos++);
            final double score = rs.getDouble(pos++);
            final int serialNum = rs.getInt(pos++);
            final int uniqueMessageIds = rs.getInt(pos++);
            final Version adeVersion = Version.parse(rs.getString(pos++));
            final int modelIntervalId = rs.getInt(pos++);
            final long endDate = startDate + m_framingFlowType.getDuration();
            final IAnalyzedInterval ai = new AnalyzedIntervalImpl(serialNum, startDate, endDate, score, uniqueMessageIds, modelIntervalId, adeVersion);

            m_result.add(ai);
        }

    }
}
