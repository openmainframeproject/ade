/*
 
    Copyright IBM Corp. 2012, 2016
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

package org.openmainframe.ade.ext.output;
/**
 * Class that contains the counts of new and never seen before messages.
 */
public class NewAndNeverSeenBeforeMessages {
    
    /**
     * If more information is needed about new messages and never seen before messages, add in this class and return the
     * NewAndNeverSeenBforeMessages object.
     */
    private final int m_numNewMessages;
    private final int m_numNeverSeenBeforeMessages;

    public NewAndNeverSeenBeforeMessages(int numNewMessages, int numNeverSeenBeforeMessages) {
        m_numNewMessages = numNewMessages;
        m_numNeverSeenBeforeMessages = numNeverSeenBeforeMessages;
    }
    
    public int getNumNewMessages(){
        return m_numNewMessages;
    }
    
    public int getNumNeverSeenBeforeMessages(){
        return m_numNeverSeenBeforeMessages;
    }

}