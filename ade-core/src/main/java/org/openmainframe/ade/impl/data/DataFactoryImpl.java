/*
 
    Copyright IBM Corp. 2008, 2016
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

import org.openmainframe.ade.data.IConfigurationData;
import org.openmainframe.ade.data.IDataFactory;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.data.IConfigurationData.SourceDataOrigin;
import org.openmainframe.ade.data.IMessageInstance.Severity;

public class DataFactoryImpl implements IDataFactory {

    @Override
    public IMessageInstance newMessageInstance(
            String sourceId, Date date, String messageID,
            String messageText, Severity severity) {

        return new MessageInstanceImpl(sourceId, date, messageID, messageText, severity);
    }

    @Override
    public IMessageInstance newMessageInstance(String sourceId, Date date,
            String messageId, String messageText, Severity severity, int count) {
        return new MessageInstanceImpl(sourceId, date, messageId, messageText, severity, count);
    }

    @Override
    public IMessageInstance newMessageInstance(
            String sourceId, Date date, String messageID,
            String messageText, Severity severity, int count, int countFailed) {
        return new MessageInstanceImpl(sourceId, date, messageID, messageText, severity, count, countFailed);
    }

    @Override
    public IMessageInstance newMessageInstance(String sourceId, Date date,
            String messageID, String messageText, String component,
            Severity severity) {
        return new MessageInstanceImpl(sourceId, date, messageID, messageText, component, severity);
    }

    @Override
    public IMessageInstance newMessageInstance(String sourceId, Date date,
            String messageID, String messageText, String component,
            Severity severity, int count, int countFailed) {
        return new MessageInstanceImpl(sourceId, date, messageID, messageText, component, severity, count, countFailed);
    }

    public IConfigurationData newConfigurationData(ISource source,
            Date creationTime, String configurationId,
            SourceDataOrigin sourceDataOrigin) {
        return new ConfigurationDataImpl(source, configurationId, sourceDataOrigin, creationTime, null);
    }

    @Override
    public TimeSeparator newTimeSeparator(String reason) {
        return new TimeSeparator(reason);
    }

    @Override
    public TimeSeparator newTimeSeparator(String source, String reason) {
        return new TimeSeparator(source, reason);
    }

}
