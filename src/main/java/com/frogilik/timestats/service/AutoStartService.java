package com.frogilik.timestats.service;

import java.io.File;

public class AutoStartService {

    private static final String REG_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "frogTimeStats";

    public static void registerAutoStart() {
        String osName = System.getProperty("os.name").toLowerCase();

        // Автозагрузка через реестр выполняется ТОЛЬКО на Windows
        if (!osName.contains("win")) {
            System.out.println(">>> Пропуск настройки автозагрузки (ОС не является Windows: " + osName + ")");
            return;
        }

        try {
            String jarPath = new File(AutoStartService.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getAbsolutePath();

            String command = jarPath.endsWith(".jar")
                    ? "javaw -jar \"" + jarPath + "\""
                    : "\"" + jarPath + "\"";

            ProcessBuilder pb = new ProcessBuilder(
                    "reg", "add", REG_KEY, "/v", APP_NAME, "/t", "REG_SZ", "/d", command, "/f"
            );
            pb.start();
            System.out.println(">>> Запись автозагрузки в реестр Windows успешно обновлена.");
        } catch (Exception e) {
            System.err.println("Ошибка при добавлении в автозагрузку Windows: " + e.getMessage());
        }
    }
}