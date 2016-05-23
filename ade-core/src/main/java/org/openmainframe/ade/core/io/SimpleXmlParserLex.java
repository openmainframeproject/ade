/*
 
    Copyright IBM Corp. 2011, 2016
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
package org.openmainframe.ade.core.io;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.TreeMap;

/**
 * A helper class for SimpleXmlParser.
 * Handles lexical parsing.
 */
class SimpleXmlParserLex {

    private boolean mNeedsToCloseStream;
    private String mSourceName;
    private Reader mSource;
    private int mLineNumber;

    enum TokenType {
        OPENING_ELEMENT, ELEMENT_NAME, TEXT, EOF, COMMENT
    };

    Token mCurrentToken = new Token();
    int mCurrentChar;
    private StringBuilder mLexTokenBuilder = new StringBuilder();
    boolean mFirstElement;

    class Token {

        String mContent;
        TokenType mType;
        boolean mOpen;
        boolean mClose;
        int mLineNumber;

        public String toString() {
            final String res = mType + " '" + mContent + "' open=" + mOpen + " closed=" + mClose;
            return res;
        }

        public boolean isTag() {
            return mType == TokenType.ELEMENT_NAME;
        }

        public boolean isOpenTag() {
            return isTag() && mOpen;
        }

        public boolean isCloseTag() {
            return isTag() && mClose;
        }

        public boolean isOpenCloseTag() {
            return isOpenTag() && mClose;
        }

        public boolean isText() {
            return mType == TokenType.TEXT;
        }

        public boolean isEof() {
            return mType == TokenType.EOF;
        }

        Map<String, String> mAttributes = new TreeMap<>();

        void clear() {
            mOpen = false;
            mClose = false;
            mAttributes.clear();
        }
    }

    SimpleXmlParserLex(String sourceName, Reader source, boolean closeStream) throws IOException {
        mSourceName = sourceName;
        mSource = source;
        mFirstElement = true;
        mNeedsToCloseStream = closeStream;
        mLineNumber = 1;
        advanceChar();
        advanceToken();
        while (mCurrentToken.mType == TokenType.OPENING_ELEMENT) {
            advanceToken();
        }

    }

    void advanceToken() throws IOException {
        while (true) {
            advanceTokenWithComments();
            if (mCurrentToken.mType != TokenType.COMMENT) {
                break;
            }
        }
    }

    private void advanceTokenWithComments() throws IOException {
        skipWhiteSpace();
        mCurrentToken.mLineNumber = mLineNumber;
        mCurrentToken.clear();
        if (mCurrentChar < 0) {
            mCurrentToken.mType = TokenType.EOF;
            return;
        }

        if (mCurrentChar == '<') {
            readElementName();
        } else {
            readText();
        }
    }

    private void readText() throws IOException {
        mCurrentToken.mType = TokenType.TEXT;
        mLexTokenBuilder.setLength(0);
        while (mCurrentChar >= 0 && mCurrentChar != '<' && mCurrentChar != '>') {
            mLexTokenBuilder.append((char) mCurrentChar);
            advanceChar();
        }
        mCurrentToken.mContent = replaceSpecialChars(mLexTokenBuilder.toString().trim());
    }

