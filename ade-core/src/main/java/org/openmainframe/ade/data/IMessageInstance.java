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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

/**
 * Represents a single message from the log.   
 */
public interface IMessageInstance {

    /**
     * Severity levels for log messages. Former levels are less severe than latter ones.
     */
    public enum Severity {
        UNKNOWN, INFO, WARNING, ERROR, FATAL;
    }

    /**
     * Sets the msgId for this {@link IMessageInstance}. It is possible to set a message Id using this method
     *     if the message Id is unknown when creating the {@link IMessageInstance}. 
     * @param msgId the message Id to set. Overrides the current message Id, if exists.
     */
    void setMessageId(String msgId);

    /** 
     * Returns this message message-id.
     */
    String getMessageId();

    /** 
     * Sets the text of the message.
     */
    void setText(String text);

    /**
     * Returns free text  of the message
     * @return message text
     */
    String getText();

    /** 
     * Sets component id. 
     */
    void setComponentId(String componentId);

    /**
     * Returns the message component id also known as location at some systems.
     * @return component id
     */
    String getComponentId();

    /** 
     * Set the source id. 
     */
    void setSourceId(String sourceId);

    /** 
     * Returns the Source of the message.
     */
    String getSourceId();

    /** 
     * Sets message timestamp. 
     * @param 
     */
    void setDateTime(Date date);

    /** 
     * Returns the message's timestamp. 
     */
    Date getDateTime();

    /**
     * Sets a value for the specified user-defined property. If a property with the specified name already exists 
     *     the new value overrides the old one.
     *  
     * @param propertyName a user-defined property name.
     * @param propertyValue value for the specified property. Use <code>null</code> for removing the property and an 
     *     empty string for specifying an empty value.
     */
    void setProperty(String propertyName, String propertyValue);

    /**
     * Retrieves the value for the user-defined property with the specified name. 
     * 
     * @param propertyName the name of the property to retrieve
     * @return the value for the specified property name and <code>null</code> if no property with the
     *     specified name exists.
     */
    String getProperty(String propertyName);

    /**
     * Returns the severity of this message instance.
     * @return Severity for this message or {@link Severity#UNKNOWN} if no severity parameter is indicated. 
     */
    Severity getSeverity();

    /**
     * Returns the count of this message instance.
     * @return the count of this message or 1 if no count parameter is indicated. 
     */

    int getCount();

    /**
     * Returns the countFailed of this message instance.
     * @return the countFailed of this message or 0 if no count parameter is indicated. 
     */

    int getCountFailed();

    static class ByTimeComparator implements Comparator<IMessageInstance>, Serializable{

        public ByTimeComparator() {
            //Take the default constructor
        }
        
        private static final long serialVersionUID = -9080123326300192577L;

        @Override
        public int compare(IMessageInstance arg0, IMessageInstance arg1) {
            return arg0.getDateTime().compareTo(arg1.getDateTime());
        }

    }

}
