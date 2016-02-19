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
package org.openmainframe.ade.summary;

import java.util.BitSet;

import javax.xml.bind.DatatypeConverter;

import org.openmainframe.ade.data.IMessageSummary;

/** An object used to specify how a MessageSummary is created */
public class SummarizationProperties {
    
    private static final int BITS_IN_BYTE = 8;

    /** number of bins for keeping the timeline for a specific {@link IMessageSummary} */
    public static final short TIMELINE_RESOLUTION = 120;

    private static final short TIMELINE_BYTES = (TIMELINE_RESOLUTION + 7) / 8;

    public static final int ENCODED_TIMELINE_LENGTH = DatatypeConverter
            .printBase64Binary(new byte[TIMELINE_BYTES]).length();

    public static BitSet bitSetFromByteArray(byte[] bytes) {
        BitSet bits = new BitSet();
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < BITS_IN_BYTE; j++) {
                if ((bytes[i] & (1 << j)) > 0) {
                    bits.set(i * BITS_IN_BYTE + j);
                }
            }
        }
        return bits;
    }

    public static byte[] BitSetToByteArray(BitSet bits) {
        byte[] bytes = new byte[(bits.length() + (BITS_IN_BYTE-1)) / BITS_IN_BYTE];
        int pos = 0;
        while ((pos = bits.nextSetBit(pos)) != -1) {
            bytes[pos / BITS_IN_BYTE] |= 1 << (pos % BITS_IN_BYTE);
            pos++;
        }
        return bytes;
    }

    public static String encodeTimeLine(short[] timeline) {
        if (timeline == null) {
            return "";
        }
        BitSet bits = new BitSet(TIMELINE_RESOLUTION);
        for (short idx : timeline) {
            bits.set(idx);
        }
        byte[] bytes = BitSetToByteArray(bits);
        return DatatypeConverter.printBase64Binary(bytes);
    }

    public static short[] decodeTimeLine(String str) {
        if (str == null) {
            return new short[0];
        }
        byte[] bytes = DatatypeConverter.parseBase64Binary(str);
        BitSet bits = bitSetFromByteArray(bytes);
        short[] timeline = new short[bits.cardinality()];
        int bitPos = 0;
        int i = 0;
        while ((bitPos = bits.nextSetBit(bitPos)) != -1) {
            timeline[i++] = (short) bitPos++;
        }
        return timeline;
    }

    public SummarizationProperties() {
        // do nothing
    }

    public SummarizationProperties(boolean summarizeText, boolean summarizeTimeLine, boolean calculateCriticalWordsScore) {
        m_summarizeText = summarizeText;
        m_summarizeTimeLine = summarizeTimeLine;
        m_calculateCriticalWordsScore = calculateCriticalWordsScore;
    }

    /** Determines whether text is summarized */
    public boolean m_summarizeText = true;

    /** Determines whether timeline is constructed */
    public boolean m_summarizeTimeLine = true;

    /** Determines with the critical words score is computed */
    public boolean m_calculateCriticalWordsScore = true;

    public String toString() {
        return "Summarization properties: summarizeText=" + m_summarizeText
                + " summarizeTimeLine=" + m_summarizeTimeLine + " calculateCriticalWordsScore=" + m_calculateCriticalWordsScore;
    }
}
