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

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;

/** 
 * Implementation of a source only contains the internal id.
 * The id is fetched using the dictionaries 
 *
 */
public class SourceImpl implements ISource {
    private static final long serialVersionUID = 1L;

    private int m_sourceInternalId;

    public SourceImpl(int sourceInternalId) {
        m_sourceInternalId = sourceInternalId;
    }

    @Override
    public final int getSourceInternalId() {
        return m_sourceInternalId;
    }

    @Override
    public final String getSourceId() throws AdeException {
        return AdeInternal.getAdeImpl().getDictionaries().getSourceIdDictionary().getWordById(m_sourceInternalId);
    }

    @Override
    public String toString() {
        try {
            return String.format("%s(%d)", getSourceId(), m_sourceInternalId);
        } catch (AdeException e) {
            throw new Error("internal bug", e);
        }
    }

    @Override
    public int compareTo(ISource o) {
        return m_sourceInternalId - o.getSourceInternalId();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ISource) {
            return m_sourceInternalId == ((ISource) o).getSourceInternalId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return m_sourceInternalId;
    }

}
