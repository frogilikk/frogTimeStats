package com.frogilik.timestats.model;

public class AppSettings {

    private ThemePalette currentTheme = ThemePalette.FROG; // Тема по умолчанию
    private boolean minimizeToTray = false;
    private boolean startWithSystem = false;

    public AppSettings() {}

    public ThemePalette getCurrentTheme() {
        return currentTheme;
    }

    public void setCurrentTheme(ThemePalette currentTheme) {
        this.currentTheme = currentTheme;
    }

    public boolean isMinimizeToTray() {
        return minimizeToTray;
    }

    public void setMinimizeToTray(boolean minimizeToTray) {
        this.minimizeToTray = minimizeToTray;
    }

    public boolean isStartWithSystem() {
        return startWithSystem;
    }

    public void setStartWithSystem(boolean startWithSystem) {
        this.startWithSystem = startWithSystem;
    }
}