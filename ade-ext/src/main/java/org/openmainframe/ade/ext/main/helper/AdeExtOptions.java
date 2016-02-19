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
package org.openmainframe.ade.ext.main.helper;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.openmainframe.ade.ext.os.AdeExtProperties;

/**
 * Abstract class that builds the options specified in the run arguments.
 * The options in this class is common to all platforms. 
 */
public abstract class AdeExtOptions {
    /**
     * Constants related to input options.
     */
    public static final String OPTION_HELP = "help";

    public static final String OPTION_SOURCES = "sources";

    public static final String OPTION_OS_TYPE = "osType";

    public static final String OPTION_INPUT_FILE = "inputFile";

    public static final String OPTION_INPUT_DIR = "inputDir";

    public static final String OPTION_GMT_OFFSET = "gmtOffset";

    public static Options buildOptions(Options subClassOptions) {
        /* Add the options from subClass */
        Options options = new Options();
        for (Object subClassOption : subClassOptions.getOptions()) {
            Option option = (Option) subClassOption;
            options.addOption(option);
        }

        /* Add the general options */
        OptionBuilder.withArgName(OPTION_HELP);
        OptionBuilder.withLongOpt(OPTION_HELP);
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Print help message and exit");
        options.addOption(OptionBuilder.create('h'));

        OptionBuilder.withArgName(OPTION_INPUT_FILE);
        OptionBuilder.withLongOpt(OPTION_INPUT_FILE);
        OptionBuilder.hasArg();
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Input file name or 'stdin'");
        options.addOption(OptionBuilder.create('f'));

        OptionBuilder.withArgName(OPTION_INPUT_DIR);
        OptionBuilder.withLongOpt(OPTION_INPUT_DIR);
        OptionBuilder.hasArg();
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Input dir name");
        options.addOption(OptionBuilder.create('d'));

        OptionBuilder.withArgName(OPTION_SOURCES);
        OptionBuilder.withLongOpt(OPTION_SOURCES);
        OptionBuilder.hasArg();
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("Source Names.");
        options.addOption(OptionBuilder.create('s'));

        OptionBuilder.withArgName(OPTION_OS_TYPE);
        OptionBuilder.withLongOpt(OPTION_OS_TYPE);
        OptionBuilder.hasArg();
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("The OS Type."
                + "If this option is omitted, the default is Linux");
        options.addOption(OptionBuilder.create('o'));

        OptionBuilder.withArgName(OPTION_GMT_OFFSET);
        OptionBuilder.withLongOpt(OPTION_GMT_OFFSET);
        OptionBuilder.hasArg();
        OptionBuilder.isRequired(false);
        OptionBuilder.withDescription("hours offset from GMT");
        options.addOption(OptionBuilder.create('g'));

        return options;
    }

    public abstract void addPlatformSpecificOptions(Options options);

    public abstract boolean readOptions(CommandLine line, AdeExtProperties adeExtProperties);
}
