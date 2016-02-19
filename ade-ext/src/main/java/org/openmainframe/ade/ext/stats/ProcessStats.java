/*
 
    Copyright IBM Corp. 2013, 2016
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
package org.openmainframe.ade.ext.stats;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.ext.AdeExt;
import org.openmainframe.ade.ext.utils.ExtFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin class for locating the appropriate output directory and file
 * name for periodic statistics recording.
 *
 * Since a periodic statistics object is not necessarily related 1:1
 * with a managed system, never the less we assume that it can be
 * meaningfully related 1:1 with an OS Process.  If this is not the
 * case, then define and use an alternative to this class.
 *
 * The stats file name associated with this object will have the form:
 *	            PID.<NN>.STARTED.<HHmmss_MMddyyyy>
 * for the value "NN" assigned to the current JVM process on the platform
 * (this will be the JVM process' PID, if Linux), and for the timestamp
 * generated in the usual way from the <clockStartTime> instance var
 * initialized in super().  Note that this time is not the Process'
 * start time, but rather a close approximation to the start of periodic
 * statistics gathering.
 *
 * This stats file will be placed in the directory defined by property adeext.statsRootDir
 *
 */

public abstract class ProcessStats extends PeriodicStats {

    protected static final String FILENAME_FORMAT_STRING = "PID.%s.STARTED.%s";
    protected static final String FILE_DT_FORMAT_STRING = "HHmmss_MMddyyyy";
    //* e.g. of componentName, "reader"
    protected String componentName; 
    private static final Logger logger = LoggerFactory.getLogger(ProcessStats.class);

    /**
     * self-explanatory CTOR
     * @param who
     */
    public ProcessStats(String who) {
        componentName = who;
    } 

    /**
     * Implements the abstract method of the parent, who has no idea
     * how to generate an appropriate filename, nor where to place it.
     *
     * @throws AdeException if the stats file could not be created
     */
    @Override
    protected final void setStatsFilePath() throws AdeException {

        logger.trace("setStatsFilePath() -> entry");
        final String path = String.format("%s/%s", getComponentStatsRoot(), getStatsFileName());
        final File F = new File(path);
        try {
            F.createNewFile();
            logger.info("setStatsFilePath() created new statistics file: " + F.getAbsolutePath());
        } catch (Throwable t) {
            final String msg = String.format("Process Stats unable to create new file (%s) ", path);
            logger.error(msg);
            throw new AdeInternalException(msg, t);
        }

        if (!F.canWrite()) {
            final String msg = String.format("Process Stats unable to write to file (%s) ", path);
            logger.error(msg);
            throw new AdeInternalException(msg);
        }

        statsFilePath = F.getAbsolutePath();
        logger.trace(String.format("setStatsFilePath() <- exit ( %s )", statsFilePath));

    } 

    /**
     * Method to produce the name String to be used for stats recording
     * for this particular instance.  The name will have the form:
     *      PID.<NN>.STARTED.<HHmmss_MMddyyyy>
     * and note that we assume that Pid uniqueness at any given point in
     * time, together with timestamp uniqueness for stat recording start
     * will be sufficient to guarantee filename uniqueness.
     *
     * @return path String as described above
     * @throws AdeInternalException
     */
    private String getStatsFileName() throws AdeInternalException {

        final SimpleDateFormat S = new SimpleDateFormat(FILE_DT_FORMAT_STRING);
        return String.format(FILENAME_FORMAT_STRING, getProcessName(), S.format(clockStartTime.getTime()));

    } 

    /**
     * Method to side-effect the creation of the appropriate root
     * dir for recording stats, if necessary, and return its path.
     *
     * @return path String as described above
     * @throws AdeException if the directory location could not be retrieved
     */
    private synchronized String getStatsRoot() throws AdeException {
        final String statsRoot = AdeExt.getAdeExt().getConfigProperties().getStatsRootDir();
        final File F = new File(statsRoot);
        if (!F.exists()) {
            logger.info("ProcessStats.getStatsRoot() creating: " + F.getAbsolutePath());
            createDirectory(F);
        }
        return F.getAbsolutePath();
    } 

    /**
     * Method to side-effect the creation of the appropriately-rooted
     * subdir for recording stats, if necessary, and return its path.
     * This subdir is determined by the componentName specified by
     * our child class.
     *
     * @return path String as described above
     * @throws AdeException if the directory location could not be retrieved
     */
    private synchronized String getComponentStatsRoot() throws AdeException {

        final String rootPath = getStatsRoot();
        final File F = new File(String.format("%s/%s", rootPath, componentName));
        if (!F.exists()) {
            logger.info("ProcessStats.getStatsRoot() creating: " + F.getAbsolutePath());
            createDirectory(F);
        }
        return F.getAbsolutePath();
    } 

    /**
     * This method uses the language-supplied means of producing
     * a process name which is either a true Linux PID value in
     * printable form, or on other platforms a credible pseudo-PID.
     *
     * @return printable <PID> value String as described above
     */
    private String getProcessName() {
        final String mgmtName = ManagementFactory.getRuntimeMXBean().getName();
        final String[] halves = mgmtName.split("@");
        return halves[0];
    } 

    /**
     * self-explanatory
     *
     * @param F
     * @throws AdeInternalException
     */
    private void createDirectory(File F) throws AdeInternalException {
        try {
            ExtFileUtils.createDir(F);
        } catch (Throwable t) {
            final String msg = String.format("createDirectory(%s) - Unexpected throwable: %s",
                    F.getAbsolutePath(), t.getMessage());
            logger.error(msg, t);
            throw new AdeInternalException(msg, t);
        }

    } 

} 
