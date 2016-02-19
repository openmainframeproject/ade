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

import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.impl.flow.modules.Separator;

/**
 * Used to inform the {@link IFrameableTarget}&lt;{@link Object},
 * {@link TimeSeparator}}&gt; that the next 
 * {@link IFrameableTarget#incomingObject(Object)}'s timestamp
 * is <b>NOT</b> consecutive with the previous one. E.g. it
 * may even occur before the previous one, or in the far future.
 * 
 */
public class TimeSeparator extends Separator {
    private String m_source = null;
    private String m_reason;

    TimeSeparator(String reason) {
        this(null, reason);
    }

    TimeSeparator(String source, String reason) {
        super(SeparatorType.TIME);
        m_source = source;
        m_reason = reason;
    }

    /**
     * @return The {@link String} denoting the reason for this {@link TimeSeparator}
     */
    public final String getReason() {
        return m_reason;
    }

    /**
     * @return The source for this {@link TimeSeparator}
     */
    public final String getSource() {
        return m_source;
    }

}
