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
package org.openmainframe.ade.scoringApi;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.PropertyAnnotation;
import org.openmainframe.ade.impl.PropertyAnnotation.MissingPropertyException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.utils.IStructuredOutputWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A partial implemented of ILearner<T>
 * 
 * Implements basic functionality of 
 * handling of arguments, debug prints and error reports.
 */
public abstract class AbstractTrainer<T> implements ILearner<T> {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AbstractTrainer.class);

    /** Arguments supplied by user */
    private Map<String, Object> m_props;

    /** Name of scorer */
    @Property(key = "scorerName", help = "name for the scorer")
    private String m_name = null;
    /** Environment variables for scorer */
    protected ScorerEnvironment m_scorerEnvironment;

    transient private Map<String, AllowedProperties> m_reqProps = new TreeMap<>();

    @Override
    public String getName() {
        if (m_name == null) {
            return this.getClass().getSimpleName();
        } else {
            return m_name;
        }
    }

    @Override
    public void setName(String name) {
        m_name = name;
    }

    @Override
    public void initTraining(ScorerEnvironment env) throws AdeException {
        m_scorerEnvironment = env;
        reset();
    }

    /** 
     * Reset scorer object to a state equivalent to that of a newly constructed object 
     * useful for allowing reuse of trainers.  Reloads the properties.  Extend to do more.
     */
    protected void reset() throws AdeException {
        setProperties();
    }

    @Override
    public void debugPrint(PrintStream out) throws AdeException {
        out.println("Scorer " + getName());
        out.println("Input arguments: " + Arrays.toString(m_props.entrySet().toArray()));
    }

    @Override
    public void printGeneralUserData(IStructuredOutputWriter out) throws Exception {
        out.simpleChild("arguments", Arrays.toString(m_props.entrySet().toArray()));
    }

    @Override
    public void setArguments(Map<String, Object> props) throws AdeException {
        m_props = props;
        setProperties();
    }

    final protected void setProperties() throws AdeException {
        extractProperties();
        processProperties();
    }

    /**
     * 
     * this is run after the extractProperties, and can be used to setup more then just the raw fields read from the properties.
     */
    protected void processProperties() throws AdeException {
    }

    final private void extractProperties() throws AdeException {
        try {
            final Object configurableObject = getConfigurableObject();

            PropertyAnnotation.setProps(configurableObject, m_props);
        } catch (IllegalArgumentException e) {
            final String help = PropertyAnnotation.getHelp(getConfigurableObject());
            throw new AdeUsageException("When reading the property list for " + getName() + ": \n" + "Help: \n" + help, e);
        } catch (MissingPropertyException e) {
            final String help = PropertyAnnotation.getHelp(getConfigurableObject());
            throw new AdeUsageException("Help for " + getName() + ": \n" + help, e);
        }
    }

    /**
     *  if the configurable object is a (static) subclass of the current object, return it here. 
     * @return
     */
    protected Object getConfigurableObject() {
        return this;
    }

    protected void usageError(String msg) throws AdeUsageException {
        throw new AdeUsageException("Error in using scorer " + getName() + ": " + msg);
    }

    class AllowedProperties {
        public boolean m_required;
        public String m_name;
        public String m_inToName;
        public Integer m_offset;
        public String m_helpString;

        public String help() {
            return "  " + (m_required ? "required" : "optional") + " parameter " + getName() + ": " + m_helpString;
        }

        public AllowedProperties(String name, String inToName, boolean required, String help) {
            this(name, inToName, null, required, help);
        }

        public AllowedProperties(String name, String inToName, Integer offset, boolean required, String help) {
            m_name = name;
            m_inToName = inToName;
            m_offset = offset;
            m_required = required;
            m_helpString = help;
        }

    }


    protected void exitWithPropertiesHelp(String reason) throws AdeException {
        StringBuilder bldUsage = new StringBuilder();
        if (reason != null) {
            bldUsage.append(reason + "\n\n");
        }
        
        bldUsage.append("Help for scorer " + getName() + ":\n");
        for (AllowedProperties rp : m_reqProps.values()) {
            bldUsage.append(rp.help() + "\n");
        }
        logger.error(bldUsage.toString());
        usageError(reason);
    }
    

    protected class TrainerNameFactory implements PropertyAnnotation.IPropertyFactory<String> {

        @SuppressWarnings("unchecked")
        @Override
        public String create(Object propVal) {
            if (!propVal.getClass().isInstance(AbstractTrainer.class)) {
                throw new IllegalArgumentException("expected the value to be of type AbstractTrainer<T>");
            } else {
                return ((AbstractTrainer<T>) propVal).getName();
            }
        }

    }

}
