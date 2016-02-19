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
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;

/** Utilities for file and directory manipulation */
public final class ExtFileUtils {

    private ExtFileUtils() {
        //private constructor
    }
    
    /** Create directory if not already exists. Throw an exception upon failure */
    public static void createDir(File target) throws AdeUsageException {
        if (!target.exists() && !target.mkdir() && !target.exists()) {
            throw new AdeUsageException("Failed creating directory " + target.getPath());
        }
    }

    private static class LogFileFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return false;
            }
            if (file.getName().startsWith(".")) {
                return false;
            }
            return true;
        }
    }

    /** Get list of log files in given directory.
     * Returns all files in directory except subdirectories and files starting with '.'
     * Result is sorted by name
     */
    public static File[] getLogFiles(Ade ade, File directory) throws AdeInternalException {
        final File[] fileArray = directory.listFiles(new LogFileFilter());
        if (fileArray == null) {
            return null;
        }
        final Comparator<? super File> fileComparator = new Comparator<File>() {

            @Override
            public int compare(File file1, File file2) {
                return file1.getName().compareToIgnoreCase(file2.getName());
            }

        };
        Arrays.sort(fileArray, fileComparator);
        return fileArray;
    }

}