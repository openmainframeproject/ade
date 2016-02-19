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

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A general base class for periodic statistics keeping.
 *
 *  In a normal usage model, some subclass(es) will be keeping actual usage data
 *  and will periodically drive appendStatString(), at which time our notion of
 *  "last recording time" is updated.
 *
 *  The subclass will have created the String which is to be appended to the
 *  associated stats file, using timing information kept by us.
 *
 *  This is assumed to continue indefinitely.  But if, for some reason, it becomes
 *  appropriate to write a final statistic, the eof() should be called, finalizing
 *  the cumulative time measure, so that the last (partial) stat can be realized
 *  and appended.
 *
 *  NOTES:
 *  We rely on the System class' ability to accurately portray nanosecond-based relative
 *  timings in order to determine relative time measures.  All such timings are
 *  *purely* relative ones.
 *
 */
public abstract class PeriodicStats {

    protected static final String DATETIME_FORMAT_STRING = "MM-dd-yyyy HH:mm:ss";
    //* clockStartTime is our only absolute
    protected Date clockStartTime = null; 
    protected String statsFilePath = null;
    private static final String SPACER = "===================================================================================";
    private long initTimeNanos;
    private long lastRecordingNanos;
    private SimpleDateFormat dateFormatter = null;
    private static final Logger logger = LoggerFactory.getLogger(PeriodicStats.class);

    /**
     * self-explanatory CTOR
     */
    public PeriodicStats() {
        reset();
        dateFormatter = new SimpleDateFormat(DATETIME_FORMAT_STRING);
    } 

    /**
     * Method to create a new dedicated stats file for this instance, and to
     * insert the input headerLines as first content to that text file.
     *
     * @param who String which identifies the caller in some way
     * @param headerLines String[] array which should describe the
     *        remaining content of the stats file
     * @throws AdeInternalException
     */
    protected final void initStatistics(String who, String[] headerLines) throws AdeException {

        setStatsFilePath();
        if (statsFilePath == null) {
            throw new AdeInternalException("No Statistics File path was specified");
        }

        appendStatString(SPACER);
        appendStatString(String.format("[%s] %s start (approximate)", currentTimeStamp(), who));
        appendStatString(SPACER);
        for (int i = 0; i < headerLines.length; i++) {
            //* should describe the format of subsequent lines
            appendStatString(headerLines[i]); 
        }
        appendStatString(SPACER);

    } 

    /**
     * To be implemented by a subclass who is better informed than we
     *
     * @throws AdeInternalException
     */
    protected abstract void setStatsFilePath() throws AdeException;

    /**
     * The logical complement of initStatistics().
     *
     * @param who String which identifies the caller in some way
     * @throws AdeInternalException
     */
    protected final void eof(String who) throws AdeInternalException {
        appendStatString(SPACER);
        appendStatString(String.format("[%s] %s end (approximate)", currentTimeStamp(), who));
        appendStatString(SPACER);
    } 

    /**
     * self-explanatory getter
     * @return long nanos time value
     */
    public final long getStatStartTime() {
        return initTimeNanos;
    } 

    /**
     * self-explanatory getter
     * @return long nanos time value
     */
    public final long getDurationNanos() {
        return System.nanoTime() - lastRecordingNanos;
    } 

    /**
     * self-explanatory getter
     * @return double nanos time value
     */
    public final double getDurationSeconds() {
        return nanosToSeconds(getDurationNanos());
    } 

    /**
     * Method to simply renew all time-related instance variables
     */
    protected void reset() {
        initTimeNanos = System.nanoTime();
        lastRecordingNanos = initTimeNanos;
        clockStartTime = new Date();
    } 

    /**
     * Method to append to the stats file corresponding to this instance,
     * and start a new timing start value
     */
    protected final void record(String textLine) throws AdeInternalException {
        appendStatString(textLine);
        lastRecordingNanos = System.nanoTime();
    } 
    
    /**
     * Convenience overload for rounding to the default (2) number of
     * decimal places
     *
     * @param numToRound
     * @return double
     */
    protected final double toRoundedDouble(double numToRound) {
        final int NUM_DECIMAL_PLACES = 2;
        return toRoundedDouble(numToRound, NUM_DECIMAL_PLACES);
    } 

    /**
     * self-explanatory method for rounding to a specified number of
     * decimal places
     *
     * @param numToRound
     * @return double
     */
    protected final double toRoundedDouble(double numToRound, int decimalPlaces) {
        final BigDecimal bd = BigDecimal.valueOf(numToRound).setScale(decimalPlaces, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    } 

    /**
     * self-explanatory method for generating a printable timestamp
     * according to the standard date format string
     *
     * @return String timestamp representation
     */
    protected final String currentTimeStamp() {
        return dateFormatter.format((new Date()).getTime());
    }

    /**
     * self-explanatory method for appending a single line to the text file
     * associated with this instance
     *
     * @param textLine
     * @throws AdeInternalException
     */
    private void appendStatString(String textLine) throws AdeInternalException {

        if (statsFilePath == null) {
            throw new AdeInternalException("No write capability");
        }
        try {
            final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(statsFilePath, true), StandardCharsets.UTF_8)));
            writer.println(textLine);
            writer.close();
        } catch (Throwable t) {
            final String msg = String.format("Unexpected throwable (%s) while appending to: %s. Statistics recording halted.",
                    t.toString(), statsFilePath);
            logger.error(msg, t);
            throw new AdeInternalException(msg, t);
        }

    } 

    /**
     * self-explanatory method for converting a nanos measurement
     * into a best-fit equivalent seconds measurement
     *
     * @param long nanos measurement
     * @return double measurement as described
     */
    private double nanosToSeconds(long nanos) {
        final double NANOS_IN_SECOND = 1.0E09;
        //* rounded to x.nn
        return toRoundedDouble(nanos / NANOS_IN_SECOND); 
    } 
    
    
//* end class
}
