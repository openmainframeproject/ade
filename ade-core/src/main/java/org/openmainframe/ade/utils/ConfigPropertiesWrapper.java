/*
 
    Copyright IBM Corp. 2008, 2016
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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;

/**
 * A wrapper over Java's Properties class.
 * Adds:
 * * An expected prefix to all property names: ignored properties with wrong prefix.
 * * Type-safe conversions to int, double and boolean
 * * Nice error reporting
 * The {@link Map} implemented is needed for this class to work with {@link Property},
 * hence only a (required) small part of the interface is implemented
 */
public class ConfigPropertiesWrapper implements Map<String, String> {

    /** Create an object that expects properties with the given prefix */
    public ConfigPropertiesWrapper(String parameterPrefix) {
        m_parameterPrefix = parameterPrefix;
    }

    /** Loads the properties in the given file.
     * properties not having the expected prefix are ignored.
     */
    public void addPropertyFile(String fileName) throws AdeUsageException {
        Properties p = loadRawProperties(fileName);
        addProperties(p, fileName);
    }

    public void addOverrides(String fileName) throws AdeUsageException {
        Properties props = loadRawProperties(fileName);
        for (Object key : props.keySet()) {
            String strKey = (String) key;
            if (!m_rawProperties.containsKey(strKey)) {
                throw new AdeUsageException("File " + fileName + " contains a property which is not an override: " + strKey);
            }
            String val = props.getProperty(strKey);
            m_rawProperties.put(strKey, new InternalProperty(strKey, val, fileName));
        }
    }

    /** Adds the given properties. Properties not having the expected prefix are ignored.
     * 
     * @param props The properties to add
     * @param source A name associated with them for error reports (e.g., file name)
     */
    public void addProperties(Properties props, String source) {
        for (Object key : props.keySet()) {
            String strKey = (String) key;
            if (strKey == null || !strKey.toLowerCase().startsWith(m_parameterPrefix)) {
                continue;
            }
            String val = props.getProperty(strKey);
            m_rawProperties.put(strKey, new InternalProperty(strKey, val, source));
        }
    }

    /** Returns whether this object contains given property */
    public boolean hasProperty(String key) throws AdeInternalException {
        return getRawPropertyValue(key) != null;
    }

    /** Returns value of given property or throws an exception if missing */
    public String getStringProperty(String key) throws AdeInternalException {
        String res = getRawPropertyValue(key);
        if (res == null) {
            throw new AdeInternalException("Property " + key + " is missing from config file");
        }
        return res;
    }

    public String getFileNameAsStringAssertExists(String key) throws AdeInternalException, AdeUsageException {
        String res = getStringProperty(key);
        if (!new File(res).exists()) {
            throw new AdeUsageException("Property " + key + " points to a non-existant file " + res);
        }
        return res;
    }

    /** Returns value of given property or throws an exception if missing */
    public String getStringProperty(String key, String defaultVal) throws AdeInternalException {
        if (hasProperty(key)) {
            return getStringProperty(key);
        } else {
            return defaultVal;
        }
    }

    /** Returns value of given property as an int.
     * Throws an exception if missing or if cannot be converted to int */
    public int getIntProperty(String key) throws AdeInternalException {
        String val = getStringProperty(key);
        try {
            return Integer.valueOf(val);
        } catch (NumberFormatException e) {
            throw new AdeInternalException("Property " + key + " should be int. Instead found: " + val, e);
        }

    }

    /** Returns value of given property as an int or defaultValue if missing.
     * Throws an exception if cannot be converted to int */
    public int getIntProperty(String key, int defaultValue) throws AdeInternalException {
        if (hasProperty(key)) {
            return getIntProperty(key);
        } else {
            return defaultValue;
        }
    }

    /** Returns value of given property as an long.
     * Throws an exception if missing or if cannot be converted to long */
    public Long getLongProperty(String key) throws AdeInternalException {
        String val = getStringProperty(key);
        try {
            return Long.valueOf(val);
        } catch (NumberFormatException e) {
            throw new AdeInternalException("Property " + key + " should be long. Instead found: " + val, e);
        }

    }

    /** Returns value of given property as an long or defaultValue if missing.
     * Throws an exception if cannot be converted to long */
    public Long getLongProperty(String key, Long defaultValue) throws AdeInternalException {
        if (hasProperty(key)) {
            return getLongProperty(key);
        } else {
            return defaultValue;
        }
    }

    /** Returns value of given property as a double.
     * Throws an exception if missing or if cannot be converted to double */
    public double getDoubleProperty(String key) throws AdeInternalException {
        String val = getStringProperty(key);
        try {
            return Double.valueOf(val);
        } catch (NumberFormatException e) {
            throw new AdeInternalException("Property " + key + " should be a double. Instead found: " + val, e);
        }
    }

