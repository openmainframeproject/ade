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
package org.openmainframe.ade.ext.main;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.ext.main.helper.AdeExtRequestType;
import org.openmainframe.ade.ext.main.helper.UploadOrAnalyze;
import org.openmainframe.ade.flow.IMessageInstanceTarget;

/**
 * Main of analyze process
 */
public class Analyze extends UploadOrAnalyze {
    /**
     * Input options
     */
    private static final String OPTION_SOURCES = "sources";

    /**
     * The entry point to Analyze.
     * 
     * @param args
     * @throws AdeException
     */
    public static void main(String[] args) throws AdeException {
        final Analyze analyze = new Analyze();
        try {
            analyze.run(args);
        } catch (AdeUsageException e) {
            analyze.getMessageHandler().handleUserException(e);
        } catch (AdeInternalException e) {
            analyze.getMessageHandler().handleAdeInternalException(e);
        } catch (AdeException e) {
            analyze.getMessageHandler().handleAdeException(e);
        } catch (Throwable e) {
            analyze.getMessageHandler().handleUnexpectedException(e);
        } finally {
            analyze.quietCleanup();
        }
    }

    /**
     * Constructor
     * @param requestType
     */
    protected Analyze() {
        super(AdeExtRequestType.ANALYZE);
    }

    /**
     * Return the MessageInstanceTarget that perform both Upload and Analyze.
     */
    @Override
    protected final IMessageInstanceTarget getMITarget() throws AdeException {
        return Ade.getAde().getActionsFactory().newLogUploaderAnalyzer();
    }

    /**
     * Method to parse specific arguments for "Analyze".
     */
    @Override
    protected final void parseArgs(String[] args) throws AdeException {
        final Options options = new Options();

        OptionBuilder.withArgName(OPTION_SOURCES);
        OptionBuilder.withLongOpt(OPTION_SOURCES);
        OptionBuilder.isRequired(false);
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Specify the Source to be analyzed.  ");
        options.addOption(OptionBuilder.create("s"));

        super.parseArgs(options, args);

    }
}
