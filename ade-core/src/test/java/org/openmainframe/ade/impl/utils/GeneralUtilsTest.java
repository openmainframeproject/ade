/*
 
    Copyright IBM Corp. 2009, 2016
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

import org.junit.Test;
import org.openmainframe.ade.impl.utils.GeneralUtils;

public class GeneralUtilsTest {

    @Test
    public void testCleanStringDoesNotRemoveValidASCII() throws Exception {
        String t1 = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
        assertEquals("All ASCII string", t1, GeneralUtils.cleanString(t1));
    }

    @Test
    public void testCleanStringRemovesOutOfRangeASCII() throws Exception {
        char vals[] = { 1, 2, 3, 4, 5, 6, 30, 31, 128, 129 };
        assertEquals("Clean all values", "          ", GeneralUtils.cleanString(new String(vals)));
    }

    @Test
    public void testCleanStringRemovesOutOfRangeASCII2() throws Exception {
        char vals[] = { 129, 2, 3, 4, 5, 6, 30, 31, 128, 129 };
        assertEquals("Clean all values", "          ", GeneralUtils.cleanString(new String(vals)));
    }

    @Test
    public void testCleanComboOfValidAndOutOfRangeASCII() throws Exception {
        char vals[] = { 1, 2, 3, 'a', 'b', 'c', 128, 129, 'D', 'E', 'F' };
        assertEquals("Clean some values", "   abc  DEF", GeneralUtils.cleanString(new String(vals)));
    }

    @Test
    public void testCleanStringReturnsNullForInputNull() throws Exception {
        assertNull("Verify that if the input is null then the output should be null.", GeneralUtils.cleanString(null));
    }

}
