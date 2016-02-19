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
package org.openmainframe.ade.impl.stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeFlowException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.flow.IStreamTarget;
import org.openmainframe.ade.impl.utils.FileUtils;


abstract class StatManager<T> implements IStreamTarget<T> {

    private File m_file;
    private String m_description;

    private BufferedWriter m_out;
    private boolean m_isFirstStat = true;

    protected StatManager(File filePath, String description) throws AdeInternalException {
        m_file = filePath;
        if (description.contains("\n")) {
            throw new AdeInternalException("description string must not contain line breaks!");
        }
        m_description = description;
    }

    @Override
    public void beginOfStream() throws AdeException, AdeFlowException {
        try {

            if (m_file.exists()) {
                m_file.delete();
            }

            final File parentDir = m_file.getParentFile();
            if (!parentDir.exists()) {
                FileUtils.createDirs(parentDir);
            }

            m_out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(m_file), StandardCharsets.UTF_8));
            m_out.write(m_description + "\n");

        } catch (FileNotFoundException e) {
            throw new AdeInternalException("File should have already been created: " + m_file.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new AdeInternalException("IO Exception occurred", e);
        }
    }

    @Override
    public void endOfStream() throws AdeException, AdeFlowException {
        beforeEndOfStream();
        try {
            m_out.close();
        } catch (IOException e) {
            throw new AdeInternalException("IO Exception occurred", e);
        }
    }

    protected synchronized void addStat(Object stat) throws AdeInternalException {
        String str = stat.toString();
        if (m_isFirstStat) {
            m_isFirstStat = false;
        } else {
            str = "," + str;
        }
        write(str);
    }

    protected synchronized void write(String str) throws AdeInternalException {
        try {
            m_out.write(str);
        } catch (IOException e) {
            throw new AdeInternalException("IO Exception occurred", e);
        }
    }

    protected synchronized void writeln(String str) throws AdeInternalException {
        write(str + "\n");
    }

    abstract protected void beforeEndOfStream() throws AdeException, AdeFlowException;

}
