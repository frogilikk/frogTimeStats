package com.frogilik.timestats.core;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class LinuxWindowTracker implements WindowTracker {

    private String currentProcess = "desktop";
    private String currentTitle = "KDE Desktop";

    @Override
    public String getActiveProcessName() {
        updateWindowInfo();
        return currentProcess;
    }

    @Override
    public String getActiveWindowTitle() {
        return currentTitle;
    }

    private void updateWindowInfo() {
        try {
            // 1. Запрашиваем заголовок окна через kdotool
            Process titleProc = Runtime.getRuntime().exec(new String[]{"kdotool", "getactivewindow", "getwindowname"});
            String title = readFirstLine(titleProc);

            // 2. Запрашиваем класс/имя приложения через kdotool
            Process classProc = Runtime.getRuntime().exec(new String[]{"kdotool", "getactivewindow", "getwindowclassname"});
            String appClass = readFirstLine(classProc);

            if (title != null && !title.isBlank()) {
                this.currentTitle = title;

                if (appClass != null && !appClass.isBlank()) {
                    this.currentProcess = appClass.toLowerCase();
                } else {
                    this.currentProcess = extractAppNameFromTitle(title);
                }
                return;
            }

            // Fallback для XWayland окон
            if (tryXdotool()) return;

        } catch (Exception ignored) {}

        this.currentProcess = "plasmashell";
        this.currentTitle = "KDE Desktop";
    }

    private boolean tryXdotool() {
        try {
            Process pidProc = Runtime.getRuntime().exec(new String[]{"xdotool", "getactivewindow", "getwindowpid"});
            String pid = readFirstLine(pidProc);

            if (pid != null && !pid.isBlank()) {
                Process commProc = Runtime.getRuntime().exec(new String[]{"cat", "/proc/" + pid + "/comm"});
                String processName = readFirstLine(commProc);

                Process titleProc = Runtime.getRuntime().exec(new String[]{"xdotool", "getactivewindow", "getwindowname"});
                String title = readFirstLine(titleProc);

                if (processName != null && !processName.isBlank()) {
                    this.currentProcess = processName;
                    this.currentTitle = (title != null && !title.isBlank()) ? title : processName;
                    return true;
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