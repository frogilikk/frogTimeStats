package com.frogilik.timestats.repository;

import com.frogilik.timestats.model.AppActivity;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ActivityRepository {

    /**
     * Сохраняет или обновляет статистику процесса за конкретную дату.
     */
    public void saveOrUpdate(AppActivity activity, LocalDate date) {
        String sql = """
            INSERT INTO daily_activity (process_name, date, window_title, duration_seconds, last_active)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(process_name, date) DO UPDATE SET
                window_title = excluded.window_title,
                duration_seconds = excluded.duration_seconds,
                last_active = excluded.last_active;
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, activity.processName());
            pstmt.setString(2, date.toString());
            pstmt.setString(3, activity.windowTitle());
            pstmt.setLong(4, activity.durationSeconds());
            pstmt.setString(5, activity.lastActive().toString());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при сохранении в БД: " + e.getMessage());
        }
    }

    /**
     * Загружает данные за СЕГОДНЯ.
     */
    public Map<String, AppActivity> loadTodayStats() {
        return getStatsForDate(LocalDate.now());
    }

    /**
     * Получает статистику за конкретный один день (например, ВЧЕРА).
     */
    public Map<String, AppActivity> getStatsForDate(LocalDate date) {
        return getStatsForRange(date, date);
    }

    /**
     * Получает СУММАРНУЮ статистику за произвольный диапазон дат [startDate, endDate].
     * Суммирует duration_seconds для каждого процесса.
     */
    public Map<String, AppActivity> getStatsForRange(LocalDate startDate, LocalDate endDate) {
        Map<String, AppActivity> result = new HashMap<>();

        String sql = """
            SELECT process_name, 
                   MAX(window_title) AS window_title, 
                   SUM(duration_seconds) AS total_duration, 
                   MAX(last_active) AS max_last_active
            FROM daily_activity
            WHERE date BETWEEN ? AND ?
            GROUP BY process_name
            ORDER BY total_duration DESC;
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate.toString());
            pstmt.setString(2, endDate.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String process = rs.getString("process_name");
                String title = rs.getString("window_title");
                long duration = rs.getLong("total_duration");

                String lastActiveStr = rs.getString("max_last_active");
                LocalDateTime lastActive = lastActiveStr != null
                        ? LocalDateTime.parse(lastActiveStr)
                        : LocalDateTime.now();

                result.put(process, new AppActivity(process, title, duration, lastActive));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка чтения данных за диапазон дат: " + e.getMessage());
        }

        return result;
    }

    /**
     * Возвращает статистику за ВСЕ время работы приложения.
     */
    public Map<String, AppActivity> getAllTimeStats() {
        return getStatsForRange(LocalDate.of(2000, 1, 1), LocalDate.of(2099, 12, 31));
    }
}