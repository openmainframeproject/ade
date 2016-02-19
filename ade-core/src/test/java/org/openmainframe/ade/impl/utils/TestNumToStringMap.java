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

import java.util.Random;

import org.junit.Assert;
import org.openmainframe.ade.impl.utils.NumStringMap;

import junit.framework.TestCase;

public class TestNumToStringMap extends TestCase {

    private Random m_random;

    public void testNumToStringMap() {
        m_random = new Random(3212323);
        NumStringMap nsm = new NumStringMap();
        boolean currentUsed[] = new boolean[30];

        verifyStatus(nsm, currentUsed);

        for (int i = 0; i < 100; ++i) {
            randomStep(nsm, currentUsed);
            verifyStatus(nsm, currentUsed);
        }
    }

    private void randomStep(NumStringMap nsm, boolean currentUsed[]) {
        int i = m_random.nextInt(currentUsed.length);
        if (currentUsed[i]) {
            nsm.removeEntry(i);
            currentUsed[i] = false;
        } else {
            nsm.put(word(i), i);
            currentUsed[i] = true;
        }
    }

    private void verifyStatus(NumStringMap nsm, boolean currentUsed[]) {
        int count = 0;
        for (int i = 0; i < currentUsed.length; ++i) {
            int idTrue = nsm.getIDFromString(word(i));
            int idFalse = nsm.getIDFromString("X" + word(i));
            String wordTrue = nsm.getStringFromID(i);
            String wordFalse = nsm.getStringFromID(i + 100);

            Assert.assertNull("res1", wordFalse);
            Assert.assertEquals("res2", NumStringMap.InvalidID, idFalse);
            if (currentUsed[i]) {
                Assert.assertEquals("res3", i, idTrue);
                Assert.assertEquals("res4", word(i), wordTrue);
                ++count;
            } else {
                Assert.assertEquals("res2", NumStringMap.InvalidID, idTrue);
                Assert.assertNull("res4", wordTrue);
            }

        }

        Assert.assertEquals("num", count, nsm.getMappingCount());

        Assert.assertEquals("num2", count, nsm.getIds().size());

        System.out.println("Verified with " + count + " values");
    }

    private String word(int i) {
        return "Word" + i;
    }

}
