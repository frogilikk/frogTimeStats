package com.frogilik.timestats.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:timestats.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initDatabase() {
        // Создаем таблицу с составным ключом (process_name + date)
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
            System.out.println(">>> База данных SQLite успешно инициализирована.");
        } catch (SQLException e) {
            System.err.println("Ошибка инициализации базы данных: " + e.getMessage());
        }
    }
}