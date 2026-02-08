package com.glaurung.batMap.controller;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.glaurung.batMap.gui.corpses.CorpsePanel;
import com.glaurung.batMap.vo.Area;
import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.graph.SparseMultigraph;

import org.junit.Before;
import org.junit.Test;

public class MapperEngineTest {

    private MapperEngine engine;
    private SparseMultigraph<Room, Exit> graph;

    @Before
    public void setUp() {
        graph = new SparseMultigraph<>();
        engine = new MapperEngine(graph, null);
        engine.area = new Area("testarea");
    }

    private Set<String> exits(String... dirs) {
        Set<String> set = new HashSet<>();
        for (String d : dirs) {
            set.add(d);
        }
        return set;
    }

    @Test
    public void firstRoom_addsVertex() {
        boolean isNew = engine.moveToRoom("room1", "teleport", "long desc", "short desc", false, exits("north", "south"));
        assertTrue(isNew);
        assertEquals(1, graph.getVertexCount());
        assertEquals(0, graph.getEdgeCount());
    }

    @Test
    public void firstRoom_setsAreaEntrance() {
        engine.moveToRoom("room1", "enter", "long", "short", false, exits("north"));
        Room room = getRoom("room1");
        assertTrue(room.isAreaEntrance());
    }

    @Test
    public void firstRoom_setsCurrent() {
        engine.moveToRoom("room1", "enter", "long", "short", false, exits("north"));
        Room room = getRoom("room1");
        assertTrue(room.isCurrent());
        assertSame(room, engine.currentRoom);
    }

