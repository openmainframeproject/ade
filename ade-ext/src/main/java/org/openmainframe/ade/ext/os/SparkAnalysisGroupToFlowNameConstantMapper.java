/*
 
    Copyright IBM Corp. 2013, 2016
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
package org.openmainframe.ade.ext.os;

import org.openmainframe.ade.flow.AnalysisGroupToFlowNameConstantMapper;

/**
 * This class provide the mapping between AnalysisGroup (defined in the Reader, such as LinuxReader)
 * and the FlowName defined in the FlowLayout.xml file. 
 */
public class SparkAnalysisGroupToFlowNameConstantMapper extends AnalysisGroupToFlowNameConstantMapper {

    public SparkAnalysisGroupToFlowNameConstantMapper() {
        super("SPARK");
    }

}
