CREATE TABLE IF NOT EXISTS areas (
    name TEXT NOT NULL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS rooms (
    id            TEXT NOT NULL,
    area_name     TEXT NOT NULL REFERENCES areas(name) ON DELETE CASCADE,
    short_desc    TEXT,
    long_desc     TEXT,
    notes         TEXT,
    label         TEXT,
    area_entrance INTEGER NOT NULL DEFAULT 0,
    indoors       INTEGER NOT NULL DEFAULT 0,
    color_r       INTEGER,
    color_g       INTEGER,
    color_b       INTEGER,
    color_a       INTEGER,
    loc_x         REAL,
    loc_y         REAL,
    PRIMARY KEY (id, area_name)
);

CREATE TABLE IF NOT EXISTS room_exits (
    room_id   TEXT NOT NULL,
    area_name TEXT NOT NULL,
    exit_name TEXT NOT NULL,
    PRIMARY KEY (room_id, area_name, exit_name),
    FOREIGN KEY (room_id, area_name) REFERENCES rooms(id, area_name) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS room_used_exits (
    room_id   TEXT NOT NULL,
    area_name TEXT NOT NULL,
    exit_name TEXT NOT NULL,
    PRIMARY KEY (room_id, area_name, exit_name),
    FOREIGN KEY (room_id, area_name) REFERENCES rooms(id, area_name) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS graph_edges (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    area_name   TEXT NOT NULL,
    exit_name   TEXT NOT NULL,
    compass_dir TEXT,
    source_room TEXT NOT NULL,
    target_room TEXT NOT NULL,
    FOREIGN KEY (source_room, area_name) REFERENCES rooms(id, area_name) ON DELETE CASCADE,
    FOREIGN KEY (target_room, area_name) REFERENCES rooms(id, area_name) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_rooms_area ON rooms(area_name);

CREATE INDEX IF NOT EXISTS idx_edges_area ON graph_edges(area_name);
