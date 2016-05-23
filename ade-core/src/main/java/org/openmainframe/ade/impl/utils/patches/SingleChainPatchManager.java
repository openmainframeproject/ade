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
package org.openmainframe.ade.impl.utils.patches;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openmainframe.ade.utils.patches.IPatch;
import org.openmainframe.ade.utils.patches.IPatchManager;
import org.openmainframe.ade.utils.patches.Version;

/**
 *	Maintains a single chain of {@link IPatch}es, each one with {@link 
 *	IPatch#fromVersion()} equal to its previous'es {@link IPatch#toVersion()} 
 */
public class SingleChainPatchManager implements IPatchManager {

    /**
     * Stored {@link IPatch}es. The head {@link IPatch} is the earliest one.
     * The implementation type is used since we want specific {@link LinkedList}
     * methods, specifically {@link LinkedList#getFirst()} and 
     * {@link LinkedList#getLast()} 
     */
    private final LinkedList<IPatch> m_patches;

    /**
     *	@param patches Contains only patches with unique {@link 
     *	IPatch#fromVersion()} and {@link IPatch#toVersion()} (with respect
     *	to {@link Version#equals(Object)}).	Also, each {@link 
     *	IPatch#toVersion()} must have a "following {@link IPatch}" with
     *	a matching {@link IPatch#fromVersion()} (and vice versa), 
     *	except the earliest and latest {@link IPatch}es. 
     */
    public SingleChainPatchManager(IPatch... patches) {
        m_patches = new LinkedList<>();

        final IPatch[] copy = Arrays.copyOf(patches, patches.length);
        Arrays.sort(copy, new PatchFromVersionCompare());
        for (IPatch patch : copy) {
            addPatch(patch);
        }
    }

    /**
     * @see {@link #SingleChainPatchManager(IPatch...)};
     */
    public SingleChainPatchManager(Collection<IPatch> patches) {
        this(patches.toArray(new IPatch[patches.size()]));
    }

    /**
     * Compares {@link IPatch} object according to their 
     * {@link IPatch#fromVersion()}.
     */
    private static class PatchFromVersionCompare implements Comparator<IPatch> {
        @Override
        public int compare(IPatch p1, IPatch p2) {
            return p1.fromVersion().compareTo(p2.fromVersion());
        }
    }

    /**
     * 
     * @see org.openmainframe.ade.utils.patches.IPatchManager#addPatch(org.openmainframe.ade.utils.patches.IPatch)
     * @param patch must have a {@link IPatch#fromVersion()} equal to the {@link 
     * 		IPatch#toVersion()} of the latest inserted {@link IPatch}, or have a
     * 		{@link IPatch#toVersion()} equal to the {@link IPatch#fromVersion()}
     * 		of the earliest inserted {@link IPatch}.
     */
    @Override
    public final void addPatch(IPatch patch) throws IllegalArgumentException {
        if (m_patches.isEmpty()) {
            m_patches.add(patch);
        } else {
            if (m_patches.getLast().toVersion().equals(patch.fromVersion())) {
                m_patches.add(patch);
            } else if (m_patches.getFirst().fromVersion().equals(patch.toVersion())) {
                m_patches.push(patch);
            } else {
                throw new IllegalArgumentException(String.format("The " 
                        + "patch V%s -> V%s cannot be added before the " 
                        + "current first patch (V%s -> V%s), or after "
                        + "the current last patch (V%s -> V%s).",
                        patch.fromVersion(), patch.toVersion(),
                        m_patches.getFirst().fromVersion(),
                        m_patches.getFirst().toVersion(),
                        m_patches.getLast().fromVersion(),
                        m_patches.getLast().toVersion()));
            }
        }
    }

    @Override
    public final IPatch getPatch(Version fromVersion, Version toVersion) {
        for (IPatch patch : m_patches) {
            final int fromVerComp = patch.fromVersion().compareTo(fromVersion);
            if (fromVerComp == 0) {
                return patch.toVersion().equals(toVersion) ? patch : null;
            }
            if (fromVerComp > 1) {
                // no more chance to find the patch as we passed it's version
                return null;
            }
        }
        return null;
    }

    @Override
    public final Set<IPatch> getPatchesFromVersion(Version fromVersion) {
        final Set<IPatch> res = new TreeSet<>();
        for (IPatch patch : m_patches) {
            final int fromVerComp = patch.fromVersion().compareTo(fromVersion);
            if (fromVerComp == 0) {
                res.add(patch);
                break;
            }
            if (fromVerComp > 1) {
                // no more chance to find the patch as we passed it's version
                break;
            }
        }
        return res;
    }

    @Override
    public final List<IPatch> getPatchChainToLatest(Version fromVersion) {
        return getPatchChain(fromVersion, m_patches.getLast().toVersion());
    }

    @Override
    public final List<IPatch> getPatchChain(Version fromVersion, Version toVersion) {
        List<IPatch> res = null;

        boolean fromVersionMatched = false;

        for (IPatch patch : m_patches) {
            if (!fromVersionMatched) {
                final int fromVerComp = patch.fromVersion().compareTo(fromVersion);
                if (fromVerComp == 0) {
                    fromVersionMatched = true;
                    res = new ArrayList<>();
                    res.add(patch);
                } else if (fromVerComp > 1) {
                    // no more chance to find the first patch as we passed 
                    // it's version
                    break;
                }
            } else {
                final int toVerComp = patch.toVersion().compareTo(toVersion);

                if (toVerComp <= 0) {
                    res.add(patch);
                }
                if (toVerComp >= 0) {
                    break;
                }
            }
        }

        return res;
    }

}
