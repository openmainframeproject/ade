/*
 
    Copyright IBM Corp. 2009, 2016
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
import java.util.HashMap;
import java.util.Map;

import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.impl.utils.HashCodeUtil;

/**
 * Basic implementation of the {@link IMessageInstance} interface.
 *
 */
public class MessageInstanceImpl implements IMessageInstance {

    private String m_sourceId;
    private Date m_date;
    private String m_msgId;
    private String m_text;
    private String m_componentId;
    private int mFHashCode = 0;
    private Map<String, String> m_properties;
    private Severity m_severity;
    private int m_count = 1;
    private int m_countFailed = 0;

    /**
     * Construct a new MessageInstanceImpl.
     * 
     * @param sourceId the source ID of the message
     * @param date the date and time of the message
     * @param messageId identifies the message type
     * @param messageText the message text
     * @param compId the message component ID
     * @param severity the message severity
     */
    MessageInstanceImpl(String sourceId, Date date,
            String messageId, String messageText,
            String compId, Severity severity) {
        super();
        m_sourceId = sourceId;
        m_date = date;
        m_msgId = messageId;
        m_text = messageText;
        m_componentId = compId;
        m_severity = severity;
    }

    /**
     * Construct a new MessageInstanceImpl without specifying the message component ID.
     * 
     * @param sourceId the source ID of the message
     * @param date the date and time of the message
     * @param messageId identifies the message type
     * @param messageText the message text
     * @param severity the message severity
     */
    MessageInstanceImpl(String sourceId, Date date,
            String messageId, String messageText, Severity severity) {
        this(sourceId, date, messageId, messageText, null, severity);
    }

    /**
     * Construct a new MessageInstanceImpl without specifying the message component ID and specifying the
     * message instance count and the failed instance count.
     * 
     * @param sourceId the source ID of the message
     * @param date the date and time of the message
     * @param messageId identifies the message type
     * @param messageText the message text
     * @param severity the message severity
     * @param count the number of message instances
     * @param countFailed the number of failed instance counts
     */
    MessageInstanceImpl(String sourceId, Date date,
            String messageId, String messageText,
            Severity severity, int count, int countFailed) {
        this(sourceId, date, messageId, messageText, severity);
        m_count = count;
        m_countFailed = countFailed;
    }

    /**
     * Construct a new MessageInstanceImpl specifying the
     * message instance count and the failed instance count.
     * 
     * @param sourceId the source ID of the message
     * @param date the date and time of the message
     * @param messageId identifies the message type
     * @param messageText the message text
     * @param component the message component ID
     * @param severity the message severity
     * @param count the number of message instances
     * @param countFailed the number of failed instance counts
     */
    MessageInstanceImpl(String sourceId, Date date,
            String messageId, String messageText, String component,
            Severity severity, int count, int countFailed) {
        this(sourceId, date, messageId, messageText, component, severity);
        m_count = count;
        m_countFailed = countFailed;
    }

    /**
     * Construct a new MessageInstanceImpl without specifying the message component ID and specifying the
     * message instance count.
     * 
     * @param sourceId the source ID of the message
     * @param date the date and time of the message
     * @param messageId identifies the message type
     * @param messageText the message text
     * @param severity the message severity
     * @param count the number of message instances
     */
    MessageInstanceImpl(String sourceId, Date date,
            String messageId, String messageText,
            Severity severity, int count) {
        this(sourceId, date, messageId, messageText, severity);
        m_count = count;
    }

    @Override
    public final String getMessageId() {
        return m_msgId;
    }

    @Override
    public final String getText() {
        return m_text;
    }

    @Override
    public final String getComponentId() {
        return m_componentId;
    }

    @Override
    public final int getCount() {
        return m_count;
    }

    @Override
    public final int getCountFailed() {
        return m_countFailed;
    }

    /**
     * Prefix the output of the toString() method with a string.
     * 
     * @param indentationPrefix the prefix to add to the output of toString().
     * 
     * @return a new string with the passed in prefix
     */
    public final String toString(String indentationPrefix) {
        return indentationPrefix + toString();
    }

    @Override
    public final String toString() {
        return m_sourceId + "-" + m_date + " messageID= " + m_msgId + " messageText= " + m_text;

    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MessageInstanceImpl) {
            final MessageInstanceImpl objMsgInstance = (MessageInstanceImpl) obj;
            return m_sourceId.equals(objMsgInstance.m_sourceId)
                    && m_date.getTime() == objMsgInstance.m_date.getTime();

        }
        return false;
    }

    @Override
    public final int hashCode() {
        if (mFHashCode == 0) {
            int result = HashCodeUtil.SEED;
            result = HashCodeUtil.hash(result, m_sourceId);
            result = HashCodeUtil.hash(result, m_date.getTime());
            mFHashCode = result;
        }
        return mFHashCode;
    }

    @Override
    public final String getSourceId() {
        return m_sourceId;
    }

    @Override
    public final Date getDateTime() {
        return m_date;
    }

    @Override
    public final void setMessageId(String msgId) {
        m_msgId = msgId;
    }

    @Override
    public final void setProperty(String propertyName, String propertyValue) {
        if (propertyName == null) {
            return;
        }
        if (propertyValue == null && m_properties != null) {
            m_properties.remove(propertyName);
            return;
        }
        if (m_properties == null) {
            m_properties = new HashMap<String, String>();
        }
        m_properties.put(propertyName, propertyValue);
    }

    @Override
    public final String getProperty(String propertyName) {
        return m_properties.get(propertyName);
    }

    @Override
    public final void setText(String text) {
        m_text = text;
    }

    @Override
    public final void setComponentId(String componentId) {
        m_componentId = componentId;
    }

    @Override
    public final void setSourceId(String sourceId) {
        m_sourceId = sourceId;
    }

    @Override
    public final void setDateTime(Date date) {
        m_date = new Date(date.getTime());
    }

    @Override
    public final Severity getSeverity() {
        return m_severity;
    }
}
