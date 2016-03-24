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
package org.openmainframe.ade.ext.output;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.dataStore.IDataStoreModels;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.ext.main.Train;
import org.openmainframe.ade.ext.main.helper.UploadOrAnalyze;
import org.openmainframe.ade.ext.os.parser.InputTimeZoneManager;
import org.openmainframe.ade.ext.xml.v2.types.ExtLimitedModelIndicator;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.openmainframe.ade.models.IModel;
import org.openmainframe.ade.models.IModelMetaData;
import org.openmainframe.ade.scoringApi.IMainScorer;
import org.openmainframe.ade.utils.patches.Version;

/**
 * Retrieve and store metadata needed for XML Generation.
 */

public class XMLMetaDataRetriever {    
    
    /**
     * An interval is 10 minutes
     */
    public static final long INTERVAL_SIZE = 10;
    
    /**
     * The number of intervals (or xml files) in a day
     */
    public static final int INTERVALS_IN_DAY = 144;

    /**
     * Return the XML Hardened Duration In Millis
     * @return
     */
    public static long getXMLHardenedDurationInMillis() {
        return DateTimeUtils.MILLIS_IN_MINUTE * INTERVAL_SIZE;
    }

    /**
     * Objects to deal with Date and Calendar
     */
    protected static DatatypeFactory s_dataTypeFactory = null;
    protected GregorianCalendar m_gc;

    /**
     * Value used to indicate a numeric variable wasn't initialized.
     */
    static final int NOT_AVAILABLE = Integer.MIN_VALUE;

    /**
     * The model internal ID that this class represent
     */
    private Integer m_currentModelInternalId = NOT_AVAILABLE;

    /**
     * The analysis group 
     */
    private String m_analysisGroup;

    /**
     * The Model creation date
     */
    private XMLGregorianCalendar m_modelCreationDate;

    /**
     * Whether the model meta data is stale.
     */
    private boolean m_isMetaDataStale = true;

    /**
     * Number of days used for training.
     */
    private int m_trainingLengthInDays;

    /**
     * The interval length in millis
     */
    private long m_intervalLengthInMillis;

    /**
     * The limited model indicator 
     */
    protected ExtLimitedModelIndicator m_limitedModelIndicator;
    
    protected FramingFlowType m_framingFlowType;

    /**
     * This constant is the smallest version of the MODEL file 
     * that can be read/understand by the current code.  
     */
    final static private Version MODEL_VERSION_SUPPORTED = Version.parse("3.2.1");
    

    /**
     * Constructor
     * @throws AdeException
     */
    public XMLMetaDataRetriever() throws AdeException {
        if (s_dataTypeFactory == null) {
            try {
                s_dataTypeFactory = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException e) {
                throw new AdeInternalException("Failed to instantiate data factory for calendar", e);
            }
        }

        m_gc = new GregorianCalendar(ExtOutputFilenameGenerator.getOutputTimeZone().toTimeZone());
    }

    /**
     * Whether limited model is supported
     * @param modelVersion
     * @return
     */
    public boolean isModelVersionSupported(String modelVersion) {
        Version version = Version.parse(modelVersion);

        /* If the version of the MODEL is newer than the model read-able by current code, then, it's supported.  
         * 
         * Note: How is the  model version determined?  It's the version of Ade Generated the model.  That means
         * even if the model format is the same, it's version could change. 
         *  
         * Let's consider a scenarios: Ade v2 installed (i.e. current code). A v1 model is migrated 
         * to the Ade V2 system.  
         * - During development of Ade V2, if we believe that V2 code can read/understand V1 model, 
         *   then MODEL_VERSION_SUPPORTED should be V1. 
         * - During development of Ade V2, if we believe that V2 code is no longer compatible with V1 model, 
         *   then MODEL_VERSION_SUPPORTED should be V2.
         *   
         * Note: This code could break, if a model newer than the current code is read by the current code.  This 
         * is because the current code doesn't know if the newer model is still compatible.  
         */
        return MODEL_VERSION_SUPPORTED.compareTo(version) <= 0;
    }

