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
package org.openmainframe.ade.output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.IAnalyzedMessageSummary;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.jaxb.AnalyzedIntervalType;
import org.openmainframe.ade.impl.jaxb.AnalyzedMessageSummaryType;
import org.openmainframe.ade.impl.jaxb.ObjectFactory;
import org.openmainframe.ade.impl.jaxb.ScoreAttributeType;
import org.openmainframe.ade.impl.jaxb.ScoreSetType;
import org.openmainframe.ade.impl.jaxb.ScoreType;
import org.openmainframe.ade.impl.jaxb.TimeLineType;
import org.openmainframe.ade.impl.utils.DateTimeUtils;
import org.openmainframe.ade.scoringApi.IScorer;
import org.openmainframe.ade.scoringApi.StatisticsChart;
import org.openmainframe.ade.summary.SummarizationProperties;
import org.openmainframe.ade.utils.AdeFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Stores the event log analysis results in XML format. The results can be later read and be visualized.
 * All {@link IAnalyzedInterval} objects that are stored should be from the same {@link ISource}.
 */
public class AnalyzedIntervalXmlStorer extends AnalyzedIntervalOutputer {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzedIntervalXmlStorer.class.getName());

	public static final String XML_ANALYZED_INTERVAL_XSD = "conf/xml/AnalyzedInterval.xsd";
    private static final String ADE_JAXB_CONTEXT = "org.openmainframe.ade.impl.jaxb";
    private static final String[] s_xslResources = {
            "AnalyzedInterval.xsl",
            "AdeCorePlex.xsl",
            "global.css"
    };

    protected String[] getXSLResources() {
        return s_xslResources;
    }

    protected static Marshaller s_marshaller;
    protected FramingFlowType m_framingFlowType;
    protected ISource m_source;
    @Property(key = "outputTimeZone", help = "Time zone used to output analysed intervals", required = false)
    private TimeZone m_outputTimeZone = DateTimeUtils.GMT_TIMEZONE;
    protected GregorianCalendar m_gc;    
    protected static DatatypeFactory s_dataTypeFactory = null;

    @Override
    public void setupSourceAndFlowType(ISource source,
            FramingFlowType framingFlowType) throws AdeException {
        m_source = source;
        synchronized (AnalyzedIntervalXmlStorer.class) {
            if (s_dataTypeFactory == null) {
                try {
                    s_dataTypeFactory = DatatypeFactory.newInstance();
                } catch (DatatypeConfigurationException e) {
                    throw new AdeInternalException("Failed to instantiate data factory for calendar", e);
                }
            }
        }
        // FIXME this is not thread safe

        m_gc = new GregorianCalendar(m_outputTimeZone);
        m_framingFlowType = framingFlowType;
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        if (s_marshaller == null) {
            JAXBContext jaxbContext;
            try {
                jaxbContext = JAXBContext.newInstance(ADE_JAXB_CONTEXT);
            } catch (JAXBException e) {
                throw new AdeInternalException("failed to create JAXBContext object for package " + ADE_JAXB_CONTEXT, e);
            }
            try {
                s_marshaller = jaxbContext.createMarshaller();
            } catch (JAXBException e) {
                throw new AdeInternalException("failed to create JAXB Marshaller object", e);
            }
            try {
                s_marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                s_marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
                s_marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION,
                        "xslt/AnalyzedInterval.xsd");

            } catch (PropertyException e) {
                throw new AdeInternalException("failed to set formatted output for JAXB Marshaller object", e);
            }

            final SchemaFactory sf = SchemaFactory.newInstance(
                    javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);

            final File analyzedIntervalSchema = new File(XML_ANALYZED_INTERVAL_XSD);
            Schema schema;
            try {
                schema = sf.newSchema(analyzedIntervalSchema);
            } catch (SAXException e) {
                throw new AdeInternalException("failed to create XML Schemal for event log analysis results", e);
            }
            s_marshaller.setSchema(schema);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void incomingObject(IAnalyzedInterval analyzedInterval) throws AdeException,
            AdeFlowException {
        if (!analyzedInterval.getInterval().getSource().getSourceId().equals(m_source.getSourceId())) {
            throw new AdeFlowException("Cannot process analyzed interval of source other than "
                    + m_source.getSourceId() + ". Got analyzed interval from source "
                    + analyzedInterval.getInterval().getSource().getSourceId());
        }

        createXsltDirectory(analyzedInterval);

        final AnalyzedIntervalType jaxbAnalyzedInterval = new AnalyzedIntervalType();

        jaxbAnalyzedInterval.setModelId(analyzedInterval.getModelInternalId());
        jaxbAnalyzedInterval.setAdeVersion(analyzedInterval.getAdeVersion().toInt());
        jaxbAnalyzedInterval.setSource(m_source.getSourceId());

        m_gc.setTimeInMillis(analyzedInterval.getInterval().getIntervalStartTime());
        jaxbAnalyzedInterval.setStartTime(s_dataTypeFactory.newXMLGregorianCalendar(m_gc));
        jaxbAnalyzedInterval.setStartTimeUnix(analyzedInterval.getInterval().getIntervalStartTime());

        m_gc.setTimeInMillis(analyzedInterval.getInterval().getIntervalEndTime());
        jaxbAnalyzedInterval.setEndTime(s_dataTypeFactory.newXMLGregorianCalendar(m_gc));
        jaxbAnalyzedInterval.setEndTimeUnix(analyzedInterval.getInterval().getIntervalEndTime());

        jaxbAnalyzedInterval.setAnomalyScore(analyzedInterval.getScore());

        final ScoreSetType intervalScoreSet = new ScoreSetType();

        final StatisticsChart intervalStats = analyzedInterval.getStatistics();

        processStatisticsChart(intervalScoreSet, intervalStats);

        jaxbAnalyzedInterval.setScoreSet(intervalScoreSet);

        final List<AnalyzedMessageSummaryType> jaxbMessageSummaries = jaxbAnalyzedInterval.getAnalyzedMessageSummary();
        final Collection<IAnalyzedMessageSummary> sortedMessages = getSortedMessages(analyzedInterval);

        for (IAnalyzedMessageSummary analyzedMessageSummary : sortedMessages) {
            final AnalyzedMessageSummaryType jaxbMessageSummary = processAnalyzedMessageSummary(analyzedMessageSummary);
            jaxbMessageSummaries.add(jaxbMessageSummary);
        }

        writeToXML(analyzedInterval, jaxbAnalyzedInterval, s_marshaller);
    }

    /**
     * Output the content to a XML file.  This method intended for override by subclass 
     * to customize the XML output format.
     * 
     * @param analyzedInterval 
     * @param jaxbAnalyzedInterval
     * @param marshaller 
     * @param source
     * @throws AdeException 
     */
    protected void writeToXML(IAnalyzedInterval analyzedInterval, AnalyzedIntervalType jaxbAnalyzedInterval, Marshaller marshaller) throws AdeException {
        final File outFile = Ade.getAde().getConfigProperties().getOutputFilenameGenerator()
                .getIntervalXmlFile(analyzedInterval,m_framingFlowType);
        if (m_verbose) {
            logger.info("saving xml in " + outFile.getAbsolutePath());
        }
        OutputStreamWriter xmlStreamWriter = null;
        try {
            final File parentdir = outFile.getParentFile();
            parentdir.mkdirs();
            outFile.createNewFile();
            xmlStreamWriter = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8);
            xmlStreamWriter.write("<?xml version='1.0' encoding='UTF-8' ?> \n");
            xmlStreamWriter.write("<?xml-stylesheet href=\"./xslt/AnalyzedInterval.xsl\" type=\"text/xsl\" ?> \n");

            final ObjectFactory factory = new ObjectFactory();
            marshaller.marshal(factory.createAnalyzedInterval(
                    jaxbAnalyzedInterval), xmlStreamWriter);
        } catch (IOException e) {
            throw new AdeInternalException("Failed to create xml file for interval "
                    + outFile.getName() + " of source " + m_source.getSourceId(), e);
        } catch (JAXBException e) {
            throw new AdeInternalException("Failed to write xml file for interval "
                    + outFile.getName() + " of source " + m_source.getSourceId(), e);
        } finally {
            if (xmlStreamWriter != null) {
                try {
                    xmlStreamWriter.close();
                } catch (IOException e) {
                    throw new AdeInternalException("Failed to close file: " + outFile.getName(), e);
                }
            }
        }
    }

    public static Collection<IAnalyzedMessageSummary> getSortedMessages(IAnalyzedInterval interval) {
        final ArrayList<IAnalyzedMessageSummary> sortedMessages = new ArrayList<>();
        sortedMessages.addAll(interval.getAnalyzedMessages());
        Collections.sort(sortedMessages, new AnomalyScoreComparator());
        return sortedMessages;
    }

    /** Comparator used to sort messages by msg-id */
    public static class AnomalyScoreComparator implements Comparator<IAnalyzedMessageSummary> {

        public final int compare(IAnalyzedMessageSummary m1, IAnalyzedMessageSummary m2) {
            try {
                int res = compareByStat(m1, m2, IScorer.ANOMALY);
                if (res != 0) {
                    return res;
                }
                res = compareByStat(m1, m2, IScorer.LOG_PROB);
                if (res != 0) {
                    return res;
                }

                final String id1 = m1.getMessageId();
                final String id2 = m2.getMessageId();
                if (id1 == null) {
                    if (id2 == null) { 
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (id2 == null) {
                    return 1;
                }
                return id1.compareTo(id2);

            } catch (AdeException e) {
                throw new RuntimeException("Error", e);
            }
        }

        private int compareByStat(IAnalyzedMessageSummary m1, IAnalyzedMessageSummary m2, String statName) throws AdeInternalException {
            double s1, s2;
            try {
                s1 = m1.getStatistics().getDoubleStatOrThrow(statName);

            } catch (AdeInternalException e) {
                logger.warn("failed to locate stat name key " + statName + ".  Stat is:");
                logger.warn(m1.toString());
                throw e;
            }
            try {
                s2 = m2.getStatistics().getDoubleStatOrThrow(statName);

            } catch (AdeInternalException e) {
                logger.warn("failed to locate stat name key " + statName + ".  Stat is:");
                logger.warn(m2.toString());
                throw e;
            }

            return Double.compare(s2, s1);
        }

    }

    protected void createXsltDirectory(IAnalyzedInterval analyzedInterval) throws AdeException {
        final File periodDir = Ade.getAde().getConfigProperties().getOutputFilenameGenerator().getIntervalXmlStorageDir(analyzedInterval);
        final File xsltDir = new File(periodDir, "xslt");

        if (!xsltDir.exists()) {
            AdeFileUtils.createDirs(xsltDir);
            final File inputXsltDir = Ade.getAde().getConfigProperties().getXsltDir();
            for (String resource : getXSLResources()) {
                final File xslResourceFile;
                if (inputXsltDir != null) {
                    xslResourceFile = new File(inputXsltDir, resource);
                } else {
                    final URL xslResourceUrl = Ade.class.getResource("/xml/" + resource);
                    try {
                        xslResourceFile = new File(xslResourceUrl.toURI());
                    } catch (URISyntaxException e) {
                        throw new AdeInternalException("could not transform URL to URI: " + xslResourceUrl, e);
                    }
                }
                final File resourceOutputFile = new File(xsltDir, resource);
                AdeFileUtils.copyFile(xslResourceFile, resourceOutputFile);
            }
        }
    }

    /**
     * @param analyzedMessageSummary
     * @return
     * @throws AdeException
     * @throws AdeInternalException
     */
    private AnalyzedMessageSummaryType processAnalyzedMessageSummary(
            IAnalyzedMessageSummary analyzedMessageSummary)
                    throws AdeException, AdeInternalException {
        final AnalyzedMessageSummaryType jaxbMessageSummary = new AnalyzedMessageSummaryType();
        jaxbMessageSummary.setMsgId(analyzedMessageSummary.getMessageId());
        jaxbMessageSummary.setAnomalyScore(analyzedMessageSummary.getFinalAnomaly());
        jaxbMessageSummary.setNumOccurrences(analyzedMessageSummary.getNumberOfAppearances());
        jaxbMessageSummary.setSummarizedText(analyzedMessageSummary.getTextSummary());
        final String cleanedTextSample = cleanTextSample(analyzedMessageSummary.getTextSample());
        jaxbMessageSummary.setTextSample(cleanedTextSample);

        final short[] timeLine = analyzedMessageSummary.getTimeLine();
        final TimeLineType tl = processTimeLine(timeLine);
        jaxbMessageSummary.setTimeLine(tl);

        final StatisticsChart messageSummaryStats = analyzedMessageSummary.getStatistics();

        final ScoreSetType messageSummaryScoreSet = new ScoreSetType();

        processStatisticsChart(messageSummaryScoreSet, messageSummaryStats);

        jaxbMessageSummary.setScoreSet(messageSummaryScoreSet);

        return jaxbMessageSummary;
    }

    private static Pattern s_invalidXMLChars = Pattern.compile("[^\\u0009\\u000a\\u000d\\u0020-\\uD7FF\\uE000-\\uFFFD]");

    protected final String cleanTextSample(String textSample) {
        final Matcher matcher = s_invalidXMLChars.matcher(textSample);
        final String res = matcher.replaceAll("");
        return res;
    }

    /**
     * @param timeLine
     * @return
     */
    private TimeLineType processTimeLine(short[] timeLine) {
        final TimeLineType tl = new TimeLineType();
        final List<Float> timeLineData = tl.getOccurrence();
        for (short occurrence : timeLine) {
            timeLineData.add((float) occurrence / SummarizationProperties.TIMELINE_RESOLUTION);
        }
        return tl;
    }

    /**
     * @param scoreSet
     * @param stats
     */
    private void processStatisticsChart(ScoreSetType scoreSet,
            StatisticsChart stats) {
        final List<ScoreType> scores = scoreSet.getScore();

        final Set<Entry<String, Double>> doubleStatsEntries = stats.getDoubleStats();

        if (doubleStatsEntries != null) {
            for (Entry<String, Double> statEntry : doubleStatsEntries) {
                final Double val = statEntry.getValue();
                final String key = statEntry.getKey();
                final ScoreType score = new ScoreType();
                score.setScore(val);
                score.setScoreName(key);
                scores.add(score);
            }
        }
        final List<ScoreAttributeType> attributes = scoreSet.getAttribute();

        final Set<Entry<String, String>> stringStatsEntries = stats.getStringStats();
        if (stringStatsEntries != null) {
            for (Entry<String, String> statEntry : stringStatsEntries) {
                final String val = statEntry.getValue();
                final String key = statEntry.getKey();
                final ScoreAttributeType attr = new ScoreAttributeType();
                attr.setValue(val);
                attr.setScoreAttributeName(key);
                attributes.add(attr);
            }
        }

    }

    @Override
    public void endOfStream() throws AdeException, AdeFlowException {
        // TODO Auto-generated method stub

    }

}