    @Test
    public void secondRoom_addsVertexAndEdge() {
        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits("north"));
        boolean isNew = engine.moveToRoom("room2", "north", "long2", "short2", false, exits("south"));
        assertTrue(isNew);
        assertEquals(2, graph.getVertexCount());
        assertEquals(1, graph.getEdgeCount());
    }

    @Test
    public void secondRoom_edgeHasCorrectDirection() {
        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits("north"));
        engine.moveToRoom("room2", "north", "long2", "short2", false, exits("south"));

        Room room1 = getRoom("room1");
        Room room2 = getRoom("room2");

        Collection<Exit> outEdges = graph.getOutEdges(room1);
        assertEquals(1, outEdges.size());
        Exit edge = outEdges.iterator().next();
        assertEquals("north", edge.getExit());
        assertSame(room2, graph.getDest(edge));
    }

    @Test
    public void revisitExistingRoom_noDuplicate() {
        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits("north"));
        engine.moveToRoom("room2", "north", "long2", "short2", false, exits("south"));
        engine.moveToRoom("room1", "south", "long1", "short1", false, exits("north"));

        assertEquals(2, graph.getVertexCount());
        // room1→room2 (north), room2→room1 (south)
        assertEquals(2, graph.getEdgeCount());
    }

    @Test
    public void duplicateExitName_notAdded() {
        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits("north"));
        engine.moveToRoom("room2", "north", "long2", "short2", false, exits("south"));
        engine.moveToRoom("room1", "south", "long1", "short1", false, exits("north"));
        // Moving north again from room1 to room2 — edge "north" already exists
        engine.moveToRoom("room2", "north", "long2", "short2", false, exits("south"));

        assertEquals(2, graph.getVertexCount());
        assertEquals(2, graph.getEdgeCount());
    }

    @Test
    public void teleport_createsVertexWithNoEdge() {
        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits("north"));
        engine.moveToRoom("room2", "teleport", "long2", "short2", false, exits("south"));

        assertEquals(2, graph.getVertexCount());
        assertEquals(0, graph.getEdgeCount());
    }

    @Test
    public void selfLoop() {
        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits("north"));
        engine.moveToRoom("room1", "north", "long1", "short1", false, exits("north"));

        assertEquals(1, graph.getVertexCount());
        assertEquals(1, graph.getEdgeCount());

        Room room = getRoom("room1");
        Exit edge = graph.getOutEdges(room).iterator().next();
        assertSame(room, graph.getSource(edge));
        assertSame(room, graph.getDest(edge));
    }

    @Test
    public void moveToRoom_returnsTrueForNew_falseForExisting() {
        boolean first = engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits());
        assertTrue(first);

        boolean second = engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits());
        assertFalse(second);
    }

    @Test
    public void roomLabelExists_caseInsensitive() {
        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits());
        engine.setLabelToCurrentRoom("shop");

        assertTrue(engine.roomLabelExists("shop"));
        assertTrue(engine.roomLabelExists("Shop"));
        assertTrue(engine.roomLabelExists("SHOP"));
        assertFalse(engine.roomLabelExists("bank"));
    }

    @Test
    public void getLabels_formatted() {
        engine.moveToRoom("room1", "teleport", "long1", "A Room", false, exits());
        engine.currentRoom.setShortDesc("A Room");
        engine.setLabelToCurrentRoom("shop");

        List<String> labels = engine.getLabels();
        assertEquals(1, labels.size());
        assertTrue(labels.get(0).contains("shop"));
        assertTrue(labels.get(0).contains("A Room"));
    }

    @Test
    public void removeLabelFromCurrent() {
        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits());
        engine.setLabelToCurrentRoom("shop");
        assertTrue(engine.roomLabelExists("shop"));

        engine.removeLabelFromCurrent();
        assertFalse(engine.roomLabelExists("shop"));
    }

    @Test
    public void makeExitsStringFromPickedRoom() {
        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits());
        engine.moveToRoom("room2", "north", "long2", "short2", false, exits());

        Room room1 = getRoom("room1");
        String exitStr = engine.makeExitsStringFromPickedRoom(room1);
        // room1 has outEdge "north", so exits should contain "north"
        assertTrue(exitStr.contains("north"));
    }

    @Test
    public void mazeMode_tracksUsedExits() {
        engine.setMazeMode(true);

        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits());
        Room room1 = getRoom("room1");
        // Manually add exits to the room (the 6-arg moveToRoom doesn't populate exits from param)
        room1.addExit("north");
        room1.addExit("south");

        engine.moveToRoom("room2", "north", "long2", "short2", false, exits());
        // After moving north from room1, "north" should be marked as used on room1
        assertFalse(room1.allExitsHaveBeenUSed()); // "south" not used yet

        Room room2 = getRoom("room2");
        room2.addExit("south");
        engine.moveToRoom("room1", "south", "long1", "short1", false, exits());
        // Now room2 had "south" used, room1 has both exits used via graph edges
        assertTrue(room2.allExitsHaveBeenUSed());
    }

    @Test
    public void reversableDirsMode_addsReverseEdges() {
        engine.setReversableDirsMode(true);

        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits("north"));
        engine.moveToRoom("room2", "north", "long2", "short2", false, exits("south"));

        Room room1 = getRoom("room1");
        Room room2 = getRoom("room2");

        // Should have both room1→room2 (north) and room2→room1 (south)
        assertEquals(2, graph.getEdgeCount());

        Collection<Exit> room2Exits = graph.getOutEdges(room2);
        assertEquals(1, room2Exits.size());
        assertEquals("south", room2Exits.iterator().next().getExit());
    }

    @Test
    public void checkDirsFromCurrentRoomTo_longDirs() {
        engine.corpsePanel = new CorpsePanel("", null);

        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits("north"));
        engine.moveToRoom("room2", "north", "long2", "short2", false, exits("east"));
        engine.moveToRoom("room3", "east", "long3", "short3", false, exits());

        Room room1 = getRoom("room1");
        engine.currentRoom = room1;

        Room room3 = getRoom("room3");
        String dirs = engine.checkDirsFromCurrentRoomTo(room3, false);
        // Long dirs: "north;east;"
        assertEquals("north;east;", dirs);
    }

    @Test
    public void checkDirsFromCurrentRoomTo_shortDirs() {
        engine.corpsePanel = new CorpsePanel("", null);

        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits("north"));
        engine.moveToRoom("room2", "north", "long2", "short2", false, exits("north"));
        engine.moveToRoom("room3", "north", "long3", "short3", false, exits("south"));
        engine.moveToRoom("room4", "south", "long4", "short4", false, exits());

        Room room1 = getRoom("room1");
        engine.currentRoom = room1;

        Room room4 = getRoom("room4");
        String dirs = engine.checkDirsFromCurrentRoomTo(room4, true);
        // Path: north, north, south → compressed: "2 n;s;"
        assertEquals("2 n;s;", dirs);
    }

    @Test
    public void setRoomDescsForRoom_setsAllFields() {
        engine.moveToRoom("room1", "teleport", "long1", "short1", false, exits());
        Room room = getRoom("room1");
        // 6-arg moveToRoom does not set descs; setRoomDescsForRoom does
        engine.setRoomDescsForRoom(room, "A long description", "A Short Desc", true, exits("north", "east"));
        assertEquals("A Short Desc", room.getShortDesc());
        assertEquals("A long description", room.getLongDesc());
        assertTrue(room.isIndoors());
        assertTrue(room.getExits().contains("north"));
        assertTrue(room.getExits().contains("east"));
    }

    private Room getRoom(String id) {
        for (Room room : graph.getVertices()) {
            if (room.getId().equals(id)) {
                return room;
            }
        }
        return null;
    }
}
