package com.frogilik.timestats.model;

import java.time.LocalDateTime;

public record AppActivity(
        String processName,     // Например: "idea64.exe"
        String windowTitle,    // Например: "frogTimeStats – Main.java"
        long durationSeconds,  // Время в секундах
        LocalDateTime lastActive
) {
    // Удобный метод для "накапливания" времени
    public AppActivity addTime(long seconds) {
        return new AppActivity(
                this.processName,
                this.windowTitle,
                this.durationSeconds + seconds,
                LocalDateTime.now()
        );
    }
}