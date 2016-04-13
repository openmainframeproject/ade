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
package org.openmainframe.ade.scoringApi;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;

/** An object of storing a collection of String and double statistics */
public class StatisticsChart {

    /** Map of double statistics: name->value */
    private SortedMap<String, Double> m_doubleStats = null;

    /** Map of String statistics: name->value */
    private SortedMap<String, String> m_stringStats = null;

    /** Set of all statistics names */
    private Set<String> m_allKeys = null;

    /** Add a double statistic.
     *  Duplicates are not allowed (not even between String and double)
     * @param name Name of statistic
     * @param val Value of statistic
     * @throws AdeInternalException
     */
    public void setStat(String name, double val) throws AdeInternalException {
        if (m_doubleStats == null) {
            m_doubleStats = new TreeMap<String, Double>();
        }
        verifyLegal(name);
        m_doubleStats.put(name, val);
    }

    /** Add a String statistic.
     *  Duplicates are not allowed (not even between String and double)
     * @param name Name of statistic
     * @param val Value of statistic
     * @throws AdeInternalException
     */
    public void setStat(String name, String val) throws AdeInternalException {
        if (val == null) {
            throw new AdeInternalException("Null values not allowed");
        }
        if (m_stringStats == null) {
            m_stringStats = new TreeMap<String, String>();
        }
        verifyLegal(name);
        m_stringStats.put(name, val);
    }

