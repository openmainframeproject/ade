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
package org.openmainframe.ade.impl.flow.modules;

import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IIntervalClassification;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.actions.Action;
import org.openmainframe.ade.impl.data.PeriodAndSerialNumFinder;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.hub.HubFrameableFramingBlock;
import org.openmainframe.ade.summary.SummarizationProperties;

/**
 * This class defines the 
 *
 */
public abstract class IntervalBuilder
        extends HubFrameableFramingBlock<IMessageInstance, Separator, IInterval, TimeSeparator> {

    protected ISource m_source;
    protected SummarizationProperties m_sumProps;
    protected FramingFlowType m_framingFlowType;
    protected IIntervalClassification m_intervalClassification;
    protected Action m_action;
    protected PeriodAndSerialNumFinder m_numFinder;

    public final void setSource(ISource source) {
        m_source = source;
    }

    public final void setSumProps(SummarizationProperties sumProps) {
        m_sumProps = sumProps;
    }

    public final void setFramingFlowType(FramingFlowType framingFlowType) throws AdeException {
        m_framingFlowType = framingFlowType;
        setIntervalNumFinderFlow(framingFlowType);
    }

    /**
     * use this to overwrite the default numbering setup. Needed for {@note AccumulativeIntervalBuilder}
     * @param framingFlowType the new parameters for the {@link PeriodAndSerialNumFinder}
     * @throws AdeException if there is trouble getting the periodMode from the Ade Configuration Properties
     */
    public final void setIntervalNumFinderFlow(FramingFlowType framingFlowType)
            throws AdeException {
        m_numFinder = new PeriodAndSerialNumFinder(framingFlowType);
    }

    public FramingFlowType getFramingFlowType() {
        return m_framingFlowType;
    }

    public final void setIntervalClassification(IIntervalClassification intervalClassification) {
        m_intervalClassification = intervalClassification;
    }

    public final void setAction(Action action) {
        m_action = action;
    }

    public abstract void init() throws AdeException;

}
