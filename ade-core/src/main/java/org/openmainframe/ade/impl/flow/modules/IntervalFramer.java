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

import java.util.Properties;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IIntervalClassification;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.actions.Action;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.factory.jaxb.PropertyType;
import org.openmainframe.ade.impl.flow.hub.HubFrameableFramingBlock;
import org.openmainframe.ade.summary.SummarizationProperties;

/**
 * This is the abstract class for all time framing objects. This class handles incoming {@link IMessageInstance} objects
 * separated by {@link TimeSeparator} separators, and send {@link IMessageInstance} objects separated by
 * {@link Separator} objects.
 */
public abstract class IntervalFramer extends
        HubFrameableFramingBlock<IMessageInstance, TimeSeparator, IMessageInstance, Separator> {

    protected FramingFlowType m_framingFlow;

    private Properties m_props = new Properties();

    /**
     * Construct a new IntervalFramer.
     * 
     * @param framingFlow - the properties for the {@link IntervalFramer} constructor
     * @throws AdeException see {@link Ade#getAde()}
     */
    public IntervalFramer(FramingFlowType framingFlow) throws AdeException {
        this();
        setFramingFlow(framingFlow);
    }

    /**
     * Construct a new IntervalFramer.
     */
    public IntervalFramer() {
        super();
    }

    protected final void setFramingFlow(FramingFlowType framingFlow) throws AdeException {
        m_framingFlow = framingFlow;

        setPropsFromFramingFlowType(framingFlow);
    }

    protected final void setPropsFromFramingFlowType(FramingFlowType framingFlow) {
        for (PropertyType property : framingFlow.getFramerProperty()) {
            m_props.setProperty(property.getKey(), property.getValue());
        }
    }

    /**
     * Get the {@link FramingFlowType}.
     * 
     * @return the {@link FramingFlowType}.
     */
    public FramingFlowType getFramingFlowType() {
        return m_framingFlow;
    }

    protected final String getProp(String key) {
        return m_props.getProperty(key);
    }

    /**
     * Create and return an {@link IntervalBuilder}.
     * 
     * @param source the source.
     * @param sumProps the {@link IMessageSummary} properties.
     * @param framingFlowType the time frame properties.
     * @param intervalClassicication the type of interval classification.
     * @param action the type of action.
     * @return an {@link IntervalBuilder}.
     * @throws AdeException if an {@link IntervalBuilder} cannot be created.
     */
    public abstract IntervalBuilder getIntervalBuilder(
            ISource source, SummarizationProperties sumProps,
            FramingFlowType framingFlowType,
            IIntervalClassification intervalClassicication, Action action) throws AdeException;

}
