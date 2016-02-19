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
package org.openmainframe.ade.output;

import java.io.File;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.jaxb.AnalyzedIntervalType;

/**
 * A class that is used in the marshalling a analysis results.
 * Marshalled files are stores in xml format.
 */
public interface IAnalysisResultMarshaller {

    /**
     * @param f analyzed interval XML file
     * @return the analyzed interval if such exists (loaded from analyzed interval XML)
     * @throws AdeException
     */
    public AnalyzedIntervalType loadAnalyzedInterval(File f) throws AdeException;

}
