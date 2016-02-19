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

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.utils.FileUtils;

/** Class for defining Ade output directories based on one root directory.*/
public class AdeDirectoriesManagerImpl implements IAdeDirectoryManager {

    private File m_outputHome;
    private File m_traceHome;
    private boolean m_traceHomeCreated;
    private File m_analysisHome;
    private File m_modelHome;
    private File m_tempHome;
    private File m_statsHome;

    /** Construct the manager given the root directory.
     *  @param outputPath The string value of root directory.
     * */
    public AdeDirectoriesManagerImpl(String outputPath) throws AdeException {
        this(outputPath, null, null);
    }

    /** Construct the manager given the root directory.
     *  @param outputPath The string value of an output path
     *  @param analysisOutputPath String value of the analysis output path
     *  @param tempPath String value of a temporary output path
     * */
    public AdeDirectoriesManagerImpl(String outputPath, String analysisOutputPath, 
            String tempPath) throws AdeException {
        m_outputHome = new File(outputPath);
        FileUtils.createDirs(m_outputHome);
        m_traceHome = new File(m_outputHome, "trace");
        m_traceHomeCreated = false;

        if (analysisOutputPath != null && !analysisOutputPath.isEmpty()) {
            m_analysisHome = new File(analysisOutputPath);
        } else {
            m_analysisHome = new File(outputPath, "analysis_adhoc");
        }
        
        FileUtils.createDirs(m_analysisHome);

        m_modelHome = new File(m_outputHome, "models");
        FileUtils.createDirs(m_modelHome);

        if (tempPath != null && !tempPath.isEmpty()) {
            m_tempHome = new File(tempPath);
        } else {
            m_tempHome = new File(m_outputHome, "temp");
        }
        FileUtils.createDirs(m_tempHome);

        m_statsHome = new File(m_outputHome, "stats");
        FileUtils.createDir(m_statsHome);
    }

    /* (non-Javadoc)
     * @see org.openmainframe.ade.AdeDirectoryManager#getOutputHome()
     */
    @Override
    public final File getOutputHome() {
        return m_outputHome;
    }

    /* (non-Javadoc)
     * @see org.openmainframe.ade.AdeDirectoryManager#getTracePath()
     */
    @Override
    public final File getTracePath() throws AdeUsageException {
        if (!m_traceHomeCreated) {
            FileUtils.createDirs(m_traceHome);
            m_traceHomeCreated = true;
        }
        return m_traceHome;
    }

    @Override
    public final File getAnalysisHome() {
        return m_analysisHome;
    }

    @Override
    public final File getModelHome() {
        return m_modelHome;
    }

    @Override
    public final File getTempHome() {
        return m_tempHome;

    }

    @Override
    public final File getStatsHome() {
        return m_statsHome;
    }

}
