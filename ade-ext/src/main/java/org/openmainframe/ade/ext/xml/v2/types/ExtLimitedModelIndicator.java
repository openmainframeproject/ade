/*
 
    Copyright IBM Corp. 2015, 2016
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
package org.openmainframe.ade.ext.xml.v2.types;

import java.util.Collection;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.impl.scoringApi.MainScorerImpl;
import org.openmainframe.ade.scores.AdeAnomalyIntervalScorer;
import org.openmainframe.ade.scoringApi.IScorer;
import org.openmainframe.ade.scoringApi.IMainScorer;

/**
 * The ExtModelQualityIndicator contains model quality related information.
 */

public enum ExtLimitedModelIndicator {
    Unknown, No, Yes;

    /**
     * Retrieve the model quality
     * 
     * @param model
     * @return
     */
    static public ExtLimitedModelIndicator getLimitedModelIndicator(IMainScorer model) {
        final MainScorerImpl mainScorer = (MainScorerImpl) model;
        final Collection<IScorer<?, IAnalyzedInterval>> scorersList = mainScorer.getTrainedScorers();
        for (IScorer<?, IAnalyzedInterval> scorer : scorersList) {
            if (scorer instanceof AdeAnomalyIntervalScorer) {
                final double[] percentile = ((AdeAnomalyIntervalScorer) scorer).getPercentiles();
                final double percentile1 = percentile[996];
                final double percentile2 = percentile[999];

                if (percentile1 - percentile2 == 0) {
                    return Yes;
                } else {
                    return No;
                }
            }
        }

        return Unknown;
    }
}
