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
package org.openmainframe.ade.impl.summary;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OVERVIEW
 * 
 * <p>A particular event-class is mapped to many messages. Each message can be
 * thought of as a different "sentence". We would like to provide the user with
 * a summary of these different sentences. This module attempts to merge, or
 * unify, the sentences together.
 * 
 * <p>The algorithms are based on the Needleman-Wunsch algorithm. See:
 * http://en.wikipedia.org/wiki/Needleman-Wunsch_algorithm
 * 
 * <p>Keywords for a web-search: global alignment, Needleman-Wunsch algorithm,
 * Levinstein distance.
 * 
 * 
 * <p>LOW LEVEL DESIGN
 * 
 * <p>Step 1: make a pass on the string messages and convert them into vectors of
 * words and tokenize the sentences getting rid of punctuation marks. (remember
 * the punctuation marks to return them to the correct place in the end e.g.
 * string, and string. should be replaced with string*, while string . and
 * string , should be replaced with string *)
 * 
 * <p>Step 2: run the Needleman-Wunsch algorithm on the two sentences and create a
 * unified string.
 * 
 */
public final class LevenshteinTextSummary {

    // Maximal number of words that are represented in an LG_message
    // structure. The rest of the words in the sentence are discarded.
    private static final int MAX_NUM_WORDS = 120;
    private static final int DEFAULT_THRESHOLD = 120;

    private static String wordSplitString = ":|,|&|=|!|>|<|/|\\.|\\\\|\\(|\\)|\\{|\\}|\\[|\\]";
    private static Pattern wordPatternSplitter = Pattern.compile(wordSplitString);

    /* For directory, we requires it to have at least 2 slashes */
    private static String directorySplitString = "([\\w\\.\\-]*/)+([\\w\\.\\-]+/)+([\\w\\.\\-]+)?";
    private static Pattern directoryPatternSplitter = Pattern.compile(directorySplitString);

    // \p{Punct} Punctuation: One of !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~
    private static String sentenceSplitString = " \t\n";
    // The asterisk ('*') is used to denote the wild-card symbol
    private static final String ASTERISK = "*";

    // Matrix used for dynamic programming
    private static int[][] algMat;
    
    private static int threshold = DEFAULT_THRESHOLD;

    private LevenshteinTextSummary() {
        //Private default constructor
    }
    public static int getThreshold() {
        return threshold;
    }

    public static void setThreshold(int threshold) {
        LevenshteinTextSummary.threshold = threshold;
    }

    /* Initialize once */
    private static synchronized void initMatrix() {
        if (algMat == null) {
            algMat = new int[MAX_NUM_WORDS + 1][MAX_NUM_WORDS + 1];

        }
    }

    /**
     * Split the string into an array of words separated by whitespace or specific delimiters.
     * 
     * @param str the string to split
     * @return an array of words corresponding to the original string
     */
    public static Word[] prepareString(String str) {
        if (str == null) {
            return new Word[0];
        }

        // 1) Split the sentence according to whitespaces
        final StringTokenizer tokenizer = new StringTokenizer(str, sentenceSplitString);

        // 2) Further split each of the resulting words according to delimiters.
        // For example, convert the word 'hello.world' into
        // 'hello' '.' 'world'

        return splitWordByDelimiters(tokenizer);

     }

    /**
     * Split the string into an array of words separated by whitespace or specific delimiters. The input
     * string should be all one token.
     * 
     * @param str the string to split
     * @return an array of words corresponding to the original string
     */
    public static Word[] prepareStringToken(String str) {
        if (str == null) {
            return new Word[0];
        }

        final ArrayList<Word> strWithDelim = new ArrayList<Word>();

        splitWordBySpace(str, strWithDelim);

        final Word[] toRet = new Word[Math.min(strWithDelim.size(), MAX_NUM_WORDS)];
        for (int i = 0; i < Math.min(strWithDelim.size(), MAX_NUM_WORDS); i++) {
            toRet[i] = strWithDelim.get(i);
        }
        return toRet;
    }

