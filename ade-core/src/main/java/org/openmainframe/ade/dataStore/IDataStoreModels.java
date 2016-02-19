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
package org.openmainframe.ade.dataStore;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.models.IModelMetaData;

/** An interface for manipulating datastore's models */
public interface IDataStoreModels<T> {

    /**
     * Load the default model unassociated with an analysis group. Loads the
     * database entry marked as default, and then loads the model file specified
     * in this entry.
     * 
     * @throws AdeException
     * 	if no default, or if file is not found.
     */
    T loadDefaultModel() throws AdeException;

    /**
     * Load the default model of the given analysis group. Loads the database entry
     * marked as default, and then loads the model file specified in this entry.
     * 
     * @param analysisGroup
     * 	the analysis group for this model
     * @return the default model, or null if no default model exists in the DB
     * @throws AdeException
     * 	if file is not found.
     */
    T loadDefaultModel(String analysisGroup) throws AdeException;

    /**
     * Stores a model in database and on file system using the default models output path
     * 
     * @param model
     *	The model to be stored
     * @param setAsDefault
     *	Whether this model will be the default model
     * @return the model internal id
     * @throws AdeException
     */
    int storeModel(T model, boolean setAsDefault) throws AdeException;

    /**
     * Load model from datastore. Loads the database entry with the given id, and
     * then loads the model file specified in this entry.
     * 
     * @param modelInternalId
     *	Specifies the model internal id to load
     * @throws AdeException
     *	if there is no model with the given ID exists in the datastore,
     *	or if file is not found
     */
    T loadModel(int modelInternalId) throws AdeException;

    /**
     * Load just the model metadata. Accesses only database and not the filesystem
     * 
     * @param modelInternalId
     *	Specifies the model internal id to load
     * @throws AdeException
     *	if there is no model with the given ID exists in the datastore
     */
    IModelMetaData loadModelMetaData(int modelInternalId) throws AdeException;

    /**
     * Returns a list {@link IModelMetaData} objects describing all previously
     * stored models
     * 
     * @return meta data for all models in db
     * @throws AdeException
     *	if failed to access datastore..
     */
    List<IModelMetaData> getModelList() throws AdeException;

    /**
     * Returns a list {@link IModelMetaData} objects describing previously stored
     * models for a given analysis group
     * 
     * @param analysisGroup
     *	The analysis-group with which the models are associated
     * @param startTime
     *	Excludes models trained over data prior to this date. Specify null
     *	for no limit
     * @param endTime
     *	Excludes models trained over data following this date. Specify
     *	null for no limit
     * @return meta data for models matching the specified analysis group and times
     * @throws AdeException
     *	if failed to access datastore..
     * @return List of ModelMetaData objects
     * @throws AdeException
     */
    List<IModelMetaData> getModelListByAnalysisGroup(String analysisGroup,
            Date startTime, Date endTime) throws AdeException;

    /**
     * Deletes the given model from the database, and optionally its filesystem
     * file as well
     * 
     * @param modelInternalId
     *	Id of model to be deleted
     * @param deleteFromFileSystem
     *	determines whether corresponding file should be deleted as well.
     * @throws AdeException
     */
    void deleteModel(int modelInternalId, boolean deleteFromFileSystem)
            throws AdeException;

    /**
     * Sets the given model as the default model for this analysis group
     * 
     * @throws AdeException
     *	if this model doesn't exist or if it is not associated with an
     *	analysis group
     */
    void setAsDefault(int modelInternalId) throws AdeException;

    /**
     *  Turns off default flag for all currently default models.
     *  NOTE: this means no models can now be used in analyze, unless you train more models. 
     * */
    void setAllModelsNotDefault() throws AdeException;

    /**
     * Imports a model stored on the filesystem but without a database record.
     * This creates a record for it in the database, containing the name of the
     * given file.
     * 
     * @param file
     *	The file to be imported.
     * @param setAsDefault
     *	Whether this event log model should be the default one.
     * @return model internal id
     * @throws AdeException
     */
    int importModelFromFile(File file, boolean setAsDefault)
            throws AdeException;

    /**
     * 
     * @return null if no deafult model, meta data for model if it exist.
     */
    IModelMetaData loadDefaultModelMetaData() throws AdeException;

    /**
     * 
     * @param analysisGroup
     * @return null if no deafult model, meta data for model if it exist.
     */
    IModelMetaData loadDefaultModelMetaData(String analysisGroup) throws AdeException;

    T readModelFromFile(File file) throws AdeException, AdeUsageException;

    Object getModelDataObject(String name);

    void setModelDataObject(String name, Object data);

    /**
     * Useful for saving the objects.  just remember that some of the objects may not be serelizable, so use "java.io.Serializable.class.isInstance(OBJ)" to test prior to saving.
     * @return
     */
    Set<String> getModelDataObjectKeys();

}
