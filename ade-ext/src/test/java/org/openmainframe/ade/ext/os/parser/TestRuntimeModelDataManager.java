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
package org.openmainframe.ade.ext.os.parser;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.openmainframe.ade.Ade;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.utils.ConfigPropertiesWrapper;
import org.openmainframe.ade.utils.patches.Version;
import org.openmainframe.ade.ext.AdeExt;
import org.openmainframe.ade.ext.main.Analyze;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

public class TestRuntimeModelDataManager {
    RuntimeModelDataManager rmdm;
    @Before
    public void setup() {
        rmdm = Mockito.spy(RuntimeModelDataManager.class);
    }
    
    @Test
    public void testWriteModelDataToFileCreatesFiles() throws AdeException, IOException, URISyntaxException {
        File dir2 = new File("AnalysisOutputPath");
        dir2.mkdir();
        Ade ade = mock(Ade.class, RETURNS_DEEP_STUBS);
        when(ade.getConfigProperties().database().getDatabaseDriver()).thenReturn("derby");
        when(ade.getConfigProperties().getOverrideVersionCheck()).thenReturn(true);
        when(ade.getDbVersion()).thenReturn(new Version(1, 0));
        when(ade.getSetupFilePath()).thenReturn("test2.properties");
        when(ade.getConfigProperties().getOutputPath()).thenReturn("AnalysisOutputPath");
        when(ade.getConfigProperties().getAnalysisOutputPath()).thenReturn("AnalysisOutputPath");
        Ade.create(ade);
        
        /* Creating some properties to be added. Ade-ext will fail to be created without it */
        Properties props = new Properties();
        props.setProperty("RMI_PORT_PARAM", "20");
        File file = new File("test2.properties");
        FileOutputStream fileOut = new FileOutputStream(file);
        props.store(fileOut, "Favorite Things");
        fileOut.close();
        
        ConfigPropertiesWrapper cpw = new ConfigPropertiesWrapper("adeExt");
        cpw.addProperties(props, "conf");
        
        AdeExt adeext = mock(AdeExt.class);
        adeext.create(ade);
        
        Analyze anal = Mockito.spy(Analyze.class);
        
        /* Running through the Analyze path to create a .ser file */
        rmdm.writeModelDataToFile("test");
        File runTimeModelDataFile = new File("AnalysisOutputPath/runtimeModelData.ser");
        assertEquals("Testing if the runtimeModelData.ser file was created ",true,runTimeModelDataFile.exists()); 
        
        /* Running through and seeing that .ser exists so you create a .ser.tmp */
        rmdm.writeModelDataToFile("test");
        rmdm.writeModelDataToFile("test");
        
        /* Reading the input and it deletes the runTimeModelData files but does not make an ffdc file*/
        rmdm.readModelDataFromFile("AnalysisOutputPath/runtimeModelData.ser.tmp");
        rmdm.readModelDataFromFile("");
        
        File runTimeModelDataFileFFDC = new File("AnalysisOutputPath/runtimeModelData.ser.ffdc");
        assertEquals("Testing if the runtimeModelData.ser file was deleted ",false,runTimeModelDataFile.exists());
        assertEquals("Testing if the runTimeModelData.ser.ffdc file does not exist ",false,runTimeModelDataFileFFDC.exists());
        
    }
    
    @After
    public void removingRemainingCreatedFiles() {
        File analDirectory = new File("AnalysisOutputPath");
        File modelDirectory = new File("AnalysisOutputPath/models");
        File adhocDirectory = new File("AnalysisOutputPath/analysis_adhoc");
        File continuousDirectory = new File("AnalysisOutputPath/continuous");
        File properties = new File("test2.properties");
        
        modelDirectory.delete();
        adhocDirectory.delete();
        continuousDirectory.delete();
        analDirectory.delete();
        properties.delete();
    }
}
