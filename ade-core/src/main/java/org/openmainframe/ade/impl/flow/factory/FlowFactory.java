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
package org.openmainframe.ade.impl.flow.factory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.flow.AnalysisGroupToFlowNameMapper;
import org.openmainframe.ade.impl.dataStore.GroupRead;
import org.openmainframe.ade.impl.flow.factory.jaxb.AnalysisGroupFlowType;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.impl.flow.factory.jaxb.LayoutType;
import org.openmainframe.ade.impl.flow.factory.jaxb.OutputerType;
import org.openmainframe.ade.impl.flow.factory.jaxb.ScoringSchemaType;
import org.openmainframe.ade.impl.flow.modules.IntervalFramer;
import org.openmainframe.ade.impl.scoringApi.MainScorerImpl;
import org.openmainframe.ade.scoringApi.IScorer;
import org.openmainframe.ade.scoringApi.IMainScorer;
import org.xml.sax.SAXException;

public class FlowFactory {

    public static final String FLOW_JAXB_PACKAGE = "org.openmainframe.ade.impl.flow.factory.jaxb";
    
    public static final String DEFAULT_FRAMERS_PACKAGE = "org.openmainframe.ade.impl.flow.modules";
    private AnalysisGroupToFlowNameMapper mAnalysisGroupToFlowNameMapper = null;

    private Map<String, FramingFlowType> m_framingFlows;
    private Map<String, FramingFlowType> m_splitFramingFlowTypesMap = new TreeMap<String, FramingFlowType>();;

    private Map<String, FlowTemplateFactory> m_flowTemplateFactories;

    public static String FLOW_LAYOUT_XSD_File_Name = "";

    public FlowFactory() throws AdeException {
        m_flowTemplateFactories = new TreeMap<String, FlowFactory.FlowTemplateFactory>();

        JAXBContext jaxbContext;
        try {
            if (Ade.getAde().getConfigProperties().getUseSparkLogs()){
                FLOW_LAYOUT_XSD_File_Name = File.separator + "FlowLayoutSpark.xsd";
            }
            else{
                FLOW_LAYOUT_XSD_File_Name = File.separator + "FlowLayout.xsd";
            }
        	
            String fileName_Flowlayout_xsd = Ade.getAde().getConfigProperties().getXsltDir() + FLOW_LAYOUT_XSD_File_Name;
            final File flowLayoutXsd = new File(fileName_Flowlayout_xsd);
            final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema;
            schema = sf.newSchema(flowLayoutXsd);
            jaxbContext = JAXBContext.newInstance(FLOW_JAXB_PACKAGE);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            unmarshaller.setSchema(schema);

            @SuppressWarnings("unchecked")
            final JAXBElement<LayoutType> layoutElement = (JAXBElement<LayoutType>) unmarshaller.unmarshal(
                    getFlowLayoutXmlFile());
            final LayoutType layout = layoutElement.getValue();

            m_framingFlows = new TreeMap<String, FramingFlowType>();
            for (FramingFlowType fft : layout.getFramingFlow()) {
                m_framingFlows.put(fft.getName(), fft);
            }
            m_splitFramingFlowTypesMap = new TreeMap<String, FramingFlowType>();
            final List<AnalysisGroupFlowType> flows = layout.getAnalysisGroupFlow();

            for (AnalysisGroupFlowType flow : flows) {
                final FlowTemplateFactory analysisGroupFlowFactory = new FlowTemplateFactory(flow);
                if (flow.getName() == null) {
                    throw new AdeUsageException("Flow missing name");
                }
                m_flowTemplateFactories.put(flow.getName(), analysisGroupFlowFactory);
            }
        } catch (SAXException e) {
            throw new IllegalArgumentException("Flow layout does not match XSD", e);
        } catch (JAXBException e) {
            throw new AdeUsageException("Failed to create flow layout from layout xml file "
                   + Ade.getAde().getConfigProperties().getFlowLayoutFile(), e);
        }
        final Class<? extends AnalysisGroupToFlowNameMapper> clazz = 
                Ade.getAde().getConfigProperties().getAnalysisGroupToFlowNameMapper();
        if (clazz != null) {
            setAnalysisGroupToFlowNameMapper(AnalysisGroupToFlowNameMapperFactory.getNewFlowMapper(clazz));
        }

    }

    private static File getFlowLayoutXmlFile() throws AdeException {
        return new File(Ade.getAde().getConfigProperties().getFlowLayoutFile());
    }

