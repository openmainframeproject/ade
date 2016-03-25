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
package org.openmainframe.ade.ext.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * An instance of this class retrieves output from a native command execution and
 * maintains it in memory to be retrieved later.
 */
public class NativeCommandOutputParser extends Thread {
    /**
     * Input stream.
     */
    protected InputStream is;

    /**
     * Output to be placed in memory.
     */
    protected StringBuffer outputFromExternalProgram;

    /**
     * Instantiates a default instance.
     *
     * @param is stream to read from
     * @param commandOutput output in memory.
     */
    public NativeCommandOutputParser(InputStream is, StringBuffer commandOutput) {
        this.is = is;
        this.outputFromExternalProgram = commandOutput;
    }

    /**
     * The execution in the separate thread happens here.
     */
    public final void run() {
        try {
            final BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String line = null;
            while ((line = br.readLine()) != null) {
                synchronized (outputFromExternalProgram) {
                    outputFromExternalProgram.append(line);
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
