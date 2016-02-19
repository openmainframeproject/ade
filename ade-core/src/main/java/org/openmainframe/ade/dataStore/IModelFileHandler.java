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
import java.io.IOException;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.models.IModel;

public interface IModelFileHandler<T extends IModel> {

    /**
     * Store a model
     * @param model to store
     * @return the file the model was stored to 
     * @throws IOException
     * @throws AdeException 
     */
    File store(T model) throws IOException, AdeException;

    /**
     * Load a model from a file
     * @param modelFile to load the model from
     * @return the loaded model
     * @throws IOException 
     * @throws AdeException 
     */
    T load(File modelFile) throws IOException, AdeException;
}
