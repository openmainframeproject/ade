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

import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.ext.os.parser.LinuxSyslogLineParser;

public class TestLinuxSyslogLineParser {
    LinuxSyslogLineParser lslp;
    String longString;
    @Before
    public void setup() {
        lslp = Mockito.spy(LinuxSyslogLineParser.class);
        longString = "(usernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusername"
                + "usernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusername"
                + "usernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusername)";
    }
    
    @Test
    public void testIsSyslogNgRestartedBadCompID() {
        assertEquals("Bad Comp ID",false,lslp.isSyslogNgRestarted("badID","badMSG"));
    }
    
    @Test
    public void testIsSyslogNgRestartedGoodCompIDWithFailedMatch() {
        assertEquals("Good Comp ID with bad body paaram ",false,lslp.isSyslogNgRestarted("syslog-ng","badMSG"));
    }
    
    @Test
    public void testIsSyslogNgRestartedGoodCompIDWithMatch() {
        assertEquals("Good Comp ID with good body param ",true,lslp.isSyslogNgRestarted("syslog-ng","starting"));
        assertEquals("Good Comp ID with good body param ",true,lslp.isSyslogNgRestarted("syslog-ng","configuration initialized"));
        assertEquals("Good Comp ID with good body param ",true,lslp.isSyslogNgRestarted("syslog-ng","reloading configuration"));
    }
    
    @Test
    public void testParseLineWithBadPattern() { 
        Pattern pattern = Pattern.compile("^([(][^)]+[)])? CMD [(](.*)[)] ?$");
        assertEquals("Pattern doesnt match ",false,lslp.parseLine(pattern,1,2,3,4,5,"() CMD ()"));
    }
    
    @Test
    public void testParseLineWithMatchingPattern() {
        Pattern pattern = Pattern.compile("^([^:]+):.*COMMAND=(.*)$");
        assertEquals("Pattern matches for all parameters ",true,lslp.parseLine(pattern,1,2,2,2,2,"(username):.COMMAND=nub"));
    }
    
    @Test
    public void testParseLineWith255CharacterHostname() { 
        Pattern pattern = Pattern.compile("^([^:]+):.*COMMAND=(.*)$");
        assertEquals("Pattern matches but hostname has over 255 chars ",true,lslp.parseLine(pattern,1,1,1,1,1,longString + ":.COMMAND=nub"));
    }
    
    @Test
    public void testParseLineWith255CharacterHostnameSecondTime() { 
        Pattern pattern = Pattern.compile("^([^:]+):.*COMMAND=(.*)$");
        lslp.parseLine(pattern,1,1,1,1,1,longString + ":.COMMAND=nub");
        
        assertEquals("Hostname over 255 characters but we go through parseLine twice to skip the logging "
                ,true,lslp.parseLine(pattern,1,1,1,1,1,longString + ":.COMMAND=nub"));
    }
    
    @Test
    public void testGettersGetCorrectInfoAfterRunningParseLine() {
        Pattern pattern = Pattern.compile("^([^:]+):.*COMMAND=(.*)$");
        lslp.parseLine(pattern,0,1,2,0,0,"(username):.COMMAND=nub");
        
        assertEquals("The message time is thee",null,lslp.getMsgTime());
        assertEquals("The source is in the first matched group third param ","(username)",lslp.getSource());
        assertEquals("The component is in the second matched group fourth param ","nub",lslp.getComponent());
        assertEquals("Severity is never set so it is UNKNOWN ",Severity.UNKNOWN,lslp.getSeverity());
        
        lslp.parseLine(pattern,0,0,0,1,2,"(PID!):.COMMAND=msgBody");
        assertEquals("The PID is in the first group and 5th param ","(PID!)",lslp.getPid());
        assertEquals("The messsage body is in second group and 6th param","msgBody",lslp.getMessageBody());
    }
    
    @Test
    public void testToString() {
        Pattern pattern = Pattern.compile("^([^:]+):.*COMMAND=(.*)$");
        lslp.parseLine(pattern,2,2,2,2,2,"(username):.COMMAND=nub");
        assertEquals("Testing to String works correctly "
                , "timestamp=(null) "
                + "hostname=(nub) "
                + "comp=(nub) "
                + "pid=(nub) "
                + "msg=(nub)"
                ,lslp.toString());
    }
}
