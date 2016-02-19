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
package org.openmainframe.ade.output;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.data.IPeriod.PeriodMode;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.PeriodUtils;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.openmainframe.ade.models.IModelMetaData;

/**
 * This class acts both as an interface, and a default implementation. It is
 * here where the file names for Ade's output is decided (the name, not 
 * the hierarchy - that is partly hard coded in {@link IAnalysisResultMarshaller})
 * the user may inherit this class and override some of the methods in order to
 * customize the file names, then the user class can be specified in the setup
 * properties file with the "ade.outputFilenameGenerator" key. 
 * Please notice that file names are created based on directories provided by the {@link IAdeDirectoryManager}.
 */
public class OutputFilenameGenerator {

    protected static final String DAY_FORMAT = "yyyy-MM-dd";
    protected static final String TIME_FORMAT = "HH-mm";

    public static String getDayFormat() {
        return DAY_FORMAT;
    }

    public static String getTimeFormat() {
        return TIME_FORMAT;
    }

    protected static final String DATE_FORMAT = getDayFormat() + "-" + getTimeFormat();

    public final String getDateFormat() {
        return DATE_FORMAT;
    }

    public static final String INTREVAL_XML_FILE_NAME_FORMAT = "interval_%s.xml";
    protected static final String CONFIG_XML_FILE_NAME_FORMAT = "configuration_%s.xml";

    protected static final String LOG_ANALYSIS_DIR_NAME = "logAnalysis";
    protected static final String CONFIG_ANALYSIS_DIR_NAME = "configurationAnalysis";

    protected final static String PERIOD_INDEX_NAME = "index.xml";

    public static String getPeriodIndexName() {
        return PERIOD_INDEX_NAME;
    }

    protected final DateFormat s_dayFormat = DateTimeUtils.getNewGmtSimpleDateFormat(getDayFormat());
    protected final DateFormat s_dateFormat = DateTimeUtils.getNewGmtSimpleDateFormat(getDateFormat());

    /**
     * @param metaData with which to create the filename ({@link IModelMetaData#
     * getModelInternalId()} is already set).
     * @return The filename for the event log model
     */
    public final String generateLogModelFilename(IModelMetaData metaData) {
        return String.format("event_log_model_%d.bin", metaData.getModelInternalId());
    }

    /**
     * @param metaData with which to create the filename ({@link IModelMetaData#
     * getModelInternalId()} is already set).
     * @return The filename for the configuration model
     */
    public final String generateConfigModelFilename(IModelMetaData metaData) {
        return String.format("config_model_%d.json", metaData.getModelInternalId());
    }

    /**
     * 
     * @param source
     * @param periodStartDate
     * @return a {@link file} object for the directory that will hold the 
     * period index (and most likely the interval files as well)
     * @throws AdeException 
     * @throws AdeInternalException 
     */
    public File getPeriodDir(ISource source, Date periodStartDate) throws AdeException {
        return getIntervalXmlStorageDir(source.getSourceId(), periodStartDate, null);
    }

    public final File getPeriodIndexFilename(ISource source, Date periodStartDate) throws AdeException {
        return new File(getPeriodDir(source, periodStartDate), getPeriodIndexName());
    }

    protected DateFormat getPeriodFormat()
            throws AdeInternalException, AdeException {
        final PeriodMode periodMode = Ade.getAde().getConfigProperties().getPeriodMode();
        switch (periodMode) {
            case YEARLY:
            case MONTHLY:
            case WEEKLY:
            case DAILY:
                return s_dayFormat;
            case HOURLY:
                return s_dateFormat;
            default:
                throw new AdeInternalException("Unknown period mode:" + periodMode);
        }
    }

    /**
     * @param sourceId
     * @param analyzedInterval The analyzed interval.
     * @return A {@link File} object for the input source and start {@link Date}
     * @throws AdeException
     */
    public File getIntervalXmlFile(IAnalyzedInterval analyzedInterval, FramingFlowType framingFlowType) throws AdeException {
        final String sourceId = analyzedInterval.getInterval().getSource().getSourceId();
        return getIntervalXmlFile(sourceId, new Date(analyzedInterval.getIntervalStartTime()), null, framingFlowType);
    }

    public String getIntervalXmlFileRelativeToIndex(IAnalyzedInterval ai, FramingFlowType framingFlowType) throws AdeException {
        return getIntervalXmlFileRelativeToIndex(new Date(ai.getIntervalStartTime()));
    }

    /**
     * @param intervalStart The date denoting the beginning of the interval
     * @return The name of the matching interval file name, relevant to the index file.
     */
    public String getIntervalXmlFileRelativeToIndex(Date intervalStart) {
        return String.format(INTREVAL_XML_FILE_NAME_FORMAT, s_dateFormat.format(intervalStart));
    }

