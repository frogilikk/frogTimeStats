package com.frogilik.timestats.service;

import com.frogilik.timestats.core.WindowTracker;
import com.frogilik.timestats.model.AppActivity;
import com.frogilik.timestats.repository.ActivityRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrackingService {

    public enum TimePeriod {
        TODAY("Сегодня"),
        YESTERDAY("Вчера"),
        WEEK("За неделю"),
        MONTH("За месяц"),
        ALL_TIME("За все время");

        private final String title;
        TimePeriod(String title) { this.title = title; }
        public String getTitle() { return title; }
    }

    private final WindowTracker windowTracker;
    private final ActivityRepository repository;
    private final Map<String, AppActivity> todayStats = new ConcurrentHashMap<>();

    private volatile boolean isRunning = false;
    private LocalDate currentTrackingDate;
    private TimePeriod currentPeriod = TimePeriod.TODAY;

    private static final int CHECK_INTERVAL_MS = 1000;
    private static final int SAVE_INTERVAL_TICKS = 5;
    private int ticksSinceLastSave = 0;

    public TrackingService(WindowTracker windowTracker, ActivityRepository repository) {
        this.windowTracker = windowTracker;
        this.repository = repository;
        this.currentTrackingDate = LocalDate.now();

        Map<String, AppActivity> todayData = repository.getStatsForRange(currentTrackingDate, currentTrackingDate);
        if (todayData != null) {
            this.todayStats.putAll(todayData);
        }
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;

        Thread trackingThread = new Thread(() -> {
            while (isRunning) {
                try {
                    tick();
                    Thread.sleep(CHECK_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            saveAllToDb();
        }, "time-tracker-thread");

        trackingThread.setDaemon(true);
        trackingThread.start();
    }

    public synchronized void stop() {
        this.isRunning = false;
        saveAllToDb();
    }

    private void tick() {
        LocalDate today = LocalDate.now();

        // Сброс при смене дня
        if (!today.equals(currentTrackingDate)) {
            saveAllToDb();
            todayStats.clear();
            currentTrackingDate = today;
            ticksSinceLastSave = 0;
            return;
        }

        String process = windowTracker.getActiveProcessName();
        String title = windowTracker.getActiveWindowTitle();

        if (process == null || process.equalsIgnoreCase("Unknown") || process.isBlank()) {
            return;
        }

        // Обновляем счетчик ТОЛЬКО в оперативной памяти за СЕГОДНЯ
        todayStats.compute(process, (key, currentActivity) -> {
            if (currentActivity == null) {
                return new AppActivity(process, title, 1, LocalDateTime.now());
            } else {
                return currentActivity.addTime(1);
            }
        });

        ticksSinceLastSave++;
        if (ticksSinceLastSave >= SAVE_INTERVAL_TICKS) {
            saveAllToDb();
            ticksSinceLastSave = 0;
        }
    }

    private void saveAllToDb() {
        todayStats.values().forEach(activity -> repository.saveOrUpdate(activity, currentTrackingDate));
    }

    public void setTimePeriod(TimePeriod period) {
        this.currentPeriod = period;
    }

    public TimePeriod getCurrentPeriod() {
        return currentPeriod;
    }

    /**
     * Возвращает статистику в зависимости от выбранного временного периода.
     */
    public Map<String, AppActivity> getStats() {
        LocalDate now = LocalDate.now();

        switch (currentPeriod) {
            case TODAY:
                return Collections.unmodifiableMap(todayStats);

            case YESTERDAY:
                LocalDate yesterday = now.minusDays(1);
                return repository.getStatsForRange(yesterday, yesterday);

            case WEEK:
                LocalDate startOfWeek = now.with(DayOfWeek.MONDAY);
                Map<String, AppActivity> weekDbStats = repository.getStatsForRange(startOfWeek, now);
                return mergeWithToday(weekDbStats);

            case MONTH:
                LocalDate startOfMonth = now.withDayOfMonth(1);
                Map<String, AppActivity> monthDbStats = repository.getStatsForRange(startOfMonth, now);
                return mergeWithToday(monthDbStats);

            case ALL_TIME:
                Map<String, AppActivity> allDbStats = repository.getAllTimeStats();
                return mergeWithToday(allDbStats);

            default:
                return Collections.unmodifiableMap(todayStats);
        }
    }

    /**
     * Объединяет архивные данные из БД с несохраненными секундами текущей сессии в RAM
     */
    private Map<String, AppActivity> mergeWithToday(Map<String, AppActivity> archivedStats) {
        Map<String, AppActivity> merged = new HashMap<>(archivedStats);

        todayStats.forEach((process, activity) -> {
            merged.compute(process, (k, existing) -> {
                if (existing == null) {
                    return activity;
                } else {
                    return new AppActivity(
                            process,
                            activity.windowTitle(),
                            existing.durationSeconds() + activity.durationSeconds(),
                            LocalDateTime.now()
                    );
                }
            });
        });

        return merged;
    }
}