package com.glaurung.batMap.gui;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;

import com.glaurung.batMap.vo.Exit;

import org.junit.Test;

public class DrawingUtilsTest {

    private static final double DELTA = 0.001;
    private static final Point2D ORIGIN = new Point2D.Double(0, 0);

    @Test
    public void roomSizeIs90() {
        assertEquals(90, DrawingUtils.ROOM_SIZE);
    }

    @Test
    public void north_noSnap() {
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("north"), false);
        assertEquals(0, result.getX(), DELTA);
        assertEquals(-180, result.getY(), DELTA);
    }

    @Test
    public void south_noSnap() {
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("south"), false);
        assertEquals(0, result.getX(), DELTA);
        assertEquals(180, result.getY(), DELTA);
    }

    @Test
    public void east_noSnap() {
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("east"), false);
        assertEquals(180, result.getX(), DELTA);
        assertEquals(0, result.getY(), DELTA);
    }

    @Test
    public void west_noSnap() {
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("west"), false);
        assertEquals(-180, result.getX(), DELTA);
        assertEquals(0, result.getY(), DELTA);
    }

    @Test
    public void northeast_noSnap() {
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("northeast"), false);
        assertEquals(180, result.getX(), DELTA);
        assertEquals(-180, result.getY(), DELTA);
    }

    @Test
    public void northwest_noSnap() {
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("northwest"), false);
        assertEquals(-180, result.getX(), DELTA);
        assertEquals(-180, result.getY(), DELTA);
    }

    @Test
    public void southeast_noSnap() {
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("southeast"), false);
        assertEquals(180, result.getX(), DELTA);
        assertEquals(180, result.getY(), DELTA);
    }

    @Test
    public void southwest_noSnap() {
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("southwest"), false);
        assertEquals(-180, result.getX(), DELTA);
        assertEquals(180, result.getY(), DELTA);
    }

    @Test
    public void up_noSnap() {
        // up without snap: compassDir starts with "u", not "n"/"s" → y unchanged
        // compassDir doesn't end with "e"/"w" → x unchanged
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("up"), false);
        assertEquals(0, result.getX(), DELTA);
        assertEquals(0, result.getY(), DELTA);
    }

    @Test
    public void down_noSnap() {
        // down without snap: compassDir starts with "d", not "n"/"s" → y unchanged
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("down"), false);
        assertEquals(0, result.getX(), DELTA);
        assertEquals(0, result.getY(), DELTA);
    }

    @Test
    public void up_snapMode() {
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("up"), true);
        // snap: x = 15 * 90 = 1350, y = -16 * 90 = -1440
        assertEquals(1350, result.getX(), DELTA);
        assertEquals(-1440, result.getY(), DELTA);
    }

    @Test
    public void down_snapMode() {
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("down"), true);
        // snap: x = -16 * 90 = -1440, y = 15 * 90 = 1350
        assertEquals(-1440, result.getX(), DELTA);
        assertEquals(1350, result.getY(), DELTA);
    }

    @Test
    public void nonCompassExit_noSnap() {
        // Non-compass exit: compassDir is null → x + 3*90 = 270, y - 3*90 = -270
        Point2D result = DrawingUtils.getRelativePosition(ORIGIN, new Exit("tunnel"), false);
        assertEquals(270, result.getX(), DELTA);
        assertEquals(-270, result.getY(), DELTA);
    }

    @Test
    public void nonOriginStartingPoint() {
        Point2D start = new Point2D.Double(100, 200);
        Point2D result = DrawingUtils.getRelativePosition(start, new Exit("north"), false);
        assertEquals(100, result.getX(), DELTA);
        assertEquals(20, result.getY(), DELTA);  // 200 - 180 = 20
    }
}
