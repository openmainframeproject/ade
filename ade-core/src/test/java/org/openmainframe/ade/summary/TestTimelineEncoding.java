/*
 
    Copyright IBM Corp. 2010, 2016
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

package org.openmainframe.ade.summary;

import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.openmainframe.ade.impl.AdeTest;
import org.openmainframe.ade.summary.SummarizationProperties;

import junit.framework.AssertionFailedError;

public class TestTimelineEncoding extends AdeTest {

    private static int NUM_TESTS = 30;
    private static int RUNS_PER_TEST = 100;
    private static short MIN_TIMELINE_LENGTH = 50;
    private static short MAX_TIMELINE_LENGTH = 1500;

    private Random rand = new Random(new Date().getTime());

    public void testTimelineEncodings() {
        for (int i = 0; i < NUM_TESTS; i++) {
            for (int j = 0; j < RUNS_PER_TEST; j++) {
                // generate timeline length
                int timelineLength = rand.nextInt(MAX_TIMELINE_LENGTH - MIN_TIMELINE_LENGTH) + MIN_TIMELINE_LENGTH;

                // create timeline and generate it
                short[] timeline = new short[timelineLength];
                for (int k = 0; k < timeline.length; k++) {
                    timeline[k] = (short) rand.nextInt(MAX_TIMELINE_LENGTH);
                }

                // encode and decode
                String encodedTimeline = SummarizationProperties.encodeTimeLine(timeline);
                short[] decodedTimeline = SummarizationProperties.decodeTimeLine(encodedTimeline);

                // create sets of timeline original and decoded elements
                Set<Short> timelineSet = new TreeSet<Short>();
                for (short t : timeline) {
                    timelineSet.add(t);
                }
                Set<Short> decodedTimelineSet = new TreeSet<Short>();
                for (short t : decodedTimeline) {
                    decodedTimelineSet.add(t);
                }

                // compare sets
                try {
                    assert timelineSet.containsAll(decodedTimelineSet) && decodedTimelineSet.containsAll(timelineSet);
                } catch (AssertionFailedError e) {
                    System.out.println("Failed assertEqual");
                    System.out.println("timeline=" + Arrays.toString(timeline));
                    System.out.println("encodedTimeline=" + encodedTimeline);
                    System.out.println("decodedTimeline=" + Arrays.toString(decodedTimeline));
                    throw e;
                }
            }
        }
    }
}
