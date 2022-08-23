/*

    Copyright Contributors to the ADE Project.

    SPDX-License-Identifier: GPL-3.0-or-later

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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.os.parser.NginxLogLineParser;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.openmainframe.ade.ext.os.parser.NginxLogParserBase.NGINX_LOG;

public class TestNginxLogLineParser {
    NginxLogLineParser slp;
    String longString;
    @Before
    public void setup() throws AdeException {
        slp = Mockito.spy(NginxLogLineParser.class);
        longString = "(usernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusername"
                + "usernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusername"
                + "usernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusernameusername)";
    }

    @Test
    public void testParseLineWithBadPattern() {
        Pattern pattern = Pattern.compile("^([(][^)]+[)])? CMD [(](.*)[)] ?$");
        assertEquals("Pattern doesnt match ",false, slp.parseLine(pattern,1,4,5,2, 3, 4,"() CMD ()"));
    }

    @Test
    public void testWithRealLog() {
        final Pattern pattern = Pattern.compile(NGINX_LOG);
        final String line = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3 (0.8.16~exp12ubuntu10.21)\"";
        assertEquals(true, slp.parseLine(pattern, 1,2,3,4,5,6,line));
        System.out.println(slp);
    }

//    @Test
//    public void testParseLineWithMatchingPattern() {
//        Pattern pattern = Pattern.compile("^([^:]+):.*COMMAND=(.*)$");
//        assertEquals("Pattern matches for all parameters ",true, slp.parseLine(pattern,1,2,2,2,2,2,"(username):.COMMAND=nub"));
//    }

//    @Test
//    public void testParseLineWith255CharacterHostname() {
//        Pattern pattern = Pattern.compile("^([^:]+):.*COMMAND=(.*)$");
//        assertEquals("Pattern matches but hostname has over 255 chars ",true, slp.parseLine(pattern,1,1,1,1,1,1,longString + ":.COMMAND=nub"));
//    }

//    @Test
//    public void testParseLineWith255CharacterHostnameSecondTime() {
//        Pattern pattern = Pattern.compile("^([^:]+):.*COMMAND=(.*)$");
//        slp.parseLine(pattern,1,1,1,1,1,1,longString + ":.COMMAND=nub");
//
//        assertEquals("Hostname over 255 characters but we go through parseLine twice to skip the logging "
//                ,true,slp.parseLine(pattern,1,1,1,1,1,1,longString + ":.COMMAND=nub"));
//    }

//    @Test
//    public void testGettersGetCorrectInfoAfterRunningParseLine() {
//        Pattern pattern = Pattern.compile("^([^:]+):.*COMMAND=(.*)$");
//        slp.parseLine(pattern,0,1,0,1,0,1,"(username):.COMMAND=nub");
//
//        assertEquals("The message time is thee",null,slp.getMsgTime());
//        assertEquals("The source is in the first matched group third param ","(username)",slp.getRemoteAddress());
//
//        slp.parseLine(pattern,0,0,2,2,2,2,"(PID!):.COMMAND=msgBody");
//        assertEquals("The messsage body is in second group and 6th param","msgBody",slp.getRequest());
//    }

//    @Test
//    public void testToString() {
//        Pattern pattern = Pattern.compile("^([^:]+):.*COMMAND=(.*)$");
//        slp.parseLine(pattern,2,2,2,2,2,2,"(username):.COMMAND=nub");
//        assertEquals("Testing to String works correctly "
//                , "timestamp=(null) "
//                        + "hostname=(nub) "
//                        + "msg=(nub)"
//                ,slp.toString());
//    }
}
