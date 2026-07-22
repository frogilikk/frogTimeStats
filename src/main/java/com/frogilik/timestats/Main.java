package com.frogilik.timestats;

import com.frogilik.timestats.core.LinuxWindowTracker;
import com.frogilik.timestats.core.WindowTracker;
import com.frogilik.timestats.core.WindowsWindowTracker;
import com.frogilik.timestats.repository.ActivityRepository;
import com.frogilik.timestats.repository.DatabaseManager;
import com.frogilik.timestats.service.TrackingService;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        DatabaseManager.initDatabase();

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        WindowTracker tracker = isWindows ? new WindowsWindowTracker() : new LinuxWindowTracker();

        ActivityRepository repository = new ActivityRepository();
        TrackingService trackingService = new TrackingService(tracker, repository);

        System.out.println(">>> Запуск трекера на ОС: " + System.getProperty("os.name"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n>>> Остановка трекера и сохранение данных...");
            trackingService.stop();
        }));

        trackingService.start();

        while (true) {
            Thread.sleep(3000);

            System.out.println("\n--- СТАТИСТИКА ЗА СЕГОДНЯ ---");
            trackingService.getStats().forEach((process, activity) -> {
                System.out.printf("Приложение: %-18s | Время: %-10s | Заголовок: %s%n",
                        activity.processName(),
                        formatDuration(activity.durationSeconds()),
                        activity.windowTitle());
            });
        }
    }

    private static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dч %02dмин", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dмин %02dсек", minutes, seconds);
        } else {
            return String.format("%dсек", seconds);
        }
    }
}