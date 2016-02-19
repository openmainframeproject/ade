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
package org.openmainframe.ade;

import java.sql.Connection;

import org.openmainframe.ade.actions.IActionsFactory;
import org.openmainframe.ade.data.IDataFactory;
import org.openmainframe.ade.dataStore.IDataStore;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.UserSpecifications;
import org.openmainframe.ade.impl.flow.factory.FlowFactory;
import org.openmainframe.ade.impl.patches.AdePatches;
import org.openmainframe.ade.output.IAnalysisResultMarshaller;
import org.openmainframe.ade.utils.patches.Version;

/**
 * The Ade main class for creating all factories, and accessing configuration and
 * data-store access.
 * As a thumb rule, instantiation of objects of all implemented classes should not be 
 * done explicitly. Rather, call {@link Ade#create()} and use the {@link Ade} 
 * singleton object to access factories for all objects by calling {@link Ade#getAde()}.
 * <br><br>
 * Use {@link Ade#close()} and {@link Ade#quietCleanup()} to close ade in the regular
 * scenario and when an exception occurred, respectively.
 */
public abstract class Ade {

    /** 
     * The system property used to specify the path to the ade setup properties file 
     */
    public static final String ADE_SETUP_FILE_PATH_PROPERTY = "ade.setUpFilePath";

    static Ade a_adeObject = null;

    protected Connection m_conn;

    /** 
     * Create the Ade singleton using the default setup file.
     */
    public static void create() throws AdeException {
        create((String) null);
    }

    /** 
     * Create the Ade singleton using the specified setup file.
     * 
     * @param setupPath - The path to the ade setup properties file.
     * @throws AdeException if the Ade singleton has already been created.
     */
    public static synchronized void create(String setupPath) throws AdeException {
        if (a_adeObject != null) {
            throw new AdeInternalException("Ade object already created");
        }
        createOverride(setupPath);
    }

    /**
     * Create the Ade singleton from an existing Ade object.
     *  
     * @param ade - A Ade instance to use as the Ade singleton.
     * @throws AdeException if the Ade singleton has already been created.
     */
    public static synchronized void create(Ade ade) throws AdeException {
        if (a_adeObject != null) {
            throw new AdeInternalException("Ade object already created");
        }
        a_adeObject = ade;
    }

    /**
     * Create the Ade singleton using the default setup file if it has not
     * already been created.
     * 
     * @return true if a new Ade singleton object was created or false
     *     if the Ade singleton was already created.
     * @throws AdeException
     */
    public static synchronized boolean createIfNeeded() throws AdeException {
        if (!isCreated()) {
            create();
            return true;
        }
        return false;
    }

    /**
     * Create the Ade singleton using the specified setup file if it has not
     * already been created.
     * 
     * @param setupPath - The path to the ade setup properties file.
     * @return true if a new Ade singleton object was created or false
     *     if the Ade singleton was already created.
     * @throws AdeException
     */
    public static synchronized boolean createIfNeeded(String setupPath) throws AdeException {
        if (!isCreated()) {
            create(setupPath);
            return true;
        }
        return false;
    }

    /**
     * Create the Ade singleton using the specified setup file overriding
     * any existing Ade singleton
     *
     * @param setupPath - The path to the ade setup properties file.
     * @throws AdeException if the Ade singleton could not be created.
     */
    public static synchronized void createOverride(String setupPath) throws AdeException {
        // close existing singleton if it exists
        if (a_adeObject != null) {
            a_adeObject.close();
        }

        AdeInternal.adeObjectImpl = new AdeInternal(setupPath);
        Ade.a_adeObject = AdeInternal.adeObjectImpl;

        if (a_adeObject == null) {
            throw new AdeInternalException("Ade object creation failed");
        }
    }

    /** 
     * Returns the Ade singleton.
    
     * @return The Ade singleton
     * @throws AdeException if the Ade singleton has not been created
     */
    public static Ade getAde() throws AdeException {
        if (a_adeObject == null) {
            throw new AdeInternalException("Ade object not created");
        }
        return a_adeObject;
    }

    /** 
     * Checks whether the Ade singleton was created
     * 
     * @return true if the Ade singleton has been created.
     */
    public static boolean isCreated() {
        return a_adeObject != null;
    }

