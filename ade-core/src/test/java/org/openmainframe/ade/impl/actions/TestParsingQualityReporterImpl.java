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
package org.openmainframe.ade.impl.actions;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.*;

import org.junit.*;
import org.openmainframe.ade.impl.actions.ParsingQualityReporterImpl;

public class TestParsingQualityReporterImpl {
    private ByteArrayOutputStream bytes;
    private ParsingQualityReporterImpl reporter;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        /* Setup mocks */
        bytes = new ByteArrayOutputStream();
        reporter = new ParsingQualityReporterImpl(new PrintStream(bytes));
    }

    @Test
    public void testLineErrorShowsError() throws UnsupportedEncodingException {

        reporter.lineError("scary error", "this error is very bad", 5, "I'm an offending line");
        String results = bytes.toString("UTF-8");
        System.out.println(results);
        assertThat(results, allOf(containsString("scary error"), containsString("5"), containsString("this error is very bad"), containsString("I'm an offending line")));
    }
    
    @Test
    public void testLineErrorDoesntShowMoreThanMaxOfType() throws UnsupportedEncodingException {
        String results;
        for (int i = 0; i < 3; i++) {
            reporter.lineError("scary error", "this error is very bad", i, "I'm an offending line");
            results = bytes.toString("UTF-8");
            assertThat(results, allOf(containsString("scary error"), containsString(Integer.toString(i)), containsString("this error is very bad"), containsString("I'm an offending line")));
        }
        reporter.lineError("scary error", "this error is very bad", 34, "I'm the 4th offensive line" );
        results = bytes.toString("UTF-8");
        assertThat(results, allOf(containsString("It will no longer be reported."), not(containsString("Error at line 34"))));
    }

}
