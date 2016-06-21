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
package org.openmainframe.ade.ext.main;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmainframe.ade.exceptions.AdeException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import junit.framework.TestCase;

/**
 * 
 *
 */
public class AdeUtilMainTest extends TestCase {

	
    @Before
    public void setUp(){
    	
    	//String filename = "C:"+File.separator+"userdata"+File.separator+"availability"+File.separator+"test_results"+File.separator+"debug"+File.separator+"ade_setup.props";
    	
    	String filename = "ade-assembly"+File.separator+"src"+File.separator;
    	filename = filename + "main"+ File.separator + "conf" + File.separator;
    	filename = filename + "setup.props";
    	
    	System.setProperty("ade.setUpFilePath",filename);
    }

	/**
	 * Test method for {@link org.openmainframe.ade.ext.main.AdeUtilMain#main(java.lang.String[])}.
	 */
	@Test
	public void testVersion() {
		
		PrintStream originalOut = System.out;
		OutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		System.setOut(ps);
		// Perform tests    
		try {
			AdeUtilMain.main(new String[]{"-v"});
		} catch (AdeException e) {
			
			e.printStackTrace();
		}    
		String Str = new String("Current Ade version (JAR): 3.2.1\r\n");
		Str.trim();
		String actualStr = os.toString();
		actualStr.trim();
		assertEquals(Str, actualStr);
		// Restore normal operation    
		System.setOut(originalOut);
	}
	/**
	 * Test method for {@link org.openmainframe.ade.ext.main.AdeUtilMain#main(java.lang.String[])}.
	 */
	@Test
	public void testDBVersion() {
		
		PrintStream originalOut = System.out;
		OutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		System.setOut(ps);
		// Perform tests    
		try {
			AdeUtilMain.main(new String[]{"-b"});
		} catch (AdeException e) {
			
			e.printStackTrace();
		}    
		String Str = new String("Current Ade DB version: 3.2.0\r\n");
		Str.trim();
		String actualStr = os.toString();
		actualStr.trim();
		assertEquals(Str, actualStr);
		// Restore normal operation    
		System.setOut(originalOut);
	}
	/**
	 * Test method for {@link org.openmainframe.ade.ext.main.AdeUtilMain#main(java.lang.String[])}.
	 */

	@Test
	public void testFlowlayout() {
		
		PrintStream originalOut = System.out;
		OutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		System.setOut(ps);
		// Perform tests    
		String flowlayoutString = new String("-f");
//		flowlayoutString = flowlayoutString + "C:" + File.separator + "userdata" ;
//		flowlayoutString = flowlayoutString + File.separator + "availability" ;
//		flowlayoutString = flowlayoutString + File.separator + "test_results" ;
//		flowlayoutString = flowlayoutString + File.separator + "debug";
		flowlayoutString = flowlayoutString +  "assembly-ade";
		flowlayoutString = flowlayoutString + File.separator + "src";
		flowlayoutString = flowlayoutString + File.separator + "main";
		flowlayoutString = flowlayoutString + File.separator + "conf";
		flowlayoutString = flowlayoutString + File.separator + "xml";
		flowlayoutString = flowlayoutString + File.separator + "Flowlayout.xml";
		
		try {
			AdeUtilMain.main(new String[]{flowlayoutString});
		} catch (AdeException e) {
			
			e.printStackTrace();
		}    
		String Str = new String("SUCCESS");
		Str.trim();
		String actualStr = os.toString();
		assertTrue(actualStr.contains(Str));

		// Restore normal operation    
		System.setOut(originalOut);
	}

}
