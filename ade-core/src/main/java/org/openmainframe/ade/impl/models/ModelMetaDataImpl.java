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
package org.openmainframe.ade.impl.models;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IPeriod;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.data.PeriodUtils;
import org.openmainframe.ade.impl.dataStore.GroupRead;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.openmainframe.ade.models.IModelMetaData;
import org.openmainframe.ade.utils.IStructuredOutputWriter;
import org.openmainframe.ade.utils.patches.Version;

public class ModelMetaDataImpl implements IModelMetaData {

    private static final long serialVersionUID = 1L;
    private Integer m_modelInternalId = null;

    private Date m_startTime;
    private Date m_endTime;
    private final Version a_adeVersion;
    private final Date m_creationDate;
    private final int m_analysisGroupId;
    private final String m_groupName;
    private ArrayList<IPeriod> m_excludedPeriods = new ArrayList<>();
    private File m_externalFileName;
    private final String[] m_commandLineArguments;
    private Date m_requestedStartTime;
    private Date m_requestedEndTime;

    public ModelMetaDataImpl(int analysisGroupId) throws AdeException {
        this(null, analysisGroupId, null, null, Ade.getAde().getVersion(),
                DateTimeUtils.getCurrentDate());
    }

    public ModelMetaDataImpl(Integer modelInternalId, int analysisGroupId,
            Date startTime, Date endTime, Version adeVersion, Date creationDate) throws AdeException {
        m_modelInternalId = modelInternalId;
        m_analysisGroupId = analysisGroupId;
        m_startTime = startTime;
        m_endTime = endTime;
        a_adeVersion = adeVersion;
        m_creationDate = creationDate;
        m_commandLineArguments = Ade.getAde().getCommandLineArguments();
        if (startTime != null) {
            m_requestedStartTime = (Date) startTime.clone();
        } else {
            m_requestedStartTime = null;
        }
        if (endTime != null) {
            m_requestedEndTime = (Date) endTime.clone();
        } else {
            m_requestedEndTime = null;
        }
        m_groupName = GroupRead.getAnalysisGroupName(m_analysisGroupId);

    }

    @Override
    public void setExternalFileName(File externalFileName) {
        m_externalFileName = externalFileName;
    }

    @Override
    public Version getAdeVersion() {
        return a_adeVersion;
    }

    @Override
    public Date getCreationDate() {
        return m_creationDate;
    }

    @Override
    public Integer getModelInternalId() {
        return m_modelInternalId;
    }

    @Override
    public void setModelInternalId(int id) {
        m_modelInternalId = id;
    }

    @Override
    public Date getStartTime() {
        return m_startTime;
    }

    @Override
    public Date getEndTime() {
        return m_endTime;
    }

    public String toString() {
        final StringBuilder res = new StringBuilder();       
        final String analysisGroup = "analysisGroup=" + m_groupName;
        res.append(String.format("Model(%d) %s:[%s-%s]", m_modelInternalId, analysisGroup,
                m_startTime == null ? null : DateTimeUtils.timestampToHumanDateAndTimeUTC(m_startTime.getTime()),
                m_endTime == null ? null : DateTimeUtils.timestampToHumanDateAndTimeUTC(m_endTime.getTime())));
        res.append(String.format(" creationTime:%s, adeVersion:%s ",
                m_creationDate, a_adeVersion));
        res.append("Excluded Periods:");
        for (IPeriod i : m_excludedPeriods) {
            res.append(" " + i.toString());
        }
        return res.toString();
    }

    @Override
    public int getSourceGroupId() {
        return m_analysisGroupId;
    }

    @Override
    public void excludePeriod(IPeriod period) {
        m_excludedPeriods.add(period);
    }

    @Override
    public Collection<IPeriod> getExcludedPeriods() {
        return m_excludedPeriods;
    }

    @Override
    public Collection<IPeriod> getIncludedPeriods() throws AdeException {
        if (m_startTime == null || m_endTime == null) {
            throw new IllegalStateException("either start time or end time not set!");
        }

        final Collection<IPeriod> includedPeriods = new ArrayList<>();

        final Date milliBeforeEndDate = new Date(m_endTime.getTime() - 1L);

        final Date firstPeriodStart = PeriodUtils.getContainingPeriodStart(m_startTime);
        final Date lastPeriodEnd = PeriodUtils.getNextPeriodStart(PeriodUtils.getContainingPeriodStart(milliBeforeEndDate));

        for (ISource source : Ade.getAde().getDataStore().sources().getSourcesForAnalysisGroup(m_analysisGroupId)) {
            final Collection<IPeriod> periods = Ade.getAde().getDataStore().periods().getAllPeriods(source, firstPeriodStart, lastPeriodEnd);
            for (IPeriod period : periods) {
                if (!period.getExcludeFromTraining()) {
                    includedPeriods.add(period);
                }
            }
        }
        return includedPeriods;
    }

    @Override
    public File getModelFileName() {
        return m_externalFileName;
    }

    static public void addTimeSpan(IModelMetaData metaData, Date startDate, Date endDate) {
        if (startDate != null && (metaData.getStartTime() == null || startDate.before(metaData.getStartTime()))) {
            metaData.setStartTime(startDate);
        }
        if (endDate != null && (metaData.getEndTime() == null || endDate.after(metaData.getEndTime()))) {
            metaData.setEndTime(endDate);
        }
    }

    @Override
    public Date getRequestedStartTime() {
        return m_requestedStartTime;
    }

    @Override
    public Date getRequestedEndTime() {
        return m_requestedEndTime;
    }

    @Override
    public String[] getCommandLineArguments() {
        return m_commandLineArguments;
    }
    
    public String getGroupName(){
        return m_groupName;
    }

    public void setStartTime(Date startTime) {
        m_startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        m_endTime = endTime;
    }

    @Override
    public void outputMetadata(IStructuredOutputWriter out) throws AdeException {
        try {
            out.simpleChild("modelInternalId", m_modelInternalId);
            out.simpleChild("m_startTime", m_startTime);
            out.simpleChild("m_endTime", m_endTime);
            out.simpleChild("Version", a_adeVersion.toString());
            out.simpleChild("m_creationDate", m_creationDate);
            out.simpleChild("m_analysisGroupId", m_analysisGroupId);
            if (m_excludedPeriods != null) {
                out.simpleChild("m_excludedPeriods", m_excludedPeriods.toString());
            }
            if (m_externalFileName != null) {
                out.simpleChild("m_externalFileName", m_externalFileName.getCanonicalPath());
            }
            if (m_commandLineArguments != null) {
                out.simpleChild("m_commandLineArguments", Arrays.toString(m_commandLineArguments));
            }
            if (m_requestedStartTime != null) {
                out.simpleChild("m_requestedStartTime", m_requestedStartTime);
            }
            if (m_requestedEndTime != null) {
                out.simpleChild("m_requestedEndTime", m_requestedEndTime);
            }
        } catch (Exception e) {
            throw new AdeInternalException("failed writing meta data of model", e);
        }

    }

}
