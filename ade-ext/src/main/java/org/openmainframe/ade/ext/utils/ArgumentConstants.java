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
package org.openmainframe.ade.ext.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.dataStore.IDataStoreSources;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.dataStore.GroupRead;

/** A class for simple utilities & constants regarding argument parsing */
public class ArgumentConstants {

    public static final String DUMP_PARSE_REPORT = "-dump_parse_report";
    public static final String SYSINFO_OS_NAME = "-Dos.name="; // String
    public static final String SYSINFO_GMT_OFFSET = "-Dgmt.offset="; // integer
    public static final int NUM_SYSINFO_ARGS = 2;

    public static final String ALL = "all";
    public static final String STDIN = "stdin";
    public static final String STDOUT = "stdout";

    public enum INPUT_SOURCE {
        LOGFILE, LOGDIR, STDIN
    }

    static public INPUT_SOURCE getInputSourceArgument(String inputSourceName) {
        for (INPUT_SOURCE source : INPUT_SOURCE.values()) {
            if (inputSourceName.equalsIgnoreCase(source.name())) {
                return source;
            }
        }
        return null;
    }

    static public Collection<ISource> getSourcesFromArgument(String sysId) throws AdeException {
        return getSourcesFromArgument(sysId, true);
    }

    static public ISource getSourceFromArgument(String sysId) throws AdeException {
        final Collection<ISource> sources = getSourcesFromArgument(sysId, false);
        if (sources.size() != 1) {
            reportInvalidSysId(String.format("Invalid system id '%s'.", sysId));
        }
        return sources.iterator().next();
    }

    static private Collection<ISource> getSourcesFromArgument(String sysId, boolean allowAll) throws AdeException {
        final IDataStoreSources dataStoreSources = Ade.getAde().getDataStore().sources();

        if (sysId.equalsIgnoreCase(ArgumentConstants.ALL)) {
            if (allowAll) {
                return dataStoreSources.getAllSources();
            }
            reportInvalidSysId("'all' not allowed here. Please specifiy a system id.");
        }

        final ArrayList<ISource> result = new ArrayList<ISource>();
        final ISource res = dataStoreSources.getSource(sysId);
        if (res != null) {
            result.add(res);
            return result;
        }

        reportInvalidSysId(String.format("Invalid system id '%s'.", sysId));
        return null;
    }

    static public Set<ISource> getAnalysisGroupSourcesFromArgument(String analysisGroup) throws AdeException {
        final IDataStoreSources dataStoreSources = Ade.getAde().getDataStore().sources();
        int analysisGroupId = GroupRead.getAnalysisGroupId(analysisGroup);
        return dataStoreSources.getSourcesForAnalysisGroup(analysisGroupId);
    }

    static private void reportInvalidSysId(String msg) throws AdeException {
        final IDataStoreSources dataStoreSources = Ade.getAde().getDataStore().sources();
        final Collection<ISource> sources = dataStoreSources.getAllSources();
        StringBuilder bldids = new StringBuilder("");
        for (ISource source : sources) {
            if (bldids.length() > 0) {
                bldids.append(", ");
            }
            bldids.append(source.getSourceId());
        }
        throw new AdeUsageException(msg + " Valid ids are: " + bldids.toString());
    }

    public static final String DATE_FORMAT_STR = "MM/dd/yyyy";

    static public Date parseDate(String dateStr) throws AdeException {
        try {
            return getDateFormatter().parse(dateStr);
        } catch (ParseException e) {
            throw new AdeUsageException("Invalid date: " + dateStr + ". Expected format is " + DATE_FORMAT_STR);
        }
    }

    public static DateFormat getDateFormatter() throws AdeException {
        final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT_STR);
        formatter.setTimeZone(Ade.getAde().getConfigProperties().getInputTimeZone());
        return formatter;
    }
}