    private void readElementName() throws IOException {
        // skip <
        advanceChar();
        if (mCurrentChar == '!') {
            readComment();
            return;
        }

        mCurrentToken.mType = TokenType.ELEMENT_NAME;

        skipWhiteSpace();
        if (mCurrentChar == '/') {
            mCurrentToken.mClose = true;
            advanceChar();
            skipWhiteSpace();
        } else if (mCurrentChar == '?') {
            if (mFirstElement) {
                mCurrentToken.mType = TokenType.OPENING_ELEMENT;
            } else {
                reportError("? only allowed in first element");
            }
            advanceChar();
            skipWhiteSpace();
        } else {
            mCurrentToken.mOpen = true;
            mFirstElement = false;// eladsh
        }
        mCurrentToken.mContent = readNmToken();
        if (mCurrentToken.mContent.length() == 0) {
            reportError("element name expected");
        }
        skipWhiteSpace();

        while (isNameStartChar()) {
            final String attrName = readNmToken();
            if (attrName.length() == 0) {
                reportError("Internal bug: attribute name expected");
            }
            skipWhiteSpace();
            if (mCurrentChar != '=') {
                reportError("'=' expected after attribute name '" + attrName + "'");
            }
            advanceChar();
            skipWhiteSpace();
            final String attrValue = readQuotedString();

            if (mCurrentToken.mAttributes.containsKey(attrName)) {
                reportError("Duplicate attribute name '" + attrName + "'");
            }
            mCurrentToken.mAttributes.put(attrName, attrValue);
            skipWhiteSpace();
        }

        if (mCurrentToken.mType == TokenType.OPENING_ELEMENT) {
            if (mCurrentChar != '?') {
                reportError("? expected in end of opening tag");
            }
            advanceChar();
            skipWhiteSpace();
        } else if (mCurrentChar == '/') {
            if (mCurrentToken.mClose) {
                reportError("Two / in the same tag");
            }

            mCurrentToken.mClose = true;
            advanceChar();
            skipWhiteSpace();
        }
        if (mCurrentChar != '>') {
            reportError("Tag '" + mCurrentToken.mContent + "' improperly terminated");
        }

        advanceChar();
    }

    private void readComment() throws IOException {
        final String dashMessage = "'<!' should be followed by '--' to start a comment";
        mCurrentToken.mType = TokenType.COMMENT;
        // Skip
        advanceChar();
        if (mCurrentChar != '-') {
            reportError(dashMessage);
        }
        advanceChar();
        if (mCurrentChar != '-') {
            reportError(dashMessage);
        }
        advanceChar();

        int dashCount = 0;
        while (mCurrentChar >= 0 && dashCount < 2) {
            if (mCurrentChar == '-') {
                ++dashCount;
            } else {
                dashCount = 0;
            }
            advanceChar();
        }
        if (mCurrentChar < 0) {
            reportError("Unterminated comment starting at line " + mCurrentToken.mLineNumber);
        }
        if (mCurrentChar != '>') {
            reportError("In comments, '--' must be followed be '>' to terminate the comment (comment starting at line " + mCurrentToken.mLineNumber);
        }
        advanceChar();
    }

    private String readQuotedString() throws IOException {
        if (mCurrentChar != '"') {
            reportError("'\"' expected");
        }
        advanceChar();
        mLexTokenBuilder.setLength(0);
        while (mCurrentChar >= 0 && mCurrentChar != '\n' && mCurrentChar != '"') {
            mLexTokenBuilder.append((char) mCurrentChar);
            advanceChar();
        }
        if (mCurrentChar != '"') {
            reportError("Unterminated string. '\"' expected");
        }
        advanceChar();
        return mLexTokenBuilder.toString();

    }

    private boolean isNameStartChar() {
        return mCurrentChar >= 0
                && (((CHARACTER_TYPES[mCurrentChar] & CHARACTER_TYPE_NAME_START_CHAR_MASK) > 0)
                        || (mCurrentChar >= 0x10000 && mCurrentChar <= 0xeffff));

    }

    private boolean isNameChar() {
        return mCurrentChar >= 0 
                && (((CHARACTER_TYPES[mCurrentChar] & CHARACTER_TYPE_NAME_CHAR_MASK) > 0)
                        || (mCurrentChar >= 0x10000 && mCurrentChar <= 0xeffff));

    }

    private String readNmToken() throws IOException {
        mLexTokenBuilder.setLength(0);
        while (isNameChar()) {
            mLexTokenBuilder.append((char) mCurrentChar);
            advanceChar();
        }
        return mLexTokenBuilder.toString();
    }

    private void skipWhiteSpace() throws IOException {
        while (mCurrentChar >= 0 && Character.isWhitespace(mCurrentChar)) {
            advanceChar();
        }
    }

