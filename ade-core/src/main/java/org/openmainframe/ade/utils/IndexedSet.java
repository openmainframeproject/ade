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
package org.openmainframe.ade.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A set implementation that also holds an arbitrary and unique index per entry.
 * If elements are only added and not removed the set of indices will be:
 * {0, 1, ..., size()-1}
 * Implemented using a HashMap<K, Integer>.
 * Indices are reset when clear() is called.
 * @param <K>
 */
public class IndexedSet<K> implements Set<K> {

    private Map<K, Integer> mMembersMap = new HashMap<>();
    private int counter = -1;

    /**
     * Returns the index of the specified element in
     * this set, or -1 if this set does not contain the element.
     * @param object
     * @return the index of the first occurrence of the specified element in this set,
     *  or -1 if this set does not contain the element.
     */
    public int indexOf(K object) {
        Integer index = mMembersMap.get(object);
        return (index == null) ? -1 : index;
    }

    /**
     * @return Returns a map of the reverse translation from (unique) 
     * value/index to key.
     */
    public Map<Integer, K> getReverseTranslationMap() {
        Map<Integer, K> transMap = new HashMap<>();

        for (Entry<K, Integer> entry : mMembersMap.entrySet()) {
            transMap.put(entry.getValue(), entry.getKey());
        }
        return transMap;
    }

    @Override
    public boolean add(K object) {
        if (mMembersMap.containsKey(object)) {
            return false;
        }
        mMembersMap.put(object, ++counter);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends K> collection) {
        boolean flag = false;
        for (K k : collection) {
            flag |= add(k);
        }
        return flag;
    }

    @Override
    public void clear() {
        mMembersMap.clear();
        counter = -1;

    }

    @Override
    public boolean contains(Object object) {
        return mMembersMap.containsKey(object);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        Boolean flag = true;
        for (Object object : collection) {
            flag &= contains(object);
        }
        return flag;
    }

    @Override
    public boolean isEmpty() {
        return mMembersMap.isEmpty();
    }

    @Override
    public Iterator<K> iterator() {
        return mMembersMap.keySet().iterator();
    }

    @Override
    public boolean remove(Object object) {
        return mMembersMap.remove(object) == null;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean flag = false;
        for (Object object : collection) {
            flag |= mMembersMap.remove(object) != null;
        }
        return flag;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        Iterator<Entry<K, Integer>> it = mMembersMap.entrySet().iterator();
        Entry<K, Integer> entry = null;
        boolean flag = false;
        while (it.hasNext()) {
            entry = it.next();
            if (!collection.contains(entry.getKey())) {
                it.remove();
                flag = true;
            }
        }
        return flag;
    }

    @Override
    public int size() {
        return mMembersMap.size();
    }

    @Override
    public Object[] toArray() {
        return mMembersMap.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return mMembersMap.keySet().toArray(array);
    }
}
