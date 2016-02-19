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
package org.openmainframe.ade.impl.actions;

import org.openmainframe.ade.actions.IActionsFactory;
import org.openmainframe.ade.actions.IParsingQualityReporter;
import org.openmainframe.ade.flow.IMessageInstanceTarget;
import org.openmainframe.ade.impl.data.TextClusteringComponentModel;

public class ActionsFactoryImpl implements IActionsFactory {

    @Override
    public final IParsingQualityReporter createParsingQualityReporter() {
        return new ParsingQualityReporterImpl();
    }

    @Override
    public final TextClusteringComponentModel getTextClusteringModel(boolean updateDataStore) {
        return new TextClusteringComponentModel(updateDataStore);
    }

    @Override
    public final IMessageInstanceTarget newLogUploader() {
        return new LogUploader();
    }

    @Override
    public final IMessageInstanceTarget newLogAnalyzer() {
        return new LogAnalyzer();
    }

    @Override
    public final IMessageInstanceTarget newLogUploaderAnalyzer() {
        return new LogUploaderAnalyzer();
    }

    @Override
    public final IMessageInstanceTarget newIntervalStatistics() {
        return new IntervalStatistics();
    }

    @Override
    public final IMessageInstanceTarget newMessageInstanceStatistics() {
        return new MessageInstanceStatistics();
    }

}