    /**
     * Update this class with the Model Meta Data from the Model Internal ID
     * @param modelInternalId
     * @param forcedModelRefresh
     * @throws AdeException 
     */
    public void retrieveXMLMetaData(Integer modelInternalId, boolean forcedModelRefresh, long intervalLengthInMillis) throws AdeException {
        m_intervalLengthInMillis = intervalLengthInMillis;

        /* Only retrieve the model meta data if the modelIntervalId has changed.  
         * ForcedModelRefresh will only make sure the necessary data (e.g. SourceGroup) is retrieved. */
        if (m_currentModelInternalId.equals(modelInternalId) || forcedModelRefresh || m_isMetaDataStale) {
            IDataStoreModels<IMainScorer> modelsApi = Ade.getAde().getDataStore().models();
            List<IModelMetaData> modelMetaDataList = modelsApi.getModelList();
            for (IModelMetaData modelMetaData : modelMetaDataList) {
                if (modelMetaData.getModelInternalId().equals(modelInternalId)) {
                    /* AnalysisGroup name is the only thing that can change for a model that we care about. */
                    m_analysisGroup = modelMetaData.getGroupName();

                    /* Only look refresh these info, if model ID is different.  This is the behavior even if 
                     * it's forced refresh. */
                    if (!m_currentModelInternalId.equals(modelInternalId)) {
                        m_currentModelInternalId = modelInternalId;

                        m_gc.setTimeInMillis(modelMetaData.getCreationDate().getTime());
                        m_modelCreationDate = s_dataTypeFactory.newXMLGregorianCalendar(m_gc);

                        /* Read the model file, only if the model version is supported */
                        String modelVersion = modelMetaData.getAdeVersion().toString();
                        if (isModelVersionSupported(modelVersion)) {
                            /**
                             * Determine the number of days contained in the model.  
                             * 
                             * The modelMetaData retrieved previously is from the Database, and it doesn't have
                             * the input argument to Train.
                             * 
                             * In order to get input argument to Train, we need to load the model.  This model could be
                             * on the filesystem or in the memory.
                             */
                            IModel currentModelForAnalysisGroup = modelsApi.loadModel(modelInternalId);
                            IMainScorer modelMainScorer = (IMainScorer) currentModelForAnalysisGroup;
                            IModelMetaData currentModelMetaData = currentModelForAnalysisGroup.getModelMetaData();

                            String[] commandLineArguments = currentModelMetaData.getCommandLineArguments();
                            Train train = new Train();
                            train.parseAdeExtArgs(commandLineArguments);

                            /* Get the actual date of the model */
                            Date modelStart, modelEnd;
                            if (train.getStartDateTime() != null) {
                                modelStart = train.getStartDateTime().toDate();
                            } else {
                                modelStart = modelMetaData.getStartTime();
                            }

                            if (train.getEndDateTime() != null) {
                                modelEnd = train.getEndDateTime().toDate();
                            } else {
                                modelEnd = modelMetaData.getEndTime();
                            }

                            long trainDurationInMillis = modelEnd.getTime() - modelStart.getTime();
                            m_trainingLengthInDays = (int) (trainDurationInMillis / DateTimeUtils.MILLIS_IN_DAY);

                            /* determine whether the limited model indicator */
                            m_limitedModelIndicator = ExtLimitedModelIndicator.getLimitedModelIndicator(modelMainScorer);

                            /* Indicate that the model data is good for future use */
                            m_isMetaDataStale = false;
                        } else {
                            /* Initialize the value to something recognize-able if the Model cannot be read */
                            m_trainingLengthInDays = NOT_AVAILABLE;
                            m_limitedModelIndicator = ExtLimitedModelIndicator.Unknown;
                        }
                    }

                    /* Found the Model and set the value.  Break out of the loop */
                    break;
                }
            }
        }
    }

    /**
     * Indicate that the ModelMetaData is stale.  And, it must be refreshed when retrieveModelMetaData() is called. 
     */
    public void markDataStale() {
        m_isMetaDataStale = true;
    }

    /**
     * Return the analysisGroupName
     * @return
     */
    public String getAnalysisGroupName() {
        return m_analysisGroup;
    }

    /**
     * Return the Model Creation Date
     * @return
     */
    public XMLGregorianCalendar getModelCreationDate() {
        return m_modelCreationDate;
    }

    /**
     * Return the number of days included in the training
     * @return
     */
    public int getNumberOfDaysInTraining() {
        return m_trainingLengthInDays;
    }

    /**
     * Return the log type
     * @return
     */
    public String getLogType() {
        return LogType.getSupportedLogType(UploadOrAnalyze.getAdeOSType());
    }

    /**
     * Return the constant we used to represent UNIX Syslog
     * @return
     */
    public String getLogTypeUnixSyslog() {
        return LogType.LOG_TYPE_UNIX_STYLE_SYSLOG.toString();
    }

    /**
     * get the interval length
     */
    public int getIntervalLengthInSeconds() {
        return (int) (m_intervalLengthInMillis / DateTimeUtils.MILLIS_IN_SECOND);
    }

    /**
     * Return the number of intervals
     */
    public int getNumberOfIntervalsInADay() {
        return (int) (DateTimeUtils.MILLIS_IN_DAY / m_intervalLengthInMillis);
    }

    /**
     * Return the ade.inputTimeZone property which was established for (prior to) 
     * the invocation of the particular main (Analyze, Upload, ... ) which is ultimately
     * driving us.       
     *  
     * This string should be of the format:  GMT+XX:YY  or  GMT-XX:YY
     * for hours XX and minutes YY. 
     * 
     * Any value recorded in the analytics database should match this
     * value, and so we'd gain nothing (and incur a non-trivial cost) by obtaining
     * it from there. 
     * 
     * @return - String as described
     * @throws AdeException 
     */
    public String getGMTOffset(String sourceId) throws AdeException {
        return InputTimeZoneManager.getTimezone(sourceId);
    }

    /**
     * Return the limited model indicator of this model
     * @return
     */
    public ExtLimitedModelIndicator getLimitedModelIndicator() {
        return m_limitedModelIndicator;
    }
}