    private static void splitWordBySpace(String str, ArrayList<Word> strWithDelim) {
        final StringTokenizer tokenizer = new StringTokenizer(str, sentenceSplitString);

        String currWord;
        while (tokenizer.hasMoreTokens()) {
            currWord = tokenizer.nextToken();
            // Ignore the word 'CONT', it is used to connect sentences.
            if ("(CONT.)".equals(currWord)) {
                continue;
            }

            splitWordByDirectory(currWord, strWithDelim);
        }
    }

    private static void splitWordByDirectory(String str, ArrayList<Word> strWithDelim) {
        boolean isFirstToken = true;
        boolean isFirstWord = true;
        final Matcher dirMatcher = directoryPatternSplitter.matcher(str);
        final String currWord = str;
        String suffix = "";
        boolean isFindMatch = false;
        int lastStartIndex = 0;
        while (dirMatcher.find()) {
            isFindMatch = true;

            /* handle the prefix before the directory */
            final String currStr = currWord.substring(lastStartIndex, dirMatcher.start());
            if (currStr.length() > 0) {
                isFirstToken = true;
                splitWordByDelimiter(currStr, isFirstToken, isFirstWord, strWithDelim);
                isFirstToken = false;
                isFirstWord = false;
            } else {
                isFirstToken = true;
            }

            /* adding the directory */
            addWord(isFirstToken && (!isFirstWord), dirMatcher.group(), strWithDelim);
            isFirstWord = false;

            /* adding the suffix after the delimiter */
            suffix = currWord.substring(dirMatcher.end());
            lastStartIndex = dirMatcher.end();
        }

        if (isFindMatch) {
            // adding the last subString after the last delimiter
            if (suffix.length() > 0) {
                isFirstToken = false;
                splitWordByDelimiter(suffix, isFirstToken, isFirstWord, strWithDelim);
            }
        } else {
         // in case no delimiter was found
            isFirstToken = true;
            splitWordByDelimiter(currWord, isFirstToken, isFirstWord, strWithDelim);
        }

    }

    private static void splitWordByDelimiter(String currWord, boolean isFirstToken, boolean isFirstWord,
            ArrayList<Word> strWithDelim) {
        /* The first section of the word doesn't have a space included - used when
         * printing the sentence
         * When printing the sentence the delimiters are substitute by spaces 
         */
        boolean isFindMatch = false;
        boolean tempIsFirstToken = isFirstToken;
        boolean tempIsFirstWord = isFirstWord;
        final Matcher delimtMatcher = wordPatternSplitter.matcher(currWord);
        String suffix = "";
        int lastStartIndex = 0;
        while (delimtMatcher.find()) {
            isFindMatch = true;

            /* handle the prefix before the delimiter */
            final String currStr = currWord.substring(lastStartIndex, delimtMatcher.end() - 1);
            if (currStr.length() > 0) {
                addWord(tempIsFirstToken && (!tempIsFirstWord), currStr, strWithDelim);
                tempIsFirstWord = false;
                tempIsFirstToken = false;
            }

            /* adding the delimiter */
            addWord(tempIsFirstToken && (!tempIsFirstWord), delimtMatcher.group(), strWithDelim);

            /* adding the suffix after the delimiter */
            suffix = currWord.substring(delimtMatcher.end());
            lastStartIndex = delimtMatcher.end();
            tempIsFirstToken = false;
        }
        if (isFindMatch) {
            // adding the last subString after the last delimiter
            if (suffix.length() > 0) {
                addWord(tempIsFirstToken && (!tempIsFirstWord), suffix, strWithDelim);
            }
        } else {
            // in case no delimiter was found
            addWord(tempIsFirstToken && (!tempIsFirstWord), currWord, strWithDelim);
            tempIsFirstWord = false;
        }
    }

