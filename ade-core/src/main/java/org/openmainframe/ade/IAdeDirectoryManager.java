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
package org.openmainframe.ade;

import java.io.File;

import org.openmainframe.ade.exceptions.AdeUsageException;

/**
 * manages creation and access to the Ade output directories.
 *
 */
public interface IAdeDirectoryManager {

    /** Returns the root directory */
    File getOutputHome();

    /** Path for analysis adhoc (processing of files) results 
     * @throws AdeUsageException */
    File getTracePath() throws AdeUsageException;

    /**
     * Path for event log analysis results
     * @return
     */
    File getAnalysisHome();

    /**
     * Path for training model results
     * @return
     */
    File getModelHome();

    /**
     * Path for temp files
     * @return
     */
    File getTempHome();

    /**
     * Path for statistics files
     * @return
     */
    File getStatsHome();

}