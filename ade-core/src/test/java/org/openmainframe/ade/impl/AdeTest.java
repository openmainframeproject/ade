/*
 
    Copyright IBM Corp. 2016
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
package org.openmainframe.ade.impl;

import java.util.logging.Logger;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IDataFactory;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dataStore.DataStoreSqlImpl;
import org.openmainframe.ade.testUtils.StringTestComparator;
import org.openmainframe.ade.utils.patches.Version;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

/**
 * All ade tests that extends this one will be able to use some utilities.
 */
public abstract class AdeTest extends TestCase {

    public static final String TESTS_SETUP_FILE = "setups/setup.props";

    /**
     * Logger
     */
    protected Logger logger;

    protected Ade a_ade;
    protected IDataFactory m_dataFactory;
    protected static DataStoreSqlImpl m_dataStore;

    /**
     * Constructor
     * 
     * @param name test name
     */
    public AdeTest(String name) {
        super(name);
    }

    /**
     * Constructor
     */
    public AdeTest() {
        super();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (!Ade.isCreated()) {
            Ade ade = mock(Ade.class, RETURNS_DEEP_STUBS);
            when(ade.getConfigProperties().database().getDatabaseDriver()).thenReturn("derby");
            when(ade.getConfigProperties().getOverrideVersionCheck()).thenReturn(true);
            when(ade.getDbVersion()).thenReturn(new Version(1, 0));
            Ade.create(ade);
        }
    }

    static public void compareObjectByText(String object1Title, Object object1,
            String object2Title, Object object2) throws AdeInternalException {
        assertTrue(object1Title + " is null", object1 != null);
        assertTrue(object2Title + " is null", object2 != null);
        new StringTestComparator(object1Title, object1.toString(),
                object2Title, object2.toString());
    }

    protected boolean requiresDb() {
        return false;
    }
}
