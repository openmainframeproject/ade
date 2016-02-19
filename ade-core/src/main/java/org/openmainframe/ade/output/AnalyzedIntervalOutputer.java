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
/**
 * 
 */
package org.openmainframe.ade.output;

import java.util.Map;

import org.openmainframe.ade.data.IAnalyzedInterval;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.flow.IStreamTarget;
import org.openmainframe.ade.impl.PropertyAnnotation;
import org.openmainframe.ade.impl.PropertyAnnotation.MissingPropertyException;
import org.openmainframe.ade.impl.PropertyAnnotation.Property;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;

public abstract class AnalyzedIntervalOutputer implements IStreamTarget<IAnalyzedInterval> {

    /** Arguments supplied by user */
    private Map<String, Object> m_props;

    @Property(key = "verbose", help = "Be verbose, output at least the file names and maybe more.", required = false)
    protected boolean m_verbose = false;

    public final void setArguments(Map<String, Object> props) throws AdeException {
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
            throw new AdeUsageException("When reading the property list for " + this.getClass().getSimpleName() + ": \n" + "Help: \n" + help, e);
        } catch (MissingPropertyException e) {
            final String help = PropertyAnnotation.getHelp(getConfigurableObject());
            throw new AdeUsageException("Help for " + this.getClass().getSimpleName() + ": \n" + help, e);
        }
    }

    /**
     *  if the configurable object is a (static) subclass of the current object, return it here. 
     * @return
     */
    protected final Object getConfigurableObject() {
        return this;
    }

    abstract public void setupSourceAndFlowType(ISource source,
            FramingFlowType framingFlowType) throws AdeException;
}
