package com.frogilik.timestats.repository;

import com.frogilik.timestats.model.AppSettings;
import com.frogilik.timestats.model.ThemePalette;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SettingsRepository {

    private final Path configPath;

    public SettingsRepository() {
        String userHome = System.getProperty("user.home");
        Path configDir = Paths.get(userHome, ".frogTimeStats");

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            System.err.println(">>> Ошибка создания директории конфигурации: " + e.getMessage());
        }

        this.configPath = configDir.resolve("settings.properties");
    }

    public AppSettings load() {
        AppSettings settings = new AppSettings();
        if (!Files.exists(configPath)) {
            return settings;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);

            String themeName = props.getProperty("theme");
            if (themeName != null) {
                try {
                    settings.setCurrentTheme(ThemePalette.valueOf(themeName));
                } catch (IllegalArgumentException ignored) {}
            }

            String minimizeToTray = props.getProperty("minimizeToTray");
            if (minimizeToTray != null) {
                settings.setMinimizeToTray(Boolean.parseBoolean(minimizeToTray));
            }

            String startWithSystem = props.getProperty("startWithSystem");
            if (startWithSystem != null) {
                settings.setStartWithSystem(Boolean.parseBoolean(startWithSystem));
            }

        } catch (IOException e) {
            System.err.println(">>> Не удалось загрузить настройки: " + e.getMessage());
        }

        return settings;
    }

    public void save(AppSettings settings) {
        if (settings == null) return;

        Properties props = new Properties();
        if (settings.getCurrentTheme() != null) {
            props.setProperty("theme", settings.getCurrentTheme().name());
        }
        props.setProperty("minimizeToTray", String.valueOf(settings.isMinimizeToTray()));
        props.setProperty("startWithSystem", String.valueOf(settings.isStartWithSystem()));

        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "frogTimeStats Application Settings");
        } catch (IOException e) {
            System.err.println(">>> Ошибка сохранения настроек: " + e.getMessage());
        }
    }
}