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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private ScheduledExecutorService scheduler;
    private volatile boolean isRunning = false;

    private LocalDate currentTrackingDate;
    private TimePeriod currentPeriod = TimePeriod.TODAY;

    private static final int CHECK_INTERVAL_MS = 1000;      // 1 секунда
    private static final int SAVE_INTERVAL_TICKS = 30;     // Автосохранение каждые 30 секунд
    private int ticksSinceLastSave = 0;

    public TrackingService(WindowTracker windowTracker, ActivityRepository repository) {
        this.windowTracker = windowTracker;
        this.repository = repository;
        this.currentTrackingDate = LocalDate.now();

        // Загружаем сохраненную за сегодня статистику из базы при старте
        Map<String, AppActivity> todayData = repository.getStatsForRange(currentTrackingDate, currentTrackingDate);
        if (todayData != null && !todayData.isEmpty()) {
            this.todayStats.putAll(todayData);
        }
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "time-tracker-thread");
            thread.setDaemon(true);
            return thread;
        });

        // Запуск периодического опроса раз в секунду
        scheduler.scheduleAtFixedRate(this::tickSafe, 0, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        System.out.println(">>> TrackingService успешно запущен.");
    }

    public synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Финальный скид данных в БД
        saveAllToDb();
        System.out.println(">>> TrackingService остановлен, данные сохранены.");
    }

    private void tickSafe() {
        try {
            tick();
        } catch (Exception e) {
            System.err.println("Ошибка при выполнении tick(): " + e.getMessage());
        }
    }

    private void tick() {
        LocalDate today = LocalDate.now();

        // Сброс при смене дня (полночь)
        if (!today.equals(currentTrackingDate)) {
            saveAllToDb();
            todayStats.clear();
            currentTrackingDate = today;
            ticksSinceLastSave = 0;
            return;
        }

        String process = windowTracker.getActiveProcessName();

        // Быстрая проверка имени процесса
        if (process == null || process.isBlank() || process.equalsIgnoreCase("Unknown")) {
            return;
        }

        // Достаем заголовок окна и полный путь к .exe
        String title = windowTracker.getActiveWindowTitle();
        String exePath = windowTracker.getActiveProcessPath();

        // Обновляем статистику за сегодня в ОЗУ
        todayStats.compute(process, (key, currentActivity) -> {
            if (currentActivity == null) {
                return new AppActivity(process, exePath, title, 1, LocalDateTime.now());
            } else {
                currentActivity.updateTitle(title);
                currentActivity.updateExePath(exePath);
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
        if (todayStats.isEmpty()) return;
        try {
            repository.saveOrUpdateAll(todayStats.values(), currentTrackingDate);
        } catch (Exception e) {
            System.err.println("Ошибка пакетного сохранения в БД: " + e.getMessage());
        }
    }

    public void setTimePeriod(TimePeriod period) {
        this.currentPeriod = period;
    }

    public TimePeriod getCurrentPeriod() {
        return currentPeriod;
    }

    public Map<String, AppActivity> getStats() {
        LocalDate now = LocalDate.now();

        switch (currentPeriod) {
            case TODAY:
                return Collections.unmodifiableMap(todayStats);

            case YESTERDAY:
                LocalDate yesterday = now.minusDays(1);
                Map<String, AppActivity> yesterdayStats = repository.getStatsForRange(yesterday, yesterday);
                return yesterdayStats != null ? yesterdayStats : Collections.emptyMap();

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

    private Map<String, AppActivity> mergeWithToday(Map<String, AppActivity> archivedStats) {
        Map<String, AppActivity> merged = (archivedStats != null) ? new HashMap<>(archivedStats) : new HashMap<>();

        todayStats.forEach((process, activity) -> {
            merged.compute(process, (k, existing) -> {
                if (existing == null) {
                    return activity;
                } else {
                    String exePath = (existing.getExePath() != null) ? existing.getExePath() : activity.getExePath();

                    return new AppActivity(
                            process,
                            exePath,
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