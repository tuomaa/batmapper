package com.glaurung.batMap.vo;

import static org.junit.Assert.*;

import org.junit.Test;

public class ExitTest {

    @Test
    public void checkWhatExitIs_shortForms() {
        assertEquals("n", Exit.checkWhatExitIs("n"));
        assertEquals("e", Exit.checkWhatExitIs("e"));
        assertEquals("s", Exit.checkWhatExitIs("s"));
        assertEquals("w", Exit.checkWhatExitIs("w"));
        assertEquals("ne", Exit.checkWhatExitIs("ne"));
        assertEquals("nw", Exit.checkWhatExitIs("nw"));
        assertEquals("se", Exit.checkWhatExitIs("se"));
        assertEquals("sw", Exit.checkWhatExitIs("sw"));
        assertEquals("u", Exit.checkWhatExitIs("u"));
        assertEquals("d", Exit.checkWhatExitIs("d"));
    }

    @Test
    public void checkWhatExitIs_longForms() {
        assertEquals("n", Exit.checkWhatExitIs("north"));
        assertEquals("e", Exit.checkWhatExitIs("east"));
        assertEquals("s", Exit.checkWhatExitIs("south"));
        assertEquals("w", Exit.checkWhatExitIs("west"));
        assertEquals("ne", Exit.checkWhatExitIs("northeast"));
        assertEquals("nw", Exit.checkWhatExitIs("northwest"));
        assertEquals("se", Exit.checkWhatExitIs("southeast"));
        assertEquals("sw", Exit.checkWhatExitIs("southwest"));
        assertEquals("u", Exit.checkWhatExitIs("up"));
        assertEquals("d", Exit.checkWhatExitIs("down"));
    }

    @Test
    public void checkWhatExitIs_caseInsensitive() {
        assertEquals("n", Exit.checkWhatExitIs("North"));
        assertEquals("n", Exit.checkWhatExitIs("NORTH"));
        assertEquals("ne", Exit.checkWhatExitIs("NorthEast"));
    }

    @Test
    public void checkWhatExitIs_unrecognizedReturnsNull() {
        assertNull(Exit.checkWhatExitIs("tunnel"));
        assertNull(Exit.checkWhatExitIs("enter"));
        assertNull(Exit.checkWhatExitIs(""));
    }

    @Test
    public void getOpposite_allDirections() {
        assertEquals("south", new Exit("n").getOpposite());
        assertEquals("north", new Exit("s").getOpposite());
        assertEquals("west", new Exit("e").getOpposite());
        assertEquals("east", new Exit("w").getOpposite());
        assertEquals("southwest", new Exit("ne").getOpposite());
        assertEquals("northwest", new Exit("se").getOpposite());
        assertEquals("northeast", new Exit("sw").getOpposite());
        assertEquals("southeast", new Exit("nw").getOpposite());
        assertEquals("down", new Exit("u").getOpposite());
        assertEquals("up", new Exit("d").getOpposite());
    }

    @Test
    public void getOpposite_longFormInput() {
        assertEquals("south", new Exit("north").getOpposite());
        assertEquals("east", new Exit("west").getOpposite());
    }

    @Test(expected = NullPointerException.class)
    public void getOpposite_nonCompassDir_throwsNPE() {
        new Exit("tunnel").getOpposite();
    }

    @Test
    public void constructorSetsCompassDir() {
        Exit exit = new Exit("north");
        assertEquals("n", exit.getCompassDir());
        assertEquals("north", exit.getExit());
    }

    @Test
    public void constructorWithNonCompassDir() {
        Exit exit = new Exit("tunnel");
        assertNull(exit.getCompassDir());
        assertEquals("tunnel", exit.getExit());
    }

    @Test
    public void toStringReturnsExitString() {
        assertEquals("north", new Exit("north").toString());
        assertEquals("tunnel", new Exit("tunnel").toString());
    }

    @Test
    public void equals_sameName() {
        assertTrue(new Exit("north").equals(new Exit("north")));
    }

    @Test
    public void equals_differentName() {
        assertFalse(new Exit("north").equals(new Exit("south")));
    }

    @Test
    public void equals_shortVsLong_notEqual() {
        // equals compares raw exit string, not compassDir
        assertFalse(new Exit("north").equals(new Exit("n")));
    }

    @Test
    public void equals_nonExitObject() {
        assertFalse(new Exit("north").equals("north"));
    }

    @Test
    public void equals_null() {
        assertFalse(new Exit("north").equals(null));
    }

    @Test
    public void currentExitDefaultFalse() {
        Exit exit = new Exit("north");
        assertFalse(exit.isCurrentExit());
    }

    @Test
    public void setCurrentExit() {
        Exit exit = new Exit("north");
        exit.setCurrentExit(true);
        assertTrue(exit.isCurrentExit());
    }

    @Test
    public void setExit() {
        Exit exit = new Exit("north");
        exit.setExit("south");
        assertEquals("south", exit.getExit());
    }
}
