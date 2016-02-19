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
package org.openmainframe.ade.impl.data;

import java.util.Date;

import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.impl.utils.DateTimeUtils;

/**
 * An implementation of a Period that stores it's members in memory. Changes to
 * the underlying period in the datastore will not be reflected here.
 * @see org.openmainframe.ade.data.IPeriod
 */
public class PeriodImpl implements IPeriod {

    private static final long serialVersionUID = 1L;

    /*
     * The internal id of the underlying Period object
     */
    private int m_periodInternalId = -1;

    /*
     * The source that this period is associated with
     */
    private ISource m_source;

    /*
     * A Date indicating when the Period starts
     */
    private Date m_startTime;

    /*
     * A Date indicating when the Period end
     */
    private Date m_endTime;

    /*
     * Whether this Period should be excluded from training
     */
    private boolean m_excludeFromTraining;

    /*
     * The status of this Period
     */
    private int m_status;

    /*
     * Text that describes this Period
     */
    private String m_comment;

    /*
     * The amount of milliseconds in a second
     */
    private  static final long MILLIS_IN_SECOND = 1000;

    /*
     * The amount of milliseconds in a minute
     */
    private  static final long MILLIS_IN_MINUTE = MILLIS_IN_SECOND * 60;

    /**
     * Constructor to create a PeriodImpl with the attributes provided
     *
     * @param periodInternalId the internal data-store id of the
     *      period that this PeriodImpl describes
     * @param source the Source that this Period is addociated with
     * @param excludeFromTraining a boolean indicating whether this Period should be used
     *      for training. A value of true means it will be excluded, false means it will
     *      be included in the training
     * @param status an integer representing the "status" of the Period
     * @param comment text that describes this Period
     * @param startTime a Date representing when this Period begins
     * @param endTime a Date representing when this period ends
     */
    public PeriodImpl(int periodInternalId, ISource source,
            boolean excludeFromTraining, int status, String comment, Date startTime,
            Date endTime) {

        m_periodInternalId = periodInternalId;
        m_source = source;
        /*
         * Create new Date objects to contain the start and end time to prevent external
         * changes to the Date parameter from propagating into here
         */
        m_startTime = new Date(startTime.getTime());
        m_endTime = new Date(endTime.getTime());
        m_excludeFromTraining = excludeFromTraining;
        m_status = status;
        m_comment = comment;
    }

    @Override
    public final ISource getSource() {
        return m_source;
    }

    @Override
    public final Date getStartTime() {
        /*
         * Create new Date object to contain the start time to prevent external
         * changes to the Date parameter from propagating into here
         */
        return new Date(m_startTime.getTime());
    }

    @Override
    public final Date getEndTime() {
        /*
         * Create new Date object to contain the end time to prevent external
         * changes to the Date parameter from propagating into here
         */
        return new Date(m_endTime.getTime());
    }

    @Override
    public final long getLengthInMillis() {
        return m_endTime.getTime() - m_startTime.getTime();
    }

    @Override
    public final int getLengthInMinutes() {
        return (int) (getLengthInMillis() / MILLIS_IN_MINUTE);
    }

    public final int getInternalId() {
        return m_periodInternalId;
    }

    @Override
    public final String toString() {
        return String.format("Period(%d) %s:[%s-%s]", m_periodInternalId, m_source.toString(),
                DateTimeUtils.timestampToHumanDateAndTimeAde(
                    m_startTime.getTime()),
                        DateTimeUtils.timestampToHumanDateAndTimeAde(m_endTime.getTime()));
    }

    @Override
    public final int getStatus() {
        return m_status;
    }

    @Override
    public final boolean getExcludeFromTraining() {
        return m_excludeFromTraining;
    }

    @Override
    public final String getComment() {
        return m_comment;
    }

    @Override
    public final void setStatus(int status) {
        m_status = status;
    }

    @Override
    public final void setExcludeFromTraining(boolean excludeFromTraining) {
        m_excludeFromTraining = excludeFromTraining;
    }

    @Override
    public final void setComment(String comment) {
        m_comment = comment;
    }
}
