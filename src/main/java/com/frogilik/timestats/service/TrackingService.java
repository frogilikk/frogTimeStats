package com.frogilik.timestats.service;

import com.frogilik.timestats.core.WindowTracker;
import com.frogilik.timestats.model.AppActivity;
import com.frogilik.timestats.repository.ActivityRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TrackingService {

    private final WindowTracker windowTracker;
    private final ActivityRepository repository;
    private final Map<String, AppActivity> stats = new HashMap<>();

    private boolean isRunning = false;
    private LocalDate currentTrackingDate; // Отслеживаем текущую дату
    private static final int CHECK_INTERVAL_MS = 1000;
    private static final int SAVE_INTERVAL_TICKS = 5;
    private int ticksSinceLastSave = 0;

    public TrackingService(WindowTracker windowTracker, ActivityRepository repository) {
        this.windowTracker = windowTracker;
        this.repository = repository;
        this.currentTrackingDate = LocalDate.now();

        // Загружаем сохранённое время за сегодня
        this.stats.putAll(repository.loadTodayStats());
    }

    public void start() {
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

    public void stop() {
        this.isRunning = false;
        saveAllToDb();
    }

    private void tick() {
        // Проверяем: не наступил ли новый день (00:00)?
        LocalDate today = LocalDate.now();
        if (!today.equals(currentTrackingDate)) {
            System.out.println("\n>>> Наступил новый день! Сохраняем вчерашние данные и обнуляем счетчики...");
            saveAllToDb();
            stats.clear();
            currentTrackingDate = today;
        }

        String process = windowTracker.getActiveProcessName();
        String title = windowTracker.getActiveWindowTitle();

        if (process == null || process.equalsIgnoreCase("Unknown") || process.isBlank()) {
            return;
        }

        AppActivity updatedActivity = stats.compute(process, (key, currentActivity) -> {
            if (currentActivity == null) {
                return new AppActivity(process, title, 1, LocalDateTime.now());
            } else {
                return currentActivity.addTime(1);
            }
        });

        ticksSinceLastSave++;
        if (ticksSinceLastSave >= SAVE_INTERVAL_TICKS) {
            if (updatedActivity != null) {
                repository.saveOrUpdate(updatedActivity, currentTrackingDate);
            }
            ticksSinceLastSave = 0;
        }
    }

    private void saveAllToDb() {
        stats.values().forEach(activity -> repository.saveOrUpdate(activity, currentTrackingDate));
    }

    public Map<String, AppActivity> getStats() {
        return Collections.unmodifiableMap(stats);
    }
}