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
import java.util.Set;

import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.data.SourceMetaData;
import org.openmainframe.ade.exceptions.AdeException;

/** An interface for manipulating datastore sources */
public interface IDataStoreSources {

    /** 
     * Adds the given source, or finds it in the database
     * If it doesn't exist a new internal id is allocated. 
     * @return Source object representing the new/existing source.
     * @throws AdeException
     */
    ISource getOrAddSource(String sourceId) throws AdeException;

    /** 
     * Does nothing if the given source exists in the database.
     * Otherwise it adds it (allocating a new internal id),
     * and set its analysis group.
     * @throws AdeException
     */
    void addSourceAndAnalysisGroup(String sourceId, int analysisGroup) throws AdeException;

    /** Finds source in the database and returns a Source object.
     * 
     * @return Source Object representing this source or null if not found
     * @throws AdeException
     */
    ISource getSource(String sourceId) throws AdeException;

    /** Deletes given source from the database with all it's associated data. */
    void deleteSource(ISource source) throws AdeException;

    /** Get a list of all sources in the database */
    Collection<ISource> getAllSources() throws AdeException;

    /** @return Meta data associated with a given source */
    SourceMetaData getSourceMetaData(ISource source) throws AdeException;

    /** Stores in the database the given meta data to the given source
     * The source is expected to already  exist in the database
     *  */
    void setSourceMetaData(ISource source, SourceMetaData sourceMetaData) throws AdeException;

    /** @return whether the given source exists in the database */
    boolean hasSource(String sourceId) throws AdeException;

    /**
     * @param source 
     *    the source to find it's analysis group
     * @return 
     *    The analysis group as it appears in a local cache,
     *    reflecting part of the 'SOURCES' table in the db
     * @throws AdeException
     *    on SQL related  exceptions
     */
    String getAnalysisGroup(String source) throws AdeException;

    /**
     * @param source
     *    the source to set it's analysis group
     * @param Analysis group internal ids of those sources that will be trained together and 
     * will use the same model for analysis. Note, in the unassigned case, this will be the unassigned group
     * id which is NOT the group internal id.
     * @throws AdeException
     *    If the input pair collides with a pre-existing
     *    pair, or on SQL related exceptions.
     */
    void setAnalysisGroup(String source, int analysisGroup) throws AdeException;

    /**
     * This method sets the analysisGroup for the given source to that which is input.
     * 
     * @param  source          The entity that produces log messages                        
     * @param  analysisGroup   This is the analysis group internal id that is being requested to be set 
     * for the input source. An analysis group contains sources that will be trained together 
     * and will use the same model for analysis.
     * @throws AdeException Throws a AdeException
     */
    void resetAnalysisGroup(String source, int analysisGroup) throws AdeException;

    /**
     * @return
     *    A {@link Set} of all possible analysis groups
     * @throws AdeException
     *    on SQL related exceptions
     */
    Set<Integer> getAllAnalysisGroups() throws AdeException;

    /**
     * This method gets all the sources that are part of the input analysis group.
     * @param analysisGroup the group internal id of those sources that will be trained together.
     * @return set of sourceIDs (a string representation of a source or entity that produces log message).
     * @throws AdeException
     */
    Set<ISource> getSourcesForAnalysisGroup(int analysisGroup) throws AdeException;

}
