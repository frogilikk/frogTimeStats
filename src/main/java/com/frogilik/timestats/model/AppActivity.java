package com.frogilik.timestats.model;

import java.time.LocalDateTime;

public class AppActivity {

    private final String processName;
    private String exePath; // Новое поле для пути к .exe
    private String windowTitle;
    private long durationSeconds;
    private LocalDateTime lastActive;

    // Старый конструктор (для обратной совместимости)
    public AppActivity(String processName, String windowTitle, long durationSeconds, LocalDateTime lastActive) {
        this(processName, null, windowTitle, durationSeconds, lastActive);
    }

    // Новый конструктор с exePath
    public AppActivity(String processName, String exePath, String windowTitle, long durationSeconds, LocalDateTime lastActive) {
        this.processName = processName;
        this.exePath = exePath;
        this.windowTitle = windowTitle;
        this.durationSeconds = durationSeconds;
        this.lastActive = lastActive;
    }

    // Мутируем текущий объект — 0 новых аллокаций в Heap!
    public AppActivity addTime(long seconds) {
        this.durationSeconds += seconds;
        this.lastActive = LocalDateTime.now();
        return this;
    }

    public void updateTitle(String newTitle) {
        if (newTitle != null && !newTitle.isBlank()) {
            this.windowTitle = newTitle;
        }
    }

    public void updateExePath(String newExePath) {
        if (newExePath != null && !newExePath.isBlank()) {
            this.exePath = newExePath;
        }
    }

    // Геттеры для совместимости с кодом
    public String processName() { return processName; }
    public String getProcessName() { return processName; }

    public String exePath() { return exePath; }
    public String getExePath() { return exePath; }

    public String windowTitle() { return windowTitle; }
    public String getWindowTitle() { return windowTitle; }

    public long durationSeconds() { return durationSeconds; }
    public long getDurationSeconds() { return durationSeconds; }

    public LocalDateTime lastActive() { return lastActive; }
    public LocalDateTime getLastActive() { return lastActive; }
}