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

package org.openmainframe.ade.core.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * A utility class for parsing text that contain numbers only files.
 * 
 * Example usage:
 * 
 * FileParser fp=new FileParser("some_file.txt");
 * while (!fp.isEof()) {
 *    // assumes each line has the following format: <real number> <string> \n
 *    // an exception is thrown on a mismatch
 *    double d=fp.getDouble();
 *    String s=fp.getWord();
 *    fp.getEol();
 * }
 */

public class NumberFileParser {
    
    /** Declare a logger */
    private static final Logger logger = LoggerFactory.getLogger(NumberFileParser.class);

    /** The name of the stream for error reports */
    private String mSourceName;

    /** If this object's constructor created the stream, it is kept here
     * so it can be closed at the end
     */
    private LineNumberReader mInputReader;

    private boolean mCloseReader;

    private enum TokenType {
        TOKEN_INT, TOKEN_DOUBLE, TOKEN_EOL, TOKEN_EOF
    };

    private static class Token {
        int mIntVal;
        double mDoubleVal;
        TokenType mType;

        public String toString() {
            return String.format("%d,%f,%s", mIntVal, mDoubleVal, mType.name());
        }
    }

    private Token mCurrentToken = new Token();
    private int mCurrentChar;

    /** Open the specified file name for parsing */
    public NumberFileParser(String pFileName) throws IOException {
        final InputStream inFile = new FileInputStream(pFileName);
        mInputReader = new LineNumberReader(new InputStreamReader(inFile, StandardCharsets.UTF_8));
        mSourceName = pFileName;
        mCloseReader = true;
        advanceChar();
        advanceToken();

    }

    /** Parse the given stream. The source name is supplied for error reports 
     * @throws IOException */
    public NumberFileParser(LineNumberReader reader, String pSourceName) throws IOException {
        mInputReader = reader;
        mSourceName = pSourceName;
        mCloseReader = false;
        advanceChar();
        advanceToken();

    }

    public final void close() throws IOException {
        if (mCloseReader) {
            mInputReader.close();
        }
    }

    /** Returns true iff current token is a double */
    public final boolean isDouble() {
        return mCurrentToken.mType == TokenType.TOKEN_DOUBLE || mCurrentToken.mType == TokenType.TOKEN_INT;
    }

    public final boolean isInt() {
        return mCurrentToken.mType == TokenType.TOKEN_INT;
    }

    /** Returns true iff current token is a end of line */
    public final boolean isEol() {
        return mCurrentToken.mType == TokenType.TOKEN_EOL;
    }

    public final boolean isEof() {
        return mCurrentToken.mType == TokenType.TOKEN_EOF;
    }

    /** Expects current token to be a double (or exception is raised)
     * Returns its value and moves to the next token. */
    public final double getDouble() throws IOException {
        if (!isDouble()) {
            parseError("Number expected");
        }
        final double v = mCurrentToken.mDoubleVal;
        advanceToken();
        return v;
    }

    /** Expects current token to be end of line (or exception is raised)
     * Moves to the next token. */
    public final void getEol() throws IOException {
        expectToken(TokenType.TOKEN_EOL, "End of line expected");
        advanceToken();
    }

    /** 
     * Reads all tokens until end of file and throws an exception if any of them is not white-space
     */
    public final void expectWhiteSpecialTillEnd() throws IOException {
        while (mCurrentToken.mType == TokenType.TOKEN_EOL) {
            advanceToken();
        }
        expectEof();
    }

    /**
     * Raises an exception if current token is not EOF
     */
    public final void expectEof() throws IOException {
        expectToken(TokenType.TOKEN_EOF, "End of file expected");
    }

    /** Expects current token to be an integer value (or an exception is raised)
     * Returns its value and moves to the next token.
     */
    public final int getInt() throws IOException {
        expectToken(TokenType.TOKEN_INT, "Integer Number expected");
        final int v = mCurrentToken.mIntVal;
        advanceToken();
        return v;
    }

    /** 
     * Raises an exception with the specified message if the current token
     * is not as specified. Tokens are specified using StreamTokenizer constants.
     */
    public final void expectToken(TokenType tokenType, String message)
            throws IOException {
        if (mCurrentToken.mType != tokenType) {
            parseError(message + "(token found " + mCurrentToken.mType + ")");
        }

    }

    /** Raises an IOException with the given message plus the current position in file */
    public final void parseError(String message) throws IOException {
        final String m = "At " + mSourceName + " line " + mInputReader.getLineNumber() + ": " + message;
        throw new IOException(m);
    }

    private void advanceToken() throws IOException {

        while (mCurrentChar >= 0 && Character.isWhitespace(mCurrentChar)) {
            if (mCurrentChar == 10) {
                mCurrentToken.mType = TokenType.TOKEN_EOL;
                advanceChar();
                return;
            }
            advanceChar();
        }

        if (mCurrentChar == -1) {
            mCurrentToken.mType = TokenType.TOKEN_EOF;
            return;
        }

        final StringBuilder val = new StringBuilder();
        while (mCurrentChar >= 0 && !Character.isWhitespace(mCurrentChar)) {
            val.append((char) mCurrentChar);
            advanceChar();
        }
        final String sval = val.toString();

        boolean canBeInt = false;
        boolean canBeDouble = true;
        try {
            mCurrentToken.mIntVal = Integer.valueOf(sval);
            canBeInt = true;
        } catch (NumberFormatException e) {
            logger.info("caught NumberFormatException", e);
        }
        try {
            mCurrentToken.mDoubleVal = Double.valueOf(sval);
            canBeDouble = true;
        } catch (NumberFormatException e) {
            logger.info("caught NumberFormatException", e);
        }

        if (!canBeInt && !canBeDouble) {
            parseError("Invalid number " + sval);
        }
        if (!canBeDouble && canBeInt) {
            parseError("Unexpected token " + sval);
        }
        if (canBeInt) {
            mCurrentToken.mType = TokenType.TOKEN_INT;
        } else {
            mCurrentToken.mType = TokenType.TOKEN_DOUBLE;
        }

    }

    private void advanceChar() throws IOException {
        mCurrentChar = mInputReader.read();
    }

}
