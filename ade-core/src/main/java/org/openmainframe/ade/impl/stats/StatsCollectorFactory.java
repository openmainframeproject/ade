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
package org.openmainframe.ade.impl.stats;

import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.flow.IStreamTarget;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;

public final class StatsCollectorFactory {
    
    private StatsCollectorFactory() {
        // Private constructor to hide the implicit public one.
    }

    public static IStreamTarget<IInterval> newIntervalStatsCollector(String analysisGroup,
            FramingFlowType framingFlowType) throws AdeException {
        return IntervalStatsCollector.getIntervalStatCollector(analysisGroup, framingFlowType);
    }

    public static IFrameableTarget<IMessageInstance,
    TimeSeparator> newMessageInstanceStatsCollector(String analysisGroup) throws AdeException {
        return new MessageInstanceStatsCollector(analysisGroup);
    }

    public static void closeAllIntervalStatCollectors() throws AdeException {
        IntervalStatsCollector.closeAllIntervalStatCollectors();
    }
}
