package com.frogilik.timestats.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static String getDatabasePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        File dbFolder;

        if (os.contains("win")) {
            // C:\Users\ИмяПользователя\AppData\Roaming\frogTimeStats
            String appData = System.getenv("APPDATA");
            dbFolder = new File((appData != null ? appData : userHome), "frogTimeStats");
        } else {
            // ~/.config/frogTimeStats (Linux / macOS)
            dbFolder = new File(userHome, ".config/frogTimeStats");
        }

        // Автоматически создаем папку, если ее еще нет
        if (!dbFolder.exists()) {
            dbFolder.mkdirs();
        }

        return new File(dbFolder, "timestats.db").getAbsolutePath();
    }

    public static Connection getConnection() throws SQLException {
        String dbUrl = "jdbc:sqlite:" + getDatabasePath();
        return DriverManager.getConnection(dbUrl);
    }

    public static void initDatabase() {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS daily_activity (
                process_name TEXT NOT NULL,
                date TEXT NOT NULL,
                window_title TEXT,
                duration_seconds INTEGER NOT NULL,
                last_active TEXT NOT NULL,
                PRIMARY KEY (process_name, date)
            );
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            System.out.println(">>> База данных SQLite успешно инициализирована по пути: " + getDatabasePath());
        } catch (SQLException e) {
            System.err.println("Ошибка инициализации базы данных: " + e.getMessage());
        }
    }
}