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
package org.openmainframe.ade.impl.scoringApi;

import java.util.TreeMap;
import java.util.Map;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.flow.factory.jaxb.LinkType;
import org.openmainframe.ade.impl.flow.factory.jaxb.PropertyType;
import org.openmainframe.ade.impl.flow.factory.jaxb.ScoringSchemaType;
import org.openmainframe.ade.scoringApi.ILearner;
import org.openmainframe.ade.scoringApi.IScorer;
import org.openmainframe.ade.scoringApi.IMainScorer;

public final class ScorerFactory {

    private static final String DEFAULT_PACKAGE = "org.openmainframe.ade.scores.";

    private ScorerFactory(){
        
    }
    public static IScorer<?, IAnalyzedInterval> createEmptyScorer(
            ScoringSchemaType scorerSchema) throws AdeException {
        return createEmptyScorer(scorerSchema, null);
    }

    @SuppressWarnings("unchecked")
    public static IScorer<?, IAnalyzedInterval> createEmptyScorer(
            ScoringSchemaType scorerSchema, IMainScorer mainScorer) throws AdeException {

        String name = scorerSchema.getScorerClass().trim();
        if (!name.contains(".")) {
            name = DEFAULT_PACKAGE + name;
        }
        Object temp;
        try {
            temp = Class.forName(name).newInstance();
        } catch (Exception e) {
            throw new AdeUsageException("Failed instansiating scorer class " + name, e);
        }
        if (!(temp instanceof ILearner)) {
            throw new AdeUsageException("Given class " + name + " is not a scorer");
        }
        final IScorer<?, IAnalyzedInterval> scorer = (IScorer<?, IAnalyzedInterval>) temp;
        final Map<String, Object> props = new TreeMap<String, Object>();
        for (PropertyType arg : scorerSchema.getScorerProperty()) {
            props.put(arg.getKey(), arg.getValue());
        }
        for (LinkType arg : scorerSchema.getLinkedScorer()) {
            assert mainScorer != null;
            props.put(arg.getKey(), mainScorer.getTrainedScorer(arg.getScorer()));
        }
        scorer.setArguments(props);
        return scorer;
    }

}
