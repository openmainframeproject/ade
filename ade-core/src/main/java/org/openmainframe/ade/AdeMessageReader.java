/*
 
    Copyright IBM Corp. 2013, 2016
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
package org.openmainframe.ade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;

import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.exceptions.AdeException;

/**
 * A base class that extends a {@link BufferedReader} to read {@link IMessageInstance} objects
 * from a given stream.
 * 
 * <p>
 * A Ade wrapper implementation should extend this class and implement the abstract method
 * {@link AdeMessageReader#readMessageInstance()}.
 */
public abstract class AdeMessageReader extends LineNumberReader {

    /**
     * Constructs a new {@link AdeMessageReader} based on the given reader. The one who constructs
     * the reader should also call {@link AdeMessageReader#close()} when reading is done. 
     * @param reader the basic reader for this {@link AdeMessageReader}.
     */
    public AdeMessageReader(Reader reader) {
        super(reader);
    }

    /**
     * reads the next {@link IMessageInstance} based on the underlying {@link BufferedReader}. 
     * @return the next available {@link IMessageInstance} or <code>null</code> if no such exists.
     * @throws AdeException 
     */
    public abstract IMessageInstance readMessageInstance() throws IOException, AdeException;
}
