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
package org.openmainframe.ade.data;

import java.util.Date;

import org.openmainframe.ade.data.IConfigurationData.SourceDataOrigin;
import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.impl.data.TimeSeparator;

/**
 * Factory for data classes.
 */
public interface IDataFactory {

    /**
     *  Creates a new message instance object.
     *
     * @param sourceId the source of this message. This is an arbitrary property that can be used
     *     to aggregate or differentiate messages
     * @param date timestamp for this message
     * @param messageId an identifier for this message instance's type.
     * @param messageText text of this message
     * @param severity severity for this message.
     * @return an object representing the message
     */
    IMessageInstance newMessageInstance(String sourceId, Date date,
            String messageId, String messageText, IMessageInstance.Severity severity);

    /**
     *  Creates a new message instance object.
     *
     * @param sourceId the source of this message. This is an arbitrary property that can be used
     *     to aggregate or differentiate messages
     * @param date timestamp for this message
     * @param messageId an identifier for this message instance's type.
     * @param messageText text of this message
     * @param component component that issued this message
     * @param severity severity for this message.
     * @return an object representing the message
     */
    IMessageInstance newMessageInstance(String sourceId, Date date,
            String messageId, String messageText, String component, IMessageInstance.Severity severity);

    /**
     *  Creates a new message instance object.
     *
     * @param sourceId the source of this message. This is an arbitrary property that can be used
     *     to aggregate or differentiate messages
     * @param date timestamp for this message
     * @param messageId an identifier for this message instance's type.
     * @param messageText text of this message
     * @param severity severity for this message.
     * @param count the number of message instances
     * @param countFailed the number of failed instance counts
     * @return an object representing the message
     */
    IMessageInstance newMessageInstance(String sourceId, Date date, String messageID,
            String messageText, Severity severity, int count, int countFailed);

    /**
     *  Creates a new message instance object.
     *
     * @param sourceId the source of this message. This is an arbitrary property that can be used
     *     to aggregate or differentiate messages
     * @param date timestamp for this message
     * @param messageId an identifier for this message instance's type.
     * @param messageText text of this message
     * @param component component that issued this message
     * @param severity severity for this message.
     * @param count the number of message instances
     * @param countFailed the number of failed instance counts
     * @return an object representing the message
     */
    IMessageInstance newMessageInstance(String sourceId, Date date, String messageID,
            String messageText, String component, Severity severity, int count, int countFailed);

    /**
     *  Creates a new message instance object.
     *
     * @param sourceId the source of this message. This is an arbitrary property that can be used
     *     to aggregate or differentiate messages
     * @param date timestamp for this message
     * @param messageId an identifier for this message instance's type.
     * @param messageText text of this message
     * @param severity severity for this message.
     * @param count number of times the message is replicated.
     * @return an object representing the message
     */
    IMessageInstance newMessageInstance(String sourceId, Date date,
            String messageId, String messageText, Severity severity, int count);

    /**
     * Creates a new configuration data object.
     * 
     * @param source The source of this configuration.
     * @param creationTime See {@link IConfigurationData#getTimestamp()}
     * @param configurationId See {@link IConfigurationData#getConfigurationId()}
     * @param sourceDataOrigin See {@link IConfigurationData#getSourceDataOrigin()}
     * @return A newly created object implementing the {@link IConfigurationData} interface. 
     */
    IConfigurationData newConfigurationData(ISource source, Date creationTime, 
            String configurationId, SourceDataOrigin sourceDataOrigin);

    /**
     * Creates a new time separator object that is related to all sources.
     * 
     * @param reason The reason for this {@link TimeSeparator}.
     *     E.g. if the reason is a new file, the reason can contain the
     *     path to the file. Mainly used for debug.
     * @return A newly created object implementing the {@link TimeSeparator}
     *     interface. This object relates to <b>ALL</b> sources!
     *     In order to obtain a {@link TimeSeparator} relating to a specific source,
     *     use {@link IDataFactory#newTimeSeparator(String, String)}.
     */
    TimeSeparator newTimeSeparator(String reason);

    /**
     * Creates a new time separator object. 
     * 
     * @param source The source that this {@link TimeSeparator} relates to.
     * @param reason The reason for this {@link TimeSeparator}.
     *     E.g. if the reason is a new file, the reason can contain the
     *     path to the file. Mainly used for debug.
     * @return A newly created object implementing the {@link TimeSeparator}
     *     interface, relating to the input source.
     */
    TimeSeparator newTimeSeparator(String source, String reason);

}
