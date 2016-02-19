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
package org.openmainframe.ade.flow;

import java.util.Collection;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;

/**
 * This class holds static utility methods and constants used in classes implementing interfaces
 * from the <code>org.openmainframe.ade.flow</code> package.
 */
public final class FlowUtils {
    
    private FlowUtils() {
        // Private constructor to hide the implicit public one.
    }

    /**
     * Invokes {@link IStreamTarget#beginOfStream()} in each of the input targets.
     * @param targets The {@link Collection} of targets.
     * @throws AdeException See {@link IStreamTarget#beginOfStream()}.
     * @throws AdeFlowException See {@link IStreamTarget#beginOfStream()}.
     */
    public static <T> void sendBeginOfStream(Collection<? extends IStreamTarget<T>> targets) throws AdeException, AdeFlowException {
        for (IStreamTarget<T> target : targets) {
            target.beginOfStream();
        }
    }

    /**
     * Invokes {@link IStreamTarget#incomingObject(Object)} with the input &lt;T&gt;
     * object in each of the input targets.
     * @param obj The object to be sent.
     * @param targets The {@link Collection} of targets.
     * @throws AdeException See {@link IStreamTarget#incomingObject(Object)}.
     * @throws AdeFlowException See {@link IStreamTarget#incomingObject(Object)}.
     */
    public static <T> void sendObject(T obj, Collection<? extends IStreamTarget<T>> targets) throws AdeException, AdeFlowException {
        for (IStreamTarget<T> target : targets) {
            target.incomingObject(obj);
        }
    }

    /**
     * Invokes {@link IStreamTarget#endOfStream()} in each of the input targets.
     * @param targets The {@link Collection} of targets.
     * @throws AdeException See {@link IStreamTarget#endOfStream()}.
     * @throws AdeFlowException See {@link IStreamTarget#endOfStream()}.
     */
    public static <T> void sendEndOfStream(Collection<? extends IStreamTarget<T>> targets) throws AdeException, AdeFlowException {
        for (IStreamTarget<T> target : targets) {
            target.endOfStream();
        }
    }

    /**
     * Invokes {@link IFrameableTarget#incomingSeparator(Object)} with the input &lt;SEP&gt;
     * object in each of the input frameables.
     * @param sep The separator to be forwarded.
     * @param frameable The {@link Collection} of targets.
     * @throws AdeException See {@link IFrameable#incomingSeparator(Object)}
     * @throws AdeFlowException See {@link IFrameable#incomingSeparator(Object)}
     */
    public static <T> void sendSeparator(T sep, Collection<? extends IFrameable<T>> frameables) throws AdeException, AdeFlowException {
        for (IFrameable<T> frameable : frameables) {
            frameable.incomingSeparator(sep);
        }
    }

}
