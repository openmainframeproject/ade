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
package org.openmainframe.ade.impl.flow.modules;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageSummary;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.flow.IStreamTarget;
import org.openmainframe.ade.impl.data.PeriodImpl;
import org.openmainframe.ade.impl.data.PeriodSummary;
import org.openmainframe.ade.impl.dataStore.DataStorePeriodSummaries;
import org.openmainframe.ade.impl.dataStore.DatastorePeriodAndSerialNumFinder;
import org.openmainframe.ade.impl.dataStore.SQL;
import org.openmainframe.ade.impl.dbUtils.DmlPreparedStatementChunkExecuter;
import org.openmainframe.ade.impl.dbUtils.DmlPreparedStatementExecuter;
import org.openmainframe.ade.impl.dbUtils.TableGeneralUtils;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.GeneralUtils;
import org.openmainframe.ade.summary.SummarizationProperties;

/**
 * Uploads intervals to the database. Assumes all intervals are generated based
 * on the same {@link IntervalFramer}, and thus share the same
 * {@link FramingFlowType}.
 */
public class IntervalDbUploader implements IStreamTarget<IInterval> {

    private PeriodSummary m_curPeriodSummary;
    private FramingFlowType m_framingFlowType;
    private DatastorePeriodAndSerialNumFinder m_psFinder;
    private DataStorePeriodSummaries m_dsPeriodSummaries;

    /**
     * Constructor for IntervalDbUploader.
     * 
     * @param source The source for this interval.
     * @param framingFlowType Contains the properties for the interval time frame.
     */
    public IntervalDbUploader(ISource source, FramingFlowType framingFlowType) throws AdeException {
        m_framingFlowType = framingFlowType;
        m_dsPeriodSummaries = AdeInternal.getAdeImpl().getDataStore().periodSummaries();
        m_psFinder = new DatastorePeriodAndSerialNumFinder(source, m_framingFlowType);
    }

    @Override
    public void beginOfStream() throws AdeException {
        // Not doing anything here

    }

    @Override
    public final void incomingObject(IInterval interval) throws AdeException {
        m_psFinder.setIntervalStartTime(interval.getIntervalStartTime());

        final PeriodImpl period = m_psFinder.getLastPeriod();
        final int num = m_psFinder.getLastSerialNum();

        setPeriodSummary(period);

        // Delete the entry (by serial number) from the MESSAGE_SUMMARIES table before updating it.
        deleteMessageSummaries(num);
        // Update the INTERVALS table.
        TableGeneralUtils.startTransaction();
        TableGeneralUtils.lockTableShare(SQL.PERIOD_SUMMARIES);
        TableGeneralUtils.lockTableExclusive(SQL.INTERVALS);
        // Delete the entry (by serial number) from the INTERVAL table before updating it.
        deleteIntervalRecord(num);
        storeIntervalRecord(num, interval);
        TableGeneralUtils.endTransaction();

        // This will update the MESSAGE_SUMMARIES table.
        storeMessageSummaries(num, interval);
    }

    private void storeIntervalRecord(int serialNum, IInterval interval) throws AdeException {
        new IntervalRecordInserter(m_curPeriodSummary.getInternalId(), interval, serialNum).execute();
    }

    private void storeMessageSummaries(int serialNum, IInterval interval) throws AdeException {
        final MessageSummariesInserter msi = new MessageSummariesInserter(m_curPeriodSummary.getInternalId(), interval,
                serialNum);

        // Update the MESSAGE_SUMMARIES table.
        TableGeneralUtils.startTransaction();
        TableGeneralUtils.lockTableShare(SQL.PERIOD_SUMMARIES);
        TableGeneralUtils.lockTableExclusive(SQL.MESSAGE_SUMMARIES);
        msi.execute();
        TableGeneralUtils.endTransaction();
    }

    // Given the input serial number, delete that record from the INTERVALS table.
    private void deleteIntervalRecord(int serialNum) throws AdeException {
        final String sql = String.format(
                "delete from %s where period_summary_internal_id=%d and interval_serial_num=%d", SQL.INTERVALS,
                m_curPeriodSummary.getInternalId(), serialNum);
        ConnectionWrapper.executeDmlDefaultCon(sql);
    }

    // Given the input serial number, delete that record from the MESSAGE_SUMMARIES table.
    private void deleteMessageSummaries(int serialNum) throws AdeException {
        final String sql = String.format(
                "delete from %s where period_summary_internal_id=%d and interval_serial_num=%d", SQL.MESSAGE_SUMMARIES,
                m_curPeriodSummary.getInternalId(), serialNum);
        ConnectionWrapper.executeDmlDefaultCon(sql);
    }

    private void setPeriodSummary(PeriodImpl period) throws AdeException {
        // Skip processing if the input period has the same ID as m_curPeriodSummary.
        if (m_curPeriodSummary != null && m_curPeriodSummary.getPeriod().getInternalId() == period.getInternalId()) {
            return;
        }

        m_curPeriodSummary = m_dsPeriodSummaries.getOrAddPeriodSummary(period, m_framingFlowType);
    }

