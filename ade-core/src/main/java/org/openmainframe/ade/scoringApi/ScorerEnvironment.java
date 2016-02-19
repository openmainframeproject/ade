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

import java.io.File;
import java.io.Serializable;

/** A collection of variables required as general background data
 * for scorers */
public class ScorerEnvironment implements Serializable {

    public ScorerEnvironment(String sourceGroup) {
        m_analysisGroup = sourceGroup;
    }

    private static final long serialVersionUID = 1L;

    /** Directory where trace files should be written to */
    public File m_traceOutputPath = null;
    public String m_analysisGroup = null;
}
