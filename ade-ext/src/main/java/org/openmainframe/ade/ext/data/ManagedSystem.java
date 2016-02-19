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
package org.openmainframe.ade.ext.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * ManagedSystem
 * This class provides a way to derive a representation of the
 * data associated with a managed system.
 *
 */
public class ManagedSystem implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ManagedSystemName msn; // Full name of the managed system
    private final Long gmt_offset; // GMT offset (64 bit "clock ticks" value)
    private final ClientOSType ostype; // Name of operating system
    private final Set<Group> systemgroups; // Groups to which this system belongs

    // ----------------------------------------
    // Constructors
    // ----------------------------------------
    public ManagedSystem(ManagedSystemName msn, Long gmt, ClientOSType ostype, Set<Group> systemgroups) {
        if (msn == null) {
            throw new IllegalArgumentException("ManagedSystemName must be non-null");
        }
        this.msn = msn;
        this.gmt_offset = gmt;
        this.ostype = ostype;
        this.systemgroups = systemgroups;
    }

    // ----------------------------------------
    // Getters
    // ----------------------------------------
    public final ManagedSystemName getName() {
        return msn;
    }

    public final Long getGmtOffset() {
        return gmt_offset;
    }

    public final ClientOSType getOSType() {
        return ostype;
    }

    public final Set<Group> getGroups() {
        return Collections.unmodifiableSet(systemgroups);
    }

    @Override
    public String toString() {
        return msn.toString();
    }

}