    /**
     * Merge sentence first with second and return their summarization. The
     * Needleman-Wunsch algorithm is used here. See
     * http://en.wikipedia.org/wiki/Needleman-Wunsch_algorithm
     * 
     * @param first the first string
     * @param second the second string
     * @return a summary of the two strings by the Levinshtein algorithm
     */
    public static String summarizeStrings(String first, String second) {
        if (first == null && second == null) {
            return "";
        }
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        if ("".equals(first.trim()) && "".equals(second.trim())) {
            return "";
        }
        if ("".equals(first.trim())) {
            return "*";
        }
        if ("".equals(second.trim())) {
            return "*";
        }

        final Word[] one = prepareString(first);
        final ArrayList<Word> summary = summarizeStrings(one, second);
        final StringBuilder printBuffer = new StringBuilder();
        for (int i = 0; i < summary.size(); i++) {
            final Word currWord = summary.get(i);
            if (currWord.ws) {
                printBuffer.append(' ');
            }
            // Convert each of the integers into a string by consulting
            // the hash-table
            printBuffer.append(currWord.getStr());
        }
        return printBuffer.toString();
    }

    /*
     * Merge sentence reference with new string and return their summarization.
     * 
     * The Needleman-Wunsch algorithm is used here. See
     * http://en.wikipedia.org/wiki/Needleman-Wunsch_algorithm
     * 
     * @param reference
     *          string to compare with
     * @param newStr
     *          string to add to the reference string
     * @return a summary of the two strings by the Levinshtein algorithm.
     */
    private static ArrayList<Word> summarizeStrings(Word[] reference, String newStr) {

        if (reference == null || newStr == null) {
            return new ArrayList<Word>();
        }

        final Word[] newStrVec = prepareString(newStr);

        assert newStrVec.length <= MAX_NUM_WORDS;
        final int lenA = reference.length + 1;
        final int lenB = newStrVec.length + 1;
        assert lenB <= MAX_NUM_WORDS + 1;

        if (Math.abs(lenA - lenB) > threshold) {
            final ArrayList<Word> toRet = new ArrayList<Word>();
            toRet.add(new Word(ASTERISK, false));
            return toRet;
        }
        // 1. initMatrix with zeros
        initMatrix();

        // 2. Calculate the matrix body.
        if (!calcScoreMat(lenA, lenB, reference, newStrVec)) {
            final ArrayList<Word> toRet = new ArrayList<Word>();
            toRet.add(new Word(ASTERISK, false));
            return toRet;
        }

        // 3. Creating the merge alignment using backtracking
        final ArrayList<Word> merge = createAlignment(lenA, lenB, reference, newStrVec);

        // 4. reversing the alignment
        return reverseAlignment(merge);
    }

    /**
     * Calculate the distance between two strings.
     * 
     * @param strA the first string
     * @param strB the second string
     * @return the calculated distance
     */
    public static int calcDistance(String strA, String strB) {

        final Word[] wordsA = prepareString(strA);
        final Word[] wordsB = prepareString(strB);
        return calcDistance(wordsA, wordsB);
    }

    /**
     * Calculate the distance between two arrays of words.
     * 
     * @param wordsA the first word array
     * @param wordsB the second word array
     * @return the calculated distance
     */
    public static int calcDistance(Word[] wordsA, Word[] wordsB) {
        final int lenA = wordsA.length + 1;
        final int lenB = wordsB.length + 1;
        assert lenA <= MAX_NUM_WORDS + 1;
        assert lenB <= MAX_NUM_WORDS + 1;

        if (Math.abs(lenA - lenB) > threshold) {
            return threshold + 1;
        }
        // 1. initMatrix with zeros
        initMatrix();

        // 2. Calculate the matrix body.
        if (!calcScoreMat(lenA, lenB, wordsA, wordsB)) {
            return threshold + 1;
        }
        return algMat[lenA - 1][lenB - 1];
    }

