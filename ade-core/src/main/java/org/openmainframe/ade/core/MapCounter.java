/*
 
    Copyright IBM Corp. 2016
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
package org.openmainframe.ade.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A class that is used in order to keep counters of objects.
 * @param <K> the type of object that is being counted by this class.
 */
public class MapCounter<K> implements IObjectCounter<K> {
    private Map<K, Integer> mCount = null;
    private int mSum = 0;

    /**
     * Constructs a new CounterMap based on a HashMap.
     * For key values that do not implement a hashCode method, only
     * the same instance of given object will be counted.
     */
    public MapCounter() {
        mCount = new HashMap<K, Integer>();
    }

    /**
     * Constructs a new CounterMap based on a given Map implementation.
     *
     * @param map a Map instance that should be used to keep track of
     *      Object instance count. The specified map should provide a means
     *      of comparing objects based on the the specific map implementation
     *      (hashCode for HashMap, Comparable for TreeMap, etc...). External
     *      changes to this Map should be avoided as they will also affect
     *      this internal Map.
     */
    public MapCounter(Map<K, Integer> map) {
        mCount = map;
    }

    /**
     * Increment by amount (positive or negative) the value corresponding to key.
     *
     * @param key the key of type K whose count should be incremented
     * @param count the integer amount that the given key count should be
     *          modified by
     */
    @Override
    public final Integer add(K key, int counter) {
        mSum += counter;

        Integer res = mCount.get(key);
        if (res != null) {
            res += counter;
        } else {
            res = counter;
        }

        mCount.put(key, res);
        return res;
    }

    /**
     * Increment the value corresponding to key by one.
     *
     * @param key the key of type K whose count should be incremented
     */
    @Override
    public final Integer add(K key) {
        return add(key, 1);
    }

    /**
     * Increments all the elements in the map by amount.
     *
     * @param amount the integer amount to modify all elements in the map by
     */
    public final void addToAll(int amount) {
        final Iterator<Entry<K, Integer>> it = mCount.entrySet().iterator();
        while (it.hasNext()) {
            mSum += amount;
            final Map.Entry<K, Integer> pairs = it.next();
            pairs.setValue(pairs.getValue() + amount);
        }
    }

    /**
     * Removes all keys who's corresponding values are zeros.
     */
    public final void removeZeros() {
        final Iterator<Entry<K, Integer>> it = mCount.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<K, Integer> pairs = it.next();
            if (pairs.getValue() == 0) {
                it.remove();
            }
        }
    }

    /**
     * Clears the counter map.
     */
    @Override
    public final void clear() {
        mCount.clear();
        mSum = 0;
    }

    /**
     * Returns true if the key is contained in the map.
     *
     * @param key the key of type K whose existence should be checked
     * @return true if the key is contained in this MapCounter, false otherwise
     */
    public final boolean containsKey(K key) {
        return mCount.containsKey(key);
    }

    /**
     * Returns the value corresponding to the key or null if the counter map doesn't
     * contain the key.
     *
     * @param key the key of type K to retrieve the count for
     * @return an Integer containing the current "count" for the key or null
     *          if the key does not exist in this map
     */
    @Override
    public final Integer get(K key) {
        return mCount.get(key);
    }

    /**
     * Returns the internal map used for counting.
     *
     * @return the internal Map instance containing key-count pairings.
     *      External changes to this Map should be avoided as they will also affect
     *      this internal Map.
     *
     */
    public final Map<K, Integer> getMap() {
        return mCount;
    }

    /**
     * Retrieves a Set containing all of the current keys in this map.
     *
     * @return a Set view of the keys contained in this map. External
     *      changes to this Set should be avoided as they will also affect
     *      this internal Set.
     */
    public final Set<K> keySet() {
        return mCount.keySet();
    }

    /**
     * Remove all the elements that are less than threshold.
     *
     * @param threshold an integer for which keys who have a count less then
     *      this value will be removed
     */
    public final void removeLessThanThreshold(int threshold) {
        final Iterator<Map.Entry<K, Integer>> it = mCount.entrySet().iterator();

        while (it.hasNext()) {
            final Map.Entry<K, Integer> pairs = it.next();
            final int value = pairs.getValue();
            if (value < threshold) {
                mSum -= value;
                it.remove();
            }
        }
    }

    /**
     * Returns the number of keys in the counter map.
     *
     * @return an integer indicating the amount of keys in this map
     */
    public final int size() {
        return mCount.size();
    }

    /**
     * Returns the sum of all counters.
     *
     * @return an integer representing the sum of all counts for all of the
     *      keys in this map
     */
    @Override
    public final int sum() {
        return mSum;
    }

    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }
}
