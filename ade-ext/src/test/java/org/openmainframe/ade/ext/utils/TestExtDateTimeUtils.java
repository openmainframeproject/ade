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
package org.openmainframe.ade.ext.utils;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.junit.Test;
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.ext.utils.ExtDateTimeUtils;
import org.openmainframe.ade.utils.patches.Version;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TestExtDateTimeUtils {
    Ade ade;
    
    @Test
    public void testTimestampToHumanDateAndTimeWithDateCoded(){
        long Dec10th2015 = 1449723600000L;
        DateFormat df = new SimpleDateFormat("d MMM yyyy HH:mm:ss", new Locale("en", "US"));
        assertEquals("Using millisecond representation of 12/10/2015 ",df.format(Dec10th2015)
                , ExtDateTimeUtils.timestampToHumanDateAndTime(Dec10th2015));
    }
    
    @Test
    public void testDateToHumanDateAndTimeWithEpochYear(){
        Date newDate = new Date(0L);
        DateFormat df = new SimpleDateFormat("d MMM yyyy HH:mm:ss", new Locale("en", "US"));
        assertEquals("Using Epoch date as the input value ", df.format(newDate) 
                ,ExtDateTimeUtils.dateToHumanDateAndTime(newDate));
    }
    
    @Test
    public void testDateToHumanDateAndTimeWithNullDate(){
        assertEquals("Using null as input date","missing",ExtDateTimeUtils.dateToHumanDateAndTime(null));
    }
    
    @Test
    public void testDateToHumanWithEpochYear(){
        Date newDate = new Date(0L);
        DateFormat df = new SimpleDateFormat("d MMM yyyy", new Locale("en", "US"));
        assertEquals("Input value = Epoch year ",df.format(newDate),ExtDateTimeUtils.dateToHuman(newDate));
    }
    
    @Test
    public void testDateToHumanWithNullDate(){
        assertEquals("Using null as input date","missing",ExtDateTimeUtils.dateToHuman(null));
    }
    
    @Test
    public void testGetCurrentTimeStamp() { 
        Date date = new Date();
        date.setTime(date.getTime());
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        date = ExtDateTimeUtils.getCurrentTimeStamp();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date);
        assertEquals("get current Day ",cal.get(Calendar.DAY_OF_MONTH),cal2.get(Calendar.DAY_OF_MONTH));
        assertEquals("get current Hour ",cal.get(Calendar.MINUTE),cal2.get(Calendar.MINUTE));
        assertEquals("get current Minute ",cal.get(Calendar.HOUR),cal2.get(Calendar.HOUR));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDaysBeforeNegativeValue(){
        assertEquals("1 days before ","",ExtDateTimeUtils.daysBefore(new Date(),-1));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDaysBeforeWithNullDate(){
        assertEquals("null date ","",ExtDateTimeUtils.daysBefore(null,1));
    }
    
    @Test
    public void testDaysBeforeWithRealDate(){
        Date date = new Date();
        date.getTime();
        Date dateMinusOneDay = new Date();
        long millisecondsInOneDay = 86400000L;
        dateMinusOneDay.setTime(date.getTime() - millisecondsInOneDay);
        assertEquals("1 days before input date ",dateMinusOneDay,ExtDateTimeUtils.daysBefore(date,1));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDaysBeforeWithNegativeNumber(){
        assertEquals("1 day before now","", ExtDateTimeUtils.daysBeforeNow(-1));
    }
    
    @Test
    public void testDaysBeforeNowWithRealDate(){
        Date date = new Date();
        date.getTime();
        Date dateMinusOneDay = new Date();
        long millisecondsInOneDay = 86400000L;
        dateMinusOneDay.setTime(date.getTime() - millisecondsInOneDay);
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateMinusOneDay);
        date = ExtDateTimeUtils.daysBeforeNow(1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date);
        assertEquals("1 day before input date ",cal.get(Calendar.DAY_OF_MONTH),cal2.get(Calendar.DAY_OF_MONTH));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDaysAfterWithNegativeNumber(){
        assertEquals("1 day after input date ","",ExtDateTimeUtils.daysAfter(new Date(),-1));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDaysAfterWithNullDate(){
        assertEquals("null date ","",ExtDateTimeUtils.daysAfter(null,1));
    }
    
    @Test
    public void testDaysAfterWithPositiveNumber(){
        Date date = new Date();
        date.getTime();
        Date datePlusOneDay = new Date();
        long millisecondsInOneDay = 86400000L;
        datePlusOneDay.setTime(date.getTime() + millisecondsInOneDay);
        assertEquals("+1 days after input date ",datePlusOneDay,ExtDateTimeUtils.daysAfter(date,1));
    }
    
    @Test
    public void testDaysAfterWithoutTimeZoneWithOneDay(){
        long Dec10th2015 = 1449723600000L;
        long Dec11th2015 = 1449810000000L;
        assertEquals("Dec 10th + 1 day ",new Date(Dec11th2015)
                ,ExtDateTimeUtils.daysAfterWithoutTimeZone(new Date(Dec10th2015),1));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDaysAfterWithoutTimeZoneWithNegativeValue(){
        assertEquals("-1 many days before ","",ExtDateTimeUtils.daysAfterWithoutTimeZone(new Date(),-1));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDaysAfterWithoutTimeZoneWithNullDate(){
        assertEquals("-1 many days before ","",ExtDateTimeUtils.daysAfterWithoutTimeZone(null,1));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDaysAfterTodayWithNegativeNumber(){
        assertEquals("-1 days after now ","",ExtDateTimeUtils.daysAfterNow(-1));
    }
    
    @Test
    public void testDaysAfterTodayWithPositiveNumber(){
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_WEEK, 1);
        Calendar date = new GregorianCalendar();
        date.setTime(ExtDateTimeUtils.daysAfterNow(1));
        assertEquals("1 day after now ",cal.get(Calendar.DAY_OF_WEEK),date.get(Calendar.DAY_OF_WEEK));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testStartOfDayWithNullDate(){
        assertEquals("Null start of day ","", ExtDateTimeUtils.startOfDay(null));
    }
    
    @Test
    public void testStartOfDay() throws ParseException {
        Date date;
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS");
        String dateInString = "10-Dec-2015 00:00:00.000";
        date = formatter.parse(dateInString);
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        assertEquals("Using startOfDay with a real date ",date,ExtDateTimeUtils.startOfDay(date));
    }
   
    @Test(expected = IllegalArgumentException.class)
    public void testEndOfDayWithNullDate(){
        assertEquals("Null end of day ","",ExtDateTimeUtils.endOfDay(null));
    }
    
    @Test
    public void testEndOfDay() throws ParseException {
        Date date;
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS");
        String dateInString = "10-Dec-2015 23:59:59.999";
        date = formatter.parse(dateInString);
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        assertEquals("Using endOfDay with a real date ",cal.getTime(),ExtDateTimeUtils.endOfDay(date));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testStartOfDayUsingOutputTimeZoneWithNullDate() throws AdeException {   
        ade = mock(Ade.class, RETURNS_DEEP_STUBS);
        when(ade.getConfigProperties().database().getDatabaseDriver()).thenReturn("derby");
        when(ade.getConfigProperties().getOverrideVersionCheck()).thenReturn(true);
        when(ade.getDbVersion()).thenReturn(new Version(1, 0));
        Ade.create(ade);
   
        assertEquals("StartOfDateUsingOutputTimeZone with null input ",null
                ,ExtDateTimeUtils.startOfDayUsingOutputTimeZone(null));
    }
    
    @Test
    public void testStartOfDayUsingOutputTimeZoneWithRealDate() throws AdeException {
        Date date = new Date();
        date.setTime(0L);
        assertEquals("StartOfDateUsingOutputTimeZone with real input ",date
                ,ExtDateTimeUtils.startOfDayUsingOutputTimeZone(date));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEndOfDayUsingOutputTimeZoneWithNullDate() throws AdeException {   
        assertEquals("StartOfDateUsingOutputTimeZone with null input ",null
                ,ExtDateTimeUtils.endOfDayUsingOutputTimeZone(null));
    }
    
}