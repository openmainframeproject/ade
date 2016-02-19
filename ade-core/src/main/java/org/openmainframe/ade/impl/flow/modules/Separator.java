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

import org.openmainframe.ade.impl.data.TimeSeparator;

/**
 * An object that represents a separator within a stream objects.
 */
public class Separator {

    /**
     * Enumerates different types of separators.
     */
    public enum SeparatorType {
        /**
         * Indicates that this {@link Separator} is a {@link TimeSeparator}.
         */
        TIME,

        /**
         * Indicates that this {@link Separator} is an {@link IntervalSeparator}.
         */
        INTERVAL,
    }

    protected SeparatorType m_type;

    /**
     * Construct a new Separator.
     * 
     * @param type The type of separator. Can be either INTERVAL or TIME.
     */
    public Separator(SeparatorType type) {
        m_type = type;
    }

    public final SeparatorType getType() {
        return m_type;
    }

}