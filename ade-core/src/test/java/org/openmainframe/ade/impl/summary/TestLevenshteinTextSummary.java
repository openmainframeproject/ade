/*
 
    Copyright IBM Corp. 2016
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
package org.openmainframe.ade.impl.summary;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openmainframe.ade.impl.summary.LevenshteinTextSummary;
import org.openmainframe.ade.impl.summary.Word;

public class TestLevenshteinTextSummary {

    private static final String[] expectedWords1 = {"The", "rain", "IS", "in", "Spain", "."};
    private static final String[] expectedWords2 = {"The", "rain", "IS", "in", "Spain", ".", "Yup"};
    
    @Test
    public void testSummarizeStringsLettersForWords() {
        final String str1 = "s i t t i n g";
        final String str2 = "k i t t e n";
        final String expected = "* i t t * n *";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common letters.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsLongSentences() {
        final String str1 = "Once upon a time, there was a boy who came to the open mainframe community looking for a friend!";
        final String str2 = "Once upon a time. was there a girl who came to Google looking for a job:";
        final String expected = "Once upon a time* a * who came to * looking for a *";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsDifferentWordsInMiddle() {
        final String str1 = "I love cocoa";
        final String str2 = "I don't like cocoa";
        final String expected = "I * cocoa";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsDifferentWordAtEnd() {
        final String str1 = "I love cocoa.";
        final String str2 = "I love milk";
        final String expected = "I love *";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsSameSentences() {
        final String str1 = "I love cocoa.";
        final String str2 = "I love cocoa";
        final String expected = "I love cocoa*";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsDifferentWordInMiddle() {
        final String str1 = "I hate cocoa.";
        final String str2 = "I love cocoa";
        final String expected = "I * cocoa*";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
     }

    @Test
    public void testSummarizeStringsDifferentWordsInMiddlePlusPunctuation() {
        final String str1 = "I dont love cocoa.";
        final String str2 = "I love cocoa";
        final String expected = "I * love cocoa*";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsDifferentWordsInMiddlePlusPunctuation2() {
        final String str1 = "I dont like cocoa.";
        final String str2 = "I love cocoa";
        final String expected = "I * cocoa*";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsRepeatedWords() {
        final String str1 = "I I I I dont like cocoa.";
        final String str2 = "I love cocoa";
        final String expected = "* I * cocoa*";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsExtraWordsAtEnd() {
        final String str1 = "I like cocoa A B C";
        final String str2 = "I like cocoa";
        final String expected = "I like cocoa *";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsDifferentPunctuationAtEnd() {
        final String str1 = "I like cocoa.";
        final String str2 = "I like cocoa,";
        final String expected = "I like cocoa*";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsDifferentPunctuationInMiddle() {
        final String str1 = "I like cocoa. A";
        final String str2 = "I like cocoa, A";
        final String expected = "I like cocoa* A";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsEndDifferentLetters() {
        final String str1 = "I like cocoa. a";
        final String str2 = "I like cocoa, b";
        final String expected = "I like cocoa*";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsEndDifferentLetters2() {
        final String str1 = "I like cocoa. a c";
        final String str2 = "I like cocoa, a b";
        final String expected = "I like cocoa* a *";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsFirstWordDifferent() {
        final String str1 = "black cocoa is awesome";
        final String str2 = "brown cocoa is awesome";
        final String expected = "* cocoa is awesome";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        String match2 = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("The summary should contain the common words.", expected, match);
        assertEquals("The summary should be the same if you switch the parameter order.", expected, match2);
    }

    @Test
    public void testSummarizeStringsFirstNull() {
        final String str1 = null;
        final String str2 = "I like turtles";
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        assertEquals("Should be the same as str2.", str2, match);
    }

    @Test
    public void testSummarizeStringsSecondNull() {
        final String str1 = "I like turtles";
        final String str2 = null;
        
        String match = LevenshteinTextSummary.summarizeStrings(str1, str2);
        assertEquals("Should be the same as str1.", str1, match);
    }

    @Test
    public void testSummarizeStringsEmptyStrings() {
        final String str1 = "";
        final String str2 = "";
        final String expectedResult = "";
        
        String match = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("Should be an empty string.", expectedResult, match);
    }

    @Test
    public void testSummarizeStringsFirstEmptyString() {
        final String str1 = "";
        final String str2 = "I like turtles";
        final String expectedResult = "*";
        
        String match = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("Should be an asterisk.", expectedResult, match);
    }

    @Test
    public void testSummarizeStringsSecondEmptyString() {
        final String str1 = "I like turtles";
        final String str2 = "";
        final String expectedResult = "*";
        
        String match = LevenshteinTextSummary.summarizeStrings(str2, str1);
        assertEquals("Should be an asterisk.", expectedResult, match);
    }

    @Test
    public void testCalcDistanceStringString() {
        final String str1 = "hello";
        final String str2 = "hello";
        final int expectedDistance = 0;
        
        int resDis = LevenshteinTextSummary.calcDistance(str1, str2);
        assertEquals("Distance of 0 since inputs are the same.", expectedDistance, resDis);
    }

    @Test
    public void testCalcDistanceStringString2() {
        final String str1 = "hello world";
        final String str2 = "hello world";
        final int expectedDistance = 0;
        
        int resDis = LevenshteinTextSummary.calcDistance(str1, str2);
        assertEquals("Distance of 0 since inputs are the same.", expectedDistance, resDis);
    }

    @Test
    public void testCalcDistanceStringStringExtraWord() {
        final String str1 = "hello foo world";
        final String str2 = "hello world";
        final int expectedDistance = 1;
        
        int resDis = LevenshteinTextSummary.calcDistance(str1, str2);
        assertEquals("Distance of 1 with an extra word in the middle.", expectedDistance, resDis);
    }

    @Test
    public void testCalcDistanceStringStringDifferentWordOrder() {
        final String str1 = "hello foo world";
        final String str2 = "hello world koo";
        final int expectedDistance = 2;
        
        int resDis = LevenshteinTextSummary.calcDistance(str1, str2);
        assertEquals("Distance of 2 since words are in a differnt order.", expectedDistance, resDis);
    }

    @Test
    public void testPrepareStringStringBoolean() {
        final String str1 = "The rain IS in Spain.";
        final Word[] words = LevenshteinTextSummary.prepareStringToken(str1);
        
        assertEquals("The amount of output words should be the same.", expectedWords1.length, words.length);
        for (int i=0; i < expectedWords1.length; i++) {
            assertEquals("Make sure each word matches.", expectedWords1[i], words[i].getStr());
        }
    }

    @Test
    public void testPrepareStringStringBooleanWithCONT() {
        //(CONT.) used to denote connected sentences
        final String str1 = "The rain IS in Spain. (CONT.) Yup";
        final Word[] words = LevenshteinTextSummary.prepareStringToken(str1);
        
        assertEquals("The amount of output words should be the same.", expectedWords2.length, words.length);
        for (int i=0; i < expectedWords2.length; i++) {
            assertEquals("Make sure each word matches.", expectedWords2[i], words[i].getStr());
        }
    }

    @Test
    public void testPrepareStringStringBooleanWithDirectories() {
        final String str1 = "dir1/dir2/dir3";
        final String expected = str1;
        final Word[] words = LevenshteinTextSummary.prepareStringToken(str1);
        
        assertEquals("There should only be one word.", 1,words.length);
        assertEquals("The input should be the same as the expected output.", expected ,words[0].getStr());
   }
}
