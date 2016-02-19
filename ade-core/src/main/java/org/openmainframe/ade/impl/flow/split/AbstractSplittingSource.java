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
import org.openmainframe.ade.flow.IStreamSource;
import org.openmainframe.ade.flow.IStreamTarget;

/**
 * This class will map StreamTargets based on their O type. Each O type will have its own set of
 * StreamTargets and its own key to reference that set.
 *
 * @param <O> Type of StreamTargets
 * @param <K> Key to represent a set of StreamTargets
 */
public abstract class AbstractSplittingSource<O, K extends Comparable<?>> implements IStreamSource<O> {

    /**
     * Mapping a key to it's targets.
     */
    private Map<K, Set<IStreamTarget<O>>> m_targetMap = new TreeMap<K, Set<IStreamTarget<O>>>();

    /**
     * Return the set of all targets.
     * 
     * @return the set of all targets (union of sets)
     */
    protected final Set<IStreamTarget<O>> getAllTargets() {
        final Set<IStreamTarget<O>> allTargets = new HashSet<IStreamTarget<O>>();
        for (Set<IStreamTarget<O>> targets : m_targetMap.values()) {
            allTargets.addAll(targets);
        }
        return allTargets;
    }

    /**
     * Utility method. Add target to input key targets (either existing key targets or not)
     */
    protected final void addTarget(IStreamTarget<O> target, K key) throws AdeException {
        final Set<IStreamTarget<O>> set = new HashSet<IStreamTarget<O>>(1);
        set.add(target);
        addTargets(set, key);
    }

    /**
     * Utility method. Add targets to input key targets (either existing key targets or not)
     */
    protected final synchronized void addTargets(Set<? extends IStreamTarget<O>> addedTargets, K key)
            throws AdeException {
        synchronized (m_targetMap) {
            Set<IStreamTarget<O>> targets = m_targetMap.get(key);
            if (targets == null) {
                targets = new HashSet<IStreamTarget<O>>();
                m_targetMap.put(key, targets);
            }
            targets.addAll(addedTargets);
        }
    }

    /**
     * Generates a key from the input O object. If the key is null then the 
     * {@link AbstractSplittingSource#handleNullKey(O)} method is called. Otherwise
     * we check {@link AbstractSplittingSource#m_targetMap} to get the key's targets.
     * If null, the {@link AbstractSplittingSource#addMissingTargets(K)} method is called.
     * When returning from that call, the key is expected to have targets (at
     * least an empty set of targets) in {@link AbstractSplittingSource#m_targetMap},
     * otherwise an exception will be thrown! Finally, the object is sent to 
     * all targets for the generated key. 
     * @param obj to be sent
     */
    protected final void sendToTargets(O obj) throws AdeException {
        final K key = generateKey(obj);

        if (key == null) {
            handleNullKey(obj);
        } else {
            Set<IStreamTarget<O>> targets;
            synchronized (m_targetMap) {
                targets = m_targetMap.get(key);
                if (targets == null) {
                    addMissingTargets(key);
                    targets = m_targetMap.get(key);
                    if (targets == null) {
                        throw new AdeInternalException("Targets should have been created ");
                    }
                }
            }
            for (IStreamTarget<O> target : targets) {
                target.incomingObject(obj);
            }
        }
    }

    /**
     * Generates a key from the input O object.
     * @param obj to generate key for
     * @return generated key for the object
     */
    private K generateKey(O obj) throws AdeFlowException {
        try {
            return genKey(obj);
        } catch (AdeException e) {
            throw new AdeFlowException("Unable to create a key for object: " + obj, e);
        }
    }

    /**
     * Generate a key for the input O.
     * 
     * @param O to generate key for
     * @return a key generated for input O
     */
    protected abstract K genKey(O obj) throws AdeException;

    /**
     * This method must(!) use either {@link AbstractSplittingSource#addTarget(IStreamTarget, K)} or
     * {@link AbstractSplittingSource#addTargets(Set, K)} to add targets to the input K.
     */
    protected abstract void addMissingTargets(K key) throws AdeException;

    /**
     * called when a key generated from the input O is null.
     */
    protected abstract void handleNullKey(O obj) throws AdeException;

}
