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
package org.openmainframe.ade.impl.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ade-core general utilities.
 */
public final class GeneralUtils {

    private static final Logger logger = LoggerFactory.getLogger(GeneralUtils.class);

    private static final int NANOS_PER_SECOND = 1000000;
    private static final char ASCII_SPACE = ' ';
    private static final char ASCII_PRINTABLE_LOW = 32;
    private static final char ASCII_PRINTABLE_HI = 128;
    private static final String[] MEM_TYPE_NAMES = { "Used", "Total", "Max" };
    private static final int MEM_TYPE_USED_IDX = 0;
    private static final int MEM_TYPE_TOTAL_IDX = 1;
    private static final int MEM_TYPE_MAX_IDX = 2;
    private static String maxMemTitle = "";
    private static long maxMemory;

    private GeneralUtils() {
        // Prevent instantiation of util class
    }

    /**
     * Logs JVM memory information.
     * 
     * @param title - A title associated with the logging event
     */
    public static synchronized void logMemStatus(String title) {

        final long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        final long[] values = new long[MEM_TYPE_NAMES.length];

        values[MEM_TYPE_USED_IDX] = usedMem;
        values[MEM_TYPE_TOTAL_IDX] = Runtime.getRuntime().totalMemory();
        values[MEM_TYPE_MAX_IDX] = Runtime.getRuntime().maxMemory();

        for (int i = 0; i < MEM_TYPE_NAMES.length; ++i) {
            logger.info(String.format("Memory status (%s): %-6s=%12d", title,
                    MEM_TYPE_NAMES[i], values[i]));
        }

        if (usedMem > maxMemory) {
            maxMemory = usedMem;
            maxMemTitle = title;
        }
    }

    /**
     * Logs max memory recorded by calls to logMemStatus().
     *  
     */
    public static void logMaxMem() {
        logger.info("Max memory was " + maxMemory + " at " + maxMemTitle);
    }

    /**
     * Returns the current thread's CPU time.
     * 
     * @return the current thread's CPU time, or 0 if CPU time is not supported
     *     for the current thread
     */
    public static long getCpuTime() {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : 0L;
    }

    /**
     * Returns the current thread's user time.
     * 
     * @return the current thread's user time, or 0 if user time is not supported
     *     for the current thread
     */
    public static long getUserTime() {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadUserTime() : 0L;
    }

    /**
     * Returns the difference between the current thread's CPU time and user time.
     * 
     * @return the current thread's system time, or 0 if CPU time is not supported
     *     for the current thread
     */
    public static long getSystemTime() {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        final long systemTime = bean.getCurrentThreadCpuTime() - bean.getCurrentThreadUserTime();
        return bean.isCurrentThreadCpuTimeSupported() ? systemTime : 0L;
    }

    /**
     * Logs a timestamp and title as elapsed time.
     * 
     * @param title - A title associated with the elapsed time.
     * @param timeInNanos - The elapsed time in nanoseconds
     */
    public static void logElapsedTime(String title, long timeInNanos) {
        logger.info(String.format("Time elapsed (%s): %d", title, timeInNanos / NANOS_PER_SECOND));
    }

    /**
     * Returns a string based on the input string, but with all characters
     * with ordinal values < 32 or >= 128 replaced with ' '.
     * 
     * @param src - The string to clean
     * 
     * @return The original string if it does not contain any characters
     *     outside the allowed range, or a new string with any characters
     *     outside the allowed range converted to ' '
     */
    public static String cleanString(String src) {
        if (src == null) {
            return null;
        }

        boolean foundBad = false;
        final CharacterIterator it = new StringCharacterIterator(src);
        for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
            if (c < ASCII_PRINTABLE_LOW || c >= ASCII_PRINTABLE_HI) {
                foundBad = true;
                break;
            }
        }

        if (!foundBad) {
            return src;
        }

        final StringBuilder res = new StringBuilder();
        for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
            if (c < ASCII_PRINTABLE_LOW || c >= ASCII_PRINTABLE_HI) {
                res.append(ASCII_SPACE);
            } else {
                res.append(c);
            }
        }

        return res.toString();
    }

    /**
     * An enum of operating system types.
     */
    public enum OS {
        WINDOWS("win"), UNIX(new String[] { "nix", "nux", "aix" });

        private String[] m_substrs;
        private static final OS S_OS = getOSFromString(System.getProperty("os.name"));

        private OS(String[] patterns) {
            m_substrs = patterns;
        }

        private OS(String pattern) {
            this(new String[] { pattern });
        }

        /**
         * Returns the OS derived from the os.name system property.
         * @return the OS derived from the os.name system property 
         */
        public static OS getOS() {
            return S_OS;
        }

        /**
         * Returns the OS derived from the input string.  The input string
         * is matched against a set of patterns to determine the OS type.
         * 
         * @param osString - A string representing an OS type, such as "win",
         *     "windows", "linux", or "aix"
         * @return an OS derived from the input string, or null if the
         *     input string could not be converted into an OS type.
         */
        public static OS getOSFromString(String osString) {
            final String osStringLowerCase = osString.toLowerCase();
            for (OS os : OS.values()) {
                for (String pattern : os.m_substrs) {
                    if (osStringLowerCase.indexOf(pattern) >= 0) {
                        return os;
                    }
                }
            }
            return null;
        }
    }
}
