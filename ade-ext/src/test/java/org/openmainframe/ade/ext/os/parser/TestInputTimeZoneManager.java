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

import java.util.Calendar;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.os.parser.InputTimeZoneManager;

public class TestInputTimeZoneManager {

    @Test(expected = AdeException.class)
    public void TestUpdateTimezoneWithNullDate() throws AdeException {
        InputTimeZoneManager.updateTimezone("5", null);
        assertEquals("getTimezone with 1L ","GMT-05:00",InputTimeZoneManager.getTimezone("5"));
    }
    

    
    @Test
    public void TestGetTimezoneWithNullDateAndNonNullConfig() throws AdeException{
        InputTimeZoneManager.s_configuredInputTimeZone = "1";
        assertEquals("getTimezone null date but not a null configuredInputTimezone ","1",
                InputTimeZoneManager.getTimezone(null));
    }
    
    @Test(expected = AdeException.class)
    public void TestGetTimezoneWithNullSourceID() throws AdeException{
        assertEquals("getTimezone null sourceID ","",InputTimeZoneManager.getTimezone(null));
    }
}
