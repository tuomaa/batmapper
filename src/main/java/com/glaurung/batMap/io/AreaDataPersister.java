package com.glaurung.batMap.io;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.glaurung.batMap.vo.AreaSaveObject;
import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.SparseMultigraph;

public class AreaDataPersister {

    private static final String SUFFIX = ".batmap";
    private static final String PATH = "batMapAreas";
    private static final String NEW_PATH = "conf";

    static SqliteAreaDatabase database;

    public static void initialize(String baseDir) {
        try {
            database = new SqliteAreaDatabase(baseDir);
            System.out.println("[BatMapper] SQLite database initialized at " + baseDir + "/conf/batmapper.db");
            // Backup database on startup
            File confDir = new File(baseDir, NEW_PATH);
            File dbFile = new File(confDir, "batmapper.db");
            if (dbFile.exists() && dbFile.length() > 0) {
                try {
                    database.backupTo(new File(confDir, "batmapper.db.bk").getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // Migrate legacy .batmap files
            migrateBatmapToSqlite(baseDir);
        } catch (Exception e) {
            System.err.println("[BatMapper] Failed to initialize SQLite database: " + e.getMessage());
            e.printStackTrace();
            database = null;
        }
    }

    public static void shutdown() {
        if (database != null) {
            database.close();
            database = null;
        }
    }

    public static void save(String basedir, SparseMultigraph<Room, Exit> graph, Layout<Room, Exit> layout) throws IOException {
        if (database != null) {
            String areaName = graph.getVertices().iterator().next().getArea().getName();
            Map<Room, Point2D> locations = new java.util.HashMap<>();
            for (Room room : graph.getVertices()) {
                Point2D coord = layout.transform(room);
                locations.put(room, coord);
            }
            try {
                database.saveArea(areaName, graph, locations);
            } catch (SQLException e) {
                throw new IOException("Failed to save area to SQLite: " + e.getMessage(), e);
            }
        } else {
            // Fallback to legacy save if database not initialized
            saveLegacy(basedir, graph, layout);
        }
    }

    public static AreaSaveObject loadData(String basedir, String areaName) throws IOException, ClassNotFoundException {
        if (database != null) {
            try {
                AreaSaveObject aso = database.loadArea(areaName);
                if (aso != null) {
                    return aso;
                }
                // Not in DB — try legacy file as fallback
                return loadLegacyData(basedir, areaName);
            } catch (SQLException e) {
                throw new IOException("Failed to load area from SQLite: " + e.getMessage(), e);
            }
        } else {
            return loadLegacyData(basedir, areaName);
        }
    }

    public static List<String> listAreaNames(String basedir) {
        if (database != null) {
            try {
                return database.listAreaNames();
            } catch (SQLException e) {
                e.printStackTrace();
                return new LinkedList<>();
            }
        }
        // Fallback: list from filesystem
        return listAreaNamesFromFiles(basedir);
    }

    /**
     * SQL-optimized cross-area room search.
     */
    public static List<SqliteAreaDatabase.SearchResult> searchRooms(String term) {
        if (database != null) {
            try {
                return database.searchRooms(term);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new LinkedList<>();
    }

    // --- Legacy methods (kept for migration and fallback) ---

    private static AreaSaveObject loadLegacyData(String basedir, String areaName) throws IOException, ClassNotFoundException {
        File dataFile = new File(getFileNameFrom(basedir, areaName));
        try (FileInputStream fileInputStream = new FileInputStream(dataFile);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            return (AreaSaveObject) objectInputStream.readObject();
        }
    }

    private static void saveLegacy(String basedir, SparseMultigraph<Room, Exit> graph, Layout<Room, Exit> layout) throws IOException {
        AreaSaveObject saveObject = new AreaSaveObject();
        saveObject.setGraph(graph);
        Map<Room, Point2D> locations = saveObject.getLocations();
        for (Room room : graph.getVertices()) {
            Point2D coord = layout.transform(room);
            locations.put(room, coord);
        }
        saveObject.setFileName(getFileNameFrom(basedir, graph.getVertices().iterator().next().getArea().getName()));

        File baseFile = new File(saveObject.getFileName());
        File target = null;
        long timestamp = Long.MAX_VALUE;
        if (baseFile.exists()) {
            for (int i = 0; i < 5; i++) {
                File backup = new File(saveObject.getFileName() + "." + i + ".bk");
                if (!backup.exists()) {
                    target = backup;
                    break;
                } else if (backup.lastModified() < timestamp) {
                    timestamp = backup.lastModified();
                    target = backup;
                }
            }
            if (target != null) {
                baseFile.renameTo(target);
            }
        }

        try (java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(new File(saveObject.getFileName()));
             java.io.ObjectOutputStream objectOutputStream = new java.io.ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(saveObject);
        }
    }

    private static List<String> listAreaNamesFromFiles(String basedir) {
        File newDir = new File(basedir, NEW_PATH);
        newDir = new File(newDir, PATH);
        File[] files = newDir.listFiles();
        LinkedList<String> names = new LinkedList<>();
        if (files != null) {
            for (File file : files) {
                if (FilenameUtils.getExtension(file.getName()).equals("batmap")) {
                    names.add(FilenameUtils.getBaseName(file.getName()));
                }
            }
        }
        return names;
    }

    static String getFileNameFrom(String basedir, String areaName) throws IOException {
        areaName = areaName.replaceAll("'", "");
        areaName = areaName.replaceAll("/", "");
        areaName = areaName + SUFFIX;
        File newDir = new File(basedir, NEW_PATH);
        newDir = new File(newDir, PATH);
        if (!newDir.exists()) {
            if (!newDir.mkdirs()) {
                throw new IOException(PATH + " doesn't exist and couldn't be created");
            }
        }
        return new File(newDir, areaName).getPath();
    }

    public static void migrateFilesToNewLocation(String basedir) {
        File oldDir = new File(PATH);
        File newDir = new File(basedir, NEW_PATH);
        newDir = new File(newDir, PATH);
        if (!oldDir.exists())
            return;
        Collection<File> oldDirFiles = FileUtils.listFiles(oldDir, null, false);

        try {
            if (oldDirFiles.size() == 0) {
                FileUtils.deleteDirectory(oldDir);
                return;
            }
            FileUtils.forceMkdir(newDir);
            for (File mapfile : oldDirFiles) {
                if (!FileUtils.directoryContains(newDir, mapfile)) {
                    FileUtils.moveFileToDirectory(mapfile, newDir, true);
                }
            }
            if (FileUtils.listFiles(oldDir, null, false).size() == 0) {
                FileUtils.deleteDirectory(oldDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Migrate legacy .batmap files to SQLite database.
     * Files are renamed to .batmap.migrated after successful migration.
     * Failed areas remain as .batmap and will retry next startup.
     */
    private static void migrateBatmapToSqlite(String baseDir) {
        File areasDir = new File(new File(baseDir, NEW_PATH), PATH);
        if (!areasDir.exists()) {
            return;
        }
        File[] files = areasDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!FilenameUtils.getExtension(file.getName()).equals("batmap")) {
                continue;
            }
            String areaName = FilenameUtils.getBaseName(file.getName());
            try {
                if (database.areaExists(areaName)) {
                    // Already migrated — rename the file
                    file.renameTo(new File(file.getPath() + ".migrated"));
                    continue;
                }
                // Load via legacy deserialization
                AreaSaveObject aso;
                try (FileInputStream fis = new FileInputStream(file);
                     ObjectInputStream ois = new ObjectInputStream(fis)) {
                    aso = (AreaSaveObject) ois.readObject();
                }
                database.importFromSaveObject(areaName, aso);
                file.renameTo(new File(file.getPath() + ".migrated"));
                System.out.println("Migrated area to SQLite: " + areaName);
            } catch (Exception e) {
                System.err.println("Failed to migrate area " + areaName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