    /** Returns value of given property as a double or defaultValue if missing.
     * Throws an exception if cannot be converted to double */
    public double getDoubleProperty(String key, double defaultValue) throws AdeInternalException {
        if (hasProperty(key)) {
            return getDoubleProperty(key);
        } else {
            return defaultValue;
        }
    }

    /** Returns value of given property as a boolean.
     * Throws an exception if missing or if cannot be converted to boolean */
    public boolean getBooleanProperty(String key) throws AdeInternalException {
        String val = getStringProperty(key);
        if (val.equalsIgnoreCase("true")) {
            return true;
        }
        if (val.equalsIgnoreCase("false")) {
            return false;
        }
        throw new AdeInternalException("Illegal boolean value " + val + " for " + key);
    }

    /** Returns value of given property as a boolean or defaultValue if missing.
     * Throws an exception if cannot be converted to boolean */
    public boolean getBooleanProperty(String key, boolean defaultValue) throws AdeInternalException {
        if (hasProperty(key)) {
            return getBooleanProperty(key);
        } else {
            return defaultValue;
        }
    }

    /** Returns value of given property as a {@link Class} or throws an exception if missing */
    @SuppressWarnings("unchecked")
    public <SUPER> Class<? extends SUPER> getClassProperty(String key, Class<SUPER> superClass) throws AdeUsageException, AdeInternalException {
        try {
            Class<?> res = Class.forName(getStringProperty(key));
            if (!superClass.isAssignableFrom(res)) {
                throw new AdeUsageException(String.format("The class defined in the %s property must implement/extend %s", key, superClass.getName()));
            }
            return (Class<? extends SUPER>) res;
        } catch (ClassNotFoundException e) {
            throw new AdeUsageException(String.format("Failed loading class property (%s)", key), e);
        }
    }

    /** Returns value of given property as a {@link Class} or throws an exception if missing */
    public <SUPER> Class<? extends SUPER> getClassProperty(String key, Class<SUPER> superClass, Class<? extends SUPER> defaultVal) throws AdeUsageException, AdeInternalException {
        if (hasProperty(key)) {
            return getClassProperty(key, superClass);
        } else {
            return defaultVal;
        }
    }

    private String m_parameterPrefix;

    private Map<String, InternalProperty> m_rawProperties = new TreeMap<>();

    private static class InternalProperty {
        String m_name;
        String m_value;
        boolean m_used;
        String m_source;

        InternalProperty(String name, String val, String source) {
            m_name = name;
            m_value = val;
            m_source = source;
            m_used = false;
        }

        @Override
        public String toString() {
            return String.format("%s=%s (source: %s)", m_name, m_value, m_source);
        }
    }

    /** Verify that all the properties have been read.
     */
    public void verifyAllPropertiesUsed() throws AdeUsageException {
        for (InternalProperty prop : m_rawProperties.values()) {
            if (!prop.m_used) {
                throw new AdeUsageException("Illegal ade property '" + prop.m_name + "' supplied in " + prop.m_source);
            }
        }
    }

    private String getRawPropertyValue(String key) throws AdeInternalException {
        if (!key.startsWith(m_parameterPrefix)) {
            throw new AdeInternalException("Key " + key + " is not prefixed with " + m_parameterPrefix);
        }
        InternalProperty prop = m_rawProperties.get(key);
        if (prop == null) {
            return null;
        }
        prop.m_used = true;
        return prop.m_value;
    }

    static private Properties trimProperties(Properties p) {
        Properties res = new Properties();
        for (Iterator<Object> it = p.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            String val = p.getProperty(key);
            if (val != null) {
                val = val.trim();
            }
            res.setProperty(key, val);
        }
        return res;
    }

    static public Properties loadRawProperties(String propertyFile) throws AdeUsageException {
        Properties props = new Properties();
        LockedInputStream lockedInputStream = null;
        try {
            lockedInputStream = new LockedInputStream(propertyFile);

            props.load(new InputStreamReader(lockedInputStream, "utf8"));
        } catch (IOException e) {
            throw new AdeUsageException("Failed reading properties from " + propertyFile + ".\nCause: " + e.getMessage(), e);
        } finally {
            if (lockedInputStream != null) {
                lockedInputStream.unlockAndClose();
            }
        }
        return trimProperties(props);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Properties: {\n");
        for (InternalProperty props : m_rawProperties.values()) {
            sb.append("\t" + props.toString() + "\n");
        }
        sb.append("}");
        return sb.toString();
    }

    // only the Map<?,?> required for PropertyAnnotation are implemented

    @Override
    public boolean containsKey(Object key) {
        return m_rawProperties.containsKey(key);
    }

    @Override
    public String get(Object key) {
        try {
            return getRawPropertyValue((String) key);
        } catch (AdeInternalException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Set<String> keySet() {
        return m_rawProperties.keySet();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<java.util.Map.Entry<String, String>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String put(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> values() {
        throw new UnsupportedOperationException();
    }

}
