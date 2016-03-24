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
package org.openmainframe.ade.ext.os.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IMessageInstance;
import org.openmainframe.ade.data.IMessageTextPreprprocessor;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.TextClusteringComponentModel;
import org.openmainframe.ade.impl.data.IThresholdSetter;
import org.openmainframe.ade.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides text preprocessing on Linux messages. Some of the support includes 
 * replacing certain strings in a log message. For example, identifying the IP address and replacing
 * it with a common string so that same messages with different IP addresses can be easily identified
 * as the same message. A special handling of wrapper messages ie. cron and sudo. 
 * For instance, extracting the component out of the wrapper message. This class also define the "Magic Words".  
 * When comparing two messages, if one message contains a magic word that does not exist in the other message, 
 * these two messages are automatically considered as different. 
 */
public class LinuxMessageTextPreprocessor implements IMessageTextPreprprocessor {
    /**
     * The logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(LinuxMessageTextPreprocessor.class);
    /**
     * Replacement value for IPv4 address.
     */
    public static final String IPV4_REPRESENTATIVE = " %%%IPV4%%% ";
    /**
     * Replacement value for IPv6 address.
     */
    public static final String IPV6_REPRESENTATIVE = " %%%IPV6%%% ";
    /**
     * Replacement value for numbers/hex strings.
     */
    public static final String NUMBER_REPRESENTATIVE = " %%%NUMBER%%% ";
    /**
     * A 2d array "mapping" replacement values (at index replacements[i][1]) for certain regular expressions
     * (at index replacements[i][0]) where i = 0 is an email address with root name, i = 1 is any email address,
     * i = 2 are IPv4 addresses, i = 3 are IPv6 addresses, i = 4 are HEX numbers of minimum length 7,
     * i = 5 are random numbers with minimum length 4, i = 6 and i = 7 are tab replacements.
     */
    private static final String[][] replacements = {
            { "\\b(root)@([a-zA-Z_0-9]+[.])+[a-zA-Z_0-9]*{2,}\\b", " %%%root@EMAIL%%% " },
            { "\\b([a-zA-Z.0-9][a-zA-Z0-9_.+-]*)@([a-zA-Z_0-9]+[.])+[a-zA-Z_0-9]*{2,}\\b", " %%%EMAIL%%% " },
            { "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){5}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b", IPV6_REPRESENTATIVE },
            { "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b", IPV4_REPRESENTATIVE },
            { "\\b[0-9A-F]{7,}\\b", " %%%NUMBER%%% " },
            { "\\b[0-9]{4,}\\b", " %%%NUMBER%%% " },
            { "[ \t]+", " " },
            { "(^[ \t])|([ \t]$)", "" },
    };
    /**
     * Regex for sudo command.
     */
    private static final String SUDO_REGEXP = "^([^:]+):.*COMMAND=(.*)$";
    /**
     * Regex for cron command.
     */
    private static final String CRON_REGEXP = "^([(][^)]+[)])? CMD [(](.*)[)] ?$";
    /**
     * Regex for puppet-agent command.
     */
    private static final String PUPPET_AGENT_REGEXP = "[(]([^)]*)[)].*";
    /**
     * Sudo command pattern object.
     */
    private static final Pattern m_sudoPattern = Pattern.compile(SUDO_REGEXP);
    /**
     * Cron command pattern object.
     */
    private static final Pattern m_cronPattern = Pattern.compile(CRON_REGEXP);
    /**
     * Puppet-agent pattern object.
     */
    private static final Pattern m_puppetAgentPattern = Pattern.compile(PUPPET_AGENT_REGEXP);
    
