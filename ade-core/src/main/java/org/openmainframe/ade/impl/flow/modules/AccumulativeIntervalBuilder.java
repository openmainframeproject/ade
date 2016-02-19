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

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.data.IIntervalClassification;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.flow.IStreamTarget;
import org.openmainframe.ade.impl.actions.Action;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.summary.SummarizationProperties;

public class AccumulativeIntervalBuilder extends IntervalBuilder {

    private final int m_splitFactor;
    private ConsecutiveIntervalBuilder m_consecutiveIntervalBuilder;
    private IntervalShiftAccumulator m_intervalShiftAccumulator;

    public AccumulativeIntervalBuilder(ISource source, SummarizationProperties sumProps,
            FramingFlowType framingFlowType, IIntervalClassification intervalClassification,
            Action action, int splitFactor) throws AdeException {
        super();
        m_splitFactor = splitFactor;
        final FramingFlowType intevalFragmentFramingFlowType = getIntervalFragmentFramingFlow(framingFlowType, splitFactor);
        m_consecutiveIntervalBuilder = new ConsecutiveIntervalBuilder(source, sumProps, intevalFragmentFramingFlowType,
                intervalClassification, action);
        m_consecutiveIntervalBuilder.setIntervalNumFinderFlow(framingFlowType);
        m_intervalShiftAccumulator = new IntervalShiftAccumulator(framingFlowType, true, m_splitFactor, true);
        m_consecutiveIntervalBuilder.addTarget(m_intervalShiftAccumulator);
    }

    private static FramingFlowType getIntervalFragmentFramingFlow(
            FramingFlowType inputFramingFlowType, int splitFactor) throws AdeException {

        final FramingFlowType res = Ade.getAde().getFlowFactory().getSplitFlow(inputFramingFlowType, splitFactor);
        res.setDuration(inputFramingFlowType.getDuration() / splitFactor);
        return res;
    }

    @Override
    public final FramingFlowType getFramingFlowType() {
        return m_intervalShiftAccumulator.getFramingFlowType();
    }

    @Override
    public final void beginOfStream() throws AdeException {
        m_consecutiveIntervalBuilder.beginOfStream();
    }

    @Override
    public final void incomingObject(IMessageInstance obj) throws AdeException {
        m_consecutiveIntervalBuilder.incomingObject(obj);
    }

    @Override
    public final void endOfStream() throws AdeException {
        m_consecutiveIntervalBuilder.endOfStream();

    }

    @Override
    public final void incomingSeparator(Separator sep) throws AdeException {
        m_consecutiveIntervalBuilder.incomingSeparator(sep);
    }

    @Override
    public void init() throws AdeException {
        //Implementing the abstract method
    }

    @Override
    public final void addTarget(IFrameableTarget<IInterval, TimeSeparator> target) throws AdeFlowException {
        m_intervalShiftAccumulator.addTarget(target);
    }

    @Override
    public final void addTarget(IStreamTarget<IInterval> target) throws AdeFlowException {
        m_intervalShiftAccumulator.addTarget(target);
    }
}
