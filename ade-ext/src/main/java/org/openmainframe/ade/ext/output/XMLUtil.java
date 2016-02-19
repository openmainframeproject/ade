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
package org.openmainframe.ade.ext.output;

import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.modules.ContinuousTimeFramer;
import org.openmainframe.ade.impl.utils.DateTimeUtils;

/**
 * XML utility class to retrieve hardened XML meta-data.
 */
public final class XMLUtil {

    private XMLUtil(){}
    
    /**
     * Returns the hardened XML duration in milliseconds.
     * @return
     */
    public static long getXMLHardenedDurationInMillis(FramingFlowType fft) {
        String permSplitFactor = fft.getPropertyByKey(ContinuousTimeFramer.PERM_SPLIT_FACTOR);
        return (long) (fft.getDuration() / Short.valueOf(permSplitFactor));
    }
    
    /**
     * Return the XML Hardening Duration in Seconds.
     * @return
     */
    public static int getXMLHardenedDurationInSeconds(FramingFlowType fft) {
        return (int) (XMLUtil.getXMLHardenedDurationInMillis(fft) / DateTimeUtils.MILLIS_IN_SECOND);
    }
    
    /**
     * Return the number of snapshots ie. the number of intervals or xml files generated in a day.
     */
    public static int getNumberOfSnapshots(FramingFlowType fft) {
        long minutesInDay = DateTimeUtils.HOURS_IN_DAY * DateTimeUtils.MINUTES_IN_HOUR;
        long numMinutesPerSnapShot = (XMLUtil.getXMLHardenedDurationInMillis(fft) / DateTimeUtils.MILLIS_IN_MINUTE);
        return (int) (minutesInDay / numMinutesPerSnapShot);
    }
}