    /**
     * The default thresholdSetter for sudo and cron commands.
     */
    private IThresholdSetter m_thresholdSetterSudoCron = new TextClusteringComponentModel.SimpleThresholdSetter(4.0);
    /**
     * The default thresholdSetter for puppet agent command.
     */
    private IThresholdSetter m_thresholdSetterPuppet = new TextClusteringComponentModel.SimpleThresholdSetter(3.0, 11);
    /**
     * Regex for matching the cron or sudo component portion of the Linux message.
     */
    private static final String CRON_SUDO_COMPONENT_REGEXP = "^((/usr/sbin/cron|/USR/SBIN/CRON|sudo|SUDO)[^:]+):.*";
    /**
     * Cron or sudo component pattern object.
     */
    private static final Pattern m_cronOrSudoComponentRegexp = Pattern.compile(CRON_SUDO_COMPONENT_REGEXP);
    /**
     * Cron exec regex used for extracting the component from a cron command.
     */
    private static final String CRON_EXECS_REGEXP = " *((([\\|;&]* *)?((exec *|EXEC *|\\[ *-x *|/user/bin/text -x *)?[^ ;|&>]+))((>&| |>|'[^']*'|[^;|&>]+|[0-9]+| )*))"
            + "((([\\|;&]+ *)?((exec *|EXEC *|\\[ *-x *|/user/bin/text -x *)?[^ ;|&>]+))((>&| |>|'[^']*'|[^;|&>]+|[0-9]+| )*))?"
            + "((([\\|;&]+ *)?((exec *|EXEC *|\\[ *-x *|/user/bin/text -x *)?[^ ;|&>]+))((>&| |>|'[^']*'|[^;|&>]+|[0-9]+| )*))?"
            + "((([\\|;&]+ *)?((exec *|EXEC *|\\[ *-x *|/user/bin/text -x *)?[^ ;|&>]+))((>&| |>|'[^']*'|[^;|&>]+|[0-9]+| )*))?"
            + "((([\\|;&]+ *)?((exec *|EXEC *|\\[ *-x *|/user/bin/text -x *)?[^ ;|&>]+))((>&| |>|'[^']*'|[^;|&>]+|[0-9]+| )*))?"
            + "&?";
    
    /**
     * Cron exec pattern object.
     */
    private static final Pattern m_cronEXECSPattern = Pattern.compile(CRON_EXECS_REGEXP);

    /**
     * Called by the Linux parser to replace substrings in the input.
     * @param in String value that contains the message text.
     * @return the processed message text string.
     */
    @Override
    public final String processString(String in) {
        try {
            if (Ade.getAde().getConfigProperties().debug().isDebugMessageIdGeneration() >= 0) {
                logger.trace("Input Message: " + in);
            }
        } catch (AdeException e) {
            logger.error("Error encountered tracing the input message: " + in, e);
        }

        return processStringWithoutTrace(in);
    }

    /**
     * Processes the string without outputting the input message. The string
     * is processed by going through the "replacements" array and effectively
     * replacing all occurrences of specified regular expressions. 
     * @param in The message text string.
     * @return The processed message text string.
     */
    public final String processStringWithoutTrace(String in) {
        String out = in;
        for (int i = 0; i < replacements.length; ++i) {
            out = out.replaceAll(replacements[i][0], replacements[i][1]);
        }
        return out;
    }

    /**
     * Determine if the input token is a magic word.
     * @param in The message text.
     */
    @Override
    public final boolean isMagicWord(String in) {
        return in.matches("^(?i)\\b(("
                + "start"
                + "|end"
                + "|fail"
                + "|exception"
                + "|finish"
                + "|exit"
                + "|begin"
                + "|terminat"
                + ")(ed)?)"
                + "|enable"
                + "|enabled"
                + "|disable"
                + "|disabled"
                + "|error"
                + "|failure"
                + "|stop"
                + "|stopped"
                + "|root"
                + "\\b$");
    }

