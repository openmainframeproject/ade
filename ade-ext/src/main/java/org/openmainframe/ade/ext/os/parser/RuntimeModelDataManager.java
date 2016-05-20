/*
 
    Copyright IBM Corp. 2015, 2016
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
package org.openmainframe.ade.ext.os.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.AdeExt;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;
import org.openmainframe.ade.ext.main.helper.UploadOrAnalyze;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime Model Data are log related statistics collected after a model is built. This information is kept in
 * JVM memory and is continuously being updated. An example of RuntimeModelData is "Last Time a Message was Seen".
 * This data is kept for the Ade Instance and it's not always possible to break down the data into different 
 * "Source".
 * 
 * The RuntimeModelDataManager class allows the caller to store the Runtime Model Data to the filesystem when the JVM 
 * is terminating.  And, when the JVM is starting, it will read the RuntimeModelData from the file system.
 * 
 * To support both Production and development env, there are two locations that Runtime Model Data could be stored:
 * - At the JVM level: in test environment, log files might contain logs from multiple sources.
 * This log file is fed to single instance of Ade as a file, thus, the JVM could be handling multiple sources.  
 * In this case, it is stored under the <Analsysis_Root>/continuous/ directory.
 * 
 * - At the source level: in production environment, each JVM is only handling logs from one source through the STDIN.  
 * In this case, it is stored under the <Analsysis_Root>/continuous/<sourceName>/ directory.
 * 
 * Here is the criteria to determine the storage location:
 * - If user config property is defined in setup.prop, store the RuntimeModelData based on the user config property.
 * - Else if input source is STDIN, store the RuntimeModelData at the Source level.
 * - Else (input source is File or DIR), store the RuntimeModelData at the JVM level.
 *  
 * Note: 
 * - RuntimeModelData is not stored at the end of UPLOAD.  It's only stored at the end of ANALYZE.  This is to avoid 
 * handling bulk load of logs containing multiple systems (i.e. it requires storing the runtime model data at the JVM level,
 * but during analyze, it might not be possible to find the previously stored runtime model data.) and renaming
 * of a system.
 * 
 * - It's possible that the Linux System Name in the log could change dynamically, without disconnecting from
 * Anomaly Detection Engine.
 * This behavior will affect the source level storage of the Runtime model Data.  Since this is a rare situation 
 * (expected to happen frequently only when a new Linux system is built), we are not going implement special logic for
 * this situation.
 */
public class RuntimeModelDataManager {
    /**
     * The default logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(RuntimeModelDataManager.class);

    /**
     * The filename for the runtime model data.
     */
    private final static String RUNTIME_MODEL_DATA_FILENAME = "runtimeModelData.ser";

    /**
     * The RuntimeModelData Version.
     * Version 1 - newly created.
     * Version 2 - added epoch time.
     */
    private static final int S_CURRENT_RUNTIME_MODEL_DATA_VERSION = 2;

    /**
     * The file where the runtimeModelData was read.
     */
    private File m_runTimeModelDataFile = null;

    /**
     * The time that this object is created.
     */
    private long m_creationTimeOfRuntimeModelData = 0;
    
    /**
     * Time constants.
     */
    public static final int EXCESS_SECONDS = 60;
    public static final int EXCESS_MINUTES = 60;
    
    /**
     * Version 2 of RuntimeModelData.
     */
    public static final int VERSION_2_TS = 2;