    @Override
    public void endOfStream() throws AdeException {
        // Not doing anything here

    }

    /**
     * Class used to insert into the INTERVALS table.
     */
    private static class IntervalRecordInserter extends DmlPreparedStatementExecuter {
        private int m_periodSummaryId;
        private int m_serialNum;
        private IInterval m_interval;

        /**
         * Constructor for IntervalRecordInserter.
         * 
         * @param periodSummaryId The ID for the PeriodSummary.
         * @param interval The Interval that contains the message summaries.
         * @param serialNum The serial number of the interval.
         */
        public IntervalRecordInserter(int periodSummaryId, IInterval interval, int serialNum) {
            super("INSERT INTO " + SQL.INTERVALS
                    + "(PERIOD_SUMMARY_INTERNAL_ID, INTERVAL_SERIAL_NUM, NUM_UNIQUE_MESSAGE_IDS,"
                    + "INTERVAL_START_TIME,CLASSIFICATION_INTERNAL_ID,ADE_VERSION,COVERAGE_FACTOR) "
                    + " VALUES(?,?,?,?,?,?,?)");
            m_periodSummaryId = periodSummaryId;
            m_serialNum = serialNum;
            m_interval = interval;
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException, AdeException {
            // Set the parameters for the passed in PreparedStatement.
            int pos = 0;
            stmt.setInt(++pos, m_periodSummaryId); // 1, PERIOD_SUMMARY_INTERNAL_ID
            stmt.setInt(++pos, m_serialNum); // 2, INTERVAL_SERIAL_NUM
            stmt.setInt(++pos, m_interval.getNumUniqueMessages()); // 3, NUM_UNIQUE_MESSAGE_IDS
            stmt.setLong(++pos, m_interval.getIntervalStartTime()); // 4, INTERVAL_START_TIME
            stmt.setInt(++pos, m_interval.getIntervalClassification().getClassID()); // 5, CLASSIFICATION_INTERNAL_ID
            stmt.setString(++pos, m_interval.getAdeVersion().toString()); // 6, ADE_VERSION
            stmt.setDouble(++pos, m_interval.getCoverageFactor()); // 7, COVERAGE_FACTOR
        }
    }

    /**
     * Class used to insert into the MESSAGE_SUMMARIES table.
     */
    private static class MessageSummariesInserter extends DmlPreparedStatementChunkExecuter {
        private int m_periodSummaryIid;
        private IInterval m_interval;
        private int m_serialNum;

        /**
         * Constructor for MessageSummariesInserter.
         * 
         * @param periodSummaryIid The ID for the PeriodSummary.
         * @param interval The Interval that contains the message summaries.
         * @param serialNum The serial number of the interval.
         */
        public MessageSummariesInserter(int periodSummaryIid, IInterval interval, int serialNum) {
            super("INSERT INTO " + SQL.MESSAGE_SUMMARIES
                    + "(PERIOD_SUMMARY_INTERNAL_ID, INTERVAL_SERIAL_NUM, MESSAGE_INTERNAL_ID, "
                    + "SEVERITY, NUM_MESSAGES, TEXT_SUMMARY, TEXT_SAMPLE, CRITICAL_WORDS_SCORE,"
                    + "ENCODED_TIME_VECTOR)" + " VALUES(?,?,?,?,?,?,?,?,?)");
            m_periodSummaryIid = periodSummaryIid;
            m_interval = interval;
            m_serialNum = serialNum;
        }

        @Override
        protected void setAllParameters(PreparedStatement stmt) throws SQLException, AdeException {
            // Set the parameters in the PreparedStatement for each message summary in the interval. 
            for (IMessageSummary msgSummary : m_interval.getMessageSummaries()) {
                int pos = 0;

                stmt.setInt(++pos, m_periodSummaryIid); // 1, PERIOD_SUMMARY_INTERNAL_ID
                stmt.setInt(++pos, m_serialNum); // 2, INTERVAL_SERIAL_NUM
                stmt.setInt(++pos, msgSummary.getMessageInternalId()); // 3, MESSAGE_INTERNAL_ID
                stmt.setInt(++pos, msgSummary.getSeverity().ordinal()); // 4, SEVERITY
                stmt.setInt(++pos, msgSummary.getNumMessageInstances()); // 5, NUM_MESSAGES
                TableGeneralUtils.setPreparedStatementString(stmt, ++pos, // 6, TEXT_SUMMARY
                        GeneralUtils.cleanString(msgSummary.getTextSummary()), SQL.MAX_LEN_TEXT);
                TableGeneralUtils.setPreparedStatementString(stmt, ++pos, // 7, TEXT_SAMPLE
                        GeneralUtils.cleanString(msgSummary.getTextSample()), SQL.MAX_LEN_TEXT);
                stmt.setInt(++pos, msgSummary.getCriticalWordsScore()); // 8, CRITICAL_WORDS_SCORE
                stmt.setString(++pos, SummarizationProperties.encodeTimeLine(msgSummary.getTimeLine())); // 9, ENCODED_TIME_VECTOR

                addBatch();
            }

        }

    }

}
