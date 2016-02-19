/*
 
    Copyright IBM Corp. 2011, 2016
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
package org.openmainframe.ade.dataStore;

import java.util.Collection;
import java.util.Date;

import org.openmainframe.ade.data.IConfigurationData;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.flow.IAdeIterator;
import org.openmainframe.ade.impl.data.PeriodImpl;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;

/** An interface for manipulating datastore periods */
public interface IDataStorePeriods {

    /**
     * Deletes given period from the database with all it's associated data.
     * @param period the period to delete
     */
    void deletePeriod(IPeriod period) throws AdeException;

    /**
     * for testing only
     */
    void updatePeriodMetaData(IPeriod period) throws AdeException;

    /**
     * Retrieves a list if {@link IPeriod} objects from the datastore.
     * @param source The source these periods are associated with
     * @param startTime Excludes periods that start prior to this date.
     * @param endTime Excludes periods that end after this date. 
     * @return a collection of periods for the given source that are fully included within time range.
     * @throws AdeException if failed to access datastore..
     */
    Collection<IPeriod> getAllPeriods(ISource source, Date minTime, Date maxTime) throws AdeException;

    /** Returns an iterator that iterates over intervals of given period and framerType */
    IAdeIterator<IInterval> getPeriodIntervals(IPeriod period, FramingFlowType framerType) throws AdeException;

    /** Returns an iterator that iterates over intervals of given period and framerType, with verbose control.*/
    IAdeIterator<IInterval> getPeriodIntervals(IPeriod period, FramingFlowType framerType, boolean verbose) throws AdeException;

    /** Returns an iterator that iterates over configurations of given periods */
    IAdeIterator<IConfigurationData> getPeriodConfigurations(Collection<PeriodImpl> periods);

    /** Reads the data associated with the given period and stores it in a file in JSon format
     * Data includes summary data and analysis results
     * Used for importing/exporting period data from/to the datastore.
     * @param period The period to be read
     * @param fileName The name of the file to be created
     * @throws AdeException
     */
    void exportPeriodToJsonFile(IPeriod period, String fileName) throws AdeException;

    /** Reads period data from given file and stores it in the datastore.
     * Writing in the datastore overwrites previous data if it existed.
     * @param fileName Name of file to be read
     * @throws AdeException
     */
    void importPeriodFromJsonFile(String fileName) throws AdeException;

    /** Returns a collection of all msg-ids in database 
     * @throws AdeException */
    Collection<String> getAllMessageIds() throws AdeException;

}
