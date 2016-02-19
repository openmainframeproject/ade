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

import java.util.List;
import java.util.Set;

/**
 * Maintains {@link IPatch} objects and allows certain queries
 */
public interface IPatchManager {

    /**
     * @param patch the {@link IPatch} to be add to this {@link IPatchManager}
     * @throws IllegalArgumentException if the input {@link IPatch} is
     * 		not suitable for insertion (reason may change from one 
     * 		implementation to another).
     */
    void addPatch(IPatch patch) throws IllegalArgumentException;

    /**
     * @param fromVersion from {@link Version}
     * @param toVersion to {@link Version}
     * @return a unique {@link patch} matching the input 'from' and 'to' 
     * 		{@link Version}s. <b>null</b> is returned if no such 
     * 		{@link IPatch} exists.
     */
    IPatch getPatch(Version fromVersion, Version toVersion);

    /**
     * @param fromVersion from {@link Version}
     * @return {@link IPatch}es from the input 'from' {@link Version}.
     * 		an empty {@link Set} is returned if no suck patches exist.
     */
    Set<IPatch> getPatchesFromVersion(Version fromVersion);

    /**
     * @param fromVersion from {@link Version}
     * @return {@link IPatch}es starting from the input {@link Version},
     * 		where each {@link IPatch}'s {@link IPatch#toVersion()} is followed
     * 		by a {@link IPatch} with an equal {@link IPatch#fromVersion()}.
     * 		The last {@link IPatch} has the latest possible {@link 
     * 		IPatch#toVersion()} (for a {@link List} holding the above relation).
     * 		if no such chain exists (no such from {@link Version}), 
     * 		<b>null</b> is returned.
     */
    List<IPatch> getPatchChainToLatest(Version fromVersion);

    /**
     * @param fromVersion from {@link Version}
     * @param toVersion to {@link Version}
     * @return {@link IPatch}es starting from the input 'from' {@link Version},
     * 		where each {@link IPatch}'s {@link IPatch#toVersion()} is followed
     * 		by a {@link IPatch} with an equal {@link IPatch#fromVersion()}.
     * 		The last {@link IPatch}'s {@link IPatch#toVersion()} equals the
     * 		input 'to' version. if no such chain exists, <b>null</b> is 
     * 		returned.
     */
    List<IPatch> getPatchChain(Version fromVersion, Version toVersion);
}
