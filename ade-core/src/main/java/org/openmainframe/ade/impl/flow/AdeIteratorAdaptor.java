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
package org.openmainframe.ade.impl.flow;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.flow.IAdeIterator;

public class AdeIteratorAdaptor<T> implements IAdeIterator<T> {

    IAdeIterator<? extends T> m_base;

    public AdeIteratorAdaptor(IAdeIterator<? extends T> base) {
        m_base = base;
    }

    @Override
    public void open() throws AdeException {
        m_base.open();
    }

    @Override
    public T getNext() throws AdeException {
        return m_base.getNext();
    }

    @Override
    public void close() throws AdeException {
        m_base.close();
    }

    @Override
    public void quietCleanup() {
        m_base.quietCleanup();
    }

}
