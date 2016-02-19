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
package org.openmainframe.ade.data;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.openmainframe.ade.data.IBasicInterval;
import org.openmainframe.ade.data.IBasicInterval.ByStartTimeComparator;

public class TestByStartTimeComparator {

    @Test
    public void test1stIntervalGreater() {
        IBasicInterval basicInterval1 = mock(IBasicInterval.class);
        IBasicInterval basicInterval2 = mock(IBasicInterval.class);
        ByStartTimeComparator bstc = new ByStartTimeComparator();
        when(basicInterval1.getIntervalStartTime()).thenReturn(2L);
        when(basicInterval2.getIntervalStartTime()).thenReturn(1L);
        assertTrue("basicInterval1 should be larger than basicInterval2.",
                bstc.compare(basicInterval1, basicInterval2)>0);
    }
    
    @Test
    public void test2ndIntervalGreater() {
        IBasicInterval basicInterval1 = mock(IBasicInterval.class);
        IBasicInterval basicInterval2 = mock(IBasicInterval.class);
        ByStartTimeComparator bstc = new ByStartTimeComparator();
        when(basicInterval1.getIntervalStartTime()).thenReturn(1L);
        when(basicInterval2.getIntervalStartTime()).thenReturn(2L);
        assertTrue("basicInterval1 should be less than basicInterval2.",
                bstc.compare(basicInterval1, basicInterval2)<0);
    }
    
    @Test
    public void testEqualIntervals() {
        IBasicInterval basicInterval1 = mock(IBasicInterval.class);
        IBasicInterval basicInterval2 = mock(IBasicInterval.class);
        ByStartTimeComparator bstc = new ByStartTimeComparator();
        when(basicInterval1.getIntervalStartTime()).thenReturn(1L);
        when(basicInterval2.getIntervalStartTime()).thenReturn(1L);
        assertTrue("basicInterval1 should be equal to basicInterval2.",
                bstc.compare(basicInterval1, basicInterval2)==0);
    }
}
