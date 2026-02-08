package com.glaurung.batMap.gui;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.glaurung.batMap.vo.Exit;

import org.junit.Test;

public class GraphUtilsTest {

    @Test
    public void canAddExit_nullCollection() {
        assertTrue(GraphUtils.canAddExit(null, "north"));
    }

    @Test
    public void canAddExit_emptyCollection() {
        Collection<Exit> exits = new ArrayList<>();
        assertTrue(GraphUtils.canAddExit(exits, "north"));
    }

    @Test
    public void canAddExit_existingSameCase() {
        Collection<Exit> exits = Arrays.asList(new Exit("north"));
        assertFalse(GraphUtils.canAddExit(exits, "north"));
    }

    @Test
    public void canAddExit_existingDifferentCase() {
        Collection<Exit> exits = Arrays.asList(new Exit("north"));
        assertFalse(GraphUtils.canAddExit(exits, "North"));
    }

    @Test
    public void canAddExit_differentExit() {
        Collection<Exit> exits = Arrays.asList(new Exit("north"));
        assertTrue(GraphUtils.canAddExit(exits, "south"));
    }

    @Test
    public void canAddExit_multipleExits() {
        Collection<Exit> exits = Arrays.asList(new Exit("north"), new Exit("south"), new Exit("east"));
        assertFalse(GraphUtils.canAddExit(exits, "south"));
        assertTrue(GraphUtils.canAddExit(exits, "west"));
    }
}
