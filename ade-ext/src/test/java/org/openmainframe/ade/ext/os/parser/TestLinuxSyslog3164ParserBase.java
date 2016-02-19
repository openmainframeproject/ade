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
package org.openmainframe.ade.ext.os.parser;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;
import org.openmainframe.ade.ext.os.parser.LinuxSyslog3164ParserBase;
import org.openmainframe.ade.ext.os.parser.LinuxSyslog3164ParserWithCompAndPid;
import org.openmainframe.ade.utils.patches.Version;

public class TestLinuxSyslog3164ParserBase {
    Ade ade;
    
    @Test
    public void testLinuxSyslog3164ParserBaseConstructor() throws AdeException {
        ade = mock(Ade.class, RETURNS_DEEP_STUBS);
        when(ade.getConfigProperties().database().getDatabaseDriver()).thenReturn("derby");
        when(ade.getConfigProperties().getOverrideVersionCheck()).thenReturn(true);
        when(ade.getDbVersion()).thenReturn(new Version(1, 0));
        Ade.create(ade);
        
        TimeZone tz= ade.getAde().getConfigProperties().getInputTimeZone();
        LinuxAdeExtProperties laep = mock(LinuxAdeExtProperties.class); 
        LinuxSyslog3164ParserBase pid = new LinuxSyslog3164ParserWithCompAndPid(laep);
        
        assertEquals("Making a new constructor. It sets the timezone "
                ,DateTimeZone.forOffsetMillis(tz.getRawOffset()),pid.getInputTimeZone());
        assertEquals("Making a new constructor. It sets the timezone "
                ,DateTimeZone.forOffsetMillis(tz.getRawOffset()),pid.getOutputTimeZone());
    }
    
    @Test
    public void testLinuxSyslog3164ParserBaseConstructorWithNullInput() throws AdeException {
        TimeZone tz= ade.getAde().getConfigProperties().getInputTimeZone();
        LinuxSyslog3164ParserBase pid = new LinuxSyslog3164ParserWithCompAndPid(null);
        assertEquals("Making a new constructor wiht null value so LinuxAdeExtProperties is made. It sets the timezone"
                ,DateTimeZone.forOffsetMillis(tz.getRawOffset()),pid.getInputTimeZone());
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testToDateWithIllegalArgumentException() throws AdeException {
        LinuxSyslog3164ParserBase pid = new LinuxSyslog3164ParserWithCompAndPid(null);
        assertEquals("Illegal Argument = bad String ",null,pid.toDate("01/20/2016",""));
    }
    
    @Test 
    public void testToDate() throws AdeException {
        LinuxSyslog3164ParserBase pid = new LinuxSyslog3164ParserWithCompAndPid(null);
        
        LinuxAdeExtProperties laep = mock(LinuxAdeExtProperties.class, RETURNS_DEEP_STUBS); 
        when(laep.isYearDefined()).thenReturn(true);

        pid.setAdeExtProperties(laep);
 
        DateTime date = DateTimeFormat.forPattern("MMM d HH:mm:ss").withZoneUTC().parseDateTime("JAN 20 10:00:00");
        
        assertEquals("toDate with good input. Since yearSetter is null the year will be 1 "
                ,date.withYear(0).toDate(),pid.toDate("1","JAN 20 10:00:00"));
    }
   
}
