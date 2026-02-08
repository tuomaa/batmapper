package com.glaurung.batMap.vo;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

public class RoomTest {

    @Test
    public void equals_sameId() {
        Room r1 = new Room("Short A", "id1");
        Room r2 = new Room("Short B", "id1");
        assertTrue(r1.equals(r2));
    }

    @Test
    public void equals_differentId() {
        Room r1 = new Room("Short A", "id1");
        Room r2 = new Room("Short A", "id2");
        assertFalse(r1.equals(r2));
    }

    @Test
    public void equals_nonRoomObject() {
        Room r1 = new Room("Short A", "id1");
        assertFalse(r1.equals("id1"));
    }

    @Test
    public void equals_null() {
        Room r1 = new Room("Short A", "id1");
        assertFalse(r1.equals(null));
    }

    @Test
    public void allExitsHaveBeenUsed_noExits() {
        Room room = new Room("desc", "id1");
        // No exits defined and no exits used — both empty sets match
        assertTrue(room.allExitsHaveBeenUSed());
    }

    @Test
    public void allExitsHaveBeenUsed_someUsed() {
        Room room = new Room("desc", "id1");
        room.addExit("north");
        room.addExit("south");
        room.useExit("north");
        assertFalse(room.allExitsHaveBeenUSed());
    }

    @Test
    public void allExitsHaveBeenUsed_allUsed() {
        Room room = new Room("desc", "id1");
        room.addExit("north");
        room.addExit("south");
        room.useExit("north");
        room.useExit("south");
        assertTrue(room.allExitsHaveBeenUSed());
    }

    @Test
    public void allExitsHaveBeenUsed_extraUsedExit() {
        Room room = new Room("desc", "id1");
        room.addExit("north");
        room.useExit("north");
        room.useExit("east");
        // usedExits has "east" which is not in exits, so containsAll fails one way
        assertFalse(room.allExitsHaveBeenUSed());
    }

    @Test
    public void resetExitUsage() {
        Room room = new Room("desc", "id1");
        room.addExit("north");
        room.useExit("north");
        assertTrue(room.allExitsHaveBeenUSed());
        room.resetExitUsage();
        assertFalse(room.allExitsHaveBeenUSed());
    }

    @Test
    public void addExit() {
        Room room = new Room("desc", "id1");
        room.addExit("north");
        assertTrue(room.getExits().contains("north"));
        assertEquals(1, room.getExits().size());
    }

    @Test
    public void addExits() {
        Room room = new Room("desc", "id1");
        room.addExits(Arrays.asList("north", "south", "east"));
        assertEquals(3, room.getExits().size());
        assertTrue(room.getExits().containsAll(Arrays.asList("north", "south", "east")));
    }

    @Test
    public void setDescs() {
        Room room = new Room("old short", "id1");
        room.setDescs("new short", "new long");
        assertEquals("new short", room.getShortDesc());
        assertEquals("new long", room.getLongDesc());
    }

    @Test
    public void constructorTwoArgs() {
        Room room = new Room("Short Desc", "uid123");
        assertEquals("Short Desc", room.getShortDesc());
        assertEquals("uid123", room.getId());
        assertNull(room.getArea());
    }

    @Test
    public void constructorThreeArgs() {
        Area area = new Area("testarea");
        Room room = new Room("Short Desc", "uid123", area);
        assertEquals("Short Desc", room.getShortDesc());
        assertEquals("uid123", room.getId());
        assertEquals(area, room.getArea());
    }

    @Test
    public void constructorIdAndArea() {
        Area area = new Area("testarea");
        Room room = new Room("uid123", area);
        assertEquals("uid123", room.getId());
        assertEquals(area, room.getArea());
        assertNull(room.getShortDesc());
    }

    @Test
    public void defaultFlags() {
        Room room = new Room("desc", "id1");
        assertFalse(room.isAreaEntrance());
        assertFalse(room.isCurrent());
        assertFalse(room.isDrawn());
        assertFalse(room.isPicked());
        assertFalse(room.isIndoors());
        assertNull(room.getColor());
        assertNull(room.getNotes());
        assertNull(room.getLabel());
        assertNull(room.getLongDesc());
        assertNotNull(room.getExits());
        assertTrue(room.getExits().isEmpty());
    }

    @Test
    public void labelGetSet() {
        Room room = new Room("desc", "id1");
        room.setLabel("shop");
        assertEquals("shop", room.getLabel());
        room.setLabel(null);
        assertNull(room.getLabel());
    }

    @Test
    public void toStringReturnsShortDesc() {
        Room room = new Room("A lovely room", "id1");
        assertEquals("A lovely room", room.toString());
    }

    @Test
    public void setExitsReplacesSet() {
        Room room = new Room("desc", "id1");
        room.addExit("north");
        HashSet<String> newExits = new HashSet<>(Arrays.asList("east", "west"));
        room.setExits(newExits);
        assertEquals(2, room.getExits().size());
        assertFalse(room.getExits().contains("north"));
    }
}
