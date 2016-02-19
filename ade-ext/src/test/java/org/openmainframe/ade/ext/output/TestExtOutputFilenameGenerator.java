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
package org.openmainframe.ade.ext.output;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openmainframe.ade.Ade;

import org.openmainframe.ade.data.IPeriod.PeriodMode;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;

import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.modules.ContinuousTimeFramer;
import org.openmainframe.ade.utils.patches.Version;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Date;
import java.util.TimeZone;

public class TestExtOutputFilenameGenerator {
    ExtOutputFilenameGenerator eofg;
    Ade ade;
    @Before
    public void setup() {
        eofg = new ExtOutputFilenameGenerator();
    }
    
    @Test
    public void testSetIntervalLengthWithRealLong() throws AdeUsageException {
        eofg.setIntervalLength(2L);
        assertEquals("",2L,eofg.getIntervalLength());
    }
    
    @Test(expected = AdeUsageException.class)
    public void testSetIntervalLengthWithBadValue() throws AdeUsageException {
        eofg.setIntervalLength(-1L);
    }
    
    @Test
    public void testGetInputTimeZone() throws AdeException {
        TimeZone tz = mock(TimeZone.class);
        tz.setID("UTC");
        
        assertEquals("Testing if inputTimeZone returns UTC ","UTC",eofg.getInputTimeZone().getID());
    }
    
    @Test
    public void testGetIntervalXmlFile() throws AdeInternalException, AdeException {
        Date date = new Date(0L);
        File file = new File("file");
        File expectedPath = new File("file/intervalPath/19700101/intervals");
        assertEquals("Testing if the correct path is made. Should return x/y/19700101/intervals"
                ,expectedPath,eofg.getIntervalXmlStorageDir("intervalPath",date,file));
    }
    
    @Test
    public void testgetIntervalXmlFile() throws AdeException {
        File file = new File("file");
        Date date = new Date(1L);
        
        /* Mocking Ade Object to setup dependencies */
        ade = mock(Ade.class, RETURNS_DEEP_STUBS);
        when(ade.getConfigProperties().database().getDatabaseDriver()).thenReturn("derby");
        when(ade.getConfigProperties().getOverrideVersionCheck()).thenReturn(true);
        when(ade.getDbVersion()).thenReturn(new Version(1, 0));
        when(ade.getConfigProperties().getPeriodMode()).thenReturn(PeriodMode.DAILY);
        when(ade.getDirectoryManager().getAnalysisHome()).thenReturn(file);
        Ade.create(ade);
        
        FramingFlowType framingFlowType = mock(FramingFlowType.class);
        when(framingFlowType.getDuration()).thenReturn(2L);
        when(framingFlowType.getPropertyByKey(ContinuousTimeFramer.PERM_SPLIT_FACTOR)).thenReturn("1");
        
        File expectedFile = new File("file/nba?/19700101/intervals/intervals/interval_1.xml");
        assertEquals("Testing xml file path, should be x/y/19700101/intervals/intervalls/interval_1.xml "
                ,expectedFile,eofg.getIntervalXmlFile("nba?", date, file, framingFlowType));
    }
}
