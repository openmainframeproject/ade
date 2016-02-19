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
package org.openmainframe.ade.utils;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openmainframe.ade.exceptions.AdeUsageException;
import org.openmainframe.ade.utils.AdeFileUtils;

public class TestAdeFileUtils {

    private String text = "This is the contents of the test file";
    private File testFile;
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Before
    public void setUp() throws Exception {
        testFile = temporaryFolder.newFile();
        final BufferedWriter logWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(testFile), StandardCharsets.UTF_8));
        
        logWriter.write(text);
        logWriter.close();
        
    }

    @Test
    public void testCopyFile() throws IOException, AdeUsageException {
        final File targetFile = temporaryFolder.newFile();
        
        AdeFileUtils.copyFile(testFile, targetFile);
        byte[] f1 = Files.readAllBytes(testFile.toPath());
        byte[] f2 = Files.readAllBytes(targetFile.toPath());
        
        assertTrue("The target file should be exactly the same as the source.", Arrays.equals(f1, f2));
    }

    @Test(expected = AdeUsageException.class)
    public void testCopyFileNoWriteAccess() throws IOException, AdeUsageException {
        final File targetFile = temporaryFolder.newFile();
        
        assertTrue("Make sure we can set the file to read only.", targetFile.setReadOnly());
        AdeFileUtils.copyFile(testFile, targetFile);
    }

    @Test
    public void testOpenLogFile() throws AdeUsageException {
        final BufferedReader reader = AdeFileUtils.openLogFile(testFile);
        
        assertNotNull("Verify that the reader is  not null.", reader);
    }

    @Test
    public void testOpenLogFileGZipFile() throws AdeUsageException, IOException {
        final File targetFile = temporaryFolder.newFile("file.gz");
        final FileOutputStream out = new FileOutputStream(targetFile);
        final GZIPOutputStream gos = new GZIPOutputStream(out);
        
        gos.write(text.getBytes(StandardCharsets.UTF_8));
        gos.close();
        
        BufferedReader reader = AdeFileUtils.openLogFile(targetFile);
        
        assertNotNull("Verify that the reader is  not null.", reader);
        
        String unzippedText = reader.readLine();
        
        assertEquals("The unzipped text should be exactly the same as the source.", text, unzippedText);
        
    }

    @Test(expected = AdeUsageException.class)
    public void testOpenLogFileBadGZipFile() throws AdeUsageException, IOException {
        final File targetFile = temporaryFolder.newFile("file.gz");
        
        AdeFileUtils.openLogFile(targetFile);
        
    }

    @Test
    public void testOpenLogFileAsInputStream() throws AdeUsageException {
        final InputStream inputStream = AdeFileUtils.openLogFileAsInputStream(testFile);
        
        assertNotNull("Verify that inputStream is not null.", inputStream);
    }

    @Test
    public void testOpenLogFileAsInputStreamGZipFile() throws AdeUsageException, IOException {
        final File targetFile = temporaryFolder.newFile("file.gz");
        final FileOutputStream out = new FileOutputStream(targetFile);
        final GZIPOutputStream gos = new GZIPOutputStream(out);
        
        gos.write(text.getBytes(StandardCharsets.UTF_8));
        gos.close();
        
        final InputStream inputStream = AdeFileUtils.openLogFileAsInputStream(targetFile);
        
        assertNotNull("Verify that inputStream is not null.", inputStream);
        
    }

    @Test(expected = AdeUsageException.class)
    public void testOpenLogFileAsInputStreamBadGZipFile() throws AdeUsageException, IOException {
        final File targetFile = temporaryFolder.newFile("file.gz");
        
        AdeFileUtils.openLogFileAsInputStream(targetFile);
    }

    @Test(expected = AdeUsageException.class)
    public void testDeleteFileWithFileNotExist() throws AdeUsageException {
        final File emptyFile = new File("fakefile");
        
        AdeFileUtils.deleteFile(emptyFile);
    }

    @Test
    public void testDeleteFile() throws AdeUsageException, IOException {
        final File targetFile = temporaryFolder.newFile("file");
        
        AdeFileUtils.deleteFile(targetFile);
        assertFalse("Verify that the file was deleted.", targetFile.exists());
    }

    @Test
    public void testDeleteFileOrLogFileNotExist() throws AdeUsageException {
        final File emptyFile = new File("fakefile");
        
        //A failure will simply log the error and will not throw an exception.
        AdeFileUtils.deleteFileOrLog(emptyFile);
        assertFalse("Verify the file still does not exist.", emptyFile.exists());
    }

    @Test
    public void testDeleteFileOrLog() throws AdeUsageException, IOException {
        final File targetFile = temporaryFolder.newFile("file");
        
        AdeFileUtils.deleteFileOrLog(targetFile);
        assertFalse("Verify the file no longer exists.", targetFile.exists());
    }
    
    @Test
    public void testCreateDir() throws AdeUsageException, IOException {
        final File targetDir = temporaryFolder.newFolder("dir1");
        final File newDir = new File(targetDir.getPath()+"/newdir");
        
        AdeFileUtils.createDir(newDir);
        assertTrue("Verify the directory was created.", newDir.exists());
    }

    @Test(expected = AdeUsageException.class)
    public void testCreateDirBadPath() throws AdeUsageException, IOException {
        final File badPath = new File("");
        
        AdeFileUtils.createDir(badPath);
    }

    @Test
    public void testCreateDirFileExists() throws AdeUsageException, IOException {
        final File aFile = temporaryFolder.newFile();
        
        AdeFileUtils.createDir(aFile);
        assertTrue("Verify the file is still there.", aFile.exists());
    }

    @Test
    public void testCreateDirs() throws AdeUsageException, IOException {
        final File targetDir = temporaryFolder.newFolder("dir1");
        final File newDir = new File(targetDir.getPath() + "/newdir1/newdir2");
        
        AdeFileUtils.createDirs(newDir);
        assertTrue("Verify the directory was created.", newDir.exists());
    }

    @Test(expected = AdeUsageException.class)
    public void testCreateDirsBadPath() throws AdeUsageException, IOException {
        final File badPath = new File("");
        
        AdeFileUtils.createDirs(badPath);
    }

    @Test
    public void testCreateDirsFileExists() throws AdeUsageException, IOException {
        final File aFile = temporaryFolder.newFile();
        AdeFileUtils.createDirs(aFile);
        assertTrue("Verify the file is still there.", aFile.exists());
    }

    @Test
    public void testDeleteRecursiveOrLog() throws IOException, AdeUsageException {
        final File targetDir = temporaryFolder.newFolder("dir1");
        
        assertTrue("Returns true if successfully deleted.", AdeFileUtils.deleteRecursiveOrLog(targetDir));
    }
    
    @Test
    public void testDeleteRecursiveOrLogMoreFiles() throws IOException, AdeUsageException {
        final File targetDir = temporaryFolder.newFolder("dir1");
        final File newDir = new File(targetDir.getPath() + "/newdir1/newdir2");
        
        AdeFileUtils.createDirs(newDir);
        assertTrue("Returns true if successfully deleted.", AdeFileUtils.deleteRecursiveOrLog(targetDir));
    }
    
    @Test
    public void testDeleteRecursiveOrLogNotDir() throws IOException, AdeUsageException {
        final File targetFile = temporaryFolder.newFile();
        
        assertTrue("Returns true if successfully deleted.", AdeFileUtils.deleteRecursiveOrLog(targetFile));
    }

}
