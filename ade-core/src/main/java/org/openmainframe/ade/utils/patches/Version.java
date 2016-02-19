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
package org.openmainframe.ade.utils.patches;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class represents a version composed of several integral 'components'
 * (e.g. the version 3.0.14 is composed of three ordered components: 3, 0 
 * and 14). Also, the {@link Comparable} interface is implemented, allowing 
 * to compare versions lexicographically (later version is bigger), with 
 * trailing zeros having no affect. An implementation is also provided for 
 * the {@link Version#toString()} and {@link Version#equals(Object)} methods.
 */
public class Version implements Comparable<Version>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final int PRIME_FOR_HASH = 31;

    /**
     * The maximal allowed number of chars for the String representation.
     */
    public static final int VERSION_MAX_STRING_LENGTH = 20;

    /** Index 0 of the array is the most significant version component. */
    private final Integer[] m_rawVersion;

    /** The punctuation that separates the version components.
     */
    private static final String COMPONENT_SEPARATOR = ".";

    /**
     * A new instance with the input components.
     * @param components version components starting with the most significant
     * (e.g. 3.0.14 has '3' as its first component). At least one component
     * must be specified.
     */
    public Version(Integer... components) {
        if (components.length == 0) {
            throw new IllegalArgumentException("Must receive at least one version component");
        }
        for (int component : components) {
            if (component < 0) {
                throw new IllegalArgumentException("Version components must be non negative");
            }
        }
        m_rawVersion = Arrays.copyOf(components, components.length);
        final String strRepresentation = this.toString();
        if (strRepresentation.length() > VERSION_MAX_STRING_LENGTH) {
            throw new IllegalArgumentException(String.format("Exceeded "
                    + "maximal string representation length: %s (max length: %d)",
                    strRepresentation, VERSION_MAX_STRING_LENGTH));
        }
    }

    /**
     * @see {@link #Version(Integer...)}
     */
    public Version(List<Integer> components) {
        this(components.toArray(new Integer[components.size()]));
    }

    /**
     * Copy constructor.
     * @param version to copy
     */
    public Version(Version version) {
        this(version.m_rawVersion);
    }

    /**
     * @return a copy of the version components starting from the most 
     * significant (e.g. 3.0.14 return {3, 0, 14}).
     */
    public final Integer[] getRawVersion() {
        return Arrays.copyOf(m_rawVersion, m_rawVersion.length);
    }

    /**
     * @return concatenated version components. <b>Warning:</b> integral
     * representation can lead to ambiguity (e.g. 1.0.3 and 10.3 are 
     * different but have the same integral representation; 2.0 and 2.0.0
     * should be considered equal, but have different integral representation).
     */
    public final int toInt() {
        final StringBuilder sb = new StringBuilder();
        for (int component : m_rawVersion) {
            sb.append(Integer.toString(component));
        }
        return Integer.parseInt(sb.toString());
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int component : m_rawVersion) {
            sb.append(Integer.toString(component));
            sb.append(COMPONENT_SEPARATOR);
        }
        // strip last separator
        sb.setLength(sb.length() - COMPONENT_SEPARATOR.length());

        return sb.toString();
    }

    /**
     * @see {@link Object#equals(Object)}
     * @return <b>true</b> if not <b>null</b> and {@link #compareTo(Version)} returns <b>0</b> 
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Version other = (Version) obj;
        if (this.compareTo(other) != 0) {
            return false;
        }
        return true;
    }

    /**
     * Compares versions lexicographically. Trailing zeros are ignored.
     * @see {@link Comparable#compareTo(Object)}
     */
    @Override
    public final int compareTo(Version other) {
        // compare lexicographically
        final int minLength = Math.min(this.m_rawVersion.length,
                other.m_rawVersion.length);
        for (int i = 0; i < minLength; i++) {
            final int componentComparison = this.m_rawVersion[i].compareTo(other.m_rawVersion[i]);
            if (componentComparison != 0) {
                return componentComparison;
            }
        }

        final int lengthComparison = Integer.valueOf(this.m_rawVersion.length).compareTo(other.m_rawVersion.length);
        // if versions match exactly
        if (lengthComparison == 0) {
            return 0;
        }

        final Version longer = lengthComparison > 0 ? this : other;
        for (int i = minLength; i < longer.m_rawVersion.length; i++) {
            // if any 'tail' component differs from zero, the version with
            // more components is bigger
            if (longer.m_rawVersion[i] > 0) {
                return lengthComparison;
            }
        }

        // if all of the 'tail' components are 0, the version are equal (e.g 
        // 2.0 equals 2.0.0).
        return 0;
    }

    /**
     * @see {@link Object#hashCode()}
     * @return a hash code using the components of the version. Trailing zeros in the version are ignored. 
     */
    @Override
    public final int hashCode() {
        
        int useableLength = m_rawVersion.length;
        
        //Ignore any trailing 0s since those are ignored in equals().
        for (int i = useableLength - 1; i >= 0; --i) {
            if (m_rawVersion[i] == 0) {
                useableLength--;
            } else {
                break;
            }
        }
        
        int myHash = 1;
        
        if (useableLength == 0) {
            return myHash;
        }
        
        for (int i = 0; i < useableLength; i++) {
            myHash = PRIME_FOR_HASH * myHash + m_rawVersion[i];
        }
        
        return myHash;
    }
    
    /**
     * Parses the input {@link String} into a newly created {@link Version} object. 
     * @param versionStr {@link String} to parse (e.g. "3.0.14") 
     * @return {@link Version} corresponding to the input
     * @throws IllegalArgumentException if the input has an empty component
     * 		 (e.g. 3..14 or 2.6.) or if a non integral numbers are used
     */

    public static Version parse(String versionStr) throws IllegalArgumentException {
        final String[] rawComponenets = versionStr.split(Pattern.quote(COMPONENT_SEPARATOR));
        final Integer[] components = new Integer[rawComponenets.length];
        for (int i = 0; i < rawComponenets.length; i++) {
            if (rawComponenets[i].isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid version string. Cannot have an empty componenet (e.g. 3..14 or 2.6.): " + versionStr);
            }
            try {
                components[i] = Integer.parseInt(rawComponenets[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid version string. NumberFormatException: " + versionStr, e);
            }
            if (components[i] < 0) {
                throw new IllegalArgumentException(
                        "Invalid version string. Only positive integers are allowed: " + versionStr);
            }
        }
        return new Version(components);
    }

}
