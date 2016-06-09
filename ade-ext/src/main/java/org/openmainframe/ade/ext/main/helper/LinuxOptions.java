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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.openmainframe.ade.ext.os.LinuxAdeExtProperties;
import org.openmainframe.ade.ext.os.AdeExtProperties;

/**
 * Builds the Linux options.
 */
public class LinuxOptions extends AdeExtOptions{
    
    private static final String OPTION_YEAR = "years";
    
    @Override
    public void addPlatformSpecificOptions(Options options){
        OptionBuilder.withArgName(OPTION_YEAR);
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("year of messages in Linux syslogs");
        options.addOption(OptionBuilder.create(OPTION_YEAR));
    }
    
    @Override
    public boolean readOptions(CommandLine line, AdeExtProperties linuxProperties){
        boolean readOptionsSuccessful = true;
        /* All the required parameters for Linux */
        List<String> requiredParameterList = new ArrayList<String>();
        requiredParameterList.add(AdeExtOptions.OPTION_GMT_OFFSET);

        List<String> optionalParameterList = new ArrayList<String>();
        optionalParameterList.add(OPTION_YEAR);

        List<String> allParameterList = new ArrayList<String>();
        allParameterList.addAll(optionalParameterList);
        allParameterList.addAll(requiredParameterList);

        /* Go through and process all the required parameters */
        for (String optionName : allParameterList) {
            String inputParameterValue;

            /* Get the value if it exist. 
             * If option wasn't defined, output an error. */
            if (line.hasOption(optionName)) {
                inputParameterValue = line.getOptionValue(optionName);

                /* Process each option */
                if (optionName.equalsIgnoreCase(OPTION_YEAR)) {
                    String m_year = line.getOptionValue(OPTION_YEAR);
                    int y = Integer.parseInt(m_year);

                    if (y < 1970 || y > 2999) {
                        System.out.println("the year entered " + m_year + " appears to be wrong");
                        System.exit(-3);
                    }

                    ((LinuxAdeExtProperties) linuxProperties).setYear(y);
                } else if (optionName.equalsIgnoreCase(AdeExtOptions.OPTION_GMT_OFFSET)) {
                    long gmtOffset = Long.parseLong(inputParameterValue);
                    ((LinuxAdeExtProperties) linuxProperties).setGmtOffset(gmtOffset);
                    ((LinuxAdeExtProperties) linuxProperties).setIsGmtOffsetDefined(true);
                } else {
                    /* How the code is written, this case should never happen. If in the future,
                     * we add another Linux option, then we have to make sure it is added in the 
                     * "allParameterList" AND checked for when we process the option, otherwise
                     * it will trigger this case.
                     */
                    readOptionsSuccessful = false;
                }
            }
        }
        return readOptionsSuccessful;
    }
}
