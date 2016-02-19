/*
 
    Copyright IBM Corp. 2015, 2016
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
package org.openmainframe.ade.ext.output;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.ext.utils.EXT_TABLES_SQL;
import org.openmainframe.ade.ext.xml.v2.types.ExtLimitedModelIndicator;
import org.openmainframe.ade.impl.data.PeriodImpl;
import org.openmainframe.ade.impl.dataStore.DatastorePeriodAndSerialNumFinder;
import org.openmainframe.ade.impl.dbUtils.DmlPreparedStatementExecuter;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.output.AnalyzedIntervalDbStorer;

public class ExtendedAnalyzedIntervalDbStorer extends AnalyzedIntervalDbStorer {

    /**
     * Override the super class with new period finder.
     */
    @Override
    public void setupSourceAndFlowType(ISource source, FramingFlowType framingFlowType) throws AdeException {
        super.setupSourceAndFlowType(source, framingFlowType);
        m_framingFlowType = framingFlowType;
        m_periodFinder = new DatastorePeriodAndSerialNumFinder(
                m_source, framingFlowType, XMLUtil.getXMLHardenedDurationInMillis(framingFlowType));
    }

    /**
     * Determine the index for the interval.
     */
    @Override
    protected int getIntervalIndex(IAnalyzedInterval analyzedInterval) throws AdeException {
        /* Note: cannot use periodFinder.getLastSerialNum() to get the serial number for partial interval before
         * Ade terminate.  This last interval will have interval start line up to the Analysis Window Length.
         * And, interval end points to the timestamp of last log message processed.
         *
         * periodFinder.getLastSerialNum() will based on the current interval start to determine the serial.  Since XML
         * Hardened interval is different than the analysis Windows, we need to use the interval end time instead.
         */
        int index = ExtOutputFilenameGenerator.getIntervalSerialNumber(
                analyzedInterval.getIntervalEndTime(), m_framingFlowType);
        return index;
    }

    @Override
    public void incomingObject(IAnalyzedInterval interval) throws AdeException, AdeFlowException {
        super.incomingObject(interval);
        int serialNum = getIntervalIndex(interval);
        if (m_intervals[serialNum]) {
            new UpdateAnalyzedExtInterval(m_cachedPeriod, serialNum, interval).execute();
        } else {
            new InsertAnalyzedExtInterval(m_cachedPeriod, serialNum, interval).execute();
        }
        m_intervals[serialNum] = true;
    }

    protected abstract class AnalyzedExtIntervalBase extends DmlPreparedStatementExecuter{
        //If a new column needs to be added in Analysis Results Ade Ext table, update this class.
        int m_periodId;
        IAnalyzedInterval m_interval;
        protected int m_serialNum;
        NewAndNeverSeenBeforeMessages m_messages;
        ExtLimitedModelIndicator m_modelQualityIndicator;

        public AnalyzedExtIntervalBase(String sqlString) throws AdeException{
            super(sqlString);
            m_modelQualityIndicator = ExtLimitedModelIndicator.Unknown;

        }

        protected ExtLimitedModelIndicator getModelQualityIndicator(IAnalyzedInterval interval) throws AdeException{
            /*At some point will need to check if limited model is supported
            Need to add version in the Analysis_results_adeExt table.*/
            Integer lastKnownModelInternalID = interval.getModelInternalId();
            XMLMetaDataRetriever xmlMetaData = new XMLMetaDataRetriever();
            xmlMetaData.retrieveXMLMetaData(lastKnownModelInternalID, true, m_framingFlowType.getDuration());
            return xmlMetaData.getLimitedModelIndicator();
        }
        
        @Override
        protected abstract void setParameters(PreparedStatement stmt) throws SQLException, AdeException;

    }

    protected class InsertAnalyzedExtInterval extends AnalyzedExtIntervalBase{

        public InsertAnalyzedExtInterval(PeriodImpl period, int serialNum, IAnalyzedInterval interval) throws AdeException        {
            super("INSERT INTO " + EXT_TABLES_SQL.ANALYSIS_RESULTS_ADEEXT +
                    "(PERIOD_INTERNAL_ID, INTERVAL_SERIAL_NUM, " +
                    "NUM_NEW_MESSAGES, NUM_NEVER_SEEN_BEFORE_MESSAGES, LIMITED_MODEL )" +
                    " VALUES(?,?,?,?,?)");
            m_periodId = period.getInternalId();
            m_interval = interval;
            m_serialNum = serialNum;
            m_modelQualityIndicator = getModelQualityIndicator(m_interval);            
            m_messages = NewAndNeverSeenBeforeMessagesUtils.processAnalyzedInterval(interval);
       
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException, AdeException{
            int pos = 1;
            stmt.setInt(pos++, m_periodId);
            stmt.setInt(pos++, m_serialNum);
            stmt.setInt(pos++, m_messages.getNumNewMessages());
            stmt.setInt(pos++, m_messages.getNumNeverSeenBeforeMessages());
            stmt.setString(pos++, m_modelQualityIndicator.toString());
        }

    }

    protected class UpdateAnalyzedExtInterval extends AnalyzedExtIntervalBase{

        public UpdateAnalyzedExtInterval(PeriodImpl period, int serialNum, IAnalyzedInterval interval) throws AdeException        {
            super("UPDATE " + EXT_TABLES_SQL.ANALYSIS_RESULTS_ADEEXT +" SET"
                    +" NUM_NEW_MESSAGES=?, NUM_NEVER_SEEN_BEFORE_MESSAGES=?, LIMITED_MODEL=?"
                    +" WHERE PERIOD_INTERNAL_ID=? and INTERVAL_SERIAL_NUM=?");
            m_periodId = period.getInternalId();
            m_interval = interval;
            m_serialNum = serialNum;
            m_modelQualityIndicator = getModelQualityIndicator(m_interval);            
            m_messages = NewAndNeverSeenBeforeMessagesUtils.processAnalyzedInterval(interval);        
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException, AdeException{
            int pos = 1;
            stmt.setInt(pos++, m_messages.getNumNewMessages());
            stmt.setInt(pos++, m_messages.getNumNeverSeenBeforeMessages());
            stmt.setString(pos++, m_modelQualityIndicator.toString());
            stmt.setInt(pos++, m_periodId);
            stmt.setInt(pos++, m_serialNum);
        }

    }

}
