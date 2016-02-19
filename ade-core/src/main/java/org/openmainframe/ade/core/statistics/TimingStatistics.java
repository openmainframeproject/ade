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
package org.openmainframe.ade.core.statistics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.openmainframe.ade.exceptions.AdeInternalException;

/** A class for obtaining profiling statistics.
 * Usage example:
 * 
 * TimingStatistics.start("myMeasureName");
 * .... some code ...
 * TimingStatistics.end("myMeasureName");
 * 
 * 
 * To obtain results call:
 *  TimingStatistics.printSummary(System.out);
 * Which will print for each measure name:
 *    number of start/end
 *    mean/min/max/std of elapsed time between each start and end pair 
 */
public class TimingStatistics {
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    /** Converts millisecond count to HH:MM:SS.SS where the seconds are reported with 0.01 precision */
    public static String timeToString(long period) {
        period /= 10;
        final long milsecs = period % 100;
        period /= 100;
        final long secs = period % 60;
        period /= 60;
        final long mins = period % 60;
        period /= 60;
        final long hours = period;
        return String.format("%02d:%02d:%02d.%02d", hours, mins, secs, milsecs);
    }

    private static class Measure {

        public String mName;
        public long mSum = 0;
        public long mMax = 0;
        public int mCount = 0;
        int mCurrentlyOpen = 0;
        boolean mUsedInParallel = false;

        public String toTabbedString() {
            String res = timeToString(mSum);
            if (mUsedInParallel) {
                res += "\t<Parallel>";
            } else {
                res += "\t" + timeToString(mCount > 0 ? mSum / mCount : 0);
            }
            res += "\t" + mCount + "\t" + timeToString(mMax);
            return res;
        }

        @Override
        public String toString() {
            String res = "Total ";
            if (mUsedInParallel) {
                res += " <Parallel>";
            } else {
                res += timeToString(mSum);
            }
            res += " mean " + timeToString(mCount > 0 ? mSum / mCount : 0) + " (x " + mCount + " times) max " + timeToString(mMax);
            return res;
        }

        void open() {
            ++mCurrentlyOpen;
            if (mCurrentlyOpen > 1) {
                mUsedInParallel = true;
            }
        }

        void close(long elapsed) {
            --mCurrentlyOpen;
            if (mCurrentlyOpen < 0) {
                mCurrentlyOpen = 0;
            }
            mSum += elapsed;
            mCount++;

            if (elapsed > mMax) {
                mMax = elapsed;
            }
        }
    }

    private HashMap<String, Measure> mMeasures = new HashMap<String, Measure>();
    private HashMap<Pair<Long, String>, Long> mThreadMeasures = new HashMap<Pair<Long, String>, Long>();
    private List<Measure> mOrderedMeasures = new ArrayList<Measure>();
    private Set<String> mDuplicateStarts = new HashSet<String>();
    private Set<String> mEndsWithoutStarts = new HashSet<String>();

    private static TimingStatistics mInstance = new TimingStatistics();

    private static ThreadLocal<List<String>> mPrefixes = new ThreadLocal<List<String>>() {
        @Override
        protected List<String> initialValue() {
            return new ArrayList<String>();
        }
    };

    private static ThreadLocal<String> mFinalPrefix = new ThreadLocal<String>();

    private static HashMap<String, Measure> getMeasures() {
        return mInstance.mMeasures;
    }

    /** Get the statistics report as a String 
     * @throws AdeInternalException if UTF-8 encoding it not supported.*/
    public static String getSummary() throws AdeInternalException {
        return getSummary(null);
    }

