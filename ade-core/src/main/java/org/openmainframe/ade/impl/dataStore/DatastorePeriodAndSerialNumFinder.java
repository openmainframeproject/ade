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

import java.util.Date;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.PeriodAndSerialNumFinder;
import org.openmainframe.ade.impl.data.PeriodImpl;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;

/** A class for converting timestamps to (Period,serial_number) pairs.
 * 
 * <p>
 * The class is optimized for repeated calls with timestamps of the same period, in which
 * case it doesn't require database access.
 *
 */
public class DatastorePeriodAndSerialNumFinder {

    private PeriodAndSerialNumFinder m_periodAndSerialNumFinder;

    private ISource m_source;
    private DataStorePeriodsImpl m_dsPeriods;
    private PeriodImpl m_curPeriod;

    /**
     *  Create a finder for the given source and {@link FramingFlowType}.
     *  
     *  @param source the Source
     *  @param framingFlowType the FramingFlowType.
     */
    public DatastorePeriodAndSerialNumFinder(ISource source, FramingFlowType framingFlowType) throws AdeException {
        m_source = source;
        m_periodAndSerialNumFinder = new PeriodAndSerialNumFinder(framingFlowType);
        m_dsPeriods = AdeInternal.getAdeImpl().getDataStore().periods();
    }
    
    public DatastorePeriodAndSerialNumFinder(ISource source, FramingFlowType framingFlowType, long intervalsInMillis) throws AdeException{
        this(source, framingFlowType);
        m_periodAndSerialNumFinder = new PeriodAndSerialNumFinder(framingFlowType.getDuration(), intervalsInMillis);
    }

    /**
     * Returns the number of intervals in a period.
     * @return number of intervals per period
     */
    public int getIntervalsPerPeriod() {
        return m_periodAndSerialNumFinder.getIntervalsPerPeriod();
    }

    /**
     * Set the finder with a given time-stamp.
     * You may then query this time-stamp's period using getPeriod()
     * and its serial number using getSerialNum()
     * 
     * <p>
     * If the given time-stamp is in the same period as the previous call to setTime(),
     * no database access is generated. Otherwise, if an existing period exists in the
     * database, it is retrieved. Otherwise a new period is added to the database.
     * 
     * @throws AdeException
     */
    public void setIntervalStartTime(long startTime) throws AdeException {
        if (m_periodAndSerialNumFinder.setIntervalStartTime(startTime)) {
            m_curPeriod = m_dsPeriods.getOrAddPeriod(m_source,
                    new Date(m_periodAndSerialNumFinder.getLastPeriodStartTime()),
                    new Date(m_periodAndSerialNumFinder.getLastPeriodEndTime()));
        }
    }

    /** Gets the serial number of an interval whose start time was supplied using last call to setTime().*/
    public int getLastSerialNum() throws AdeInternalException {
        verifySet();
        return m_periodAndSerialNumFinder.getLastSerialNum();
    }

    /** Gets period containing the time-stamp that was supplied using last call to setTime().*/
    public PeriodImpl getLastPeriod() throws AdeInternalException {
        verifySet();
        return m_curPeriod;
    }

    private void verifySet() throws AdeInternalException {
        if (m_curPeriod == null) {
            throw new AdeInternalException("interval not set");
        }
    }

}
