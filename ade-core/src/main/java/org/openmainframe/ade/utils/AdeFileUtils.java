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
package org.openmainframe.ade.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.openmainframe.ade.exceptions.AdeUsageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Miscellaneous file utilities.*/
public final class AdeFileUtils {

    private static final String ZIP = ".gz";
    private static final String FAILED_DELETING_FILE = "Failed deleting file ";

    private static final Logger logger = LoggerFactory
            .getLogger(AdeFileUtils.class);

    private AdeFileUtils() {
        //Make the default constructor private.
    }
    
    /**
     * Copy a file on the filesystem.
     * 
     * @param source the source {@link File} to be copied from
     * @param target the target {@link File} to be copied to
     * @throws AdeUsageException if the copy failed
     * */
    public static void copyFile(File source, File target) throws AdeUsageException {
        final String closeFail = " failed to close.";
        FileInputStream fisSource = null;
        FileOutputStream fosTarget = null;
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            fisSource = new FileInputStream(source);
            fosTarget = new FileOutputStream(target);
            inputChannel = fisSource.getChannel();
            outputChannel = fosTarget.getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());

            inputChannel.close();
            inputChannel = null;

            outputChannel.close();
            outputChannel = null;
            
            fisSource.close();
            fisSource = null;
            
            fosTarget.close();
            fosTarget = null;
        } catch (IOException e) {
            throw new AdeUsageException("Copy file utility error", e);
        } finally {
            if (inputChannel != null) {
                try {
                    inputChannel.close();
                } catch (IOException e1) {
                    // quiet cleanup
                    logger.warn(inputChannel.toString() + closeFail, e1);
                }
            }
            if (outputChannel != null) {
                try {
                    outputChannel.close();
                } catch (IOException e1) {
                    // quiet cleanup
                    logger.warn(outputChannel.toString() + closeFail, e1);
                }
            }
            if (fisSource != null) {
                try {
                    fisSource.close();
                } catch (IOException e1) {
                    logger.warn(fisSource.toString() + closeFail, e1);
                }
            }
            if (fosTarget != null) {
                try {
                    fosTarget.close();
                } catch (IOException e1) {
                    logger.warn(fosTarget.toString() + closeFail, e1);
                }
            }
        }
    }

    /** Returns a BufferedReader for reading given file.
     * If file name ends with .gz it uses GZIPInputStream as the underlying stream.
     * The file is expected to be in UTF-8 encoding.
     * 
     * @param file the {@link File} to be opened as a {@link BufferedReader}
     * @return a {@link BufferedReader} for the passed in file
     * @throws AdeUsageException if the file could not be opened
     */
    public static BufferedReader openLogFile(File file)
            throws AdeUsageException {
        BufferedReader logReader;
        final String fileName = file.getPath();
        try {
            if (file.getName().endsWith(ZIP)) {
                logReader = new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(new FileInputStream(fileName)), StandardCharsets.UTF_8));
            } else {
                logReader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(fileName), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new AdeUsageException("Failed opening file " + fileName + ": "
                    , e);
        }
        return logReader;
    }

    /** Returns an InputStream for reading given file.
     * If file name ends with .gz it uses GZIPInputStream as the underlying stream.
     * 
     * @param file the {@link File} to be opened as an {@link InputStream}
     * @return an {@link InputStream} for the passed in file
     * @throws AdeUsageException if the file could not be opened
     */
    public static InputStream openLogFileAsInputStream(File file)
            throws AdeUsageException {
        final String fileName = file.getPath();
        try {
            if (file.getName().endsWith(ZIP)) {
                return new BufferedInputStream(
                        new GZIPInputStream(new FileInputStream(fileName)));
            } else {
                return new BufferedInputStream(new FileInputStream(fileName));
            }
        } catch (IOException e) {
            throw new AdeUsageException("Failed opening file " + fileName + ": "
                    , e);
        }

    }

    /**
     * Delete a file from file system.
     * 
     * @param file the {@link File} to delete
     * @throws AdeUsageException if the file could not be deleted
     * */
    public static void deleteFile(File file) throws AdeUsageException {
        if (!file.delete()) {
            throw new AdeUsageException(FAILED_DELETING_FILE + file.getPath());
        }
    }

    /**
     * Delete a file from the file system or log the failure if unable to delete it.
     * 
     * @param file the {@link File} to delete
     * */
    public static void deleteFileOrLog(File file) {
        if (!file.delete()) {
            logger.warn(FAILED_DELETING_FILE + file.getPath());
        }
    }

    /**
     * Create a directory if it does not already exist.
     * 
     * @param target the {@link File} representing the directory to be created
     * @throws AdeUsageException if the directory could not be created
     * */
    public static void createDir(File target) throws AdeUsageException {
        if (!target.exists() && !target.mkdir()) {
            throw new AdeUsageException("Failed creating directory " + target.getPath());
        }
    }

    /**
     * Create a directory if it does not already exist, including any necessary, non-existent parent directories.
     *
     *@param target the {@link File} representing the directory to be created
     *@throws AdeUsageException if the directory could not be created
     **/
    public static void createDirs(File target) throws AdeUsageException {
        if (!target.exists() && !target.mkdirs()) {
            throw new AdeUsageException("Failed creating directory " + target.getPath());
        }
    }

    /**
     * Delete the passed in file. If the passed in file is a directory, then recursively delete any internal
     * sub-directories.
     *  
     * @param path the {@link File} to delete
     * @return true if the delete was successful
     */
    public static boolean deleteRecursiveOrLog(File path) {

        if (path.isDirectory()) {
            boolean success = true;
            final File[] files = path.listFiles();
            for (File file : files) {
                success = deleteRecursiveOrLog(file) && success;
            }
            if (!success) {
                logger.warn("Cannot delete " + path.getPath() + ": failed to delete all files inside");
                return false;
            }
        }
        if (!path.delete()) {
            logger.warn(FAILED_DELETING_FILE + path.getPath());
            return false;
        }
        return true;
    }
}
