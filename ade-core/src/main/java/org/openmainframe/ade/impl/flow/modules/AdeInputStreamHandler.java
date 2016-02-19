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
package org.openmainframe.ade.impl.flow.modules;

import java.io.IOException;
import java.io.InputStream;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.AdeInputStream;
import org.openmainframe.ade.AdeMessageReader;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.TimeSeparator;
import org.openmainframe.ade.impl.flow.hub.HubFrameableFramingBlock;

/**
 * A {@link AdeInputStreamHandler} handles input streams one after the other and generates a stream of
 * {@link IMessageInstance}s that will be sent to this handler's targets. The handler also sends a
 * {@link TimeSeparator} when the stream of {@link IMessageInstance}s is not guaranteed to be consequent in time.
 * Usually that happens between {@link IMessageInstance}s that originate from two different {@link AdeInputStream}s.
 */
public abstract class AdeInputStreamHandler extends
        HubFrameableFramingBlock<AdeInputStream, TimeSeparator, IMessageInstance, TimeSeparator> {
    protected TimeSeparator m_sep;

    /**
     * Constructs a new {@link AdeInputStreamHandler}.
     */
    public AdeInputStreamHandler() throws AdeException {
        // use the data factory to obtain the new TimeSeparator instance
        m_sep = Ade.getAde().getDataFactory().newTimeSeparator("New sequnce with some more debug information about the sequnece");
    }

    /**
     * Prepares the new stream for its targets. Specifically, it sends a {@link TimeSeparator} before any
     * {@link IMessageInstance} is being sent. 
     */
    @Override
    public final void beginOfStream() throws AdeException {
        sendBeginOfStream();
    }

    /**
     * Retrieves the reader matching this stream by calling
     * {@link AdeInputStreamHandler#getReader(AdeInputStream)} and uses it to read through the messages.
     * The default implementation assumes that no more messages can be read from the stream after the last
     * {@link IMessageInstance} is pulled from the reader, but generally, it is possible that event though this reader 
     * is not able to read any {@link IMessageInstance} from the input stream, the stream has not ended, and
     * another reader can continue reading from the stream. This can be checked, for instance by calling
     * {@link InputStream#available()} after receiving null from {@link AdeMessageReader#readMessageInstance()}
     */
    @Override
    public void incomingObject(AdeInputStream stream) throws AdeException {
        IMessageInstance mi;
        final AdeMessageReader reader = getReader(stream);
        try {
            while ((mi = reader.readMessageInstance()) != null) {
                beforeSendMessage(mi);
                // now we send the message instance
                sendObject(mi);
            }
            // no need to close the reader! it is based on a stream given as an argument.
            // the one providing the stream must take care to close it.
        } catch (IOException e) {
            throw new AdeInternalException("IO Exception reading stream", e);
        }

    }

    /**
     * If you need to do anything after the message was extracted from the log and before it is passed on to the flow.
     * A subclass can override this method to preprocess the message if needed.
     * 
     * @param mi The message instance.
     */
    protected void beforeSendMessage(IMessageInstance mi) throws AdeException {
    }

    /**
     * Returns a reader that is suitable to read messages from the given stream at its current position.
     * 
     * @param stream the stream to read from
     * @return  a {@link AdeMessageReader} 
     * @throws AdeException if a {@link AdeMessageReader} cannot be returned.
     */
    protected abstract AdeMessageReader getReader(AdeInputStream stream) throws AdeException;

    @Override
    public final void endOfStream() throws AdeException {
        sendEndOfStream();
    }

    @Override
    public final void incomingSeparator(TimeSeparator sep) throws AdeException {
        sendSeparator(sep);

    }

    /**
     * Send the {@link TimeSeparator}.
     */
    public final void sendDiscontinuitySeperator() throws AdeException {
        sendSeparator(m_sep);
    }
}
