package main.db;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_FILE = "quizpro.db";
    private static final String URL     = "jdbc:sqlite:" + DB_FILE;
    private static Connection   connection;

    // ── Init ──────────────────────────────────────────────────────────────

    public static void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(URL);
            execute("PRAGMA journal_mode=WAL;");
            execute("PRAGMA foreign_keys=ON;");
            createTables();
            migrateSchema();   // safely adds any columns added in later versions
            System.out.println("[DB] Initialized — " + DB_FILE);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "SQLite driver not found. Make sure sqlite-jdbc.jar is on the classpath.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    public static Connection get() {
        try {
            if (connection == null || connection.isClosed()) initialize();
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Database connection error: " + e.getMessage(), e);
        }
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException ignored) {}
    }

    // ── Schema creation ───────────────────────────────────────────────────

    private static void createTables() throws SQLException {

        execute(
            "CREATE TABLE IF NOT EXISTS users (" +
            "    id       INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    username TEXT    NOT NULL UNIQUE," +
            "    password TEXT    NOT NULL," +
            "    role     TEXT    NOT NULL CHECK(role IN ('STUDENT','TEACHER'))" +
            ");"
        );

        execute(
            "CREATE TABLE IF NOT EXISTS classes (" +
            "    id         INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    name       TEXT    NOT NULL," +
            "    teacher_id INTEGER NOT NULL," +
            "    FOREIGN KEY (teacher_id) REFERENCES users(id)" +
            ");"
        );

        execute(
            "CREATE TABLE IF NOT EXISTS class_students (" +
            "    class_id   INTEGER NOT NULL," +
            "    student_id INTEGER NOT NULL," +
            "    PRIMARY KEY (class_id, student_id)," +
            "    FOREIGN KEY (class_id)   REFERENCES classes(id)," +
            "    FOREIGN KEY (student_id) REFERENCES users(id)" +
            ");"
        );

        execute(
            "CREATE TABLE IF NOT EXISTS quizzes (" +
            "    id       INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    title    TEXT    NOT NULL," +
            "    class_id INTEGER NOT NULL DEFAULT 0" +
            ");"
        );

        execute(
            "CREATE TABLE IF NOT EXISTS class_quizzes (" +
            "    class_id INTEGER NOT NULL," +
            "    quiz_id  INTEGER NOT NULL," +
            "    PRIMARY KEY (class_id, quiz_id)," +
            "    FOREIGN KEY (class_id) REFERENCES classes(id)," +
            "    FOREIGN KEY (quiz_id)  REFERENCES quizzes(id)" +
            ");"
        );

        execute(
            "CREATE TABLE IF NOT EXISTS questions (" +
            "    id             INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    quiz_id        INTEGER NOT NULL," +
            "    position       INTEGER NOT NULL," +
            "    type           TEXT    NOT NULL CHECK(type IN ('MCQ','SUBJECTIVE'))," +
            "    question_text  TEXT    NOT NULL," +
            "    marks          INTEGER NOT NULL," +
            "    correct_answer TEXT," +
            "    FOREIGN KEY (quiz_id) REFERENCES quizzes(id)" +
            ");"
        );

        execute(
            "CREATE TABLE IF NOT EXISTS question_options (" +
            "    id          INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    question_id INTEGER NOT NULL," +
            "    position    INTEGER NOT NULL," +
            "    option_text TEXT    NOT NULL," +
            "    FOREIGN KEY (question_id) REFERENCES questions(id)" +
            ");"
        );

        execute(
            "CREATE TABLE IF NOT EXISTS attempts (" +
            "    id            INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    student_id    INTEGER NOT NULL," +
            "    quiz_id       INTEGER NOT NULL," +
            "    score         INTEGER NOT NULL," +
            "    total_marks   INTEGER NOT NULL," +
            "    pending_marks INTEGER NOT NULL DEFAULT 0," +
            "    violations    INTEGER NOT NULL DEFAULT 0," +
            "    attempted_at  INTEGER NOT NULL," +
            "    FOREIGN KEY (student_id) REFERENCES users(id)," +
            "    FOREIGN KEY (quiz_id)    REFERENCES quizzes(id)" +
            ");"
        );

        // manual_score: -1 = pending (subjective not yet graded) or N/A
        //               >= 0 = awarded marks
        execute(
            "CREATE TABLE IF NOT EXISTS attempt_answers (" +
            "    id           INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    attempt_id   INTEGER NOT NULL," +
            "    question_id  INTEGER NOT NULL," +
            "    answer_text  TEXT    NOT NULL DEFAULT ''," +
            "    manual_score INTEGER NOT NULL DEFAULT -1," +
            "    FOREIGN KEY (attempt_id)  REFERENCES attempts(id)," +
            "    FOREIGN KEY (question_id) REFERENCES questions(id)" +
            ");"
        );
    }

    // ── Schema migration ──────────────────────────────────────────────────
    /**
     * Safely adds any columns that may be missing from an older DB file.
     * Uses "ALTER TABLE ADD COLUMN" which silently does nothing if the
     * column already exists (we catch the error and ignore it).
     * This means existing data is NEVER lost when the schema evolves.
     */
    private static void migrateSchema() {
        // Add pending_marks to attempts if missing (added in v2)
        safeAddColumn("attempts",         "pending_marks", "INTEGER NOT NULL DEFAULT 0");
        // Add manual_score to attempt_answers if missing (added in v2)
        safeAddColumn("attempt_answers",  "manual_score",  "INTEGER NOT NULL DEFAULT -1");
    }

    private static void safeAddColumn(String table, String column, String definition) {
        try {
            execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition + ";");
            System.out.println("[DB] Migration: added column " + column + " to " + table);
        } catch (SQLException e) {
            // Column already exists — this is expected on any run after the first
            // SQLite error code 1 = generic error, message contains "duplicate column name"
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static void execute(String sql) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        }
    }

    /**
     * Returns the row ID of the last successful INSERT on this connection.
     * This is the correct way to get generated keys in SQLite JDBC.
     */
    public static int lastInsertId() throws SQLException {
        try (Statement st = get().createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static void beginTransaction() {
        try { get().setAutoCommit(false); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    public static void commit() {
        try { get().commit(); get().setAutoCommit(true); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    public static void rollback() {
        try { get().rollback(); get().setAutoCommit(true); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }
}
