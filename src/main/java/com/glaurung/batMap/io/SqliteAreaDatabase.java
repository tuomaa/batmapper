package com.glaurung.batMap.io;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.glaurung.batMap.vo.Area;
import com.glaurung.batMap.vo.AreaSaveObject;
import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

public class SqliteAreaDatabase {

    private Connection connection;
    private final String dbPath;

    public SqliteAreaDatabase(String baseDir) throws SQLException {
        // Explicitly load the JDBC driver — DriverManager's ServiceLoader
        // may not find it when loaded by BatClient's plugin classloader
        loadDriver();
        File confDir = new File(baseDir, "conf");
        if (!confDir.exists()) {
            confDir.mkdirs();
        }
        this.dbPath = new File(confDir, "batmapper.db").getPath();
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initSchema();
    }

    /**
     * Constructor that takes a direct path to a database file (for testing).
     */
    SqliteAreaDatabase(File dbFile) throws SQLException {
        loadDriver();
        this.dbPath = dbFile.getPath();
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initSchema();
    }

    private static void loadDriver() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
            stmt.execute("PRAGMA busy_timeout=5000");

            stmt.execute("CREATE TABLE IF NOT EXISTS areas ("
                    + "name TEXT NOT NULL PRIMARY KEY"
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS rooms ("
                    + "id            TEXT NOT NULL,"
                    + "area_name     TEXT NOT NULL REFERENCES areas(name) ON DELETE CASCADE,"
                    + "short_desc    TEXT,"
                    + "long_desc     TEXT,"
                    + "notes         TEXT,"
                    + "label         TEXT,"
                    + "area_entrance INTEGER NOT NULL DEFAULT 0,"
                    + "indoors       INTEGER NOT NULL DEFAULT 0,"
                    + "color_r       INTEGER,"
                    + "color_g       INTEGER,"
                    + "color_b       INTEGER,"
                    + "color_a       INTEGER,"
                    + "loc_x         REAL,"
                    + "loc_y         REAL,"
                    + "PRIMARY KEY (id, area_name)"
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS room_exits ("
                    + "room_id   TEXT NOT NULL,"
                    + "area_name TEXT NOT NULL,"
                    + "exit_name TEXT NOT NULL,"
                    + "PRIMARY KEY (room_id, area_name, exit_name),"
                    + "FOREIGN KEY (room_id, area_name) REFERENCES rooms(id, area_name) ON DELETE CASCADE"
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS room_used_exits ("
                    + "room_id   TEXT NOT NULL,"
                    + "area_name TEXT NOT NULL,"
                    + "exit_name TEXT NOT NULL,"
                    + "PRIMARY KEY (room_id, area_name, exit_name),"
                    + "FOREIGN KEY (room_id, area_name) REFERENCES rooms(id, area_name) ON DELETE CASCADE"
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS graph_edges ("
                    + "id          INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "area_name   TEXT NOT NULL,"
                    + "exit_name   TEXT NOT NULL,"
                    + "compass_dir TEXT,"
                    + "source_room TEXT NOT NULL,"
                    + "target_room TEXT NOT NULL,"
                    + "FOREIGN KEY (source_room, area_name) REFERENCES rooms(id, area_name) ON DELETE CASCADE,"
                    + "FOREIGN KEY (target_room, area_name) REFERENCES rooms(id, area_name) ON DELETE CASCADE"
                    + ")");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_rooms_area ON rooms(area_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_edges_area ON graph_edges(area_name)");
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveArea(String areaName, SparseMultigraph<Room, Exit> graph,
                         Map<Room, Point2D> locations) throws SQLException {
        boolean wasAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            // Delete existing data for this area (cascades to room_exits, room_used_exits, graph_edges)
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM rooms WHERE area_name = ?")) {
                ps.setString(1, areaName);
                ps.executeUpdate();
            }
            // Upsert area
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR IGNORE INTO areas (name) VALUES (?)")) {
                ps.setString(1, areaName);
                ps.executeUpdate();
            }

            // Insert rooms (OR IGNORE handles rare legacy data with duplicate room IDs)
            Set<String> seenRoomIds = new HashSet<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR IGNORE INTO rooms (id, area_name, short_desc, long_desc, notes, label, "
                            + "area_entrance, indoors, color_r, color_g, color_b, color_a, loc_x, loc_y) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                for (Room room : graph.getVertices()) {
                    ps.setString(1, room.getId());
                    ps.setString(2, areaName);
                    ps.setString(3, room.getShortDesc());
                    ps.setString(4, room.getLongDesc());
                    ps.setString(5, room.getNotes());
                    ps.setString(6, room.getLabel());
                    ps.setInt(7, room.isAreaEntrance() ? 1 : 0);
                    ps.setInt(8, room.isIndoors() ? 1 : 0);
                    Color color = room.getColor();
                    if (color != null) {
                        ps.setInt(9, color.getRed());
                        ps.setInt(10, color.getGreen());
                        ps.setInt(11, color.getBlue());
                        ps.setInt(12, color.getAlpha());
                    } else {
                        ps.setNull(9, java.sql.Types.INTEGER);
                        ps.setNull(10, java.sql.Types.INTEGER);
                        ps.setNull(11, java.sql.Types.INTEGER);
                        ps.setNull(12, java.sql.Types.INTEGER);
                    }
                    Point2D loc = locations != null ? locations.get(room) : null;
                    if (loc != null) {
                        ps.setDouble(13, loc.getX());
                        ps.setDouble(14, loc.getY());
                    } else {
                        ps.setNull(13, java.sql.Types.REAL);
                        ps.setNull(14, java.sql.Types.REAL);
                    }
                    ps.executeUpdate();
                    seenRoomIds.add(room.getId());
                }
            }

            // Insert room_exits (only for rooms we actually inserted)
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR IGNORE INTO room_exits (room_id, area_name, exit_name) VALUES (?, ?, ?)")) {
                for (Room room : graph.getVertices()) {
                    if (!seenRoomIds.contains(room.getId())) continue;
                    for (String exitName : room.getExits()) {
                        ps.setString(1, room.getId());
                        ps.setString(2, areaName);
                        ps.setString(3, exitName);
                        ps.executeUpdate();
                    }
                }
            }

            // Insert room_used_exits (only for rooms we actually inserted)
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR IGNORE INTO room_used_exits (room_id, area_name, exit_name) VALUES (?, ?, ?)")) {
                for (Room room : graph.getVertices()) {
                    if (!seenRoomIds.contains(room.getId())) continue;
                    for (String exitName : room.getUsedExits()) {
                        ps.setString(1, room.getId());
                        ps.setString(2, areaName);
                        ps.setString(3, exitName);
                        ps.executeUpdate();
                    }
                }
            }

            // Insert graph_edges
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO graph_edges (area_name, exit_name, compass_dir, source_room, target_room) "
                            + "VALUES (?, ?, ?, ?, ?)")) {
                for (Exit exit : graph.getEdges()) {
                    Pair<Room> endpoints = graph.getEndpoints(exit);
                    ps.setString(1, areaName);
                    ps.setString(2, exit.getExit());
                    ps.setString(3, exit.getCompassDir());
                    ps.setString(4, endpoints.getFirst().getId());
                    ps.setString(5, endpoints.getSecond().getId());
                    ps.executeUpdate();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(wasAutoCommit);
        }
    }

    public AreaSaveObject loadArea(String areaName) throws SQLException {
        Area area = new Area(areaName);

        // Check if area exists
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM areas WHERE name = ?")) {
            ps.setString(1, areaName);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
        }

        // Build rooms - one instance per ID
        Map<String, Room> roomMap = new HashMap<>();
        Map<Room, Point2D> locations = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, short_desc, long_desc, notes, label, area_entrance, indoors, "
                        + "color_r, color_g, color_b, color_a, loc_x, loc_y FROM rooms WHERE area_name = ?")) {
            ps.setString(1, areaName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                Room room = new Room(id, area);
                room.setShortDesc(rs.getString("short_desc"));
                room.setLongDesc(rs.getString("long_desc"));
                room.setNotes(rs.getString("notes"));
                room.setLabel(rs.getString("label"));
                room.setAreaEntrance(rs.getInt("area_entrance") == 1);
                room.setIndoors(rs.getInt("indoors") == 1);

                int colorR = rs.getInt("color_r");
                if (!rs.wasNull()) {
                    int colorG = rs.getInt("color_g");
                    int colorB = rs.getInt("color_b");
                    int colorA = rs.getInt("color_a");
                    room.setColor(new Color(colorR, colorG, colorB, colorA));
                }

                double locX = rs.getDouble("loc_x");
                if (!rs.wasNull()) {
                    double locY = rs.getDouble("loc_y");
                    locations.put(room, new Point2D.Double(locX, locY));
                }

                roomMap.put(id, room);
            }
        }

        // Load room_exits
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT room_id, exit_name FROM room_exits WHERE area_name = ?")) {
            ps.setString(1, areaName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Room room = roomMap.get(rs.getString("room_id"));
                if (room != null) {
                    room.addExit(rs.getString("exit_name"));
                }
            }
        }

        // Load room_used_exits
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT room_id, exit_name FROM room_used_exits WHERE area_name = ?")) {
            ps.setString(1, areaName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Room room = roomMap.get(rs.getString("room_id"));
                if (room != null) {
                    room.useExit(rs.getString("exit_name"));
                }
            }
        }

        // Build graph
        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<>();
        for (Room room : roomMap.values()) {
            graph.addVertex(room);
        }

        // Load graph_edges
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT exit_name, source_room, target_room FROM graph_edges WHERE area_name = ?")) {
            ps.setString(1, areaName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String exitName = rs.getString("exit_name");
                Room source = roomMap.get(rs.getString("source_room"));
                Room target = roomMap.get(rs.getString("target_room"));
                if (source != null && target != null) {
                    Exit exit = new Exit(exitName);
                    graph.addEdge(exit, new Pair<Room>(source, target), EdgeType.DIRECTED);
                }
            }
        }

        AreaSaveObject aso = new AreaSaveObject();
        aso.setGraph(graph);
        aso.setLocations(locations);
        return aso;
    }

    public List<String> listAreaNames() throws SQLException {
        List<String> names = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM areas ORDER BY name")) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }

    public boolean areaExists(String areaName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM areas WHERE name = ?")) {
            ps.setString(1, areaName);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    /**
     * Search rooms across all areas by matching short_desc or long_desc.
     * Returns lightweight SearchResult objects without reconstructing graphs.
     */
    public List<SearchResult> searchRooms(String term) throws SQLException {
        List<SearchResult> results = new ArrayList<>();
        String pattern = "%" + term.toLowerCase() + "%";
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT r.id, r.area_name, r.short_desc, r.long_desc "
                        + "FROM rooms r "
                        + "WHERE LOWER(r.short_desc) LIKE ? OR LOWER(r.long_desc) LIKE ?")) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new SearchResult(
                        rs.getString("id"),
                        rs.getString("area_name"),
                        rs.getString("short_desc"),
                        rs.getString("long_desc")
                ));
            }
        }
        return results;
    }

    /**
     * Import from a legacy AreaSaveObject (used during .batmap migration).
     */
    public void importFromSaveObject(String areaName, AreaSaveObject aso) throws SQLException {
        saveArea(areaName, aso.getGraph(), aso.getLocations());
    }

    /**
     * Backup the database using SQLite's backup API via a file copy.
     */
    public void backupTo(String path) throws SQLException, IOException {
        File backupFile = new File(path);
        if (backupFile.exists()) {
            backupFile.delete();
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("VACUUM INTO '" + path.replace("'", "''") + "'");
        }
    }

    /**
     * Lightweight search result — no graph needed.
     */
    public static class SearchResult {
        private final String roomId;
        private final String areaName;
        private final String shortDesc;
        private final String longDesc;

        public SearchResult(String roomId, String areaName, String shortDesc, String longDesc) {
            this.roomId = roomId;
            this.areaName = areaName;
            this.shortDesc = shortDesc;
            this.longDesc = longDesc;
        }

        public String getRoomId() { return roomId; }
        public String getAreaName() { return areaName; }
        public String getShortDesc() { return shortDesc; }
        public String getLongDesc() { return longDesc; }
    }
}