    /**
     * returns a collection of all entries for double statistics in this {@link StatisticsChart}
     * @return a collection of all entries for double statistics in this {@link StatisticsChart} or null if no double 
     * stats are available for this {@link StatisticsChart}
     */
    public Set<Entry<String, Double>> getDoubleStats() {
        if (m_doubleStats != null) {
            return m_doubleStats.entrySet();
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * returns a collection of all entries for string statistics in this {@link StatisticsChart}
     * @return a collection of all entries for string statistics in this {@link StatisticsChart} or null if no string 
     * stats are available for this {@link StatisticsChart}
     */
    public Set<Entry<String, String>> getStringStats() {
        if (m_stringStats != null) {
            return m_stringStats.entrySet();
        } else {
            return Collections.emptySet();
        }
    }

    /** @return the given double statistic or throws an exception if it does not exist */
    public double getDoubleStatOrThrow(String name) throws AdeInternalException {
        final Double res = getDoubleStat(name);
        if (res == null) {
            throw new AdeInternalException("Missing double statistic " + name + "\nAvailable stats: " + this);
        }
        return res;
    }

    /** @return true if a double statistic with the given name exists */
    public boolean hasDoubleStat(String name) {
        return getDoubleStat(name) != null;
    }

    /** @return the given double statistic or null if it does not exist */
    public Double getDoubleStat(String name) {
        if (m_doubleStats == null) {
            return null;
        }
        return m_doubleStats.get(name);
    }

    /** @return the given String statistic or throws an exception if it does not exist */
    public String getStringStatOrThrow(String name) throws AdeInternalException {
        final String res = getStringStat(name);
        if (res == null) {
            throw new AdeInternalException("Missing string statistic " + name);
        }
        return res;
    }

    /** @return true if a String statistic with the given name exists */
    public boolean hasStringStat(String name) {
        return getStringStat(name) != null;
    }

    /** @return the given double statistic or null if it does not exist */
    public String getStringStat(String name) {
        if (m_stringStats == null) {
            return null;
        }
        return m_stringStats.get(name);
    }

    /** @return a map with all statistics sorted by name.
     * Double values are converted to strings using String.valueOf()
     */
    public SortedMap<String, String> getAllStatisticsSorted() {
        final SortedMap<String, String> res = new TreeMap<String, String>();

        if (m_stringStats != null) {
            res.putAll(m_stringStats);
        }
        if (m_doubleStats != null) {
            for (Map.Entry<String, Double> val : m_doubleStats.entrySet()) {
                res.put(val.getKey(), String.valueOf(val.getValue()));
            }
        }
        return res;
    }

    /** @return a map with all statistics sorted by name.
     * The Object values can be either Double or String
     */
    public SortedMap<String, Object> getAllStatisticsAsObjectsSorted() {
        final SortedMap<String, Object> res = new TreeMap<String, Object>();

        if (m_stringStats != null) {
            res.putAll(m_stringStats);
        }
        if (m_doubleStats != null) {
            res.putAll(m_doubleStats);
        }
        return res;
    }

    /** Verify if the given statistic name does not already exists */
    private void verifyLegal(String name) throws AdeInternalException {
        if (m_allKeys == null) {
            m_allKeys = new TreeSet<String>();
        }
        if (m_allKeys.contains(name)) {
            throw new AdeInternalException("Duplicate statistic " + name);
        }
        m_allKeys.add(name);
    }

    /** Adds all the statistics in another StatisticsChart object to this object,
     * with their names prefixed by the given name 
     */
    public void add(String name, StatisticsChart other) throws AdeInternalException {
        if (other.m_doubleStats != null) {
            for (Map.Entry<String, Double> val : other.m_doubleStats.entrySet()) {
                setStat(name + "." + val.getKey(), val.getValue());
            }
        }
        if (other.m_stringStats != null) {
            for (Map.Entry<String, String> val : other.m_stringStats.entrySet()) {
                setStat(name + "." + val.getKey(), val.getValue());
            }
        }
    }

    /** Adds all the statistics in another StatisticsChart object to this object,
     * The names of the other statistics or copies as is. 
     */
    public void add(StatisticsChart other) throws AdeInternalException {
        if (other.m_doubleStats != null) {
            for (Map.Entry<String, Double> val : other.m_doubleStats.entrySet()) {
                setStat(val.getKey(), val.getValue());
            }
        }
        if (other.m_stringStats != null) {
            for (Map.Entry<String, String> val : other.m_stringStats.entrySet()) {
                setStat(val.getKey(), val.getValue());
            }
        }
    }

    /** Remove the given statistic from the object.
     * @param key
     * @throws AdeInternalException if statistic does not exist
     */
    public void removeStat(String key) throws AdeInternalException {
        if (!m_allKeys.remove(key)) {
            throw new AdeInternalException("No statistic named " + key);
        }
        if (m_doubleStats.remove(key) == null && m_stringStats.remove(key) == null) {
            throw new AdeInternalException("Internal consistency problem: no key found");
        }

    }

    public String toString() {
        StringBuilder bldres = new StringBuilder("");
        if (m_doubleStats != null) {
            for (Entry<String, Double> dStat : m_doubleStats.entrySet()) {
                bldres.append(String.format("%-15s: %10.6f\n", dStat.getKey(), dStat.getValue()));
            }
        }
        if (m_stringStats != null) {
            for (Entry<String, String> sStat : m_stringStats.entrySet()) {
                bldres.append(String.format("%-15s: %s\n", sStat.getKey(), sStat.getValue()));
            }
        }
        return bldres.toString();
    }

    /** Replaces the statistics names according to the given map.
     *  A statistic that does not exist in the map is removed.
     * @throws AdeUsageException if map contains statistics that do not exists in this object.
     */
    public void applyResultMapping(SortedMap<String, String> mapping) throws AdeUsageException {
        checkValidity(mapping);
        m_doubleStats = applyResultMapping(m_doubleStats, mapping);
        m_stringStats = applyResultMapping(m_stringStats, mapping);
    }

    /** Verify all the names that are keys in the given map are contained as statistics in this object    *
     *  
     * @throws AdeUsageException with the details of the missing statistics if there are any
     */
    private void checkValidity(SortedMap<String, String> mapping) throws AdeUsageException {
        StringBuilder bldinvalidMaps = null;
        for (String key : mapping.keySet()) {
            if (!m_allKeys.contains(key)) {
                if (bldinvalidMaps == null) {
                    bldinvalidMaps = new StringBuilder(key);
                } else {
                    bldinvalidMaps.append(", " + key);
                }
            }
        }
        if (bldinvalidMaps != null && bldinvalidMaps.toString() != null) {
            throw new AdeUsageException("The following result mapping were not found: " + bldinvalidMaps.toString() + ". Valid staistics are " + m_allKeys);
        }
    }

    /** Apply mapping on a given map.
     * Map keys are renamed, and those missing are removed
     * 
     * @param src Map to be processed.
     * @param mapping Mapping according to which to process src
     * @return
     */
    static <T> TreeMap<String, T> applyResultMapping(SortedMap<String, T> src, SortedMap<String, String> mapping) {
        if (src == null) {
            return null;
        }
        final TreeMap<String, T> result = new TreeMap<String, T>();
        for (SortedMap.Entry<String, T> entry : src.entrySet()) {
            final String newName = mapping.get(entry.getKey());
            if (newName != null) {
                result.put(newName, entry.getValue());
            }
        }
        return result;
    }
}
