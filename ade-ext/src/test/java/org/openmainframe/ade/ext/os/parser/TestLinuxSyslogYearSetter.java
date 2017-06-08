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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.openmainframe.ade.ext.os.parser.LinuxSyslogYearSetter;

public class TestLinuxSyslogYearSetter {
    LinuxSyslogYearSetter lsys;
    
    @Test
    public void testGetDesiredYearWithLastSeenMessageNotNull() {
        lsys = new LinuxSyslogYearSetter("", 0);
        DateTimeZone dtz = DateTimeZone.forID("America/New_York");
        DateTime dt = new DateTime(dtz);
        lsys.getDesiredYear(dt);
        assertEquals("Last seen message != null ",0,lsys.getDesiredYear(dt));
    }
    
    @Test
    public void testGetDesiredYearWithMsgTimeDiffLessThanIncrementDays() {
        lsys = new LinuxSyslogYearSetter("", 1988);
        DateTimeZone dtz = DateTimeZone.forID("Europe/Berlin");
        DateTime dt = new DateTime(dtz);
        assertEquals("messageTimeDiff < INCREMENT_DAYS_LIMIT_IN_MILLIS ",1988,lsys.getDesiredYear(dt));
    }
    
    @Test
    public void testSetYear() {
        lsys = new LinuxSyslogYearSetter(null, -1);
        lsys.setYear(1988);
        DateTimeZone dtz = DateTimeZone.forID("Europe/Berlin");
        DateTime dt = new DateTime(dtz);
        assertEquals("Setting year ",1988,lsys.getDesiredYear(dt));
    }
    
    @Test
    public void testGetDesiredYearWithNegativeMessageTimeDifference() {
        lsys = new LinuxSyslogYearSetter("", -100);
        DateTimeZone dtz = DateTimeZone.forID("Europe/Berlin");
        DateTime dt = new DateTime(dtz);
        lsys.getDesiredYear(dt);
        
        dtz = DateTimeZone.forID("America/New_York");
        dt = new DateTime(dtz);
        assertEquals("messageTimeDifference < 0 ",-100,lsys.getDesiredYear(dt));
    }

    @Test
    public void testGetDesiredYearWithTimeDifferenceOverAllowedTime() {
        lsys = new LinuxSyslogYearSetter("", -2);
        DateTimeZone dtz;
        dtz = DateTimeZone.forID("America/New_York");
        DateTime dt = new DateTime(1L, dtz);
        lsys.getDesiredYear(dt);

        dt = new DateTime(dtz);
        int actual = lsys.getDesiredYear(dt);
        assertEquals("absolute messageTimeDifference > DECREMENT_DAYS_ALLOWANCE_IN_MILLIS ",-1, actual);
    }
    
    @Test
    public void testGetDesiredYearWithMessageDifferenceUnderIncrementDaysLimit() {
        lsys = new LinuxSyslogYearSetter("", 2014);
        DateTime dt = new DateTime(0L);
        lsys.getDesiredYear(dt);
        
        lsys.setYear(2015);
        
        dt = new DateTime(0L);
        assertEquals("messageTimeDifference > INCREMENT_DAYS_LIMIT_IN_MILLIS ",2014,lsys.getDesiredYear(dt));
    }
    
    @Test
    public void testToString() {
        String source = "aSource";
        int nineties = 1990;
        lsys = new LinuxSyslogYearSetter(source, nineties);
        DateTimeZone dtz = DateTimeZone.forID("America/New_York");
        DateTime dt = new DateTime(0L, dtz);
        lsys.getDesiredYear(dt);

        String actual = lsys.toString();
        assertEquals("Testing toString output "," source="+ source
                        + " year="+ nineties
                        + " lastSeenMessage=1 Jan 1991 00:00:00(662688000000)", actual);
    }
}