    /*
     * Calculates the score Mat of the needleman algorithm, over a part of the 
     * linear programming matrix. in each row, the next row is filled between minj and maxj
     * indices, assuming beyond them that score is infinity (i.e. the length of the 
     * compared sentence +1 ). The range between minj and maxj is determined by the m_threshold parameter.
     * 
     * @param lenA length of first sentence
     * @param lenB length of second sentence
     * @param one first sentence split into words
     * @param two second sentence split into words
     * @return true if the sentences are mostly similar, and false otherwise 
     * (i.e. result of summarization is a single *).
     */
    private static boolean calcScoreMat(int lenA, int lenB, Word[] one, Word[] two) {
        // Boundary condition
        int minj = 0;
        int maxj = Math.min(threshold, lenB - 1);
        int j;

        for (j = minj; j <= maxj; ++j) {
            algMat[0][j] = j;
        }

        for (int i = 1; i < lenA; ++i) {
            // Calculate main mF
            int valueNorthWest = threshold + 1; // Infinity 
            int valueWest = threshold; // Infinity
            minj = 0;

            //each table slot is min(north+1, west+1, northwest+mismatch)
            //where mismatch=1 if the strings at that position don't match, 0 if they do.
            //in each iteration over the columns, each place is updated based on the previously 
            //computed places
            for (j = minj; j <= maxj; ++j) {
                final int north = algMat[i - 1][j];
                final int currentVal = Math.min(Math.min(valueWest, north) + 1, valueNorthWest);
                algMat[i][j] = currentVal;
                valueWest = currentVal;
                //update northwest value for the next calculation
                //not defined beyond the size of the matrix
                if (j < lenB - 1) {
                    valueNorthWest = north
                            + (matchCost(one[i - 1].getStr(), two[j].getStr()));
                }
            }

            // Update maxj
            if (maxj < lenB - 1) {
                ++maxj;
                algMat[i][maxj] = Math.min(valueWest + 1, valueNorthWest);
            }
            while (algMat[i][minj] > threshold) {
                ++minj;
                if (minj > maxj) {
                    return false;
                }
            }
            while (algMat[i][maxj] > threshold) {
                --maxj;
                if (maxj < minj) {
                    return false;
                }
            }
        }
        return maxj == lenB - 1;

    }

    /*
     * This method uses the score matrix to find the alignment between the two
     * texts. It uses a backtrack algorithm. The starting point is
     * algMat[lenA-1][lenB-1] and backtrack until reaching algMat[0][0]
     * 
     * @param lenA the length of the first sentence
     * @param lenB the length of the second sentence
     * @param one the first sentence split into words
     * @param two the second sentence split into words
     * @return [merge] will contain, at the end of the backtrack sequence,the
     *         merge of the two sequences.
     */
    private static ArrayList<Word> createAlignment(int lenA, int lenB,
            Word[] one, Word[] two) {

        final ArrayList<Word> merge = new ArrayList<Word>();
        int i = lenA - 1;
        int j = lenB - 1;
        while (i > 0 && j > 0) {
            // Calculate the lowest cost out of the neighboring cells
            // cost if we moved diagonally
            final int costDiag = algMat[i - 1][j - 1]
                    + matchCost(one[i - 1].getStr(), two[j - 1].getStr());

            // Cost if we moved vertically. Skip a character in A.
            final int costVert = algMat[i - 1][j] + 1;

            // Cost if we moved horizontally. Skip a character in compared.
            final int costHorz = algMat[i][j - 1] + 1;

            // Choose the optimal backtrack
            if (algMat[i][j] == costDiag) {
                // We got here from algMat[i-1][j-1]
                wordMerge(one[i - 1], two[j - 1], merge);
                i--;
                j--;
            } else if (algMat[i][j] == costVert) {
                // we got here from algMat[i-1][j]
                final Word wd = new Word(ASTERISK, one[i - 1].ws);
                merge.add(wd);
                i--;
            } else {
                // we got here from mF[i][j-1]
                assert algMat[i][j] == costHorz;
                final Word wd = new Word(ASTERISK, two[j - 1].ws);
                merge.add(wd);
                j--;
            }
        }

        if (i > 0 || j > 0) {
            merge.add(new Word(ASTERISK, false));
        }

        return merge;
    }

