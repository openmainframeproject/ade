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
package org.openmainframe.ade.impl.data;

import java.util.Date;

import org.openmainframe.ade.AdeInternal;
import org.openmainframe.ade.data.IConfigurationData;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;

/**
 * An internal class aggregating all the data a DB row holds on a configuration
 */
public class ConfigurationDataImpl implements IConfigurationData {
    private ISource m_source;
    private String m_configurationId;
    private SourceDataOrigin m_sourceDataOrigin;
    private Date m_creationTime;
    private PeriodImpl m_period;

    ConfigurationDataImpl(ISource source, String configurationId, SourceDataOrigin sourceDataOrigin, Date creationTime, PeriodImpl period) {
        m_configurationId = configurationId;
        m_sourceDataOrigin = sourceDataOrigin;
        m_creationTime = creationTime;
        m_source = source;
        m_period = period;
    }

    public ConfigurationDataImpl(IConfigurationData cd) throws AdeException {
        this(cd.getSource(), cd.getConfigurationId(), cd.getSourceDataOrigin(), cd.getTimestamp(), calcContainingPeriod(cd));
    }

    private static PeriodImpl calcContainingPeriod(IConfigurationData cd) throws AdeException {
        final Date periodStart = PeriodUtils.getContainingPeriodStart(cd.getTimestamp());
        final Date periodEnd = PeriodUtils.getNextPeriodStart(periodStart);
        return AdeInternal.getAdeImpl().getDataStore().periods().getOrAddPeriod(cd.getSource(), periodStart, periodEnd);
    }

    public PeriodImpl getContainingPeriod() {
        return m_period;
    }

    @Override
    public String getConfigurationId() {
        return m_configurationId;
    }

    @Override
    public SourceDataOrigin getSourceDataOrigin() {
        return m_sourceDataOrigin;
    }

    @Override
    public ISource getSource() {
        return m_source;
    }

    @Override
    public Date getTimestamp() {
        return m_creationTime;
    }

}