    /** Get the flow by name(input).
     * @param name the name of the Flow you are getting
     * @return analysisGroupFlowFactory returns Flow from the Flow Factory
     * @throws AdeUsageException Incorrect usage of Ade
     */
    public final synchronized FlowTemplateFactory getFlowByName(String name) throws AdeUsageException {

        final FlowTemplateFactory analysisGroupFlowFactory = m_flowTemplateFactories.get(name);

        if (analysisGroupFlowFactory == null) {
            throw new AdeUsageException("Undefined flow for name " + name
                    + ". Make sure it is defined in the layout file, or define a default flow.");
        }
        return analysisGroupFlowFactory;
    }

    public final FlowTemplateFactory getFlowBySourceId(String sourceId) 
            throws AdeUsageException, AdeInternalException, AdeException {
        return getFlowByAnalysisGroup(Ade.getAde().getDataStore().sources().getAnalysisGroup(sourceId));
    }

    public final synchronized FlowTemplateFactory getFlowByAnalysisGroup(String ag) 
            throws AdeUsageException, AdeInternalException {

        if (mAnalysisGroupToFlowNameMapper == null) {
            throw new AdeInternalException("No analysis group to flow name mapper");
        }
        final String name = mAnalysisGroupToFlowNameMapper.getFlowName(ag);
        if (name == null) {
            return null;
        }
        return getFlowByName(name);
    }

    public final IMainScorer getEmptyMainScorer(int analysisGroup) throws AdeException {
        String groupName = GroupRead.getAnalysisGroupName(analysisGroup);
        final FlowTemplateFactory analysisGroupFlowFactory = getFlowByAnalysisGroup(groupName);
        return analysisGroupFlowFactory.getEmptyMainScorer(analysisGroup);
    }

    public final IMainScorer getTrainedMainScorer(int analysisGroup
            , List<IScorer<?, IAnalyzedInterval>> scorers, String flowName
            , String finalMessageScorer, String finalIntScorer) throws AdeException {
        return new MainScorerImpl(analysisGroup, scorers
                , flowName, finalMessageScorer, finalIntScorer);
    }

    public final synchronized void setAnalysisGroupToFlowNameMapper(AnalysisGroupToFlowNameMapper mapper) {
        mAnalysisGroupToFlowNameMapper = mapper;
    }

    public final Map<String, FramingFlowType> getAllFramingFlows() {
        return m_framingFlows;
    }

    public final FramingFlowType getSplitFlow(FramingFlowType in, int splitFactor) {
        return createDerivedFlow(in, "_split_" + splitFactor, in.getDuration() / splitFactor);
    }

    public final FramingFlowType getJoinedFlow(FramingFlowType in, int intervalFactor) {
        return createDerivedFlow(in, "_X" + intervalFactor, in.getDuration() * intervalFactor);
    }

    private FramingFlowType createDerivedFlow(FramingFlowType in, String suffix, long duration) {
        final String name = in.getName() + suffix;
        FramingFlowType joined = m_splitFramingFlowTypesMap.get(name);
        if (joined == null) {
            joined = new FramingFlowType();
            joined.setConsecutive(in.isConsecutive());
            joined.setDatabaseId(-1);
            joined.setDuration(duration);
            joined.setFramerClass(null);
            joined.setName(name);
            m_splitFramingFlowTypesMap.put(name, joined);
        }
        return joined;
    }
    
    public class FlowTemplateFactory {

        private Map<String, ScoringSchemaType> m_scoringSchemas;
        private List<OutputerType> m_outputerSchemas;
        private String m_finalMessageAnomalyScorer;
        private String m_finalIntervalAnomalyScorer;
        private int m_trainFrameFactor = 1;
        private String m_uploadFramer;
        private String m_analysisFramer;
        private String m_flowName;

        public FlowTemplateFactory(AnalysisGroupFlowType flow) throws AdeException {
            m_flowName = flow.getName();
            m_uploadFramer = flow.getUploadFramingFlow();
            m_analysisFramer = flow.getAnalysisFramingFlow();
            m_scoringSchemas = new TreeMap<String, ScoringSchemaType>();

            if (m_uploadFramer == null && m_analysisFramer == null) {
                throw new AdeUsageException("No framers define in the flow \"" 
                            + m_flowName + "\".  Define an upload frameing flow or an analysis framing flow or both.");
            }
            if (m_analysisFramer == null) {
                m_analysisFramer = m_uploadFramer;
            }
            if (m_uploadFramer == null) {
                m_uploadFramer = m_analysisFramer;
            }
            m_finalIntervalAnomalyScorer = flow.getFinalAnomalyIntervalScorer();
            m_finalMessageAnomalyScorer = flow.getFinalAnomalyMessageScorer();

            if (flow.getTrainingIntervalFactor() != null) {
                m_trainFrameFactor = flow.getTrainingIntervalFactor();
            }

            m_outputerSchemas = flow.getOutputer();

            // go over the scorer schemas and for each one map its flow to the schema
            for (ScoringSchemaType scoringSchema : flow.getScoringSchema()) {
                m_scoringSchemas.put(scoringSchema.getId(), scoringSchema);

                // This is still specified in the xml, but is unused.
                final String trainingFlowId = scoringSchema.getTrainingFramingFlow();
                if (trainingFlowId != null && !m_framingFlows.containsKey(trainingFlowId)) {
                    throw new AdeUsageException("Scorer " + scoringSchema.getId() 
                                                   + " requires undefined flow " + trainingFlowId);
                }

            }

        }

