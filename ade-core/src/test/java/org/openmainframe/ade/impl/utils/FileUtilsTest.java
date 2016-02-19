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
package org.openmainframe.ade.impl.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.impl.utils.FileUtils;

public class FileUtilsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testAssertExistsOneFile() throws Exception {
        File f = tempFolder.newFile();
        FileUtils.assertExists(f);
    }

    @Test
    public void testAssertExistsTwoFiles() throws Exception {
        File f1 = tempFolder.newFile();
        File f2 = tempFolder.newFile();
        FileUtils.assertExists(f1, f2);
    }

    @Test(expected = FileNotFoundException.class)
    public void testAssertExistsThrowsExceptionForNonExistentFile() throws Exception {
        File f = tempFolder.newFile();
        f.delete();
        FileUtils.assertExists(f);
    }

    @Test(expected = FileNotFoundException.class)
    public void testAssertExistsThrowsExceptionOnNonExistentSecondFile() throws Exception {
        File f1 = tempFolder.newFile();
        File f2 = tempFolder.newFile();
        f2.delete();
        FileUtils.assertExists(f1, f2);
    }

    @Test
    public void testListFilesSortedReturnsFileNamesSorted() throws Exception {
        tempFolder.newFile("a");
        tempFolder.newFile("b");
        tempFolder.newFile("c");

        File[] files = FileUtils.listFilesSorted(tempFolder.getRoot());
        assertArrayEquals("Assert filename in alpha order",
                new String[] { "a", "b", "c" },
                new String[] { files[0].getName(), files[1].getName(), files[2].getName() });
    }

    @Test
    public void testListFilesSortedIgnoresDotFiles() throws Exception {
        tempFolder.newFile("a");
        tempFolder.newFile(".b");

        File[] files = FileUtils.listFilesSorted(tempFolder.getRoot());
        assertEquals("Validate only 1 file returned", 1, files.length);
        assertEquals("Validate correct file returned", "a", files[0].getName());
    }

    @Test
    public void testListFilesSortedIgnoresDirs() throws Exception {
        tempFolder.newFile("a");
        tempFolder.newFolder("b");
        tempFolder.newFile("c");

        File[] files = FileUtils.listFilesSorted(tempFolder.getRoot());
        assertEquals("Validate 2 files returned", 2, files.length);
        assertArrayEquals("Validate correct files returned",
                new String[] { "a", "c" },
                new String[] { files[0].getName(), files[1].getName() });
    }

    @Test
    public void testListFilesSortedReturnsEmptyArrayForNonDirectoryInput() throws Exception {
        File[] files = FileUtils.listFilesSorted(tempFolder.newFile());
        assertNotNull("Assert returned not null", files);
        assertEquals("Assert array is empty", 0, files.length);
    }

    @Test
    public void testCreateDir() throws Exception {
        File newDir = new File(tempFolder.getRoot(), "newDir");
        FileUtils.createDir(newDir);
        assertTrue("Assert new directory created", newDir.exists());
    }

    @Test
    public void testCreateDirs() throws Exception {
        File newParentDir = new File(tempFolder.getRoot(), "newParentDir");
        File newChildDir = new File(newParentDir, "newChildDir");

        FileUtils.createDirs(newChildDir);
        assertTrue("Assert new directory created", newParentDir.exists());
        assertTrue("Assert new directory created", newChildDir.exists());
    }

    @Test
    public void testOpenPrintWriterToFileSuccessOnExistingFile() throws Exception {
        assertNotNull("Assert method returns PrintWriter", FileUtils.openPrintWriterToFile(tempFolder.newFile(), false));
    }

    @Test(expected = AdeUsageException.class)
    public void testOpenPrintWriterToFileThrowsAdeUsageException() throws Exception {
        FileUtils.openPrintWriterToFile(new File("/xxx/xxx/xxx/badfile"), false);
    }
}
