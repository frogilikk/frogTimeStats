package com.frogilik.timestats.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class LinuxWindowTracker implements WindowTracker {

    private String currentProcess = "desktop";
    private String currentTitle = "KDE Desktop";
    private String currentIconName = null;
    private long lastUpdateTimestamp = 0;

    @Override
    public String getActiveProcessName() {
        updateWindowInfoIfNeeded();
        return currentProcess;
    }

    @Override
    public String getActiveWindowTitle() {
        updateWindowInfoIfNeeded();
        return currentTitle;
    }

    @Override
    public String getActiveProcessPath() {
        updateWindowInfoIfNeeded();
        return currentIconName; // Возвращаем системное имя иконки или путь
    }

    private synchronized void updateWindowInfoIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTimestamp < 300) {
            return;
        }
        lastUpdateTimestamp = now;

        try {
            // 1. Запрашиваем заголовок
            Process titleProc = Runtime.getRuntime().exec(new String[]{"kdotool", "getactivewindow", "getwindowname"});
            String title = readFirstLine(titleProc);

            // 2. Запрашиваем класс приложения (WM_CLASS)
            Process classProc = Runtime.getRuntime().exec(new String[]{"kdotool", "getactivewindow", "getwindowclassname"});
            String appClass = readFirstLine(classProc);

            if (title != null && !title.isBlank()) {
                this.currentTitle = title;

                if (appClass != null && !appClass.isBlank()) {
                    this.currentProcess = appClass.toLowerCase();
                    // Сохраняем класс окна как идеальную подсказку для иконки
                    this.currentIconName = appClass;
                } else {
                    this.currentProcess = extractAppNameFromTitle(title);
                    this.currentIconName = this.currentProcess;
                }
                return;
            }

            // Fallback через xprop для X11 / XWayland (дает точное имя иконки _NET_WM_ICON_NAME или WM_CLASS)
            if (tryXprop()) return;

        } catch (Exception ignored) {}

        this.currentProcess = "plasmashell";
        this.currentTitle = "KDE Desktop";
        this.currentIconName = "plasmashell";
    }

    private boolean tryXprop() {
        try {
            // Получаем ID активного окна
            Process idProc = Runtime.getRuntime().exec(new String[]{"xdotool", "getactivewindow"});
            String windowId = readFirstLine(idProc);

            if (windowId != null && !windowId.isBlank()) {
                // Достаем класс и заголовок через xprop
                Process xpropProc = Runtime.getRuntime().exec(new String[]{"xprop", "-id", windowId, "WM_CLASS"});
                String wmClassLine = readFirstLine(xpropProc);

                if (wmClassLine != null && wmClassLine.contains("=")) {
                    // xprop возвращает: WM_CLASS(STRING) = "nav-history", "Firefox"
                    String[] parts = wmClassLine.split("=");
                    String[] classes = parts[1].replace("\"", "").trim().split(",");
                    if (classes.length > 0) {
                        this.currentProcess = classes[classes.length - 1].trim().toLowerCase();
                        this.currentIconName = classes[0].trim(); // Первое значение — часто точное имя иконки
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private String extractAppNameFromTitle(String title) {
        if (title.contains(" — ")) {
            String[] parts = title.split(" — ");
            return parts[parts.length - 1].toLowerCase().trim();
        }
        if (title.contains(" - ")) {
            String[] parts = title.split(" - ");
            return parts[parts.length - 1].toLowerCase().trim();
        }
        return title.toLowerCase().trim();
    }

    private String readFirstLine(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            process.waitFor();
            return (line != null) ? line.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }
}