        public final Collection<IntervalFramer> getAllIntervalFramers() throws AdeException {
            final Set<String> allIds = new TreeSet<String>();
            allIds.add(m_analysisFramer);
            allIds.add(m_uploadFramer);
            return getIntervalFramers(allIds);
        }

        public final IntervalFramer getAnalysisIntervalFramer() throws AdeException {
            return getIntervalFramer(m_analysisFramer);
        }

        public final IntervalFramer getUploadIntervalFramer() throws AdeException {
            return getIntervalFramer(m_uploadFramer);
        }

        public final String getUploadFramer() {
            return m_uploadFramer;
        }

        private Collection<IntervalFramer> getIntervalFramers(Collection<String> framingFlows) throws AdeException {
            final Collection<IntervalFramer> framers = new ArrayList<IntervalFramer>();

            for (String flowId : framingFlows) {
                framers.add(getIntervalFramer(flowId));
            }
            return framers;
        }

        private IntervalFramer getIntervalFramer(String flowId) throws AdeUsageException,
                AdeInternalException {
            final FramingFlowType framingFlow = m_framingFlows.get(flowId);

            String className = framingFlow.getFramerClass();
            if (!className.contains(".")) {
                className = DEFAULT_FRAMERS_PACKAGE + "." + className;
            }
            try {
                final Class<?> framerClass = (Class<?>) Class.forName(className);

                final Object res = framerClass.getConstructor(FramingFlowType.class).newInstance(framingFlow);

                if (!(res instanceof IntervalFramer)) {
                    throw new AdeUsageException(className 
                            + " defined in the layout file for framing analysis group is not an IntervalFrmaer. ");
                }

                return (IntervalFramer) res;

            } catch (NoSuchMethodException e) {
                throw new AdeUsageException(String.format("The constructor for %s must accept a single %s argument"
                        , className, Properties.class), e);
            } catch (SecurityException|IllegalAccessException e) {
                throw new AdeUsageException(String.format("The constructor for %s must be visible", className), e);
            } catch (InstantiationException e) {
                throw new AdeUsageException(String.format(
                        "The IntervalFramer provided %s is abstract", className), e);
            } catch (IllegalArgumentException e) {
                throw new AdeInternalException(String.format(
                        "Should not have met this exception while trying to instantiate %s", className), e);
            } catch (InvocationTargetException e) {
                throw new AdeUsageException(String.format(
                        "The contstructor for %s threw an exception", className), e);
            } catch (ClassNotFoundException e) {
                throw new AdeUsageException("Unknown class " + className
                        + ". Please make sure the class is in the classpath, or correct your layout file.", e);
            }
        }

        final Integer getTrainFrameFactor() {
            return m_trainFrameFactor;
        }

        public final IMainScorer getEmptyMainScorer(int analysisGroup) throws AdeException {

            final MainScorerImpl scorer = new MainScorerImpl(analysisGroup,
                    m_scoringSchemas, m_framingFlows.get(m_uploadFramer), m_finalMessageAnomalyScorer, m_flowName,
                    m_finalIntervalAnomalyScorer, m_trainFrameFactor);
            getMyFramingFlows();

            return scorer;
        }

        public final Map<String, ScoringSchemaType> getScorerSchemas() {
            return m_scoringSchemas;
        }

        public final List<OutputerType> getOutputers() {
            return m_outputerSchemas;
        }

        public final Map<String, FramingFlowType> getMyFramingFlows() {
            final Map<String, FramingFlowType> res = new TreeMap<String, FramingFlowType>();
            res.put(m_uploadFramer, m_framingFlows.get(m_uploadFramer));
            res.put(m_analysisFramer, m_framingFlows.get(m_analysisFramer));
            return res;
        }
    }

}
