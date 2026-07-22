package com.frogilik.timestats.ui;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SidebarView extends VBox {

    private final TranslateTransition transition;
    private final double sidebarWidth = 200;

    public SidebarView(Runnable onTodayClick) {
        super(10);
        setPrefWidth(sidebarWidth);
        setMaxWidth(sidebarWidth);
        setPadding(new Insets(20, 10, 20, 10));
        setStyle("-fx-background-color: #181825; -fx-border-color: #313244; -fx-border-width: 0 1 0 0;");

        // Изначально смещаем меню полностью влево за пределы экрана
        setTranslateX(-sidebarWidth);

        // Настраиваем анимацию скольжения
        transition = new TranslateTransition(Duration.millis(200), this);

        Label logo = new Label("frogTimeStats");
        logo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #a6e3a1; -fx-padding: 0 0 15 10;");

        Button btnToday = UiFactory.createSidebarButton("📅  Сегодня", true);
        Button btnAnalytics = UiFactory.createSidebarButton("📊  Аналитика", false);
        Button btnSettings = UiFactory.createSidebarButton("⚙️  Настройки", false);

        btnToday.setOnAction(e -> onTodayClick.run());

        getChildren().addAll(logo, btnToday, btnAnalytics, btnSettings);

        // Когда мышь уходит за границы всей панели — прячем её обратно
        setOnMouseExited(e -> hide());
    }

    public void show() {
        transition.stop();
        transition.setToX(0); // Возвращаем в видимую область
        transition.play();
    }

    public void hide() {
        transition.stop();
        transition.setToX(-sidebarWidth); // Убираем влево за экран
        transition.play();
    }
}