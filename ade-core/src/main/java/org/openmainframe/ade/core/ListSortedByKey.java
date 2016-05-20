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
package org.openmainframe.ade.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/** An object for storing a list of V objects, sorted by key K objects
 * And optionally secondary sort by the V objects.
 * <p/>
 * Primary sorting by keys:
 * either natural order (must be comparable), or by a comparator specified by the constructor.
 * Default is ascending order, but can be reversed using invertKeyOrdering()
 * <p/>
 * Secondary sorting by values:
 * No secondary sort by default - values will be ordered based on their addition order.
 * Use natural ordering (must be comparable) by calling setValueNaturalOrder()
 * or specify a comparator using setValueComparator()
 * In either case, you may invert using invertValueOrdering()
 * <p/>
 * Complexity:
 * add() operations and all other modifiers have O(1) complexity.
 * getKey() and getValue() sort the list if called after a modifying method (e.g., add),
 * thus having O(nlogn) in that case, and O(1) otherwise
 * 
 * @param <K> The type of the Key that sorts this list
 * @param <V> The type of the Value that is stored in this list
 */
public class ListSortedByKey<K, V> {
    /**
     * Internal list of items (=key value pair).
     */
    private ArrayList<Item> mItems = new ArrayList<Item>();
    /**
     * Is list sorted (sorting is performed lazily).
     */
    private boolean mSorted = true;
    /**
     * Comparator of keys.
     */
    private Comparator<? super K> mKeyComparator;
    /**
     * Comparator of values.
     */
    private Comparator<? super V> mValueComparator;
    /**
     * Order of value sorting (1=ascending, -1=descending).
     */
    private int mValueOrderingSign = 1;
    /**
     * Order of key sorting (1=ascending, -1=descending).
     */
    private int mKeyOrderingSign = 1;
    /**
     * Do values have natural ordering.
     */
    private boolean mNaturalValueComparator;

    /**
     * Create an empty list with keys in natural order, no secondary sort of values.
     * For lists that have a key or value type that does not implement comparable, an
     * appropriate comparator should be set before using this list.
     */
    public ListSortedByKey() {
        mKeyComparator = null;
        mValueComparator = null;
    }

    /**
     * Create an empty list with keys in given order, no secondary sort of values.
     * For lists that have a value type that does not implement comparable, an
     * appropriate comparator should be set before using this list.
     *
     * @param comparator the comparator to use for key comparison
     */
    public ListSortedByKey(Comparator<? super K> comparator) {
        mKeyComparator = comparator;
    }

    /**
     * Returns a string containing the copyright information for this class.
     * @return a string containing the copyright information for this class
     */
    static String copyright() {
        return Copyright.IBM_COPYRIGHT;
    }

    /**
     * Sets a secondary sort of values using given comparator.
     */
    public final void setValueComparator(Comparator<? super V> comparator) {
        mValueComparator = comparator;
        mNaturalValueComparator = false;
        mSorted = false;
    }

    /**
     * Sets a secondary sort of values using natural ordering.
     */
    public final void setValueNaturalOrder() {
        mValueComparator = null;
        mNaturalValueComparator = true;
        mSorted = false;
    }

    /**
     * Adds a key value pair to the list.
     *
     * @param key The key that should be associated with the value to add
     * @param value The value to add to the list
     */
    public final void add(K key, V value) {
        mItems.add(new Item(key, value));
        mSorted = false;
    }

    /**
     * Empties the list.
     */
    public final void clear() {
        mItems.clear();
        mSorted = true;
    }

    /**
     * Returns the current size of this list.
     *
     * @return number of key value pairs
     */
    public final int size() {
        return mItems.size();
    }

    /**
     * Retrieves the key at the given index.
     * For lists that have a key or value type that does not implement comparable, an
     * appropriate comparator should be set before use of this method.
     *
     * @param i the index of the key to retrieve
     * @return the i'th key in the list
     */
    public final K getKey(int i) {
        sort();
        return mItems.get(i).mKey;
    }

    /**
     * Retrieves the value at the given index.
     * For lists that have a key or value type that does not implement comparable, an
     * appropriate comparator should be set before use of this method.
     *
     * @param i the index of the value to retrieve
     * @return the i'th value in the list
     */
    public final V getValue(int i) {
        sort();
        return mItems.get(i).mValue;
    }

    /**
     * Verifies the list is sorted.
     */
    private void sort() {
        if (!mSorted) {
            Collections.sort(mItems);
            mSorted = true;
        }
    }

    @Override
    public final String toString() {
        final StringBuilder res = new StringBuilder();
        sort();
        res.append("[");
        for (Item item : mItems) {
            res.append(String.format(" (%s,%s)", item.mKey.toString(), item.mValue.toString()));
        }
        res.append(" ]");
        return res.toString();
    }

    /**
     * Use descending order of keys.
     */
    public final void invertKeyOrdering() {
        mKeyOrderingSign = -mKeyOrderingSign;
        mSorted = false;
    }

    /**
     * Use descending order of values. Has no effect if secondary sort is disabled.
     */
    public final void invertValueOrdering() {
        mValueOrderingSign = -mValueOrderingSign;
        mSorted = false;
    }

    private static <T> Comparable<T> toComparable(T obj) {
        return (Comparable<T>) obj;
    }

    /**
     *  An internal class for holding the (key,value) pair.
     *  Its compareTo method defines the way the list is sorted.
     */
    private class Item implements Comparable<Item> {

        private V mValue;
        private K mKey;

        Item(K key, V value) {
            mKey = key;
            mValue = value;
        }

        @Override
        public boolean equals(Object obj) {
            // The exact same object is obviously equal
            if (this == obj) {
                return true;
            }
            // If the object is null then they can't be equal or
            // if the class do not match
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return compareTo((Item) obj) == 0;
        }

        @Override
        public int compareTo(Item other) {
            // The comparison result
            int res;

            // First order by keys, either by a user-defined comparator or by natural ordering
            res = getKeyComparisonResult(other.mKey);

            // If the keys are the same and we have a means of comparing values,
            // then compare the values to get the result
            if (res == 0 && (mValueComparator != null || mNaturalValueComparator)) {
                // Order by values, either using natural ordering or using user defined comparator
                res = getValueComparisonResult(other.mValue);
            }

            return res;

        }

        /**
         * @see {@link Object#hashCode()}
         * @return a hash code using the key value pair. 
         */
        @Override
        public final int hashCode() {
            int myHash;
            myHash = mKey.hashCode();
            
            if (mValueComparator != null || mNaturalValueComparator) {
                myHash = myHash + mValue.hashCode();
            }
            
            return myHash;
        }
        
        private final int getKeyComparisonResult(K otherKey) {

            if (mKeyComparator != null) {
                return mKeyComparator.compare(mKey, otherKey) * mKeyOrderingSign;
            } else {
                return toComparable(mKey).compareTo(otherKey) * mKeyOrderingSign;
            }
        }

        private final int getValueComparisonResult(V otherValue) {
            if (mNaturalValueComparator) {
                return toComparable(mValue).compareTo(otherValue) * mValueOrderingSign;
            } else {
                return mValueComparator.compare(mValue, otherValue) * mValueOrderingSign;
            }
        }
    }
}
