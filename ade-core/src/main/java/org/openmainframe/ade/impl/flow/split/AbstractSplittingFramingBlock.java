/*
 
    Copyright IBM Corp. 2012, 2016
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
package org.openmainframe.ade.impl.flow.split;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.flow.IFrameable;
import org.openmainframe.ade.flow.IFrameableTarget;
import org.openmainframe.ade.flow.IFramingBlock;

/**
 * This class will map Frameable objects based on their Separator.  Each separator will have its own
 * Set of Frameables and its own key to reference that set.
 *
 * @param <T> Frameable objects
 * @param <U> Separator between those Frameable objects
 * @param <V> Key to represent the set of the Frameable objects
 */
public abstract class AbstractSplittingFramingBlock<T, U, V extends Comparable<?>>
        extends AbstractSplittingBlock<T, V> implements
        IFramingBlock<T, T, U> {

    /**
     * Mapping a key to it's frameables.
     */
    private Map<V, Set<IFrameable<U>>> m_frameablesMap = new TreeMap<V, Set<IFrameable<U>>>();

    /**
     * Return the set of all frameables.
     * 
     * @return the set of all frameables (union of sets)
     */
    protected final Set<IFrameable<U>> getAllFrameables() {
        final Set<IFrameable<U>> allFrameables = new HashSet<IFrameable<U>>();
        for (Set<IFrameable<U>> targets : m_frameablesMap.values()) {
            allFrameables.addAll(targets);
        }
        return allFrameables;
    }

    /**
     * Utility method. Add frameable target to input key targets (either existing key targets or not)
     */
    protected final void addFrameableTarget(IFrameableTarget<T, U> frameableTarget, V key)
            throws AdeException {
        final Set<IFrameableTarget<T, U>> set = new HashSet<IFrameableTarget<T, U>>(1);
        set.add(frameableTarget);
        addFrameableTargets(set, key);
    }

    /**
     * Utility method. Add frameable targets to input key targets (either existing key targets or not)
     */
    protected final void addFrameableTargets(Set<IFrameableTarget<T, U>> frameableTargets, V key)
            throws AdeException {
        super.addTargets(frameableTargets, key);
        synchronized (m_frameablesMap) {
            Set<IFrameable<U>> frameables = m_frameablesMap.get(key);
            if (frameables == null) {
                frameables = new HashSet<IFrameable<U>>();
                m_frameablesMap.put(key, frameables);
            }
            frameables.addAll(frameableTargets);
        }
    }

    /**
     * Generates a key from the input U object. If the key is null then the 
     * {@link AbstractSplittingFramingBlock#handleNullSepKey(U)} method is called. Otherwise
     * we check {@link AbstractSplittingFramingBlock#m_frameablesMap} to get the key's targets.
     * If null, the {@link AbstractSplittingSource#addMissingTargets(V)} method is called.
     * When returning from that call, the key is expected to have frameables (at
     * least an empty set of targets) at {@link AbstractSplittingFramingBlock#m_frameablesMap},
     * otherwise an exception will be thrown! Finally, the U input is sent to all 
     * frameables for the generated key. 
     * @param sep to be sent
     */
    protected final void sendToFrameables(U sep) throws AdeException {
        final V key = generateSepKey(sep);

        if (key == null) {
            handleNullSepKey(sep);
        } else {
            Set<IFrameable<U>> frameables;
            synchronized (m_frameablesMap) {
                frameables = m_frameablesMap.get(key);
                if (frameables == null) {
                    addMissingTargets(key);
                    frameables = m_frameablesMap.get(key);
                    if (frameables == null) {
                        throw new AdeInternalException("Frameables should have been created ");
                    }
                }
            }
            for (IFrameable<U> frameable : frameables) {
                frameable.incomingSeparator(sep);
            }
        }
    }

    /**
     * Generate a key for the separator.
     * 
     * @param sep the separator to generate a key for
     * @return a key for the separator
     * @throws AdeFlowException key is failed to be generated
     */
    private V generateSepKey(U sep) throws AdeFlowException {
        try {
            return genSepKey(sep);
        } catch (AdeException e) {
            throw new AdeFlowException("Unable to create a key for seperator: %s: " + sep, e);
        }
    }

    /**
     * Generate a key for the separator.
     * 
     * @param U to generate key for
     * @return a key generated for input obj
     */
    protected abstract V genSepKey(U obj) throws AdeException;

    /**
     * Called when a key generated from the input SEP is null.
     */
    protected abstract void handleNullSepKey(U sep) throws AdeException;
}
