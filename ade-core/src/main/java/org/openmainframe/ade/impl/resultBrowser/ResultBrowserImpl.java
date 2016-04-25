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
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.impl.dbUtils.QueryStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.SpecialSqlQueries;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.resultBrowser.RawSourceMetaData;
import org.openmainframe.ade.resultBrowser.ResultBrowser;

public class ResultBrowserImpl extends ResultBrowser {

    private Connection m_connection;

    public ResultBrowserImpl(Connection con) {
        m_connection = con;
    }

    @Override
    public final List<IAnalyzedInterval> getAnalyzedIntervals(int sourceInternalId, Date date, FramingFlowType framingFlowType) throws AdeException {
        final int periodInternalId = PeriodIdFinder.getPeriodInternalIdByStartTime(sourceInternalId, date, framingFlowType, m_connection);
        if (periodInternalId < 0) {
            return new ArrayList<IAnalyzedInterval>();
        }
        return AnalyzedIntervalLoader.loadAnalyzedIntervalsByPeriod(periodInternalId, framingFlowType, m_connection);
    }

    @Override
    public final Date getLastPeriodForSource(int sourceInternalId) throws AdeException {
        // only retrieve periods with analyzed intervals
        final String sql = "select max(P.start_time) from " + SQL.PERIODS + " as P join " + SQL.ANALYSIS_RESULTS + " as AR on P.PERIOD_INTERNAL_ID=AR.PERIOD_INTERNAL_ID where P.SOURCE_INTERNAL_ID=" + sourceInternalId;
        return SpecialSqlQueries.executeTimestampQuery(sql, m_connection, false);
    }

    @Override
    public final Set<String> getAllAnalyzedSources() throws AdeException {
        final AnalyzedSourcesGetter asg = new AnalyzedSourcesGetter(m_connection);
        asg.executeQuery();
        return asg.m_analyzedSources;
    }

    private static class AnalyzedSourcesGetter extends QueryStatementExecuter {
        private Set<String> m_analyzedSources = new TreeSet<String>();

        private AnalyzedSourcesGetter(Connection conn) {
            super("select " + SQL.SOURCES + ".SOURCE_ID from " + SQL.ANALYSIS_RESULTS 
                    + " join " + SQL.PERIODS + " on " + SQL.ANALYSIS_RESULTS 
                    + ".PERIOD_INTERNAL_ID=" + SQL.PERIODS + ".PERIOD_INTERNAL_ID " 
                    + " join " + SQL.SOURCES + " on " + SQL.PERIODS + ".SOURCE_INTERNAL_ID=" 
                    + SQL.SOURCES + ".SOURCE_INTERNAL_ID", conn);
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            int pos = 1;
            final String source = rs.getString(pos++);
            m_analyzedSources.add(source);
        }

    }

    @Override
    public final RawSourceMetaData getSourceMetaData(String sourceStr) throws AdeException {
        final RawSourceMetaDataFetcher fetcher = new RawSourceMetaDataFetcher(sourceStr);
        fetcher.executeQuery();
        return fetcher.getRawSourceMetaData();
    }

    private class RawSourceMetaDataFetcher extends QueryStatementExecuter {

        private String m_source;
        private RawSourceMetaData m_res = null;

        public RawSourceMetaDataFetcher(String source) {
            super("select SOURCE_INTERNAL_ID, ANALYSIS_GROUP from " + SQL.SOURCES + " where SOURCE_ID='" + source + "'", m_connection);
            m_source = source;
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException, AdeException {
            int i = 1;
            final int internalId = rs.getInt(i++);
            final String analysisGroup = rs.getString(i++);

            m_res = new RawSourceMetaData(internalId, m_source, analysisGroup);
        }

        public RawSourceMetaData getRawSourceMetaData() {
            return m_res;
        }
    }

}
