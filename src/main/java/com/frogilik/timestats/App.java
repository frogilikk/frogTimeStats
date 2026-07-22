package com.frogilik.timestats;

import com.frogilik.timestats.core.LinuxWindowTracker;
import com.frogilik.timestats.repository.ActivityRepository;
import com.frogilik.timestats.service.TrackingService;
import com.frogilik.timestats.ui.MainViewController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private TrackingService trackingService;

    @Override
    public void start(Stage stage) {
        // 1. Инициализируем репозиторий и трекер
        ActivityRepository repository = new ActivityRepository();
        LinuxWindowTracker linuxTracker = new LinuxWindowTracker();

        trackingService = new TrackingService(linuxTracker, repository);

        // 2. Запускаем фоновый поток отслеживания
        trackingService.start();

        // 3. Создаем контроллер UI и сцену
        MainViewController mainViewController = new MainViewController(trackingService);
        Scene scene = new Scene(mainViewController.getView(), 950, 650);

        stage.setTitle("frogTimeStats");
        stage.setScene(scene);

        // 4. Гарантируем сохранение в БД при закрытии окна
        stage.setOnCloseRequest(event -> {
            if (trackingService != null) {
                trackingService.stop(); // Остановит поток и сбросит остатки в БД
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}