    /** 
     * Returns the setup file path used to construct the current Ade singleton
     * 
     * @return the setup file path used to construct the current Ade singleton
     */
    public abstract String getSetupFilePath();

    /**
     * Returns a factory for creating data classes, e.g., MessageInstance objects.
     * 
     * @return a factory for creating data classes, e.g., MessageInstance objects.
     * 
     * @throws AdeInternalException 
     */
    public abstract IDataFactory getDataFactory() throws AdeInternalException;

    /**
     * Returns a factory for creating action classes, e.g., Trainer and Analyzer.
     * 
     * @return a factory for creating action classes, e.g., Trainer and Analyzer.
     * 
     */
    public abstract IActionsFactory getActionsFactory();

    /**
     * Returns a factory for creating flow objects.
     * 
     * @return a factory for creating flow objects.
     * 
     * @throws AdeException 
     */
    public abstract FlowFactory getFlowFactory() throws AdeException;

    /**
     * Returns the configuration properties in effect.  These are the properties read
     * from the setup files, overridden by system properties.  Optional properties will
     * have defaults.  These properties were read from the input configuration file,
     * and can no longer be modified.
     * 
     * @return the ade configuration properties
     */
    public abstract IAdeConfigProperties getConfigProperties();

    /**
     * Returns the Ade dataStore.
     *
     * @return the Ade dataStore.
     * 
     * @throws AdeException
     **/
    public abstract IDataStore getDataStore() throws AdeException;

    /**
     * Releases the Ade dataStore.
     * 
     * @throws AdeException
     */
    public abstract void releaseDataStore() throws AdeException;

    /** 
     * Returns the analysis result marshaller.
     *
     * @return the analysis result marshaller.
     * */
    public abstract IAnalysisResultMarshaller getAnalysisResultMarshaller() throws AdeException;

    /**
     * Creates a {@link IDataStore} for ade internal objects if one does not already exist.
     * If a {@link IDataStore} already exists, it deletes its contents.
     * <br>
     * In order to use a {@link IDataStore} that already exists, one should call
     * {@link Ade#getDataStore()}. One cannot call this method twice or following a call
     * to {@link Ade#getDataStore()}.
     * <br>
     * The specific type of {@link IDataStore} to create is defined in the configuration file.
     * 
     * @return interface for accessing the {@link IDataStore} just created. 
     * @throws AdeException if {@link Ade#createDataStore()} or {@link Ade#getDataStore()}
     * already called, or if {@link IDataStore} creation failed.
     */
    public abstract IDataStore createDataStore() throws AdeException;

    /**
     * Returns the version of the Ade engine.
     * 
     * @return the version of the Ade engine.
     */
    public abstract Version getVersion();

    /**
     * Returns the version of the Ade database.
     * 
     * @return the version of the Ade database.
     */
    public abstract Version getDbVersion();

    /**
     * Close all resources allocated to the current Ade singleton.
     */
    public abstract void close() throws AdeException;

    /**
     * Closes and cleans all resources for handling the case of a fatal error.
     * Rolls back database, if possible, and throws no exceptions.
     */
    public abstract void quietCleanup();

    /**
     * Return the patches required for upgrading installed Ade versions.
     * 
     * @return the patches required for upgrading installed Ade versions.
     */
    public AdePatches patches() {
        return new AdePatches();
    }

    /**
     * Returns the {@link IAdeDirectoryManager} for creating and accessing the
     * Ade output directories.
     * 
     * @return the directory manager
     */
    public abstract IAdeDirectoryManager getDirectoryManager();

    /**
     * Provides access to the {@link IDataStore} with the given SQL connection.
     * Use {@link Ade#releaseDataStore()} when use of the DataStore with the
     * given connection is done.
     * 
     * @param conn - A connection to the underlying datastore database.
     * @return the DataStore
     * @throws AdeException
     */
    public abstract IDataStore getDataStore(Connection conn) throws AdeException;

    /** 
     * Returns the user specifications.
     * @return the user specifications.
     */
    public abstract UserSpecifications getUserSpecifications();

    /**
     * Stores the command line arguments used to invoke the current main process.
     * 
     * @param args - An array of command line arguments.
     */
    public abstract void setCommandLineArguments(String[] args);

    /**
     * Returns the command line arguments used to invoke the current main process.
     * 
     * @return the command line arguments used to invoke the current main process.
     */
    public abstract String[] getCommandLineArguments();
}
