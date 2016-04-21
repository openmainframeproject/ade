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
package org.openmainframe.ade.resultBrowser;

import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.resultBrowser.ResultBrowserImpl;

/** An object reserved for browsing results through a web-application
 * that doesn't want to create the whole Ade object.
 * I.e., it offers quick, light-weight access to Ade analysis results
 *
 */
public abstract class ResultBrowser {

    /** Create the object attached to the given connection */
    public static ResultBrowser create(Connection con) {
        return new ResultBrowserImpl(con);
    }

    /** @return the start time of the last period for which this source has analyzed data */
    abstract public Date getLastPeriodForSource(int sourceInternalId) throws AdeException;

    /** @return the analysis results for a period with the given start time and the given source */
    abstract public List<IAnalyzedInterval> getAnalyzedIntervals(int sourceInternalId, Date date, FramingFlowType framingFlowType) throws AdeException;

    abstract public Set<String> getAllAnalyzedSources() throws AdeException;

    abstract public RawSourceMetaData getSourceMetaData(String sourceStr) throws AdeException;

}