    /*
     * Reverse the order of the merge array and remove sequences of wild-card (*)
     * symbols. 
     * 
     * @param merge
     *          - original alignment before reversing
     */
    private static ArrayList<Word> reverseAlignment(List<Word> merge) {
        final ArrayList<Word> toRet = new ArrayList<Word>();

        if (merge.isEmpty()) {
            return new ArrayList<Word>();
        }

        // Last symbol, insert as is.
        Word prevWord = merge.get(merge.size() - 1);
        toRet.add(prevWord);
        // The last word has already been processed and so start loop at size-2 
        for (int k = merge.size() - 2; k >= 0; k--) {
            final Word currWord = merge.get(k);
            // Looks for a sequence of "*" and add to the new List if not
            if (!(ASTERISK.equals(prevWord.getStr()) && ASTERISK.equals(currWord.getStr()))) {
                prevWord = currWord;
                toRet.add(currWord);
            }
        }

        if (toRet.size() > MAX_NUM_WORDS) {
            // It is possible for the resulting merged message to be long, chop it
            // off.
            for (int k = MAX_NUM_WORDS + 1; k < toRet.size(); k++) {
                toRet.remove(k);
            }

        }
        return toRet;
    }

    private static int matchCost(String a, String b) {
        if (a.equals(b)) {
            return 0;
        } else {
            return 1;
        }
    }

    // Merge token [a] with [b] and deposit the result at the end of [wordVec]
    private static void wordMerge(Word a, Word b, ArrayList<Word> wordVec) {
        final boolean wordWS = a.ws || b.ws;
        String wordWD;
        if (a.getStr().equals(b.getStr())) {
            wordWD = a.getStr();
        } else {
            wordWD = ASTERISK;
        }
        final Word word = new Word(wordWD, wordWS);
        wordVec.add(word);
    }

    /*
     * This method is responsible for second phase splitting of each word in the
     * sentence
     * 
     * @param splitVecWs
     *          - the vector from the previous splitting phase
     */
    private static Word[] splitWordByDelimiters(StringTokenizer tokenizer) {
        String currWord;
        final ArrayList<Word> strWithDelim = new ArrayList<Word>();
        boolean isFirstWord = true;
        while (tokenizer.hasMoreTokens()) {
            currWord = tokenizer.nextToken();
            // Ignore the word 'CONT', it is used to connect sentences.
            if ("(CONT.)".equals(currWord)) {
                continue;
            }

            // The first section of the word doesn't have a space included - used when
            // printing the sentence
            // When printing the sentence the delimiters are substitute by spaces
            boolean isFirstToken = true;
            boolean isFindMatch = false;
            final Matcher delimtMatcher = wordPatternSplitter.matcher(currWord);
            String suffix = "";
            int lastStartIndex = 0;
            while (delimtMatcher.find()) {
                isFindMatch = true;
                // handle the prefix before the delimiter
                final String currStr = currWord.substring(lastStartIndex,
                        delimtMatcher.end() - 1);
                if (currStr.length() > 0) {
                    addWord(isFirstToken && (!isFirstWord), currStr, strWithDelim);
                    isFirstWord = false;
                    isFirstToken = false;
                }
                // adding the delimiter
                addWord(isFirstToken && (!isFirstWord), delimtMatcher.group(),
                        strWithDelim);
                // adding the suffix after the delimiter
                suffix = currWord.substring(delimtMatcher.end());
                lastStartIndex = delimtMatcher.end();
                isFirstToken = false;
            }
            if (isFindMatch) {
                // adding the last subString after the last delimiter
                if (suffix.length() > 0) {
                    addWord(isFirstToken && (!isFirstWord), suffix, strWithDelim);
                }
            } else {
                // in case no delimiter was found
                addWord(isFirstToken && (!isFirstWord), currWord, strWithDelim);
                isFirstWord = false;
            }
            isFirstWord = false;
        }
        final Word[] toRet = new Word[Math.min(strWithDelim.size(), MAX_NUM_WORDS)];
        for (int i = 0; i < Math.min(strWithDelim.size(), MAX_NUM_WORDS); i++) {
            toRet[i] = strWithDelim.get(i);
        }
        return toRet;
    }

    /*
     * Add a word into the word-vector for this message.
     */
    private static void addWord(boolean wordBool, String str, List<Word> vec) {
        if (vec.size() < MAX_NUM_WORDS) {

            final Word wd = new Word(str, wordBool);
            vec.add(wd);

        }
    }

}
