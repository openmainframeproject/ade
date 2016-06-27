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

import java.util.Vector;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IInterval;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.impl.data.IntervalClassificationEnum;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.factory.FlowFactory;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.hub.HubFrameableFramingBlock;

public class IntervalShiftAccumulator extends HubFrameableFramingBlock<IInterval, TimeSeparator, IInterval, TimeSeparator> {

    private Vector<IInterval> m_intervals;
    private int m_shiftDevider;
    private FramingFlowType m_framingFlowType;
    private boolean m_isSlidingWindow;
    private FramingFlowType m_splitFlow;

    /**
     * Construct a new IntervalShiftAccumulator.
     * 
     * @param framingFlowType the time frame properties.
     * @param isDestFlow if true, the interval is a split flow. Otherwise, it is a joined flow.
     * @param shiftDevider the split or join factor.
     * @param isSlidingWindow if true, the time frame is a sliding window.
     */
    public IntervalShiftAccumulator(FramingFlowType framingFlowType, boolean isDestFlow, int shiftDevider,
            boolean isSlidingWindow) throws AdeException {
        m_shiftDevider = shiftDevider;
        m_intervals = new Vector<IInterval>(m_shiftDevider);
        m_isSlidingWindow = isSlidingWindow;
        final FlowFactory flowFactory = Ade.getAde().getFlowFactory();
        if (isDestFlow) {
            m_framingFlowType = framingFlowType;
            m_splitFlow = flowFactory.getSplitFlow(framingFlowType, m_shiftDevider);
        } else {
            m_framingFlowType = flowFactory.getJoinedFlow(framingFlowType, m_shiftDevider);
            m_splitFlow = framingFlowType;
        }
    }

    @Override
    public final void beginOfStream() throws AdeException {
        sendBeginOfStream();

    }

    @Override
    public final void incomingObject(IInterval interval) throws AdeException {
        m_intervals.add(interval);
        if (m_intervals.size() == m_shiftDevider
                || (!m_isSlidingWindow && (interval.getSerialNum() + 1) % m_shiftDevider == 0)) {
            createAndSendInterval();
        }
    }

    protected final void createAndSendInterval() throws AdeException {

        final IInterval top = m_intervals.firstElement();
        m_intervals.remove(0);
        for (IInterval i : m_intervals) {
            top.join(i);
        }
        fixFramingFlowType(top);
        top.fixIntervalSerialNum();
        if (!m_isSlidingWindow) {
            top.alignIntervalTimes();
            m_intervals.clear();
        }
        sendObject(top);
    }

    private void fixFramingFlowType(IInterval top) throws AdeException {
        final FramingFlowType fft = top.getIntervalFramingFlowType();

        if (!m_splitFlow.equals(fft)) {
            throw new AdeFlowException("flow types missmatch");
        }
        top.setIntervalFramingFlowType(getFramingFlowType());
    }

    @Override
    public final void endOfStream() throws AdeException {
        while (!m_intervals.isEmpty()) {
            if (m_isSlidingWindow) {
                // this will skip all the temporary intervals, and go right to the real interval, scoring and saving it.
                while (!m_intervals.isEmpty()
                        && m_intervals.get(0).getIntervalClassification().equals(IntervalClassificationEnum.CONTINUOUS)) {
                    m_intervals.remove(0);
                }
            }
            if (!m_intervals.isEmpty()) {
                createAndSendInterval();
            }
        }
        sendEndOfStream();

    }

    /**
     * Get the {@link FramingFlowType}.
     * 
     * @return the {@link FramingFlowType}.
     */
    public final FramingFlowType getFramingFlowType() {
        return m_framingFlowType;
    }

    @Override
    public final void incomingSeparator(TimeSeparator sep) throws AdeException {

        while (!m_intervals.isEmpty()) {
            createAndSendInterval();
        }

        //Transparent
        sendSeparator(sep);
    }

}
