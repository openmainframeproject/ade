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
package org.openmainframe.ade.models;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.dataStore.IDataStore;
import org.openmainframe.ade.dataStore.IDataStoreModels;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.utils.IStructuredOutputWriter;
import org.openmainframe.ade.utils.patches.Version;

/**
 * Interface for meta data object for a training model. Each model has its meta data stored
 * in the {@link IDataStore}. The model itself is stored on the filesystem. The name
 * of the file that holds the model is a part of the model meta data and links between the
 * model record in the database and the actual model. 
 */
public interface IModelMetaData extends Serializable {

    /**
     * @return full pathname to the model file this metadata refers to.
     */
    File getModelFileName();

    /**
     * @return the Ade version used to create the model this metadata refers to.
     */
    Version getAdeVersion();

    /**
     * Returns the internal datastore ID for the model this metadata refers to. This
     * ID can be later used to directly access a training model.
     * @return the internal datastore ID for the model this metadata refers to.
     * @see IDataStoreModels#loadModel(int)
     */
    Integer getModelInternalId();

    /**
     * @return 
     * The start time of the first period used for training this model
     * If there are no periods, then the first message time is taken
     */
    Date getStartTime();

    /**
     * @return 
     * The end time of the last period used for training this model
     * If there are no periods, then the last message time is taken
     */
    Date getEndTime();

    /**
     * @return 
     * The end time as requested by the user.  If no end time is given, returns null;
     */
    Date getRequestedEndTime();

    /**
     * @return 
     * The start time as requested by the user.  If no start time is given, returns null;
     */
    Date getRequestedStartTime();

    /**
     * Returns the date the model was trained at.
     * @return the date this model was trained at.
     */
    Date getCreationDate();
    
    /**
     * Returns the group name for the group internal id.
     * @return
     */
    String getGroupName();

    /**  
     * Sets the start time of the first period used for training this model
     */
    void setStartTime(Date startTime);

    /**  
     * Sets the end time of the last period used for training this model
     */
    void setEndTime(Date endTime);

    /**
     * Returns the {@link SourceGroup} associated with this model. A model may be associated either with a
     * {@link ISource} or with a {@link SourceGroup} 
     * @return the {@link SourceGroup} identifier associated with this model or null if the model is associated with a 
     * {@link SourceGroup} 
     */
    int getSourceGroupId();

    /**
     * Sets the filename of the file that stores this model.
     * @param fileName
     */
    void setExternalFileName(File fileName);

    /**
     * Sets an internal id for this model.
     * Note: the internal-id is obtained after storing a model in the database.
     */
    void setModelInternalId(int id);

    /**
     * get a collection of periods that where excluded from the training of this model.
     */
    Collection<IPeriod> getExcludedPeriods();

    /**
     * get a collection of periods that where included in the training of this model.
     * Note: This is NOT the actual list of periods the model was trained on. Rather,
     * these are the periods of sources CURRENTLY belonging to the model's analysis 
     * group, CURRENTLY in the DB contained between the model's startTime and 
     * endTime that are not CURRENTLY excluded.
     * @throws AdeException 
     */
    Collection<IPeriod> getIncludedPeriods() throws AdeException;

    String[] getCommandLineArguments();

    void outputMetadata(IStructuredOutputWriter out) throws AdeException;

    /**
     * Mark the provided {@link IPeriod} as one excluded from training
     * @param period excluded from training
     */
    void excludePeriod(IPeriod period);

}
