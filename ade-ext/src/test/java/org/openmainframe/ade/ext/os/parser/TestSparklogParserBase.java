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
import java.util.Calendar;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;
import org.openmainframe.ade.ext.os.parser.SparklogParserBase;
import org.openmainframe.ade.ext.os.parser.SparklogParser;
import org.openmainframe.ade.utils.patches.Version;

public class TestSparklogParserBase {
    Ade ade;

    public void setup() throws AdeException{
        ade = mock(Ade.class, RETURNS_DEEP_STUBS);
        when(ade.getConfigProperties().database().getDatabaseDriver()).thenReturn("derby");
        when(ade.getConfigProperties().getOverrideVersionCheck()).thenReturn(true);
        when(ade.getDbVersion()).thenReturn(new Version(1, 0));
        Ade.create(ade);
    }
    
    @Test
    public void testSparklogParserBaseConstructor() throws AdeException {
        TimeZone tz= ade.getAde().getConfigProperties().getInputTimeZone();
        LinuxAdeExtProperties laep = mock(LinuxAdeExtProperties.class); 
        SparklogParserBase pid = new SparklogParser(laep);
        
        assertEquals("Making a new constructor. It sets the timezone "
                ,DateTimeZone.forOffsetMillis(tz.getRawOffset()),pid.getInputTimeZone());
        assertEquals("Making a new constructor. It sets the timezone "
                ,DateTimeZone.forOffsetMillis(tz.getRawOffset()),pid.getOutputTimeZone());
    }
    
    @Test
    public void testSparklogParserBaseConstructorWithNullInput() throws AdeException {
        TimeZone tz= ade.getAde().getConfigProperties().getInputTimeZone();
        SparklogParserBase pid = new SparklogParser(null);
        assertEquals("Making a new constructor wiht null value so LinuxAdeExtProperties is made. It sets the timezone"
                ,DateTimeZone.forOffsetMillis(tz.getRawOffset()),pid.getInputTimeZone());
    }
    
    @Test 
    public void testToDate() throws AdeException {
        LinuxAdeExtProperties laep = mock(LinuxAdeExtProperties.class, RETURNS_DEEP_STUBS);
        SparklogParserBase pid = new SparklogParser(laep);

        when(laep.isYearDefined()).thenReturn(true);

        pid.setAdeExtProperties(laep);
        DateTime date = DateTimeFormat.forPattern("dd/MM/yy HH:mm:ss").withZoneUTC().parseDateTime("01/02/03 10:05:25");
        
        assertEquals("toDate with good input. Since yearSetter is null the year will be 1 "
                ,date.toDate(),pid.toDate("","01/02/03 10:05:25"));
    }


    @Test
    public void testRegexPatternsTimeStamp() throws AdeException{
    setup();
    String line = "17/06/08 14:37:39 INFO ExecutorRunnable: Starting Executor Container";
    SparklogParser s = new SparklogParser(null);
    s.parseLine(line);
    Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    // Time Stamp checks
    c.setTime(s.getMsgTime());
    assertEquals(c.get(Calendar.YEAR), 2008);
    assertEquals(c.get(Calendar.MONTH), 5);
    assertEquals(c.get(Calendar.DAY_OF_MONTH), 17);
    assertEquals(c.get(Calendar.HOUR_OF_DAY), 14);
    assertEquals(c.get(Calendar.MINUTE), 37);
    assertEquals(c.get(Calendar.SECOND), 39);

    // Tests for source , component and message body
    assertEquals("info", s.getSource());
    //assertEquals("ExecutorRunnable", s.getComponent());
    assertEquals("master", s.getComponent());
    assertEquals("Starting Executor Container", s.getMessageBody());
    }
   
}
