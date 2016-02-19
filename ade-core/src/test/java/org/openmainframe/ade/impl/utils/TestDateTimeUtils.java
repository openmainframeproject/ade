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
package org.openmainframe.ade.impl.utils;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.utils.DateTimeUtils;

public class TestDateTimeUtils {

    @Test
    public void testMillisecondsToHumanTimePeriodLessThanHalfOfMillisecondsInSec(){
        Long period = 10000L;
        assertEquals("Period of time % Milliseconds in a second < 500L","00:00:10 (hh:mm:ss)",DateTimeUtils.millisecondsToHumanTime(period));
    }
    
    @Test
    public void testMillisecondsToHumanTimePeriodMoreThanHalfOfMillisecondsInSec(){
        Long period = 500L;
        assertEquals("Period of time time % Milliseconds in a second >= 500L","00:00:01 (hh:mm:ss)",DateTimeUtils.millisecondsToHumanTime(period));
    }
    
    @Test
    public void testMillisecondsToDecimalHumanTimePeriodLessThanMillisecondsInSec(){
        Long period = 999L;
        assertEquals("Period of time < 1 second","999 milliseconds",DateTimeUtils.millisecondsToDecimalHumanTime(period));
    }
    
    @Test
    public void testMillisecondsToDecimalHumanTimePeriodLessThanMillisecondsInMin(){
        Long period = 1001L;
        assertEquals("Period of time < 1 minute","1 seconds",DateTimeUtils.millisecondsToDecimalHumanTime(period));
    }
    
    @Test
    public void testMillisecondsToDecimalHumanTimePeriodMoreThanMillisecondsInMin(){
        Long period = 60001L;
        assertEquals("Period of time >= 1 minute","1 minutes",DateTimeUtils.millisecondsToDecimalHumanTime(period));
    }
    
    @Test
    public void testMillisecondsToDecimalHumanTimePeriodLessThanMillisecondsInHour(){
        Long period = 360000L;
        assertEquals("Period of time < 1 hour","6 minutes",DateTimeUtils.millisecondsToDecimalHumanTime(period));
    }
    
    @Test
    public void testMillisecondsToDecimalHumanTimePeriodMoreThanMillisecondsInHour(){
        Long period = 3600001L;
        assertEquals("Period of time >= 1 hour","1 hours",DateTimeUtils.millisecondsToDecimalHumanTime(period));
    }
    
    @Test
    public void testMillisecondsToDecimalHumanTimePeriodLessThanMillisecondsInDay(){
        Long period = 82800000L;
        assertEquals("Period of time < 1 day","23 hours",DateTimeUtils.millisecondsToDecimalHumanTime(period));
    }
    
    @Test
    public void testMillisecondsToDecimalHumanTimePeriodMoreThanMillisecondsInDay(){
        Long period = 86400000L;
        assertEquals("Period of time >= to 1 day","1 days",DateTimeUtils.millisecondsToDecimalHumanTime(period));
    }
    
    @Test 
    public void testGetNewGmtSimpleDateFormat(){
        SimpleDateFormat sdf = new SimpleDateFormat("1");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf.setLenient(true);
        assertEquals("testGMT",sdf, DateTimeUtils.getNewGmtSimpleDateFormat("1"));
    }
    
    @Test
    public void testGetGmtGregorianCalendar(){
        TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");
        Calendar res = new GregorianCalendar(GMT_TIMEZONE);
        // Monday = 2.
        res.setFirstDayOfWeek(2); 
        res.set(Calendar.YEAR, 2015);
        res.set(Calendar.MONTH, 12);
        res.set(Calendar.DAY_OF_MONTH, 7);
        res.set(Calendar.HOUR_OF_DAY, 0);
        res.set(Calendar.MINUTE, 0);
        res.set(Calendar.SECOND, 0);
        res.set(Calendar.MILLISECOND, 0);
        assertEquals("Making a Gregorian Calendar ",res,DateTimeUtils.getGmtGregorianCalendar(2015,12,7));
    }
    
    @Test
    public void testParseTimeZoneGMT1() throws AdeUsageException{
        assertEquals("Parsing GMT ",TimeZone.getTimeZone("GMT"),DateTimeUtils.parseTimeZone("GMT"));
    }
    
    @Test
    public void testParseTimeZoneGMT2() throws AdeUsageException{
        assertEquals("Parsing GMT+0 ",TimeZone.getTimeZone("GMT"),DateTimeUtils.parseTimeZone("GMT+0"));
    }
    
    @Test(expected = AdeUsageException.class)
    public void testParseTimeZoneEmpty() throws AdeUsageException{
        assertEquals("Parsing an empty string ",TimeZone.getTimeZone("GMT"),DateTimeUtils.parseTimeZone(""));
    }
    
    @Test
    public void testParseDurationEmpty() throws AdeUsageException{
        assertEquals("Parsing duration with an empty string ",0,DateTimeUtils.parseDuration(""));
    }
    
    @Test
    public void testParseDurationBadString() throws AdeUsageException{
        assertEquals("Parsing duration with a bad string ",0,DateTimeUtils.parseDuration("abc"));
    }
    
    @Test
    public void testParseDurationDigit() throws AdeUsageException{
        assertEquals("Parsing duration with a digit as a string ",0,DateTimeUtils.parseDuration("1"));
    }
    
    @Test
    public void testParseDurationLetter() throws AdeUsageException{
        assertEquals("Parsing duration with a letter as a string ",0,DateTimeUtils.parseDuration("A"));
    }
    
    @Test
    public void testParseDuration1m() throws AdeUsageException{
        assertEquals("Parsing duration 1m (minute) ",60000,DateTimeUtils.parseDuration("1m"));
    }
   
    @Test
    public void testParseDuration1H() throws AdeUsageException{
        assertEquals("Parsing duration 1H (1 hour) ",3600000,DateTimeUtils.parseDuration("1H"));
    }
    
    @Test
    public void testParseDuration1D() throws AdeUsageException{
        assertEquals("Parsing duration 1D (1 day) ",86400000,DateTimeUtils.parseDuration("1D"));
    }
    
    @Test
    public void testParseDuration1W() throws AdeUsageException{
        assertEquals("Parsing duration 1W (1 week)",604800000,DateTimeUtils.parseDuration("1W"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testParseDurationBadInput() throws AdeUsageException{
        assertEquals("Parsing duration with a bad input ",0,DateTimeUtils.parseDuration("1a"));
    }
   
}