    private void advanceChar() throws IOException {
        if (mCurrentChar == '\n') {
            ++mLineNumber;
        }
        mCurrentChar = mSource.read();
    }

    public void reportError(String msg) throws IOException {
        throw new IOException(String.format("%s, line %d: %s", mSourceName, mLineNumber, msg));
    }

    public void close() throws IOException {
        if (mNeedsToCloseStream) {
            mSource.close();
        }
    }

    private String replaceSpecialChars(String src) {
        boolean found = false;
        for (int i = 0; i < src.length(); ++i) {
            if (src.charAt(i) == '&') {
                found = true;
                break;
            }
        }
        if (!found) {
            return src;
        }

        final StringBuilder res = new StringBuilder();
        int i = 0;
        while (i < src.length()) {
            final char c = src.charAt(i);
            if (c != '&') {
                res.append(c);
                ++i;
            } else {
                int j = i + 1;
                while (j < src.length() && src.charAt(j) != ';' && (j - i) < 10) {
                    ++j;
                }
                if (j < src.length() && src.charAt(j) == ';') {
                    final String temp = convertSpecialChar(src.substring(i + 1, j));
                    if (temp == null) {
                        res.append(c);
                        ++i;
                    } else {
                        res.append(temp);
                        i = j + 1;
                    }
                } else {
                    res.append(c);
                    ++i;
                }
            }
        }
        return res.toString();
    }

    private String convertSpecialChar(String src) {
        if (src.equals("amp")) {
            return "&";
        } else if (src.equals("lt")) {
            return "<";
        } else if (src.equals("gt")) {
            return ">";
        } else if (src.equals("quot")) {
            return "\"";
        } else if (src.equals("apos")) {
            return "'";
        } else if (src.startsWith("#x")) {
            final String num = src.substring(2);
            final int len = num.length();
            int code = 0;
            try {
                code = Integer.valueOf(num, 16);
            } catch (NumberFormatException e) {
                return null;
            }
            if (len == 2) {
                return "" + (char) code;
            }
            final StringBuilder res = new StringBuilder();
            res.appendCodePoint(code);
            return res.toString();
        } else if (src.startsWith("#")) {
            final String num = src.substring(1);
            int code = 0;
            try {
                code = Integer.valueOf(num);
            } catch (NumberFormatException e) {
                return null;
            }

            final StringBuilder res = new StringBuilder();
            res.appendCodePoint(code);
            return res.toString();
        }

        return null;
    }

    private static byte[] CHARACTER_TYPES = new byte[65536];
    private static final byte CHARACTER_TYPE_NAME_CHAR_MASK = 0x1;
    private static final byte CHARACTER_TYPE_NAME_START_CHAR_MASK = 0x2;

    private static void fillCharTypes(int mask, int... rs) {
        for (int i = 0; i < rs.length; i += 2) {
            for (int r = rs[i]; r <= rs[i + 1]; ++r) {
                CHARACTER_TYPES[r] |= mask;
            }
        }
    }

    static {
        fillCharTypes(CHARACTER_TYPE_NAME_CHAR_MASK | CHARACTER_TYPE_NAME_START_CHAR_MASK,
                ':', ':',
                'A', 'Z',
                '_', '_',
                'a', 'z',
                0xC0, 0xD6,
                0xD8, 0xF6,
                0xF8, 0x2FF,
                0x370, 0x37D,
                0x37F, 0x1FFF,
                0x200C, 0x200D,
                0x2070, 0x218F,
                0x2C00, 0x2FEF,
                0x3001, 0xD7FF,
                0xF900, 0xFDCF,
                0xFDF0, 0xFFFD);

        fillCharTypes(CHARACTER_TYPE_NAME_CHAR_MASK,
                '-', '-',
                '.', '.',
                '0', '9',
                0xB7, 0xB7,
                0x0300, 0x036F,
                0x203F, 0x2040);
    }

}