    /**
     * Helper method for comparing lists of magic words. If the missing word is null or equals root 
     * then it is considered non-paired magic word otherwise, it is considered a paired magic word with 
     * no match.
     * @param missing When comparing two lists of magic words, if a magic word is contained in 
     *     the longer list but not in the shorter list at a specified index it is considered missing.
     */
    @Override
    public final boolean isNonPairedMagicWord(String missing) {
        return (missing == null || missing.equalsIgnoreCase("root"));
    }
    /**
     * Returns true unconditionally. The message text will be considered all one token.
     */
    @Override
    public final boolean treatPathsAsOneToken() {
        return true;
    }

    /**
     * Update the component for special case scenarios. (When the component is sudo, cron, or
     * puppet-agent)
     * @param component The component string value.
     * @param text the message text in syslog.
     * @return A Pair<String, ThresholdSetter> object where the String value is the component
     * and the ThresholdSetter is the threshold value for determining what cluster a string belongs to.
     * (ie. whether the string is "close enough" in distance to one of the clusters in the model)
     */
    @Override
    public final Pair<String, IThresholdSetter> updateComponent(String component, String text) {

        IThresholdSetter thresholdSetter = null;
        if (component.equalsIgnoreCase("sudo")) {
            final Matcher matcher = m_sudoPattern.matcher(text);
            if (matcher.find()) {
                String userName = matcher.group(1);
                if (userName == null) {
                    userName = "";
                }
                final String group = processStringWithoutTrace(matcher.group(2));
                component = (component.trim() + "(" + userName.trim() + "):" + cronCMDSpliter(group)).trim();
                if (component.length() > 120) {
                    component = component.substring(0, 120);
                }
                thresholdSetter = m_thresholdSetterSudoCron;
            }
        } else if (component.equalsIgnoreCase("/usr/sbin/cron")) {
            final Matcher matcher = m_cronPattern.matcher(text);
            if (matcher.find()) {
                String userName = matcher.group(1);
                if (userName == null) {
                    userName = "";
            }
                String group = processStringWithoutTrace(matcher.group(2));
                component = (component + userName + ":" + cronCMDSpliter(group)).trim();
                if (component.length() > 120) {
                    component = component.substring(0, 120);
                }
                thresholdSetter = m_thresholdSetterSudoCron;
            }
        } else if (component.equalsIgnoreCase("puppet-agent")) {
            final Matcher matcher = m_puppetAgentPattern.matcher(text);
            if (matcher.find()) {
                String group = processStringWithoutTrace(matcher.group(1));
                thresholdSetter = m_thresholdSetterPuppet;
                if (group.length() > 100) {
                    group = group.substring(0, 100);
                }
                component = component + ":" + group;
            }
        }
        return new Pair<String, IThresholdSetter>(component, thresholdSetter);
    }

    /**
     * Parses the cron command to extract the component.
     * @param text The cron command.
     * @return The component string value.
     */
    private String cronCMDSpliter(String text) {
        StringBuilder bldcomponent = new StringBuilder("");
        final Matcher matcher = m_cronEXECSPattern.matcher(text);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount() && matcher.group(i) != null; i = i + 7) {
                bldcomponent.append(" " + matcher.group(i + 2).trim());
                bldcomponent.append(" " + matcher.group(i + 3).trim());
            }
        }
        String component = bldcomponent.toString();
        return component.trim();
        
    }

    /**
     * Attempts to extract the wrapper message from the message instance passed in.
     * @param mi A message instance that may be a wrapper.
     * @return A MessageInstance The wrapper message or null if there is no wrapper message.
     * @throws AdeInternalException
     * @throws AdeException
     */
    public final IMessageInstance getExtraMessage(IMessageInstance mi) throws AdeInternalException, AdeException {
        final Matcher matcher = m_cronOrSudoComponentRegexp.matcher(mi.getComponentId());
        if (matcher.find()) {
            final String newMessageId = matcher.group(1).toLowerCase();
            return Ade.getAde().getDataFactory().newMessageInstance(mi.getSourceId(), mi.getDateTime(),
                    newMessageId, mi.getText(), "wrapper", mi.getSeverity());
        }
        return null;
    }
}