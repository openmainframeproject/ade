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

import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.IMessageInstance.Severity;
import org.openmainframe.ade.impl.data.MessageInstanceImpl;

import junit.framework.TestCase;

public class TestMessageInstance extends TestCase {

    public void testMessageInstance() {
        Date msgDate = new Date();
        String sid = "hello";

        // initialize the message instance key    
        String msgCompId = "compA";
        int msgID = 3;
        String msgTxt = "new message text";
        // initialize a new MessageInstance
        IMessageInstance msgInstance = new MessageInstanceImpl(sid, msgDate, "msg" + msgID, msgTxt, msgCompId, Severity.ERROR);
        assertEquals("msg" + msgID, msgInstance.getMessageId());
        assertEquals(msgCompId, msgInstance.getComponentId());
        assertEquals(msgTxt, msgInstance.getText());
        assertEquals(Severity.ERROR, msgInstance.getSeverity());

        IMessageInstance msgInstance2 = new MessageInstanceImpl(sid, msgDate, "msg" + msgID, msgTxt, msgCompId, Severity.ERROR);
        assertEquals(msgInstance2, msgInstance);
    }
}
