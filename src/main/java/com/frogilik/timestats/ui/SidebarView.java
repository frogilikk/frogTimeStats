package com.frogilik.timestats.ui;

import com.frogilik.timestats.service.TrackingService;
import com.frogilik.timestats.service.TrackingService.TimePeriod;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.Consumer;

public class SidebarView extends VBox {

    private final TranslateTransition transition;
    private final double sidebarWidth = 200;

    private final Button periodButton;
    private final VBox periodsSubMenu;
    private boolean isMenuExpanded = false;
    private Timeline menuAnimation;

    public SidebarView(TrackingService trackingService, Consumer<TimePeriod> onPeriodChanged) {
        super(10);
        setPrefWidth(sidebarWidth);
        setMaxWidth(sidebarWidth);
        setPadding(new Insets(20, 10, 20, 10));
        setStyle("-fx-background-color: #181825; -fx-border-color: #313244; -fx-border-width: 0 1 0 0;");

        // Изначально смещаем весь сайдбар влево за пределы экрана
        setTranslateX(-sidebarWidth);

        // Настраиваем анимацию выезда самого сайдбара
        transition = new TranslateTransition(Duration.millis(200), this);

        Label logo = new Label("frogTimeStats");
        logo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #a6e3a1; -fx-padding: 0 0 15 10;");

        // 1. Главная кнопка выбора периода
        periodButton = new Button("📅  " + trackingService.getCurrentPeriod().getTitle() + "  ▾");
        periodButton.setMaxWidth(Double.MAX_VALUE);
        periodButton.setAlignment(Pos.CENTER_LEFT);
        periodButton.setStyle(
                "-fx-background-color: #313244; " +
                        "-fx-text-fill: #cdd6f4; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        );

        // 2. Вложенный контейнер для подпунктов
        periodsSubMenu = new VBox(2); // Уменьшили межстрочный зазор для чёткости
        periodsSubMenu.setPadding(new Insets(0, 0, 0, 10));

        // Начальное состояние: полностью скрыто
        periodsSubMenu.setPrefHeight(0);
        periodsSubMenu.setMinHeight(0);
        periodsSubMenu.setMaxHeight(0);
        periodsSubMenu.setVisible(false);
        periodsSubMenu.setManaged(false);

        // Заполняем подменю кнопками периодов
        for (TimePeriod period : TimePeriod.values()) {
            Button subItem = new Button("• " + period.getTitle());
            subItem.setMaxWidth(Double.MAX_VALUE);
            subItem.setAlignment(Pos.CENTER_LEFT);
            subItem.setPrefHeight(28); // Фиксированная высота кнопки убирает тряску текста при подсчете
            subItem.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-text-fill: #bac2de; " +
                            "-fx-font-size: 12px; " +
                            "-fx-cursor: hand;"
            );

            // Эффект наведения на пункт списка
            subItem.setOnMouseEntered(e -> subItem.setStyle("-fx-background-color: #45475a; -fx-text-fill: #a6e3a1; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 4;"));
            subItem.setOnMouseExited(e -> subItem.setStyle("-fx-background-color: transparent; -fx-text-fill: #bac2de; -fx-font-size: 12px; -fx-cursor: hand;"));

            subItem.setOnAction(e -> {
                trackingService.setTimePeriod(period);
                periodButton.setText("📅  " + period.getTitle() + "  ▾");

                if (onPeriodChanged != null) {
                    onPeriodChanged.accept(period);
                }

                toggleSubMenu(false);
                hide();
            });

            periodsSubMenu.getChildren().add(subItem);
        }

        // --- ЛОГИКА НАВЕДЕНИЯ И ЗАКРЫТИЯ ПРИ УХОДЕ МЫШИ ---

        // Открываем при наведении на главную кнопку
        periodButton.setOnMouseEntered(e -> toggleSubMenu(true));
        periodButton.setOnAction(e -> toggleSubMenu(!isMenuExpanded));

        // Автозакрытие при уходе мыши с области списка кнопок
        periodsSubMenu.setOnMouseExited(e -> {
            // Проверяем, что мышь действительно ушла наружу, а не вернулась на главную кнопку
            if (!periodButton.isHover()) {
                toggleSubMenu(false);
            }
        });

        // Остальные кнопки
        Button btnAnalytics = UiFactory.createSidebarButton("📊  Аналитика", false);
        Button btnSettings = UiFactory.createSidebarButton("⚙️  Настройки", false);

        getChildren().addAll(logo, periodButton, periodsSubMenu, btnAnalytics, btnSettings);

        // При выходе курсора из всего сайдбара — сворачиваем меню и прячем панель
        setOnMouseExited(e -> {
            toggleSubMenu(false);
            hide();
        });
    }

    /**
     * Плавное и плавное раздвижение/сжатие без подёргиваний
     */
    private void toggleSubMenu(boolean expand) {
        if (isMenuExpanded == expand) return;
        isMenuExpanded = expand;

        if (menuAnimation != null) {
            menuAnimation.stop();
        }

        // Точный расчет высоты (28px высота кнопки + 2px зазор между ними)
        double targetHeight = expand ? (periodsSubMenu.getChildren().size() * 30.0) : 0.0;

        if (expand) {
            periodsSubMenu.setVisible(true);
            periodsSubMenu.setManaged(true);
        }

        // Анимируем строгий диапазон высот (prefHeight & maxHeight)
        menuAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(periodsSubMenu.prefHeightProperty(), periodsSubMenu.getPrefHeight()),
                        new KeyValue(periodsSubMenu.maxHeightProperty(), periodsSubMenu.getMaxHeight())
                ),
                new KeyFrame(Duration.millis(180), // Оптимальная длительность для плавных 60 FPS
                        new KeyValue(periodsSubMenu.prefHeightProperty(), targetHeight),
                        new KeyValue(periodsSubMenu.maxHeightProperty(), targetHeight)
                )
        );

        menuAnimation.setOnFinished(e -> {
            if (!expand) {
                periodsSubMenu.setVisible(false);
                periodsSubMenu.setManaged(false);
            }
        });

        menuAnimation.play();
    }

    public void show() {
        transition.stop();
        transition.setToX(0);
        transition.play();
    }

    public void hide() {
        transition.stop();
        transition.setToX(-sidebarWidth);
        transition.play();
    }
}