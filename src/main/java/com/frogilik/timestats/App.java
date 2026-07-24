package com.frogilik.timestats;

import com.frogilik.timestats.service.TrackingService;
import com.frogilik.timestats.ui.MainViewController;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.image.BufferedImage;

public class App extends Application {

    private Stage primaryStage;
    private TrackingService trackingService;
    private MainViewController mainViewController;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // Запрещаем автоматическое завершение JavaFX при закрытии окон
        Platform.setImplicitExit(false);

        trackingService = Main.getTrackingService();
        mainViewController = new MainViewController(trackingService);

        Scene scene = new Scene(mainViewController.getView(), 950, 650);

        stage.setTitle("frogTimeStats");
        stage.setScene(scene);

        // Перехватываем "крестик"
        stage.setOnCloseRequest(event -> {
            event.consume();
            hideStage();
        });

        // Создаем системный трей
        createSystemTray();

        stage.show();
    }

    private void hideStage() {
        Platform.runLater(() -> {
            if (primaryStage != null) {
                primaryStage.hide();
                System.out.println(">>> Окно скрыто в трей");

                // 1. Окращаем работу UI в фоновом режиме
                if (mainViewController != null) {
                    mainViewController.stopAutoUpdate();
                }

                // 2. Вызываем GC для освобождения кучи до 15-30 МБ
                System.gc();
            }
        });
    }

    private void showStage() {
        System.out.println(">>> Запрос на открытие окна из трея...");
        Platform.runLater(() -> {
            if (primaryStage == null) return;

            // Возобновляем обновление UI при поимке фокуса
            if (mainViewController != null) {
                mainViewController.startAutoUpdate();
            }

            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }

            if (primaryStage.isIconified()) {
                primaryStage.setIconified(false);
            }

            primaryStage.toFront();
            primaryStage.requestFocus();
            primaryStage.setAlwaysOnTop(true);
            primaryStage.setAlwaysOnTop(false);
        });
    }

    private void createSystemTray() {
        // Получаем нативный трей
        SystemTray systemTray = SystemTray.get();
        if (systemTray == null) {
            System.err.println(">>> ОШИБКА: Системный трей недоступен!");
            return;
        }

        systemTray.setTooltip("frogTimeStats");

        // Задаем иконку
        BufferedImage icon = createTrayIconImage();
        systemTray.setImage(icon);

        // Настраиваем меню
        Menu menu = systemTray.getMenu();

        menu.add(new MenuItem("Показать окно", e -> showStage()));
        menu.add(new MenuItem("Выход", e -> {
            if (mainViewController != null) {
                mainViewController.stopAutoUpdate();
            }
            if (trackingService != null) {
                trackingService.stop();
            }
            Platform.exit();
            System.exit(0);
        }));

        System.out.println(">>> Dorkbox Native SystemTray успешно создан!");
    }

    private BufferedImage createTrayIconImage() {
        int size = 32; // Для Linux/Windows 32x32
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new java.awt.Color(166, 227, 161)); // Catppuccin Green
        g2.fillOval(2, 2, size - 4, size - 4);

        g2.dispose();
        return image;
    }
}