    /** Get the statistics report for a specified measure as a String 
     * @throws AdeInternalException if UTF-8 encoding it not supported.*/
    public static String getSummary(String measure) throws AdeInternalException {
        final ByteArrayOutputStream bo = new ByteArrayOutputStream();
        PrintStream out;
        try {
            out = new PrintStream(bo, false, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new AdeInternalException("Unsupported encoding encountered for the PrintStream.", e);
        }
        printSummary(out, measure);
        try {
            return bo.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new AdeInternalException("Unsupported encoding encountered for the ByteArrayOutputStream.", e);
        }
    }

    /** Print the statistics report to specified stream */
    public static void printSummary(PrintStream out) {
        printSummary(out, null);
        printErrorSummary(out);
    }

    /** Print the statistics report to specified stream in UTF-8 format.
     * @throws FileNotFoundException 
     * @throws AdeInternalException if UTF-8 encoding is not supported. */
    public static void printToFile(File outFile) throws FileNotFoundException, AdeInternalException {
        PrintStream out;
        try {
            out = new PrintStream(outFile, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new AdeInternalException("Unsupported encoding encountered.", e);
        }
        printTabbedSummary(out, null);
        printErrorSummary(out);
        out.close();
    }

    private static void printErrorSummary(PrintStream out) {
        if (mInstance.mDuplicateStarts.size() > 0) {
            out.println("ERROR: start eithout end on " + mInstance.mDuplicateStarts);
        }
        if (mInstance.mDuplicateStarts.size() > 0) {
            out.println("ERROR: start eithout end on " + mInstance.mDuplicateStarts);
        }
    }

    /** Print the statistics report to specified stream for a specified measure */
    static public void printTabbedSummary(PrintStream out, String measure) {
        if (measure == null) {
            out.println("Profiling statistics: (hh:mm:ss)");
            out.println("Name\tTotal\tMean\tCount\tMax");
        }

        for (Measure m : getSortedMeasures()) {
            if (measure == null || m.mName.equals(measure)) {
                out.printf(m.mName + "\t" + m.toTabbedString());
            }
            out.println();
        }
    }

    /** Print the statistics report to specified stream for a specified measure */
    static public void printSummary(PrintStream out, String measure) {
        if (measure == null) {
            out.println("Profiling statistics: (hh:mm:ss)");
        }
        for (Measure m : getSortedMeasures()) {
            if (measure == null || m.mName.equals(measure)) {
                out.printf("  %-25s: %s", m.mName, getMeasures().get(m.mName).toString());
                out.println();
            }
        }
    }

    private static List<Measure> getSortedMeasures() {
        final List<Measure> sortedMeasures = new ArrayList<Measure>(mInstance.mOrderedMeasures);
        final Comparator<Measure> comparator = new Comparator<Measure>() {
            public int compare(Measure m1, Measure m2) {
                return Long_compare(m1.mSum, m2.mSum);
            }

            private int Long_compare(long l1, long l2) {
                return Long.valueOf(l1).compareTo(Long.valueOf(l2));
            }
        };

        Collections.sort(sortedMeasures, comparator);
        return sortedMeasures;
    }

    /** Start measuring the specified measure name */
    synchronized static public void start(String name) {
        if (mFinalPrefix.get() != null) {
            name = mFinalPrefix.get() + name;
        }
        final Pair<Long, String> key = new Pair<Long, String>(Thread.currentThread().getId(), name);
        final Long old = mInstance.mThreadMeasures.put(key, System.currentTimeMillis());
        if (old != null) {
            mInstance.mDuplicateStarts.add(name);
        }
        getOrAddMeasure(name).open();
    }

    static private Measure getOrAddMeasure(String name) {
        Measure measure = getMeasures().get(name);
        if (measure == null) {
            measure = new Measure();
            measure.mName = name;
            getMeasures().put(name, measure);
            mInstance.mOrderedMeasures.add(measure);
        }
        return measure;
    }

    /** Stop measuring the specified measure name and update statistics */
    synchronized static public void end(String name) {
        if (mFinalPrefix.get() != null) {
            name = mFinalPrefix.get() + name;
        }
        final Pair<Long, String> key = new Pair<Long, String>(Thread.currentThread().getId(), name);
        final Long lastTime = mInstance.mThreadMeasures.remove(key);
        if (lastTime == null) {
            mInstance.mEndsWithoutStarts.add(name);
            return;
        }
        final Measure measure = getMeasures().get(name);
        // this isn't supposed to happen at this point
        if (measure == null) {
            return;
        }
        measure.close(System.currentTimeMillis() - lastTime);
    }

    /** Clear all internal data collected */
    public static void clear() {
        getMeasures().clear();
        mPrefixes.get().clear();
        mInstance.mOrderedMeasures.clear();
    }

    synchronized static public void pushPrefix(String prefix) {
        mPrefixes.get().add(prefix);
        recalcFinalPrefix();
        start("all");
    }

    synchronized static public void popPrefix() {
        end("all");
        if (mPrefixes.get().size() > 0) {
            mPrefixes.get().remove(mPrefixes.get().size() - 1);
        }
        recalcFinalPrefix();
    }

    private static void recalcFinalPrefix() {

        final StringBuilder sb = new StringBuilder();
        final List<String> prefs = mPrefixes.get();
        for (String p : prefs) {
            sb.append(p);
            sb.append("-");
        }
        mFinalPrefix.set(sb.toString());
    }
}
