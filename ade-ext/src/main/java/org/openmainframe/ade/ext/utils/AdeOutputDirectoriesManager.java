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
package org.openmainframe.ade.ext.utils;

import java.io.File;

import org.openmainframe.ade.exceptions.AdeException;

/** Class for defining Ade output directories based on one root directory */
public class AdeOutputDirectoriesManager {

    private File m_outputHome;
    private File m_modelsPath;
    private File m_continuousOutputPath;
    private File m_anaAdhocOutputPath;

    /** Construct the manager given the root directory */
    public AdeOutputDirectoriesManager(String outputPath) throws AdeException {
        m_outputHome = new File(outputPath);
        m_modelsPath = new File(m_outputHome, "models");
        m_continuousOutputPath = new File(m_outputHome, "continuous");
        m_anaAdhocOutputPath = new File(m_outputHome, "analysis_adhoc");

        ExtFileUtils.createDir(m_modelsPath);
        ExtFileUtils.createDir(m_continuousOutputPath);
        ExtFileUtils.createDir(m_anaAdhocOutputPath);
    }

    /** Returns the root directory */
    public final File getOutputHome() {
        return m_outputHome;
    }

    /** Path for model xml files */
    public final File getModelsPath() {
        return m_modelsPath;
    }

    /** Path for analysis continuous results */
    public final File getContinousOutputPath() {
        return m_continuousOutputPath;
    }

    /** Path for analysis adhoc (processing of files) results */
    public final File getAnalysisAdhocOutputPath() {
        return m_anaAdhocOutputPath;
    }
}