    /**
     * @param sourceId
     * @param intervalStart The date denoting the beginning of the interval
     * @param customAnalysisOutputPath The {@link File} representing the custom
     * path used for analysis output files.  
     * @return A {@link File} object for the input source and start {@link Date}
     * @throws AdeException
     */
    public File getIntervalXmlFile(String sourceId, Date intervalStart, File customAnalysisOutputPath,
            FramingFlowType framingFlowType) throws AdeException {
        final File periodDir = getIntervalXmlStorageDir(sourceId, intervalStart, customAnalysisOutputPath);

        final File intervalXml = new File(periodDir, getIntervalXmlFileRelativeToIndex(intervalStart));
        return intervalXml;
    }

    protected File getIntervalXmlStorageDir(IAnalyzedInterval analyzedInterval)
            throws AdeException {
        final String sourceId = analyzedInterval.getInterval().getSource().getSourceId();
        return getIntervalXmlStorageDir(sourceId, new Date(analyzedInterval.getInterval().getIntervalStartTime()));
    }

    /**
     * 
     * @param sourceId
     * @param dateInInterval The date denoting the a date that falls within the interval.
     * @return a {@link File} object for the period directory that should hold the
     * interval of the give source and start time
     * @throws AdeException 
     */
    public final File getIntervalXmlStorageDir(String sourceId, Date dateInInterval) throws AdeException, AdeException {
        return getIntervalXmlStorageDir(sourceId, dateInInterval, null);
    }

    /**
     * 
     * @param sourceId
     * @param dateInInterval The date within the interval
     * @return a {@link File} object for the period directory that should hold the
     * interval of the given source and start time
     * @throws AdeException 
     */
    public File getIntervalXmlStorageDir(String sourceId, Date dateInInterval,
            File customAnalysisOutputPath) throws AdeInternalException,
                    AdeException {
        final DateFormat periodFormat = getPeriodFormat();

        final File logAnalysisDir = new File(getMarshallDirBySource(sourceId, customAnalysisOutputPath), LOG_ANALYSIS_DIR_NAME);
        final File periodDir = new File(logAnalysisDir, periodFormat.format(PeriodUtils.getContainingPeriodStart(dateInInterval)));
        return periodDir;
    }

    /************************** Configuration outputs ***********************************/

    /**
     * @param sourceId
     * @param configurationTime The date denoting the time the configuration was taken
     * @param customAnalysisOutputPath The {@link File} representing the custom
     * path used for analysis output files.  
    
     * @return A {@link File} object for the input source and {@link Date}
     * @throws AdeException
     */
    public final File getConfigXmlFile(String sourceId, Date configurationTime) throws AdeException {
        return getConfigXmlFile(sourceId, configurationTime, null);
    }

    static protected File getConfigAnalysisDir(String sourceId, File customAnalysisOutputPath) throws AdeException {
        return new File(getMarshallDirBySource(sourceId, customAnalysisOutputPath), CONFIG_ANALYSIS_DIR_NAME);
    }

    /**
     * @param sourceId
     * @param configurationTime The date denoting the time the configuration was taken
     * @param customAnalysisOutputPath The {@link File} representing the custom
     * path used for analysis output files.  
     * @return A {@link File} object for the input source and {@link Date}
     * @throws AdeException
     */
    public final File getConfigXmlFile(String sourceId, Date configurationTime, File customAnalysisOutputPath) throws AdeException {
        final File configXml = new File(getConfigAnalysisDir(sourceId, customAnalysisOutputPath), String.format(CONFIG_XML_FILE_NAME_FORMAT, s_dateFormat.format(configurationTime)));
        return configXml;
    }

    static protected File getMarshallDirBySource(String sourceId, File customAnalysisOutputPath) throws AdeException {
        if (customAnalysisOutputPath == null) {
            customAnalysisOutputPath = Ade.getAde().getDirectoryManager().getAnalysisHome();
        }
        return new File(customAnalysisOutputPath, sourceId);
    }

    /**
     * @param sourceId the source we want the directory for
     * @return the output directory for this source (not guaranteed to exist!)
     * @throws AdeException
     */
    public final File getRawSourceDir(String sourceId) throws AdeException {
        return getRawSourceDir(sourceId, Ade.getAde().getDirectoryManager().getAnalysisHome());
    }

    /**
     * @param sourceId the source we want the directory for
     * @param customAnalysisOutputPath The {@link File} representing the custom
     * path used for analysis output files.  
     * @return the output directory for this source (not guaranteed to exist!)
     * @throws AdeException
     */
    public final File getRawSourceDir(String sourceId, File customAnalysisOutputPath) throws AdeException {
        return getMarshallDirBySource(sourceId, customAnalysisOutputPath);
    }

}
