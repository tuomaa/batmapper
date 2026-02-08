package com.glaurung.batMap.io;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AreaDataPersisterTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void listAreaNames_emptyDir() throws IOException {
        File baseDir = tempFolder.newFolder("base");
        File confDir = new File(baseDir, "conf");
        File areasDir = new File(confDir, "batMapAreas");
        areasDir.mkdirs();

        List<String> names = AreaDataPersister.listAreaNames(baseDir.getPath());
        assertNotNull(names);
        assertTrue(names.isEmpty());
    }

    @Test
    public void listAreaNames_batmapFiles() throws IOException {
        File baseDir = tempFolder.newFolder("base2");
        File areasDir = new File(new File(baseDir, "conf"), "batMapAreas");
        areasDir.mkdirs();

        new File(areasDir, "sunderland.batmap").createNewFile();
        new File(areasDir, "shadowkeep.batmap").createNewFile();

        List<String> names = AreaDataPersister.listAreaNames(baseDir.getPath());
        assertEquals(2, names.size());
        assertTrue(names.contains("sunderland"));
        assertTrue(names.contains("shadowkeep"));
    }

    @Test
    public void listAreaNames_nonBatmapFilesFiltered() throws IOException {
        File baseDir = tempFolder.newFolder("base3");
        File areasDir = new File(new File(baseDir, "conf"), "batMapAreas");
        areasDir.mkdirs();

        new File(areasDir, "sunderland.batmap").createNewFile();
        new File(areasDir, "notes.txt").createNewFile();
        new File(areasDir, "backup.batmap.0.bk").createNewFile();

        List<String> names = AreaDataPersister.listAreaNames(baseDir.getPath());
        assertEquals(1, names.size());
        assertEquals("sunderland", names.get(0));
    }

    @Test
    public void listAreaNames_nonexistentDir() {
        List<String> names = AreaDataPersister.listAreaNames("/nonexistent/path/that/does/not/exist");
        assertNotNull(names);
        assertTrue(names.isEmpty());
    }
}
