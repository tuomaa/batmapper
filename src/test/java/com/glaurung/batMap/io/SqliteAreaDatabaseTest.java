package com.glaurung.batMap.io;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

public class SqliteAreaDatabaseTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private SqliteAreaDatabase db;

    @Before
    public void setUp() throws Exception {
        File dbFile = tempFolder.newFile("test.db");
        dbFile.delete(); // SqliteAreaDatabase will create it
        db = new SqliteAreaDatabase(dbFile);
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void roundTrip_singleRoomNoEdges() throws SQLException {
        Area area = new Area("testarea");
        Room room = new Room("room1", area);
        room.setShortDesc("A dark room");
        room.setLongDesc("You are in a dark room. It smells musty.");
        room.setIndoors(true);
        room.setAreaEntrance(true);
        room.setNotes("some notes");
        room.setLabel("start");
        room.setColor(new Color(255, 0, 0, 128));
        room.addExit("north");
        room.addExit("south");
        room.useExit("north");

        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        graph.addVertex(room);

        Map<Room, Point2D> locations = new HashMap<>();
        locations.put(room, new Point2D.Double(100.5, 200.75));

        db.saveArea("testarea", graph, locations);

        AreaSaveObject loaded = db.loadArea("testarea");
        assertNotNull(loaded);

        SparseMultigraph<Room, Exit> loadedGraph = loaded.getGraph();
        assertEquals(1, loadedGraph.getVertexCount());
        assertEquals(0, loadedGraph.getEdgeCount());

        Room loadedRoom = loadedGraph.getVertices().iterator().next();
        assertEquals("room1", loadedRoom.getId());
        assertEquals("A dark room", loadedRoom.getShortDesc());
        assertEquals("You are in a dark room. It smells musty.", loadedRoom.getLongDesc());
        assertTrue(loadedRoom.isIndoors());
        assertTrue(loadedRoom.isAreaEntrance());
        assertEquals("some notes", loadedRoom.getNotes());
        assertEquals("start", loadedRoom.getLabel());
        assertEquals(new Color(255, 0, 0, 128), loadedRoom.getColor());
        assertTrue(loadedRoom.getExits().contains("north"));
        assertTrue(loadedRoom.getExits().contains("south"));
        assertEquals(2, loadedRoom.getExits().size());
        assertTrue(loadedRoom.getUsedExits().contains("north"));
        assertEquals(1, loadedRoom.getUsedExits().size());
        assertEquals("testarea", loadedRoom.getArea().getName());

        Point2D loc = loaded.getLocations().get(loadedRoom);
        assertNotNull(loc);
        assertEquals(100.5, loc.getX(), 0.001);
        assertEquals(200.75, loc.getY(), 0.001);
    }

    @Test
    public void roundTrip_multipleRoomsWithEdges() throws SQLException {
        Area area = new Area("sunderland");
        Room room1 = new Room("uid1", area);
        room1.setShortDesc("Town square");
        room1.setLongDesc("A busy town square");
        room1.setAreaEntrance(true);
        room1.addExit("north");

        Room room2 = new Room("uid2", area);
        room2.setShortDesc("North road");
        room2.setLongDesc("A road heading north");
        room2.addExit("south");
        room2.addExit("north");

        Room room3 = new Room("uid3", area);
        room3.setShortDesc("Gate");
        room3.setLongDesc("A large gate");

        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        graph.addVertex(room1);
        graph.addVertex(room2);
        graph.addVertex(room3);

        Exit exit1 = new Exit("north");
        graph.addEdge(exit1, new Pair<Room>(room1, room2), EdgeType.DIRECTED);
        room1.addExit("north");

        Exit exit2 = new Exit("south");
        graph.addEdge(exit2, new Pair<Room>(room2, room1), EdgeType.DIRECTED);
        room2.addExit("south");

        Exit exit3 = new Exit("north");
        graph.addEdge(exit3, new Pair<Room>(room2, room3), EdgeType.DIRECTED);

        Map<Room, Point2D> locations = new HashMap<>();
        locations.put(room1, new Point2D.Double(100, 300));
        locations.put(room2, new Point2D.Double(100, 200));
        locations.put(room3, new Point2D.Double(100, 100));

        db.saveArea("sunderland", graph, locations);

        AreaSaveObject loaded = db.loadArea("sunderland");
        assertNotNull(loaded);

        SparseMultigraph<Room, Exit> loadedGraph = loaded.getGraph();
        assertEquals(3, loadedGraph.getVertexCount());
        assertEquals(3, loadedGraph.getEdgeCount());

        // Verify edge topology: find room1 and check its out-edges
        Room loadedRoom1 = null;
        Room loadedRoom2 = null;
        Room loadedRoom3 = null;
        for (Room r : loadedGraph.getVertices()) {
            if (r.getId().equals("uid1")) loadedRoom1 = r;
            else if (r.getId().equals("uid2")) loadedRoom2 = r;
            else if (r.getId().equals("uid3")) loadedRoom3 = r;
        }
        assertNotNull(loadedRoom1);
        assertNotNull(loadedRoom2);
        assertNotNull(loadedRoom3);

        // room1 -> room2 via "north"
        assertEquals(1, loadedGraph.getOutEdges(loadedRoom1).size());
        Exit outEdge1 = loadedGraph.getOutEdges(loadedRoom1).iterator().next();
        assertEquals("north", outEdge1.getExit());
        Pair<Room> endpoints1 = loadedGraph.getEndpoints(outEdge1);
        assertSame(loadedRoom1, endpoints1.getFirst());
        assertSame(loadedRoom2, endpoints1.getSecond());

        // room2 has 2 out-edges
        assertEquals(2, loadedGraph.getOutEdges(loadedRoom2).size());

        // Verify locations use same Room instances as graph vertices
        Map<Room, Point2D> loadedLocs = loaded.getLocations();
        assertNotNull(loadedLocs.get(loadedRoom1));
        assertNotNull(loadedLocs.get(loadedRoom2));
        assertNotNull(loadedLocs.get(loadedRoom3));
        assertEquals(100.0, loadedLocs.get(loadedRoom1).getX(), 0.001);
        assertEquals(300.0, loadedLocs.get(loadedRoom1).getY(), 0.001);
    }

    @Test
    public void roundTrip_nullDescsAndNoColor() throws SQLException {
        Area area = new Area("emptyarea");
        Room room = new Room("r1", area);
        // Leave shortDesc, longDesc, notes, label, color all null

        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        graph.addVertex(room);

        db.saveArea("emptyarea", graph, new HashMap<Room, Point2D>());

        AreaSaveObject loaded = db.loadArea("emptyarea");
        Room loadedRoom = loaded.getGraph().getVertices().iterator().next();
        assertNull(loadedRoom.getShortDesc());
        assertNull(loadedRoom.getLongDesc());
        assertNull(loadedRoom.getNotes());
        assertNull(loadedRoom.getLabel());
        assertNull(loadedRoom.getColor());
    }

    @Test
    public void loadArea_nonExistent() throws SQLException {
        assertNull(db.loadArea("does_not_exist"));
    }

    @Test
    public void listAreaNames_empty() throws SQLException {
        List<String> names = db.listAreaNames();
        assertTrue(names.isEmpty());
    }

    @Test
    public void listAreaNames_multipleSaved() throws SQLException {
        saveEmptyArea("beta");
        saveEmptyArea("alpha");
        saveEmptyArea("gamma");

        List<String> names = db.listAreaNames();
        assertEquals(3, names.size());
        // Should be alphabetically ordered
        assertEquals("alpha", names.get(0));
        assertEquals("beta", names.get(1));
        assertEquals("gamma", names.get(2));
    }

    @Test
    public void areaExists_trueAndFalse() throws SQLException {
        saveEmptyArea("exists");
        assertTrue(db.areaExists("exists"));
        assertFalse(db.areaExists("nope"));
    }

    @Test
    public void saveArea_overwritesPreviousData() throws SQLException {
        Area area = new Area("overwrite");
        Room room1 = new Room("r1", area);
        room1.setShortDesc("old desc");

        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        graph.addVertex(room1);
        db.saveArea("overwrite", graph, new HashMap<Room, Point2D>());

        // Overwrite with different data
        Room room2 = new Room("r2", area);
        room2.setShortDesc("new desc");
        SparseMultigraph<Room, Exit> graph2 = new SparseMultigraph<>();
        graph2.addVertex(room2);
        db.saveArea("overwrite", graph2, new HashMap<Room, Point2D>());

        AreaSaveObject loaded = db.loadArea("overwrite");
        assertEquals(1, loaded.getGraph().getVertexCount());
        Room loadedRoom = loaded.getGraph().getVertices().iterator().next();
        assertEquals("r2", loadedRoom.getId());
        assertEquals("new desc", loadedRoom.getShortDesc());
    }

    @Test
    public void searchRooms_matchesShortAndLongDesc() throws SQLException {
        Area area1 = new Area("area1");
        Room r1 = new Room("r1", area1);
        r1.setShortDesc("Marketplace");
        r1.setLongDesc("A bustling marketplace full of traders");

        Room r2 = new Room("r2", area1);
        r2.setShortDesc("Dark alley");
        r2.setLongDesc("A narrow dark alley");

        SparseMultigraph<Room, Exit> g1 = new SparseMultigraph<>();
        g1.addVertex(r1);
        g1.addVertex(r2);
        db.saveArea("area1", g1, new HashMap<Room, Point2D>());

        Area area2 = new Area("area2");
        Room r3 = new Room("r3", area2);
        r3.setShortDesc("Town market");
        r3.setLongDesc("The town market is quiet");
        SparseMultigraph<Room, Exit> g2 = new SparseMultigraph<>();
        g2.addVertex(r3);
        db.saveArea("area2", g2, new HashMap<Room, Point2D>());

        // Search for "market" — should match r1 (shortDesc), r3 (shortDesc), r1 (longDesc has "marketplace")
        List<SqliteAreaDatabase.SearchResult> results = db.searchRooms("market");
        // r1 matches on both shortDesc and longDesc but only appears once
        // r3 matches on shortDesc
        assertEquals(2, results.size());

        Set<String> foundIds = new HashSet<>();
        for (SqliteAreaDatabase.SearchResult sr : results) {
            foundIds.add(sr.getRoomId());
        }
        assertTrue(foundIds.contains("r1"));
        assertTrue(foundIds.contains("r3"));
    }

    @Test
    public void searchRooms_caseInsensitive() throws SQLException {
        Area area = new Area("test");
        Room r = new Room("r1", area);
        r.setShortDesc("The Dark Forest");
        r.setLongDesc("Tall trees block the light");
        SparseMultigraph<Room, Exit> g = new SparseMultigraph<>();
        g.addVertex(r);
        db.saveArea("test", g, new HashMap<Room, Point2D>());

        List<SqliteAreaDatabase.SearchResult> results = db.searchRooms("dark forest");
        assertEquals(1, results.size());
        assertEquals("r1", results.get(0).getRoomId());
    }

    @Test
    public void searchRooms_noMatches() throws SQLException {
        saveEmptyArea("empty");
        List<SqliteAreaDatabase.SearchResult> results = db.searchRooms("nonexistent");
        assertTrue(results.isEmpty());
    }

    @Test
    public void importFromSaveObject() throws SQLException {
        Area area = new Area("imported");
        Room room = new Room("uid1", area);
        room.setShortDesc("Imported room");
        room.setLongDesc("This was imported");

        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        graph.addVertex(room);

        AreaSaveObject aso = new AreaSaveObject();
        aso.setGraph(graph);
        aso.getLocations().put(room, new Point2D.Double(50, 75));

        db.importFromSaveObject("imported", aso);

        assertTrue(db.areaExists("imported"));
        AreaSaveObject loaded = db.loadArea("imported");
        assertEquals(1, loaded.getGraph().getVertexCount());
        Room loadedRoom = loaded.getGraph().getVertices().iterator().next();
        assertEquals("Imported room", loadedRoom.getShortDesc());
    }

    @Test
    public void roundTrip_roomIdentityForLocations() throws SQLException {
        // Verify that the Room instances used as keys in the locations map
        // are the exact same instances as the graph vertices
        Area area = new Area("identity");
        Room room = new Room("r1", area);
        room.setShortDesc("test");

        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        graph.addVertex(room);
        Map<Room, Point2D> locations = new HashMap<>();
        locations.put(room, new Point2D.Double(42, 84));

        db.saveArea("identity", graph, locations);
        AreaSaveObject loaded = db.loadArea("identity");

        Room graphVertex = loaded.getGraph().getVertices().iterator().next();
        // The locations map must be keyed by the same Room instance
        Point2D loc = loaded.getLocations().get(graphVertex);
        assertNotNull("Location map must use same Room instance as graph vertex", loc);
        assertEquals(42.0, loc.getX(), 0.001);
        assertEquals(84.0, loc.getY(), 0.001);
    }

    @Test
    public void roundTrip_parallelEdgesSameDirection() throws SQLException {
        // Two edges from the same source, both named "north" — SparseMultigraph allows this
        // but they go to different rooms
        Area area = new Area("parallel");
        Room r1 = new Room("r1", area);
        Room r2 = new Room("r2", area);
        Room r3 = new Room("r3", area);

        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        graph.addVertex(r1);
        graph.addVertex(r2);
        graph.addVertex(r3);

        Exit e1 = new Exit("north");
        Exit e2 = new Exit("south");
        graph.addEdge(e1, new Pair<Room>(r1, r2), EdgeType.DIRECTED);
        graph.addEdge(e2, new Pair<Room>(r1, r3), EdgeType.DIRECTED);

        Map<Room, Point2D> locations = new HashMap<>();
        locations.put(r1, new Point2D.Double(0, 0));
        locations.put(r2, new Point2D.Double(0, -90));
        locations.put(r3, new Point2D.Double(0, 90));

        db.saveArea("parallel", graph, locations);
        AreaSaveObject loaded = db.loadArea("parallel");

        assertEquals(3, loaded.getGraph().getVertexCount());
        assertEquals(2, loaded.getGraph().getEdgeCount());
    }

    private void saveEmptyArea(String name) throws SQLException {
        Area area = new Area(name);
        Room room = new Room("placeholder_" + name, area);
        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        graph.addVertex(room);
        db.saveArea(name, graph, new HashMap<Room, Point2D>());
    }
}
