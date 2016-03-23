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
package org.openmainframe.ade.ext.output;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.data.IPeriod.PeriodMode;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.data.PeriodUtils;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.openmainframe.ade.output.OutputFilenameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class ExtOutputFilenameGenerator extends OutputFilenameGenerator {
    /**
     * SLF4J logger
     */
    static Logger s_logger = LoggerFactory.getLogger(ExtOutputFilenameGenerator.class);

    private static long s_intervalLength = Integer.MAX_VALUE;

    public static void setIntervalLength(long intervalLength) throws AdeUsageException {
        if (s_intervalLength != Integer.MAX_VALUE && s_intervalLength != intervalLength) {
            /* We support only single interval length for each JVM instance during analyze. */
            throw new AdeUsageException("Trying to change intervalLength in ExtOutputFilenameGenerator from "
                    + s_intervalLength + " to " + intervalLength);
        }

        s_logger.warn("Setting interval length to " + intervalLength + " in ExtOutputFilenameGenerator.");

        s_intervalLength = intervalLength;
    }

    private static final String EXT_INTERVAL_RELATIVE_PATH = "intervals";

    /**
     * The names for 1.8, 3.1 and 3.1L files 
     */
    public static final String EXT_INTERVAL_XML_EXTENSION = ".xml";
    public static final String EXT_INTERVAL_ZIP_EXTENSION = ".gz";
    public static final String EXT_INTERVAL_XML_FILE_NAME_BASE_FORMAT = EXT_INTERVAL_RELATIVE_PATH + "/interval_%s";

    protected static final String EXT_INTERVAL_XML_FILE_NAME_FORMAT = EXT_INTERVAL_XML_FILE_NAME_BASE_FORMAT + EXT_INTERVAL_XML_EXTENSION;

    public static final String EXT_INTERVAL_XML_V2_FILE_NAME_FORMAT = EXT_INTERVAL_XML_FILE_NAME_FORMAT;

    public static final String EXT_INTERVAL_XML_V2F_FILE_NAME_FORMAT = EXT_INTERVAL_XML_FILE_NAME_BASE_FORMAT + "_debug" + EXT_INTERVAL_XML_EXTENSION;

    public static final String EXT_INTERVAL_XML_V2F_FILE_NAME_FORMAT_IN_ZIP = EXT_INTERVAL_XML_FILE_NAME_BASE_FORMAT + "_debug" + EXT_INTERVAL_XML_EXTENSION + EXT_INTERVAL_ZIP_EXTENSION;

    public static final String EXT_INTERVAL_XML_V1_FILE_NAME_FORMAT = EXT_INTERVAL_XML_FILE_NAME_BASE_FORMAT + "_V1" + EXT_INTERVAL_XML_EXTENSION;

    /**
     * Date format
     */
    protected static final String EXT_DAY_FORMAT = "yyyyMMdd";

    public static String getDayFormat() {
        return EXT_DAY_FORMAT;
    }

    protected static final ThreadLocal<DateFormat> s_dayFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return DateTimeUtils.getNewGmtSimpleDateFormat(getDayFormat());
        }
    };

    protected final DateFormat s_dateFormat = DateTimeUtils.getNewGmtSimpleDateFormat(getDateFormat());

    /**
     * the input timezone
     */
    private static DateTimeZone s_inputDateTimeZone;

    public static DateTimeZone getInputTimeZone() throws AdeException {
        if (s_inputDateTimeZone == null) {
            final TimeZone inputTimeZone = Ade.getAde().getConfigProperties().getInputTimeZone();
            s_inputDateTimeZone = DateTimeZone.forOffsetMillis(inputTimeZone.getRawOffset());
        }

        return s_inputDateTimeZone;
    }

    /**
     * The output timezone
     */
    private static DateTimeZone s_outputDateTimeZone;

    public static DateTimeZone getOutputTimeZone() throws AdeException {
        if (s_outputDateTimeZone == null) {
            final TimeZone outputTimeZone = Ade.getAde().getConfigProperties().getOutputTimeZone();
            s_outputDateTimeZone = DateTimeZone.forOffsetMillis(outputTimeZone.getRawOffset());
        }

        return s_outputDateTimeZone;
    }

    /**
     * A public method to get the Interval Serial Number given the end time.
     * @param intervalStartTime
     * @param intervalEndTime
     * @return
     * @throws AdeException
     */
    public static int getIntervalSerialNumber(long intervalEndTime, FramingFlowType framingFlowType) throws AdeException {
        final long xmlHardenedLength = XMLUtil.getXMLHardenedDurationInMillis(framingFlowType);
        final long intervalEndTimeMillis = intervalEndTime;
        final DateTime dateTime = new DateTime(intervalEndTimeMillis).withZone(getOutputTimeZone());
        final DateTime startOfDay = dateTime.minusMillis(1).withTimeAtStartOfDay();

        final long diffInMillis = dateTime.getMillis() - startOfDay.getMillis();
        int intervalSerialNum = (int) (diffInMillis / xmlHardenedLength) - 1;
        if (((int) diffInMillis % xmlHardenedLength) != 0) {
            intervalSerialNum++;
        }

        return intervalSerialNum;
    }

    /**
     * Return the Period Directory Format.
     * 
     * This method is over ridden, so that it pickup the static variable from
     * this class instead of the parent.
     */
    @Override
    protected final DateFormat getPeriodFormat() throws AdeInternalException, AdeException {
        final PeriodMode periodMode = Ade.getAde().getConfigProperties().getPeriodMode();
        switch (periodMode) {
            case YEARLY:
            case MONTHLY:
            case WEEKLY:
            case DAILY:
                return s_dayFormat.get();
            case HOURLY:
                return s_dateFormat;
            default:
                throw new AdeInternalException("Unknown period mode:" + periodMode);
        }
    }

    /**
     * Return the Interval XML file object that will be used by the UI.
     * 
     * @param sourceId
     * @param intervalStart The date denoting the beginning of the interval
     * @return A {@link File} object for the input source and start {@link Date}
     * @throws AdeException
     */
    @Override
    public final File getIntervalXmlFile(IAnalyzedInterval analyzedInterval, FramingFlowType framingFlowType) throws AdeException {
        return getIntervalXmlFile(analyzedInterval, EXT_INTERVAL_XML_FILE_NAME_FORMAT, framingFlowType);
    }

    /**
     * Return the Interval XML file for 3.1 XML format.
     * 
     * @param sourceId
     * @param intervalStart The date denoting the beginning of the interval
     * @return A {@link File} object for the input source and start {@link Date}
     * @throws AdeException
     */
    public final File getIntervalXmlV2FFile(IAnalyzedInterval analyzedInterval, FramingFlowType framingFlowType) throws AdeException {
        return getIntervalXmlFile(analyzedInterval, EXT_INTERVAL_XML_V2F_FILE_NAME_FORMAT, framingFlowType);
    }

    /**
     * Return the Interval XML file for 3.1 XML format.
     * 
     * @param sourceId
     * @param intervalStart The date denoting the beginning of the interval
     * @return A {@link File} object for the input source and start {@link Date}
     * @throws AdeException
     */
    public final File getIntervalXmlV2FFileInZIP(IAnalyzedInterval analyzedInterval, FramingFlowType framingFlowType) throws AdeException {
        return getIntervalXmlFile(analyzedInterval, EXT_INTERVAL_XML_V2F_FILE_NAME_FORMAT_IN_ZIP, framingFlowType);
    }

    /**
     * Return the Interval XML file for 3.1 Lite XML format.
     * 
     * @param sourceId
     * @param intervalStart The date denoting the beginning of the interval
     * @return A {@link File} object for the input source and start {@link Date}
     * @throws AdeException
     */
    public final File getIntervalXmlV2File(IAnalyzedInterval analyzedInterval, FramingFlowType framingFlowType) throws AdeException {
        return getIntervalXmlFile(analyzedInterval, EXT_INTERVAL_XML_V2_FILE_NAME_FORMAT, framingFlowType);
    }

    /**
     * Return the Interval XML file for 1.8 XML format.
     * 
     * @param sourceId
     * @param intervalStart The date denoting the beginning of the interval
     * @return A {@link File} object for the input source and start {@link Date}
     * @throws AdeException
     */
    public final File getIntervalXmlV1File(IAnalyzedInterval analyzedInterval, FramingFlowType framingFlowType) throws AdeException {
        return getIntervalXmlFile(analyzedInterval, EXT_INTERVAL_XML_FILE_NAME_FORMAT, framingFlowType);
    }

    /**
     * Given a pattern, return the XML filename based on the AnalyzedInterval
     * 
     * @param analyzedInterval
     * @param pattern
     * @return
     * @throws AdeException
     */
    private File getIntervalXmlFile(IAnalyzedInterval analyzedInterval, 
            String pattern, FramingFlowType framingFlowType) throws AdeException {
        final String sourceId = analyzedInterval.getInterval().getSource().getSourceId();

        final int intervalSerialNum = getIntervalSerialNumber(analyzedInterval.getIntervalEndTime(), framingFlowType);

        final File periodDir = getPeriodDir(analyzedInterval.getInterval().getSource(),
                PeriodUtils.getContainingPeriodStart(new Date(analyzedInterval.getIntervalEndTime() - 1L)));

        return getIntervalXmlFile(sourceId, intervalSerialNum, periodDir, pattern);
    }

    /**
     * Return the Interval XML Storage Directory.  
     * 
     */
    @Override
    public final File getIntervalXmlStorageDir(IAnalyzedInterval analyzedInterval) throws AdeException {
        final String sourceId = analyzedInterval.getInterval().getSource().getSourceId();
        final Long intervalEnd = analyzedInterval.getIntervalEndTime() - 1L;
        return getIntervalXmlStorageDir(sourceId, new Date(intervalEnd));
    }

    /**
     * @param sourceId
     * @param intervalStart The date denoting the beginning of the interval
     * @param customAnalysisOutputPath The {@link File} representing the custom
     * path used for analysis output files.  
     * @return A {@link File} object for the input source and start {@link Date}
     * @throws AdeException
     */
    @Override
    public final File getIntervalXmlFile(String sourceId, Date intervalStart, 
            File customAnalysisOutputPath, FramingFlowType framingFlowType) throws AdeException {
        final File periodDir = getIntervalXmlStorageDir(sourceId, intervalStart, customAnalysisOutputPath);

        final File intervalXml = new File(periodDir, getIntervalXmlFileRelativeToIndex2(intervalStart, framingFlowType));
        return intervalXml;
    }

    /* This method intended to replace getIntervalXmlFileRelativeToIndex().*/
    public final String getIntervalXmlFileRelativeToIndex2(Date intervalStart, 
            FramingFlowType framingFlowType) throws AdeException {
        setIntervalLength(framingFlowType.getDuration());
        final long intervalEnd = intervalStart.getTime() + s_intervalLength;
        final int serial = getIntervalSerialNumber(intervalEnd, framingFlowType);
        return getIntervalXmlFileRelativeToIndex(serial);
    }

    private File getIntervalXmlFile(String sourceId, int serialNumber, File periodDir, String pattern) throws AdeException {
        final File intervalXml = new File(periodDir, getIntervalXmlFileRelativeToIndex(serialNumber, pattern));
        return intervalXml;
    }

    @Override
    public final String getIntervalXmlFileRelativeToIndex(IAnalyzedInterval ai, FramingFlowType framingFlowType) throws AdeException {
        final int intervalSerialNum = getIntervalSerialNumber(ai.getIntervalEndTime(), framingFlowType);

        return getIntervalXmlFileRelativeToIndex(intervalSerialNum);
    }

    /**
     * Return the Interval XML filename that would be used by the GUI.
     * 
     * @param serialNumber
     * @return
     */
    public final String getIntervalXmlFileRelativeToIndex(int serialNumber) {
        return getIntervalXmlFileRelativeToIndex(serialNumber, EXT_INTERVAL_XML_FILE_NAME_FORMAT);
    }

    /**
     * Return the Interval XML filename given a pattern
     * @param serialNumber
     * @param pattern
     * @return
     */
    private String getIntervalXmlFileRelativeToIndex(int serialNumber, String pattern) {
        return String.format(pattern, serialNumber);
    }

    /**
     * 
     * @param sourceId
     * @param dateInInterval The date within the interval
     * @return a {@link File} object for the period directory that should hold the
     * interval of the given source and start time
     * @throws AdeException 
     */
    public final File getIntervalXmlStorageDir(String sourceId, Date dateInInterval, File customAnalysisOutputPath)
            throws AdeInternalException, AdeException {
        final File periodDir = getPeriodDir(sourceId, PeriodUtils.getContainingPeriodStart(dateInInterval));
        final File intevalXmlStorageDir = new File(periodDir, EXT_INTERVAL_RELATIVE_PATH);
        return intevalXmlStorageDir;
    }

    @Override
    public final File getPeriodDir(ISource source, Date periodStartDate) throws AdeException {
        return getPeriodDir(source.getSourceId(), periodStartDate);
    }

    /**
     * Return the length of the interval
     * @return s_intervalLength the length of the interval
     */
    public long getIntervalLength() {
        return s_intervalLength;
    }
    
    protected final File getPeriodDir(String sourceId, Date periodStartDate) throws AdeException {
        final DateFormat periodFormat = getPeriodFormat();

        final File logAnalysisDir = getMarshallDirBySource(sourceId, null);
        final File periodDir = new File(logAnalysisDir, periodFormat.format(periodStartDate));
        return periodDir;
    }
}
