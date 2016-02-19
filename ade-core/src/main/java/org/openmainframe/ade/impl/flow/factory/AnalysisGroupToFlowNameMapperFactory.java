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
package org.openmainframe.ade.impl.flow.factory;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.flow.AnalysisGroupToFlowNameMapper;

public final class AnalysisGroupToFlowNameMapperFactory {
    
    private AnalysisGroupToFlowNameMapperFactory() {
        //private constructor
    }
    public static AnalysisGroupToFlowNameMapper getNewFlowMapper() throws AdeException {
        final Class<? extends AnalysisGroupToFlowNameMapper> clazz =
                Ade.getAde().getConfigProperties().getAnalysisGroupToFlowNameMapper();
        if (clazz == null) {
            throw new AdeUsageException(String.format(
                    "In order to use the Ade configuration analysis feature, the user must provide the propery %s with a class implementing the %s interface",
                    "analysisGroupToFlowNameMapperClass", AnalysisGroupToFlowNameMapper.class));
        }
        return getNewFlowMapper(clazz);
    }

    public static AnalysisGroupToFlowNameMapper getNewFlowMapper(Class<? extends AnalysisGroupToFlowNameMapper> clazz) throws AdeException {
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException e) {
            throw new AdeUsageException(String.format(
                    "The %s implementing class provided property must have the default constructor (no arguments) visible (public)",
                    clazz), e);
        } catch (InstantiationException e) {
            throw new AdeUsageException(String.format(
                    "The %s implementing class provided property must have include the default constructor (no arguments)",
                    clazz), e);
        }
    }
}
