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
package org.openmainframe.ade.impl.data;

import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;

public class PeriodSummary {

    private PeriodImpl m_period;

    private int m_periodSummaryInternalId;

    private FramingFlowType m_framingFlowType;

    public PeriodSummary(int periodSummaryInternalId, PeriodImpl period, FramingFlowType framingFlowType) {
        m_periodSummaryInternalId = periodSummaryInternalId;
        m_period = period;
        m_framingFlowType = framingFlowType;
    }

    public final PeriodImpl getPeriod() {
        return m_period;
    }

    public final int getInternalId() {
        return m_periodSummaryInternalId;
    }

    public final FramingFlowType getFramingFlowType() {
        return m_framingFlowType;
    }

    public String toString() {
        return String.format("Summary(%d) %s of %s", m_periodSummaryInternalId, m_framingFlowType.toString(), m_period.toString());
    }
}
