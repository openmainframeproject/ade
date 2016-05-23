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

import java.util.Map;
import java.util.TreeMap;

import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.factory.jaxb.LinkType;
import org.openmainframe.ade.impl.flow.factory.jaxb.OutputerType;
import org.openmainframe.ade.impl.flow.factory.jaxb.PropertyType;

public final class OutputerFactory {

    private static final String DEFAULT_PACKAGE = "org.openmainframe.ade.output.";

    private OutputerFactory() {
        //private constructor
    }
    
    public static AnalyzedIntervalOutputer createEmptyOutputer(OutputerType outputerSchema,
            ISource source, FramingFlowType framingFlowType) throws AdeException {

        String name = outputerSchema.getOutputerClass();
        if (!name.contains(".")) {
            name = DEFAULT_PACKAGE + name;
        }
        AnalyzedIntervalOutputer temp;
        try {
            temp = (AnalyzedIntervalOutputer) Class.forName(name).newInstance();
            temp.setupSourceAndFlowType(source, framingFlowType);
        } catch (Exception e) {
            throw new AdeUsageException("Failed instansiating outputer class " + name, e);
        }
        AnalyzedIntervalOutputer outputer = (AnalyzedIntervalOutputer) temp;
        Map<String, Object> props = new TreeMap<String, Object>();
        for (PropertyType arg : outputerSchema.getOutputerProperty()) {
            props.put(arg.getKey(), arg.getValue());
        }
        for (LinkType arg : outputerSchema.getLinkedScorer()) {
            props.put(arg.getKey(), arg.getScorer());
        }
        outputer.setArguments(props);
        return outputer;
    }

}
