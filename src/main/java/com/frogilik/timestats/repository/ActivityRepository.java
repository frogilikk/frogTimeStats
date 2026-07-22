package com.frogilik.timestats.repository;

import com.frogilik.timestats.model.AppActivity;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ActivityRepository {

    /**
     * Сохраняет статистику с указанием конкретной даты
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
     * Загружает данные за СЕГОДНЯ
     */
    public Map<String, AppActivity> loadTodayStats() {
        return getStatsForDate(LocalDate.now());
    }

    /**
     * Получить статистику за конкретный день (например, ВЧЕРА: LocalDate.now().minusDays(1))
     */
    public Map<String, AppActivity> getStatsForDate(LocalDate date) {
        Map<String, AppActivity> result = new HashMap<>();
        String sql = "SELECT process_name, window_title, duration_seconds, last_active FROM daily_activity WHERE date = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, date.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String process = rs.getString("process_name");
                String title = rs.getString("window_title");
                long duration = rs.getLong("duration_seconds");
                LocalDateTime lastActive = LocalDateTime.parse(rs.getString("last_active"));

                result.put(process, new AppActivity(process, title, duration, lastActive));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка чтения данных за дату: " + e.getMessage());
        }

        return result;
    }

    /**
     * СУММАРНАЯ статистика за последние N дней (например, за 7 дней)
     */
    public Map<String, Long> getAggregatedStatsForLastDays(int days) {
        Map<String, Long> result = new HashMap<>();
        LocalDate startDate = LocalDate.now().minusDays(days);

        String sql = """
            SELECT process_name, SUM(duration_seconds) as total_duration
            FROM daily_activity
            WHERE date >= ?
            GROUP BY process_name
            ORDER BY total_duration DESC;
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                result.put(rs.getString("process_name"), rs.getLong("total_duration"));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при подсчете аналитики за период: " + e.getMessage());
        }

        return result;
    }
}