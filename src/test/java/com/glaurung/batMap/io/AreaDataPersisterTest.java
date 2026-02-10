package com.glaurung.batMap.io;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.glaurung.batMap.vo.Area;
import com.glaurung.batMap.vo.AreaSaveObject;
import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;

public class AreaDataPersisterTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final File TEST_DATA_AREAS = new File("test_data/batMapAreas");

    @Before
    public void setUp() {
        AreaDataPersister.shutdown();
    }

    @After
    public void tearDown() {
        AreaDataPersister.shutdown();
    }

    // --- Filesystem fallback tests (database is null) ---

    @Test
    public void listAreaNames_emptyDir_fallback() throws IOException {
        File baseDir = tempFolder.newFolder("base");
        File areasDir = new File(new File(baseDir, "conf"), "batMapAreas");
        areasDir.mkdirs();

        List<String> names = AreaDataPersister.listAreaNames(baseDir.getPath());
        assertNotNull(names);
        assertTrue(names.isEmpty());
    }

    @Test
    public void listAreaNames_batmapFiles_fallback() throws IOException {
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
    public void listAreaNames_nonBatmapFilesFiltered_fallback() throws IOException {
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
    public void listAreaNames_nonexistentDir_fallback() {
        List<String> names = AreaDataPersister.listAreaNames("/nonexistent/path/that/does/not/exist");
        assertNotNull(names);
        assertTrue(names.isEmpty());
    }

    // --- SQLite-backed tests ---

    @Test
    public void initialize_createsDatabase() throws IOException {
        File baseDir = tempFolder.newFolder("initTest");
        AreaDataPersister.initialize(baseDir.getPath());

        File dbFile = new File(new File(baseDir, "conf"), "batmapper.db");
        assertTrue(dbFile.exists());
    }

    @Test
    public void listAreaNames_fromSqlite() throws IOException {
        File baseDir = tempFolder.newFolder("sqliteList");
        AreaDataPersister.initialize(baseDir.getPath());

        Area area = new Area("testarea");
        Room room = new Room("r1", area);
        room.setShortDesc("test");
        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        graph.addVertex(room);

        Map<Room, Point2D> locations = new HashMap<>();
        locations.put(room, new Point2D.Double(0, 0));
        try {
            AreaDataPersister.database.saveArea("testarea", graph, locations);
        } catch (java.sql.SQLException e) {
            fail("SQLException: " + e.getMessage());
        }

        List<String> names = AreaDataPersister.listAreaNames(baseDir.getPath());
        assertEquals(1, names.size());
        assertEquals("testarea", names.get(0));
    }

    @Test
    public void loadData_fromSqlite() throws Exception {
        File baseDir = tempFolder.newFolder("sqliteLoad");
        AreaDataPersister.initialize(baseDir.getPath());

        Area area = new Area("loadtest");
        Room room = new Room("r1", area);
        room.setShortDesc("A room");
        room.setLongDesc("A long desc");
        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        graph.addVertex(room);

        Map<Room, Point2D> locations = new HashMap<>();
        locations.put(room, new Point2D.Double(50, 100));
        AreaDataPersister.database.saveArea("loadtest", graph, locations);

        AreaSaveObject loaded = AreaDataPersister.loadData(baseDir.getPath(), "loadtest");
        assertNotNull(loaded);
        assertEquals(1, loaded.getGraph().getVertexCount());
        Room loadedRoom = loaded.getGraph().getVertices().iterator().next();
        assertEquals("A room", loadedRoom.getShortDesc());
    }

    @Test
    public void searchRooms_viaPersister() throws Exception {
        File baseDir = tempFolder.newFolder("sqliteSearch");
        AreaDataPersister.initialize(baseDir.getPath());

        Area area = new Area("searcharea");
        Room r1 = new Room("r1", area);
        r1.setShortDesc("Forest clearing");
        r1.setLongDesc("A clearing in the forest");
        Room r2 = new Room("r2", area);
        r2.setShortDesc("Dark cave");
        r2.setLongDesc("A damp cave");

        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        graph.addVertex(r1);
        graph.addVertex(r2);
        AreaDataPersister.database.saveArea("searcharea", graph, new HashMap<Room, Point2D>());

        List<SqliteAreaDatabase.SearchResult> results = AreaDataPersister.searchRooms("forest");
        assertEquals(1, results.size());
        assertEquals("r1", results.get(0).getRoomId());
        assertEquals("searcharea", results.get(0).getAreaName());
    }

    // --- Migration tests using real test_data ---

    private File setUpMigrationDir(String name) throws IOException {
        File baseDir = tempFolder.newFolder(name);
        File areasDir = new File(new File(baseDir, "conf"), "batMapAreas");
        areasDir.mkdirs();
        return baseDir;
    }

    private int copyBatmapFiles(File areasDir) throws IOException {
        return copyBatmapFiles(areasDir, null);
    }

    /**
     * Copies .batmap files from test_data to areasDir. If filter is non-null,
     * only files accepted by the filter are copied.
     * Returns the number of files copied.
     */
    private int copyBatmapFiles(File areasDir, FileFilter filter) throws IOException {
        assertTrue("test_data/batMapAreas must exist at project root", TEST_DATA_AREAS.exists());
        int count = 0;
        File[] files = TEST_DATA_AREAS.listFiles();
        assertNotNull(files);
        for (File f : files) {
            if (f.getName().endsWith(".batmap") && (filter == null || filter.accept(f))) {
                FileUtils.copyFileToDirectory(f, areasDir);
                count++;
            }
        }
        return count;
    }

    @Test
    public void migration_allBatmapFilesMigrated() throws Exception {
        File baseDir = setUpMigrationDir("migAll");
        File areasDir = new File(new File(baseDir, "conf"), "batMapAreas");
        int batmapCount = copyBatmapFiles(areasDir);
        assertTrue("Should have test .batmap files", batmapCount > 0);

        AreaDataPersister.initialize(baseDir.getPath());

        // Count outcomes
        File[] remainingBatmap = areasDir.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(".batmap");
            }
        });
        File[] migratedFiles = areasDir.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(".batmap.migrated");
            }
        });
        int migrated = migratedFiles != null ? migratedFiles.length : 0;
        int failed = remainingBatmap != null ? remainingBatmap.length : 0;

        assertEquals("migrated + failed should equal original count",
                batmapCount, migrated + failed);
        assertTrue("At least 90% of areas should migrate successfully, but only "
                        + migrated + "/" + batmapCount + " did",
                migrated >= (int) (batmapCount * 0.9));

        // DB area count should match the number of successfully migrated files
        List<String> areaNames = AreaDataPersister.listAreaNames(baseDir.getPath());
        assertEquals(migrated, areaNames.size());

        // DB file should exist
        assertTrue(new File(new File(baseDir, "conf"), "batmapper.db").exists());
    }

    @Test
    public void migration_batmapFilesRenamedToMigrated() throws Exception {
        File baseDir = setUpMigrationDir("migRename");
        File areasDir = new File(new File(baseDir, "conf"), "batMapAreas");
        FileUtils.copyFileToDirectory(new File(TEST_DATA_AREAS, "sunderland.batmap"), areasDir);

        assertTrue("sunderland.batmap should exist before migration",
                new File(areasDir, "sunderland.batmap").exists());

        AreaDataPersister.initialize(baseDir.getPath());

        assertFalse("sunderland.batmap should be gone after migration",
                new File(areasDir, "sunderland.batmap").exists());
        assertTrue("sunderland.batmap.migrated should exist after migration",
                new File(areasDir, "sunderland.batmap.migrated").exists());
    }

    @Test
    public void migration_sunderlandLoadsCorrectly() throws Exception {
        File baseDir = setUpMigrationDir("migSunderland");
        File areasDir = new File(new File(baseDir, "conf"), "batMapAreas");
        FileUtils.copyFileToDirectory(new File(TEST_DATA_AREAS, "sunderland.batmap"), areasDir);

        AreaDataPersister.initialize(baseDir.getPath());

        AreaSaveObject aso = AreaDataPersister.loadData(baseDir.getPath(), "sunderland");
        assertNotNull("sunderland should load from SQLite", aso);
        assertTrue("sunderland should have rooms",
                aso.getGraph().getVertexCount() > 0);
        assertTrue("sunderland should have edges",
                aso.getGraph().getEdgeCount() > 0);

        // All rooms should have area set to "sunderland"
        for (Room room : aso.getGraph().getVertices()) {
            assertNotNull("Room should have area", room.getArea());
            assertEquals("sunderland", room.getArea().getName());
        }

        // Locations map should use the same Room instances as the graph
        Map<Room, Point2D> locations = aso.getLocations();
        Collection<Room> vertices = aso.getGraph().getVertices();
        for (Map.Entry<Room, Point2D> entry : locations.entrySet()) {
            assertTrue("Location map room must be a graph vertex (same instance)",
                    vertices.contains(entry.getKey()));
            assertNotNull("Location should not be null", entry.getValue());
        }

        // Every edge should have valid endpoints that are graph vertices
        for (Exit exit : aso.getGraph().getEdges()) {
            Pair<Room> endpoints = aso.getGraph().getEndpoints(exit);
            assertNotNull(endpoints);
            assertTrue("Edge source should be a graph vertex",
                    vertices.contains(endpoints.getFirst()));
            assertTrue("Edge target should be a graph vertex",
                    vertices.contains(endpoints.getSecond()));
        }
    }

    @Test
    public void migration_roundTripFidelity() throws Exception {
        // Load from legacy, migrate, load from SQLite, compare counts
        File baseDir = setUpMigrationDir("migFidelity");
        File areasDir = new File(new File(baseDir, "conf"), "batMapAreas");
        FileUtils.copyFileToDirectory(new File(TEST_DATA_AREAS, "sunderland.batmap"), areasDir);

        // Load via legacy deserialization first (before migration)
        AreaSaveObject legacy = AreaDataPersister.loadData(baseDir.getPath(), "sunderland");
        assertNotNull(legacy);
        int legacyVertices = legacy.getGraph().getVertexCount();
        int legacyEdges = legacy.getGraph().getEdgeCount();
        int legacyLocations = legacy.getLocations().size();

        // Now initialize to trigger migration
        AreaDataPersister.initialize(baseDir.getPath());

        // Load from SQLite
        AreaSaveObject sqlite = AreaDataPersister.loadData(baseDir.getPath(), "sunderland");
        assertNotNull(sqlite);

        assertEquals("Vertex count should match after migration",
                legacyVertices, sqlite.getGraph().getVertexCount());
        assertEquals("Edge count should match after migration",
                legacyEdges, sqlite.getGraph().getEdgeCount());
        assertEquals("Location count should match after migration",
                legacyLocations, sqlite.getLocations().size());

        // Verify room fields are preserved: check each room by ID
        Map<String, Room> legacyRooms = new HashMap<>();
        for (Room r : legacy.getGraph().getVertices()) {
            legacyRooms.put(r.getId(), r);
        }
        for (Room sqliteRoom : sqlite.getGraph().getVertices()) {
            Room legacyRoom = legacyRooms.get(sqliteRoom.getId());
            assertNotNull("Room " + sqliteRoom.getId() + " should exist in legacy data", legacyRoom);
            assertEquals("shortDesc should match for " + sqliteRoom.getId(),
                    legacyRoom.getShortDesc(), sqliteRoom.getShortDesc());
            assertEquals("longDesc should match for " + sqliteRoom.getId(),
                    legacyRoom.getLongDesc(), sqliteRoom.getLongDesc());
            assertEquals("notes should match for " + sqliteRoom.getId(),
                    legacyRoom.getNotes(), sqliteRoom.getNotes());
            assertEquals("label should match for " + sqliteRoom.getId(),
                    legacyRoom.getLabel(), sqliteRoom.getLabel());
            assertEquals("isAreaEntrance should match for " + sqliteRoom.getId(),
                    legacyRoom.isAreaEntrance(), sqliteRoom.isAreaEntrance());
            assertEquals("isIndoors should match for " + sqliteRoom.getId(),
                    legacyRoom.isIndoors(), sqliteRoom.isIndoors());
            assertEquals("color should match for " + sqliteRoom.getId(),
                    legacyRoom.getColor(), sqliteRoom.getColor());
            assertEquals("exits should match for " + sqliteRoom.getId(),
                    legacyRoom.getExits(), sqliteRoom.getExits());
        }
    }

    @Test
    public void migration_searchAcrossMigratedAreas() throws Exception {
        File baseDir = setUpMigrationDir("migSearch");
        File areasDir = new File(new File(baseDir, "conf"), "batMapAreas");
        FileUtils.copyFileToDirectory(new File(TEST_DATA_AREAS, "sunderland.batmap"), areasDir);
        FileUtils.copyFileToDirectory(new File(TEST_DATA_AREAS, "batcity.batmap"), areasDir);

        AreaDataPersister.initialize(baseDir.getPath());

        List<SqliteAreaDatabase.SearchResult> results = AreaDataPersister.searchRooms("road");
        assertNotNull(results);
        assertTrue("Search across migrated data should find results", results.size() > 0);

        // Results should reference areas we migrated
        boolean foundSunderland = false;
        boolean foundBatcity = false;
        for (SqliteAreaDatabase.SearchResult r : results) {
            if ("sunderland".equals(r.getAreaName())) foundSunderland = true;
            if ("batcity".equals(r.getAreaName())) foundBatcity = true;
        }
        assertTrue("Search should find results from sunderland or batcity",
                foundSunderland || foundBatcity);
    }

    @Test
    public void migration_idempotent_secondInitSkipsMigrated() throws Exception {
        File baseDir = setUpMigrationDir("migIdem");
        File areasDir = new File(new File(baseDir, "conf"), "batMapAreas");
        FileUtils.copyFileToDirectory(new File(TEST_DATA_AREAS, "sunderland.batmap"), areasDir);

        // First init: migrates
        AreaDataPersister.initialize(baseDir.getPath());
        List<String> names1 = AreaDataPersister.listAreaNames(baseDir.getPath());
        assertEquals(1, names1.size());
        AreaDataPersister.shutdown();

        // .batmap should be gone, .batmap.migrated should exist
        assertFalse(new File(areasDir, "sunderland.batmap").exists());
        assertTrue(new File(areasDir, "sunderland.batmap.migrated").exists());

        // Second init: should be a no-op for migration
        AreaDataPersister.initialize(baseDir.getPath());
        List<String> names2 = AreaDataPersister.listAreaNames(baseDir.getPath());
        assertEquals(1, names2.size());
        assertEquals("sunderland", names2.get(0));

        // Data should still be intact
        AreaSaveObject aso = AreaDataPersister.loadData(baseDir.getPath(), "sunderland");
        assertNotNull(aso);
        assertTrue(aso.getGraph().getVertexCount() > 0);
    }

    @Test
    public void migration_backupCreatedOnSecondInit() throws Exception {
        File baseDir = setUpMigrationDir("migBackup");
        File areasDir = new File(new File(baseDir, "conf"), "batMapAreas");
        FileUtils.copyFileToDirectory(new File(TEST_DATA_AREAS, "sunderland.batmap"), areasDir);

        // First init: creates DB, no backup (DB was empty/new)
        AreaDataPersister.initialize(baseDir.getPath());
        AreaDataPersister.shutdown();

        File confDir = new File(baseDir, "conf");
        File dbFile = new File(confDir, "batmapper.db");
        assertTrue(dbFile.exists());
        assertTrue(dbFile.length() > 0);

        // Second init: DB exists and has data, so backup should be created
        AreaDataPersister.initialize(baseDir.getPath());

        File backupFile = new File(confDir, "batmapper.db.bk");
        assertTrue("Backup should be created on second init", backupFile.exists());
        assertTrue("Backup should not be empty", backupFile.length() > 0);
    }
}