    /**
     * Writes the model data to file. First, we need to get the current time stamp so we can calculate the time since
     * we last wrote the RuntimeModelData. Then when writing out the data, we write the version, current time stamp,
     * the number of entries, and the data as byte array. After the data has successfully been written to the
     * temporary file we rename it to the real file name. At the end, we log information if writing the model data to
     * file was successful or not.
     * @param sourceId the source name.
     * @throws AdeException 
     */
    public final void writeModelDataToFile(String sourceId) throws AdeException {
        if (UploadOrAnalyze.getAdeRequestType() != AdeExtRequestType.ANALYZE) {
            logger.warn("Skip writing RuntimeModelData, because it's request type is: " + UploadOrAnalyze.getAdeRequestType());
            return;
        }        
        /*
         * One known scenario for this case is when MessageReader starts reading messages but there is no input or 
         * the input is an invalid message without the source ID. Then, the MessageReader receives EOF. (readline()
         * received a null value)
         */
        if (sourceId == null) {
            logger.warn("Skip writing RuntimeModelData, because sourceId is null.");
            return;
        }

        /* Keeps track of the time since last writing the RuntimeModelData. */
        final long currentTimestamp = System.currentTimeMillis();
        String timeSinceLastRuntimeModelDataWriting;
        if (m_creationTimeOfRuntimeModelData == 0) {
            /* Don't do the calculation. */
            timeSinceLastRuntimeModelDataWriting = "N/A";
        } else {
            final long milliseconds = currentTimestamp - m_creationTimeOfRuntimeModelData;
            final int seconds = (int) (milliseconds / DateTimeUtils.MILLIS_IN_SECOND) % EXCESS_SECONDS;
            final int minutes = (int) ((milliseconds / DateTimeUtils.MILLIS_IN_MINUTE) % EXCESS_MINUTES);
            final int hours = (int) ((milliseconds / DateTimeUtils.MILLIS_IN_HOUR) % DateTimeUtils.HOURS_IN_DAY);
            timeSinceLastRuntimeModelDataWriting = String.format("%02d:%02d:%02d",
                    hours, minutes, seconds);
        }
        int numberOfEntries = 0;
        boolean isWritingSuccessful = false;
        final Set<String> modelObjectKeys = Ade.getAde().getDataStore().models().getModelDataObjectKeys();

        /* Initialize the output streams. */
        final File file = getRuntimeModelDataPath(sourceId);
        final File tempFile = getTempFile(file);

        /* Delete the temp file. */
        if (tempFile.exists()) {
            final boolean deleted = tempFile.delete();
            if (!deleted) {
                final String msg = "Writing RuntimeModelData: Runtime model data temporary file exists, this might mean a previous write failed:" + tempFile.getAbsolutePath()
                        + ". Trying to delete this file but failed: " + tempFile.getAbsolutePath();

                logger.error(msg);
            } else {
                final String msg = "Writing RuntimeModelData: Runtime model data temporary file exists, this might mean a previous write failed:" + tempFile.getAbsolutePath()
                        + ". Deleted this file successfully: " + tempFile.getAbsolutePath();
                logger.info(msg);
            }
        }

        /* Write out the data */
        DataOutputStream dos = null;
        try {
            String filePath = tempFile.getParent();
            File dir = new File(filePath);
            if (dir.exists()){
                if (!tempFile.exists()){
                    tempFile.createNewFile();
                }               
            } else{
                String msg = "Attempting to create directory: " + filePath;
                logger.info(msg);
                if (!dir.mkdirs()){
                    throw new AdeUsageException("Could not create directory " + filePath);
                }
                tempFile.createNewFile();
            }            
            final FileOutputStream fos = new FileOutputStream(tempFile);
            dos = new DataOutputStream(fos);

            /* Write the version of the data. */
            dos.writeInt(S_CURRENT_RUNTIME_MODEL_DATA_VERSION);

            /* Write the current timestamp. */
            m_creationTimeOfRuntimeModelData = currentTimestamp;
            dos.writeLong(currentTimestamp);

            /* Write number of entries in the file. */
            dos.writeInt(modelObjectKeys.size());

            /* Write out the data as byte array. */
            for (String modelObjectKey : modelObjectKeys) {
                ObjectOutputStream oos = null;
                numberOfEntries++;
                try {
                    final Object modelObject = Ade.getAde().getDataStore().models().getModelDataObject(modelObjectKey);

                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    oos = new ObjectOutputStream(baos);
                    oos.writeObject(modelObject);
                    final byte[] dataArray = baos.toByteArray();

                    dos.writeUTF(modelObjectKey);
                    dos.writeInt(dataArray.length);
                    dos.write(dataArray);
                } catch (Exception e) {
                    /* Throw a specific exception, so that we know which AdeObjectKey is having a problem. */
                    throw new AdeUsageException(
                            "Error writing model object for: " + modelObjectKey, e);
                } finally {
                    if (oos != null) {
                        oos.close();
                    }
                }
            }

            /* Writing to temp file was successful. Now, rename it! */
            if (dos != null) {
                dos.close();
                dos = null;
                if (!tempFile.renameTo(file)) {
                    final String msg = "Writing of runtimeModelData, failed to rename " + tempFile.getName() + " to " + file.getName();
                    throw new AdeUsageException(msg);
                }
            }
            isWritingSuccessful = true;
        } catch (Throwable e) {
            final String msg = "Writing of runtimeModelData failed:" + file.getAbsoluteFile()
                    + ".  RuntimeModelData version: " + S_CURRENT_RUNTIME_MODEL_DATA_VERSION
                    + ".  It has been " + timeSinceLastRuntimeModelDataWriting + " since previous RuntimeModelData was written.";
            logger.error(msg, e);
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                    dos = null;
                } catch (IOException e) {
                    final String msg = "Writing of runtimeModelData, error closing DataOutputStream for file: " + tempFile.getAbsolutePath();
                    logger.error(msg, e);
                }
            }
            if (isWritingSuccessful) {
                logger.info("Writing of runtimeModelData completed successfully: " + file.getAbsoluteFile()
                        + ".  RuntimeModelData version: " + S_CURRENT_RUNTIME_MODEL_DATA_VERSION
                        + ". There was " + numberOfEntries + " entries written."
                        + ".  It has been " + timeSinceLastRuntimeModelDataWriting + " since previous RuntimeModelData was written.");
            } else {
                logger.warn("Writing of runtimeModelData failed: " + file.getAbsoluteFile()
                        + ".  It has been " + timeSinceLastRuntimeModelDataWriting + " since previous RuntimeModelData was written.");
            }
        }
    }

    /**
     * Reads the model data from a file. After doing some preliminary checks (i.e. checking for null RunTimeModelData
     * file and existence of temporary file) we get the version, the timestamp if the model data is above version 2 and
     * we get the time since we last read the RuntimeModelData. Then we begin reading the contents of the file by 
     * reading the data from a byte array. If reading was successful, we set the model data object in Ade.
     * using the file. If the reading failed, then we write to log a failure message and rename the file so it is kept in as 
     * FFDC. At the end of reading, we clean up the input streams, log the success or failure of reading, and delete the 
     * file that was read.
     * @param sourceId the source name.
     * @throws AdeException 
     */
    public final void readModelDataFromFile(String sourceId) throws AdeException {
        /* Initialize the output streams */
        final File file = getRuntimeModelDataPath(sourceId);
        final File tempFile = getTempFile(file);

        if (m_runTimeModelDataFile != null) {
            /* Runtime model data will only be read once for each JVM instance.  It's possible that this method 
             * will be called with multiple sourceIDs (with different paths to the runtimeModelData), 
             * and all the other source IDs after the first time its been read will be ignored.
             */
            logger.warn("Runtime model data already read from " + m_runTimeModelDataFile.getAbsolutePath()
                    + ".  The Runtime Model data from " + file.getAbsolutePath() + " will be ignored.");
            return;
        }

        if (tempFile.exists()) {
            final boolean deleted = tempFile.delete();

            if (!deleted) {
                final String msg = "Reading RuntimeModelData: Runtime model data temporary file exists, this might mean a previous write failed:" + tempFile.getAbsolutePath()
                        + ". Trying to delete this file but failed: " + tempFile.getAbsolutePath();

                logger.error(msg);
            } else {
                final String msg = "Reading RuntimeModelData: Runtime model data temporary file exists, this might mean a previous write failed:" + tempFile.getAbsolutePath()
                        + ". Successfully deleted this file: " + tempFile.getAbsolutePath();

                logger.info(msg);
            }
        }

        if (!file.exists()) {
            logger.warn("Reading RuntimeModelData: Runtime model data file does not exist.  Nothing to load: " + file.getAbsolutePath());
            return;
        }

        /* Whether the reading process was successful */
        boolean isReadingSuccessful = false;

        /* Read the content from the file */
        final long currentTimestamp = System.currentTimeMillis();
        int version = -1;
        int numberOfEntries = -1;
        DataInputStream dis = null;
        FileInputStream fis;
        String timeSinceLastRuntimeModelDataWriting = "-1";
        final ArrayList<Entry<String, Object>> tmpModelData = new ArrayList<Map.Entry<String, Object>>();
        try {
            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);

            /* Read the version the data */
            version = dis.readInt();
            if (version > S_CURRENT_RUNTIME_MODEL_DATA_VERSION) {
                final String msg = "Reading RuntimeModelData: Runtime model data file is at version: " + version
                        + ", but current code supports only version " + S_CURRENT_RUNTIME_MODEL_DATA_VERSION
                        + ".  This file will not be used: " + file.getAbsolutePath();

                logger.warn(msg);

                return;
            }

            /* Read the timestamp only if this is version 2 or above */
            long creationTimeOfRuntimeModelData = 0;
            if (version >= VERSION_2_TS) {
                creationTimeOfRuntimeModelData = dis.readLong();
            }

            /* the time since last writing of the RuntimeModelData */
            if (creationTimeOfRuntimeModelData == 0) {
                /* Don't do the calculation */
                timeSinceLastRuntimeModelDataWriting = "N/A";
            } else {
                final long milliseconds = currentTimestamp - creationTimeOfRuntimeModelData;

                final int seconds = (int) (milliseconds / DateTimeUtils.MILLIS_IN_SECOND) % EXCESS_SECONDS;
                final int minutes = (int) ((milliseconds / DateTimeUtils.MILLIS_IN_MINUTE) % EXCESS_MINUTES);
                final int hours = (int) ((milliseconds / DateTimeUtils.MILLIS_IN_HOUR) % DateTimeUtils.HOURS_IN_DAY);
                timeSinceLastRuntimeModelDataWriting = String.format("%02d:%02d:%02d",
                        hours, minutes, seconds);
            }

            /* Read number of entries in the file */
            numberOfEntries = dis.readInt();

            /* Read the data from byte array */
            for (int i = 0; i < numberOfEntries; i++) {
                final String modelObjectKey = dis.readUTF();
                try {
                    final int sizeOfByteArray = dis.readInt();
                    final byte[] bArray = new byte[sizeOfByteArray];
                    dis.read(bArray);

                    final ByteArrayInputStream bais = new ByteArrayInputStream(bArray);
                    final ObjectInputStream ois = new ObjectInputStream(bais);
                    final Object data = ois.readObject();

                    final Entry<String, Object> entry = new SimpleEntry<String, Object>(modelObjectKey, data);
                    tmpModelData.add(entry);
                } catch (Exception e) {
                    throw new AdeUsageException("Reading RuntimeModelData: Error reading model object for: " + modelObjectKey
                            + " at entry " + i + " from file: " + file.getAbsolutePath(), e);
                }
            }

            /* If reading is successful, then add the read data to the Ade */
            for (Entry<String, Object> entry : tmpModelData) {
                final String modelObjectKey = entry.getKey();
                final Object data = entry.getValue();
                Ade.getAde().getDataStore().models().setModelDataObject(modelObjectKey, data);
            }

            m_runTimeModelDataFile = file;

            isReadingSuccessful = true;
        } catch (Throwable e) {
            String msg = "Reading of RuntimeModelData failed: " + file.getAbsoluteFile()
                    + ".  RuntimeModelData read was version: " + version
                    + ", current code support version " + S_CURRENT_RUNTIME_MODEL_DATA_VERSION
                    + ".  There was " + numberOfEntries + " entries to be read"
                    + ".  It has been " + timeSinceLastRuntimeModelDataWriting + " since previous RuntimeModelData was written.";
            logger.error(msg, e);

            /* close the input stream */
            if (dis != null) {
                try {
                    dis.close();
                    dis = null;
                } catch (IOException e1) {
                    msg = "Reading of RuntimeModelData: Error closing DataInputStream for file: " + file.getAbsolutePath();
                    logger.error(msg, e1);
                }
            }

            /* Rename the failed file, so that it's kept as FFDC. */
            final File errorFile = getErrorFile(file);
            boolean fileGone = true;
            if (errorFile.exists()) {
                fileGone = errorFile.delete();

                if (!fileGone) {
                    msg = "Reading of RuntimeModelData failed: keeping FFDC, but FFDC file for runtimeModelData already exists: "
                            + errorFile.getAbsolutePath()
                            + ".  Tried to delete this file but failed.";
                    logger.error(msg);
                } else {
                    msg = "Reading of RuntimeModelData failed: keeping FFDC, but FFDC file for runtimeModelData already exists: "
                            + errorFile.getAbsolutePath()
                            + ".  Successfully deleted this file.";
                    logger.info(msg);
                }
            }

            if (fileGone) {
                /* rename it the error RuntimeModelData file */
                if (!file.renameTo(errorFile)) {
                    msg = "Reading of RuntimeModelData: failed to rename ffdc file "
                            + file.getName() + " to " + errorFile.getName();
                    logger.error(msg);
                } else {
                    msg = "Reading of RuntimeModelData: successfully renamed "
                            + file.getName() + " to " + errorFile.getName();
                    logger.info(msg);
                }
            }

        } finally {
            if (dis != null) {
                try {
                    dis.close();
                    dis = null;
                } catch (IOException e) {
                    final String msg = "Reading of RuntimeModelData: Error closing DataInputStream for file: " + file.getAbsolutePath();
                    logger.error(msg, e);
                }
            }

            /* Reading was successful */
            if (isReadingSuccessful) {
                logger.info("Reading of RuntimeModelData completed successfully: " + file.getAbsoluteFile()
                        + ".  RuntimeModelData read was version: " + version
                        + ", current code support version " + S_CURRENT_RUNTIME_MODEL_DATA_VERSION
                        + ".  There was " + numberOfEntries + " entries to be read"
                        + ".  It has been " + timeSinceLastRuntimeModelDataWriting + " since previous RuntimeModelData was written.");
            } else {
                logger.warn("Reading of RuntimeModelData failed: " + file.getAbsoluteFile()
                        + ".  It has been " + timeSinceLastRuntimeModelDataWriting + " since previous RuntimeModelData was written.");
            }

            /* Always delete the file that was read.  It's possible that the file has already been deleted. */
            if (file.exists()) {
                final boolean deleted = file.delete();

                if (!deleted) {
                    final String msg = "Reading RuntimeModelData: Tried to delete the RuntimeModelData file but failed: "
                            + file.getAbsolutePath();

                    logger.error(msg);
                } else {
                    final String msg = "Reading RuntimeModelData: Successfully deleted the RuntimeModelData file: "
                            + file.getAbsolutePath();

                    logger.info(msg);
                }
            }
        }

    }

    /**
     * Returns the path where the runtime model data is stored.  
     * The RuntimeModelData is an attributes of the JVM.  In case a single Ade instance (JVM) is used to process
     * multiple sources, the RuntimeModelData will contain data for all sources.
     * In production env, one JVM will only have one source.  Therefore, the runtimeModelData is stored at the source
     * qualified directory.
     * In test env, one JVM might process multiple sources (for example, z/OS Analyze of the AMA logs.  The AMA logs
     * contains multiple sources.).  Therefore, the runtimeModelData should be stored at a JVM level directory.
     * @param sourceId the source name.
     * @return The runtime model data file with the correct path.
     * @throws AdeException 
     */
    private File getRuntimeModelDataPath(String sourceId) throws AdeException {
        boolean isStoreAtSource;

        final Boolean isRuntimeModelDataConfiguredToStoreAtSource = AdeExt.getAdeExt().getConfigProperties().isRuntimeModelDataStoreAtSource();
        if (isRuntimeModelDataConfiguredToStoreAtSource != null) {
            isStoreAtSource = isRuntimeModelDataConfiguredToStoreAtSource;
        } else {
            /* User didn't define property for how to store the runtimeModelData */
            final boolean isInputSourceSTDIN = UploadOrAnalyze.isInputSourceSTDIN();
            if (isInputSourceSTDIN) {
                /* STDIN is the only supported input mode for production environment */
                isStoreAtSource = true;
            } else {
                /* non-STDIN is the typical input mode for Analytic Testing.
                 * In Analytic Testing, it's possible that log file contains logs from multiple sources.  
                 * Therefore, runtimeModelData need to store at the JVM level. */
                isStoreAtSource = false;
            }
        }
        /* Get the runtimeModelDataPath depending on whether it's stored at Source or JVM level */
        File runtimeModelDataFile;
        if (isStoreAtSource) {
            final String path = Ade.getAde().getConfigProperties().getAnalysisOutputPath();
            final File dir = new File(path, sourceId);
            runtimeModelDataFile = new File(dir, RUNTIME_MODEL_DATA_FILENAME);
        } else {
            final String path = Ade.getAde().getConfigProperties().getAnalysisOutputPath();
            runtimeModelDataFile = new File(path, RUNTIME_MODEL_DATA_FILENAME);
        }
        return runtimeModelDataFile;
    }

    /**
     * Returns the name of the temporary runtimeModelDataFile.
     * @param runtimeModelDataFile the runtime model data file.
     * @return the temporary file (where the file name is the runtime model data file name with ".tmp" appended)
     */
    private File getTempFile(File runtimeModelDataFile) {
        final File ret = new File(runtimeModelDataFile.getParent(),
                runtimeModelDataFile.getName() + ".tmp");

        return ret;
    }

    /**
     * Returns the name of the error runtime model data file.
     * @param runtimeModelDataFile the runtime model data file.
     * @return the error file (where the file name is the runtime model data file name with ".ffdc" appended)
     */
    private File getErrorFile(File runtimeModelDataFile) {
        final File ret = new File(runtimeModelDataFile.getParent(),
                runtimeModelDataFile.getName() + ".ffdc");

        return ret;
    }
}
