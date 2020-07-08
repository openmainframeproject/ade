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
package org.openmainframe.ade.ext.main.helper;

import org.openmainframe.ade.AdeInputStream;
import org.openmainframe.ade.AdeMessageReader;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.ext.AdeExt;
import org.openmainframe.ade.ext.os.AdeExtProperties;
import org.openmainframe.ade.ext.os.parser.LinuxSyslogLineParser;
import org.openmainframe.ade.ext.os.parser.LinuxSyslogMessageReader;
import org.openmainframe.ade.ext.os.parser.SparklogLineParser;
import org.openmainframe.ade.ext.os.parser.SparklogMessageReader;
import org.openmainframe.ade.ext.stats.MessageRateStats;
import org.openmainframe.ade.impl.data.FileSeperator;

public class AdeInputStreamHandlerLinux extends AdeInputStreamHandlerExt {
    /**
     * Constructor
     * @param adeExtProperties
     * @throws AdeException
     */
    public AdeInputStreamHandlerLinux(AdeExtProperties adeExtProperties) throws AdeException {
        super(adeExtProperties);
    }
    public static boolean isSpark() throws AdeException{
        return AdeExt.getAdeExt().getConfigProperties().isSparkLog();
    }

    /**
     * Handling a stream
     */
    @Override
    public final void incomingObject(AdeInputStream stream) throws AdeException, AdeFlowException {
        super.incomingObject(stream);
        final AdeMessageReader adeMessageReader = stream.getReader();
        /* Only output the MessageRateStats only if there was some log messages */
        if (adeMessageReader.getLineNumber() > 0) {
            /* End of stream has reached, output the MessageRateStats */
            MessageRateStats.generateReportForAllSources();
        }
    }

    /**
     * This is the processing after a message has been read, before sending to Ade for Upload/Analyze.
     */
    @Override
    protected final void beforeSendMessage(IMessageInstance mi) throws AdeException {
        /* Determine if there are gaps caused by logging service not available */
        handleLoggerUnavailable(mi);

        final MessageRateStats msgRateStats = MessageRateStats.getMessageRateStatsForSource(mi.getSourceId());
        /* Get the reader object */

        if (isSpark()){
            final SparklogMessageReader sparkReader = (SparklogMessageReader) a_adeInputStream.getReader();
            // Keep statistics for this MI
            msgRateStats.addMessage(mi.getMessageId(), mi.getDateTime().getTime(), sparkReader.isWrapperMessage());
        }
        else{
            final LinuxSyslogMessageReader linuxReader = (LinuxSyslogMessageReader) a_adeInputStream.getReader();
            msgRateStats.addMessage(mi.getMessageId(), mi.getDateTime().getTime(), linuxReader.isWrapperMessage());
        }

        super.beforeSendMessage(mi);
    }

    /**
     * Determine if the Logging service was not available, and handle it accordingly.
     * 
     * Logger Unavailable is different than no mesage being logged.
     * @throws AdeException 
     * @throws AdeFlowException 
     */
    private void handleLoggerUnavailable(IMessageInstance mi) throws AdeFlowException, AdeException {
        if (!isSpark()){
            if (LinuxSyslogLineParser.isSyslogNgRestarted(mi)) {
                /* Indicate the SysLogNg has restarted.  */
                incomingSeparator(new FileSeperator(mi.getSourceId(), "syslog-ng starting"));
                MessageRateStats.getMessageRateStatsForSource(mi.getSourceId()).markLoggerStarting(mi.getDateTime().getTime());
            }
        }
    }

}
