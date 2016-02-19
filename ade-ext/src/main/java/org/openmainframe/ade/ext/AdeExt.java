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
package org.openmainframe.ade.ext;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.ext.utils.AdeOutputDirectoriesManager;
import org.openmainframe.ade.ext.utils.AdeExtConfigProperties;

/** A singleton object concentrating AdeExt resources */
public class AdeExt {

    static private AdeExt m_adeExt = null;

    private Ade a_ade;
    private AdeOutputDirectoriesManager m_outputDirectoriesManager;
    private AdeExtConfigProperties m_configProperties;

    /** Create the AdeExt singleton object.
     *  When creating it, the Ade object should already be created
     * @param ade  The Ade singleton object
     * @throws AdeException
     */

    static synchronized public void create(Ade ade) throws AdeException {
        if (m_adeExt != null) {
            throw new AdeInternalException("Ade Ext object already created");
        }
        m_adeExt = new AdeExt(ade);
    }

    /** Checks whether AdeExt has been created */
    public static boolean isCreated() {
        return m_adeExt != null;
    }

    /** Returns AdeExt singleton object.
     * This call is only allowed after a call to create() */
    static public AdeExt getAdeExt() throws AdeException {
        if (m_adeExt == null) {
            throw new AdeInternalException("Ade Ext object not created");
        }
        return m_adeExt;
    }

    private AdeExt(Ade ade) throws AdeException {
        a_ade = ade;
        m_configProperties = new AdeExtConfigProperties(a_ade.getSetupFilePath());
        m_outputDirectoriesManager = new AdeOutputDirectoriesManager(a_ade.getConfigProperties().getOutputPath());
    }

    /** Return an object that manages the output directories */
    public AdeOutputDirectoriesManager getOutputDirectoryManager() {
        return m_outputDirectoriesManager;
    }

    /** Returns an object with Ext specific configuration properties */
    public AdeExtConfigProperties getConfigProperties() {
        return m_configProperties;
    }

    /** Closes Ext specific resources */
    public void close() throws AdeException {

    }

    /** Clean resources for the case of a fatal exception. */
    public void quietCleanup() {

    }
}
