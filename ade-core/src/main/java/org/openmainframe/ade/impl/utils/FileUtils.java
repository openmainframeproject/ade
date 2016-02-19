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

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Comparator;

import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General purpose file utilities.
 *
 */
public final class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    private static final String FILE_ENCODING = "UTF-8";

    private FileUtils() {
        // Prevent instantiation of utility class
    }

    /**
     * Create a directory if it does not exist.  All parent directories
     * must already exist.
     * 
     * @param target - the directory to create
     * 
     * @throws AdeUsageException if the directory could not be created
     */
    public static void createDir(File target) throws AdeUsageException {
        if (!target.exists() && !target.mkdir() && !target.exists()) {
            throw new AdeUsageException("Failed creating directory " + target.getPath());
        }
    }

    /**
     * Create a directory, including all necessary parent directories
     * if it does not exist.
     * 
     * @param target - the directory to create
     * 
     * @throws AdeUsageException if the directory, or any parent directory,
     *     could not be created
     */
    public static void createDirs(File target) throws AdeUsageException {
        if (!target.exists() && !target.mkdirs() && !target.exists()) {
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

    /**
     * Given a directory, return an array of files sorted by base filename.  
     * Subdirectories and files with names starting with "." are ignored.
     * 
     * @param target - The directory
     * 
     * @throws AdeInternalException
     */
    public static File[] listFilesSorted(File directory) throws AdeInternalException {
        final File[] fileArray = directory.listFiles(new LogFileFilter());
        if (fileArray == null) {
            return new File[0];
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

    /**
     * Return a PrintWriter ready to write to a specified file.
     * 
     * @param file - The file to write to.
     * @param verbose - Log the opening of the file
     * 
     * @throws AdeUsageException if the file could not be found
     */
    public static PrintWriter openPrintWriterToFile(File file, boolean verbose) throws AdeUsageException {
        try {
            if (verbose) {
                logger.info("Opening " + file.getPath());
            }
            return new PrintWriter(file, FILE_ENCODING);
        } catch (FileNotFoundException e) {
            throw new AdeUsageException("Cannot open file " + file.getPath(), e);
        } catch (UnsupportedEncodingException e) {
            throw new AdeUsageException("Platform does not support encoding " + FILE_ENCODING, e);
        }
    }

    /**
     * Assert that all specified files exist.  If a file does not exist,
     * throw a FileNotFoundException.
     * 
     * @param files - An array of files
     * 
     * @throws FileNotFoundException if any of the specified files does not exist.
     */
    public static void assertExists(File... files) throws FileNotFoundException {
        for (File file : files) {
            if (!file.exists()) {
                throw new FileNotFoundException(file.getAbsolutePath());
            }
        }
    }

}