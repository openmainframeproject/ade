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

/**
 * This class executes an external program and captures the output in memory.
 */
public class NativeCommand {
    /**
     * Output from the external program.
     */
    protected StringBuffer commandOutput = null;

    /**
     * Error from the external program,
     */
    protected StringBuffer commandErrorOutput = null;

    /**
     * Runs the external program and captures the output in memory
     * for later retrieval.
     * 
     * @param command the program to execute
     * 
     * @return the program return code
     * 
     * @throws an exception if there is a problem executing the program
     */
    public final int exec(String command) throws Exception {
        final Runtime r = Runtime.getRuntime();
        final Process process = r.exec(command);

        /*
         * Wait for the program to complete and process the output.
         */
        return processOutput(process);
    }

    /**
     * Runs the external program and captures the output in memory
     * for later retrieval.
     * 
     * @param args the arguments of the program to execute
     * 
     * @return the program return code
     * 
     * @throws an exception if there is a problem executing the program
     */
    public final int exec(String[] args) throws Exception {
        final Runtime r = Runtime.getRuntime();
        final Process process = r.exec(args);

        /*
         * Wait for the program to complete and process the output.
         */
        return processOutput(process);
    }

    /**
     * Return the output from the last executed external program.
     * 
     * @return output from the last executed external program
     */
    public final String getCommandOutput() {
        return commandOutput.toString();
    }

    /**
     * Return the error output from the last executed program.
     * 
     * @return error output from the last executed program
     */
    public final String getCommandErrorOutput() {
        return commandErrorOutput.toString();
    }

    /**
     * Wait for the program to complete and handle the output the 
     * program may produce.
     * 
     * @param process Process that the program is running in
     * 
     * @return return code from command execution.  A value of 0 typically
     * means that the command executed normally.
     * 
     * @throws an exception if there is a problem executing the program
     */
    protected final int processOutput(Process process) throws Exception {
        /*
         * Allocate memory for output.
         */
        commandOutput = new StringBuffer();

        /*
         * Allocate memory for error.
         */
        commandErrorOutput = new StringBuffer();

        /*
         * Copy the output and the error of this program and place it into memory.  
         */
        final NativeCommandOutputParser errorParser = new NativeCommandOutputParser(process.getErrorStream(), commandErrorOutput);
        final NativeCommandOutputParser outputParser = new NativeCommandOutputParser(process.getInputStream(), commandOutput);

        errorParser.start();
        outputParser.start();

        /*
         * Wait for the program execution process to terminate.
         */
        final int rc = process.waitFor();

        /*
         * In the event that the program stops before the stream processor
         * threads can finish, give them a chance to complete copying the 
         * output.
         */
        errorParser.join();
        outputParser.join();

        return rc;
    }
}