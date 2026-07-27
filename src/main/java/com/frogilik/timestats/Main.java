package com.frogilik.timestats;

import com.frogilik.timestats.core.LinuxWindowTracker;
import com.frogilik.timestats.core.WindowTracker;
import com.frogilik.timestats.core.WindowsWindowTracker;
import com.frogilik.timestats.model.AppSettings;
import com.frogilik.timestats.repository.ActivityRepository;
import com.frogilik.timestats.repository.DatabaseManager;
import com.frogilik.timestats.repository.SettingsRepository;
import com.frogilik.timestats.service.AutoStartService;
import com.frogilik.timestats.service.TrackingService;
import com.frogilik.timestats.util.SingleInstanceManager;
import dorkbox.systemTray.SystemTray;
import javafx.application.Application;

public class Main {

    private static TrackingService trackingService;
    private static AppSettings appSettings;
    private static SettingsRepository settingsRepository;

    public static TrackingService getTrackingService() {
        return trackingService;
    }

    public static AppSettings getAppSettings() {
        return appSettings;
    }

    public static SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    public static void main(String[] args) {
        // === ПРОВЕРКА НА ЕДИНСТВЕННЫЙ ЭКЗЕМПЛЯР ===
        if (SingleInstanceManager.isAlreadyRunning(App::restoreWindow)) {
            System.out.println("Приложение уже запущено! Показываем окно и завершаем дубликат.");
            System.exit(0);
            return;
        }

        // Загружаем сохраненные настройки с диска
        settingsRepository = new SettingsRepository();
        appSettings = settingsRepository.load();

        // Принудительно задаем GTK3 для Dorkbox и JavaFX под Linux
        System.setProperty("jdk.gtk.version", "3");

        // Включаем нативный автодетект DBus/AppIndicator для Linux
        SystemTray.FORCE_GTK2 = false;

        // 0. Автодобавление в реестр (Windows)
        AutoStartService.registerAutoStart();

        // 1. Инициализируем БД
        DatabaseManager.initDatabase();

        // 2. Определяем ОС и нужный трекер
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isWindows = osName.contains("win");
        WindowTracker tracker = isWindows ? new WindowsWindowTracker() : new LinuxWindowTracker();

        System.out.println(">>> Запуск трекера на ОС: " + System.getProperty("os.name"));

        // 3. Создаем репозиторий и сервис
        ActivityRepository repository = new ActivityRepository();
        trackingService = new TrackingService(tracker, repository);

        // 4. Запуск фонового отслеживания
        trackingService.start();

        // 5. Хук завершения процесса
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (trackingService != null) {
                System.out.println("\n>>> Остановка трекера и сохранение данных...");
                trackingService.stop();
            }
        }));

        // 6. Запускаем JavaFX UI Приложение
        Application.launch(App.class, args);
    }
}