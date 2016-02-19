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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.openmainframe.ade.ext.os.parser.LinuxSyslog5424ParserBase;

public class TestLinuxSyslog5424ParserBase {
    LinuxSyslog5424ParserBase ls5424pb;
    String source;
    String pattern;
    Date millisecondsOfDateWithoutOffset;
    Date millisecondsOfDateWithOffset;  
    @Before
    public void setup() {
        ls5424pb = new LinuxSyslog5424ParserBase();
        source = "";
        millisecondsOfDateWithoutOffset = new Date(-18000000L);
        millisecondsOfDateWithOffset = new Date(0L);  
    }
    
    @Test
    public void testToDateWithSecondPatternFormatter() {
        pattern = "1969-12-31T19:00:00-05:00";
        assertEquals("Input time pattern is yyyy-MM-dd'T'H:m:sZ ",millisecondsOfDateWithOffset,ls5424pb.toDate(source,pattern));
    }
    
    @Test
    public void testToDateWithThirdPatternFormatter() {
        pattern = "1969-12-31T19:00:00-05:00";
        assertEquals("Input time pattern is yyyy-MM-dd'T'H:m:s.SSSSSSZZ",millisecondsOfDateWithOffset,ls5424pb.toDate(source,pattern));
    }
    
    @Test
    public void testToDateWithFourthPatternFormatter() {
        pattern = "1969-12-31T19:00:00z"; 
        assertEquals("Input time pattern is yyyy-MM-dd'T'H:m:sZZ ",millisecondsOfDateWithoutOffset,ls5424pb.toDate(source,pattern));
    }
    
    @Test
    public void testGetLastDeterminedTestDateTime() throws ParseException {
        pattern = "1969-12-31T19:00:00z";
        DateTime date = DateTime.parse(pattern);
        assertEquals("Input time pattern is yyyy-MM-dd'T'H:m:sZ ",millisecondsOfDateWithoutOffset,ls5424pb.toDate("",pattern));
        assertEquals("Making sure toDate sets the right value ",date,ls5424pb.getLastDeterminedDateTime());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testToDateWithEmptyStrings() {
        String s = "";
        ls5424pb.toDate(source,s);
    }
    
    @Test
    public void testParseLineWithABadString() {
        assertEquals("Giving ParseLine a bad string ",false,ls5424pb.parseLine("bad string"));
    }
    
    @Test
    public void testParseLineWithASyslog() {       
        String example5424Syslog = "<46>1 2014-05-30T08:52:40.620950-04:00 host-name rsyslogd  - - rsyslogd's groupid changed to 103";
        assertEquals("Input is an example syslog ",true,ls5424pb.parseLine(example5424Syslog));
    }
    
    @Test
    public void testParseLineWithASyslogThatHasNilComponent() {  
        String syslogWithNilComponent = "<46>1 2014-05-30T08:52:40.620950-04:00 host-name -  - - rsyslogd's groupid changed to 103";
        assertEquals("Testing for correct handling of nil component ",true,ls5424pb.parseLine(syslogWithNilComponent));
    }
    
    @Test
    public void testParseLineWithASyslogThatHasNilPid() {  
        String syslogWithNilPid = "<46>1 2014-05-30T08:52:40.620950-04:00 host-name rsyslogd - - - rsyslogd's groupid changed to 103";
        assertEquals("Testing for correct handling of nil Pid ",true,ls5424pb.parseLine(syslogWithNilPid));
    }
}
