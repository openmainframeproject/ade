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
package org.openmainframe.ade.impl.flow.modules;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.data.IIntervalClassification;
import org.openmainframe.ade.data.ISource;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.flow.FlowUtils;
import org.openmainframe.ade.impl.actions.Action;
import org.openmainframe.ade.impl.flow.factory.jaxb.FramingFlowType;
import org.openmainframe.ade.summary.SummarizationProperties;

public class ContinuousTimeFramer extends ConsecutiveTimeFramer {

    private short m_tempSplitFactor;
    private FramingFlowType m_outerFramingFlow;
    public static final String TEMP_SPLIT_FACTOR = "Temporary_Split_Factor";
    public static final String PERM_SPLIT_FACTOR = "Permanent_Split_Factor";
    private static final int MILLISECONDS_IN_ONE_MINUTE = 60000;
    private static final int MILLISECONDS_IN_TEN_MINUTES = 600000;
    private static final int MILLISECONDS_IN_TWO_MINUTES = 120000;

    /**
     * Construct a new ContinuousTimeFramer.
     * 
     * @param framingFlow Contains the properties for the interval time frame.
     */
    public ContinuousTimeFramer(FramingFlowType framingFlow)
            throws AdeException {
        super();
        m_outerFramingFlow = framingFlow;
        setPropsFromFramingFlowType(m_outerFramingFlow);
        processSplitFactors();
    }
    
    /**
     * Process the split factors. The temporary split factor will be set as the framing flow. The permanent
     * split factor will be a property type of the framing flow. Validation is done on both split factors
     * before setting them.
     * @throws AdeException
     */
    private final void processSplitFactors() throws AdeException {
        final String tempSplitFactor = getProp(TEMP_SPLIT_FACTOR);
        final String permSplitFactor = getProp(PERM_SPLIT_FACTOR);
        final long duration = m_outerFramingFlow.getDuration();
        short permSplitFactorVal;
        short tempSplitFactorVal;
        if (permSplitFactor != null){
            permSplitFactorVal = validatePermSplitFactor(permSplitFactor);
        } else {
            permSplitFactorVal = (short) (duration / MILLISECONDS_IN_TEN_MINUTES);
        }
        if (tempSplitFactor != null){
            short permDurationInMinutes = (short) ((duration / permSplitFactorVal) / MILLISECONDS_IN_ONE_MINUTE);
            tempSplitFactorVal = validateTempSplitFactor(tempSplitFactor,permDurationInMinutes,permSplitFactorVal);
        } else {
            tempSplitFactorVal = (short) (duration / MILLISECONDS_IN_TWO_MINUTES);
        }      
        setTempSplitFactor(tempSplitFactorVal);
    }       
    
    /**
     * Validation for the permanent split factor. The split factor must be a positive short number 
     * and must be less than or equal to the number of minutes in the analysis flow duration.
     * @param splitFactor the String value of the split factor.
     * @return permSplitFactor the short value of the permanent split factor.
     * @throws AdeFlowException
     */
    private final short validatePermSplitFactor(String splitFactor) throws AdeFlowException{
        short permSplitFactor = Short.valueOf(splitFactor);
        if (permSplitFactor <= 0 || 
                ((m_outerFramingFlow.getDuration() /permSplitFactor) < MILLISECONDS_IN_ONE_MINUTE)){
            throw new AdeFlowException("Permanent Split Factor cannot be negative or 0.");
        }
        return permSplitFactor;
    }
    
    /**
     * Validation for the temporary split factor. The temporary split factor must be a 
     * positive short number and must be a factor of the duration of the output. 
     * @param splitFactor The temporary split factor parsed out of the flow layout.
     * @param permDurationInMinutes The duration of data output in a permanent XML file.
     * @param permSplitFactor the permanent split factor obtained from the flow layout.
     * @return the temporary split factor. Note: The temporary split factor fed into the flow
     * layout takes into account ONLY the permanent split factor duration. ie. the time in 
     * milliseconds between two consecutive permanent XML files. We need to multiply 
     * the temporary split factor by the permanent split factor to get the temporary
     * split factor for the ENTIRE duration.
     * @throws AdeFlowException
     */
    private final short validateTempSplitFactor(String splitFactor, short permDurationInMinutes, 
            short permSplitFactor) throws AdeFlowException{
        short tempSplitFactor = Short.valueOf(splitFactor);
        if (tempSplitFactor <= 0 || (permDurationInMinutes % tempSplitFactor != 0)){
            throw new AdeFlowException("Temporary Split Factor cannot be negative or 0 and MUST be a factor of the "
                    + "Permanent Split Duration : " + permDurationInMinutes);
        }
        return (short)(tempSplitFactor * permSplitFactor);
    }

    /**
     * Get the temporary split factor for the time frames.
     * @return the split factor for the time frames.
     */
    public final short getTempSplitFactor() {
        return m_tempSplitFactor;
    }

    /**
     * Set the temporary split factor for the time frames. 
     * @param the split factor
     */ 
    public final void setTempSplitFactor(short tempSplitFactor) throws AdeException {
        m_tempSplitFactor = tempSplitFactor;
        setFramingFlow(Ade.getAde().getFlowFactory().getSplitFlow(m_outerFramingFlow, m_tempSplitFactor));  
    }

    @Override
    protected final IntervalSeparator generateIntervalSeparator() {
        if (isIntervalTemporary()) {
            return new TemporaryIntervalSeparator(m_currentTimeFrameStartTime,
                    alignToOuterTimeFrame(m_currentTimeFrameStartTime));
        } else {
            return new IntervalSeparator(m_currentTimeFrameStartTime);
        }

    }

    private long alignToOuterTimeFrame(Long time) {
        return m_alignmentOffset + m_outerFramingFlow.getDuration()
                * ((time - m_alignmentOffset) / m_outerFramingFlow.getDuration());
    }

    /**
     * @param firstMessageTime - the time (in milliseconds) of the first message in a file
     * @throws AdeException - see {@link FlowUtils#sendSeparator(Object, java.util.Collection)}
     * @throws AdeFlowException - see {@link FlowUtils#sendSeparator(Object, java.util.Collection)}
     */
    @Override
    protected final void setFirstMessageTimeAndSendSeparator(long firstMessageTime) throws AdeException {
        // set the beginning of the current time frame to the time of the first message aligned
        // with respect to the time framer's duration 
        m_currentTimeFrameStartTime = alignToOuterTimeFrame(firstMessageTime);
        // send the separator to mark a new time frame.
        sendSeparator(generateIntervalSeparator());
    }

    private boolean isIntervalTemporary() {
        return this.alignToOuterTimeFrame(m_currentTimeFrameStartTime) != m_currentTimeFrameStartTime;
    }

    @Override
    public final IntervalBuilder getIntervalBuilder(
            ISource source, SummarizationProperties sumProps,
            FramingFlowType framingFlowType,
            IIntervalClassification intervalClassicication, Action action)
                    throws AdeException {
        return new AccumulativeIntervalBuilder(source, sumProps, framingFlowType, intervalClassicication,
                action, (int) getTempSplitFactor());
    }

    @Override
    public final FramingFlowType getFramingFlowType() {
        return m_outerFramingFlow;
    }

}
