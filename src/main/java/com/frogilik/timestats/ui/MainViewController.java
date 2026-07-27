package com.frogilik.timestats.ui;

import com.frogilik.timestats.Main;
import com.frogilik.timestats.model.AppActivity;
import com.frogilik.timestats.model.AppSettings;
import com.frogilik.timestats.model.ThemePalette;
import com.frogilik.timestats.service.TrackingService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.*;

public class MainViewController {

    private final StackPane root;
    private final TrackingService trackingService;
    private final AppSettings settings;
    private Timeline autoUpdateTimeline;

    // Контейнеры экранов
    private final ScrollPane analyticsScrollPane;
    private final VBox scrollContent;
    private final SettingsView settingsView;
    private final StackPane fixedWaveContainer;

    // Компоненты экрана аналитики
    private final HeaderWidget headerWidget;
    private final CustomPieChartWidget pieChartWidget;
    private final WaveProgressBar waveProgressBar;
    private final VBox detailsListContainer;
    private final SidebarView sidebarView;
    private final Label totalTimeValueLabel;
    private final Label topAppValueLabel;
    private final Label detailsTitle;
    private final VBox cardTotal;
    private final VBox cardTopApp;

    // Кэш процессов и цветов
    private final Map<String, ProcessRowWidget> processRowMap = new HashMap<>();
    private final Map<String, Color> appColorMap = new HashMap<>();

    public MainViewController(TrackingService trackingService) {
        this.trackingService = trackingService;
        this.settings = (Main.getAppSettings() != null) ? Main.getAppSettings() : new AppSettings();

        root = new StackPane();
        root.setPrefSize(950, 650);

        // 1. Создаем виджеты аналитики
        headerWidget = new HeaderWidget();
        pieChartWidget = new CustomPieChartWidget();

        totalTimeValueLabel = new Label("0ч 0мин");
        cardTotal = UiFactory.createMetricCard("Всего активен:", totalTimeValueLabel);

        topAppValueLabel = new Label("—");
        cardTopApp = UiFactory.createMetricCard("Топ приложение:", topAppValueLabel);
        HBox cardsBox = new HBox(15, cardTotal, cardTopApp);

        detailsTitle = new Label("Детализация:");
        detailsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        detailsListContainer = new VBox(10);

        // 2. Основное содержимое аналитики
        scrollContent = new VBox(15);
        scrollContent.setPadding(new Insets(20));
        scrollContent.getChildren().addAll(
                headerWidget,
                pieChartWidget,
                cardsBox,
                detailsTitle,
                detailsListContainer
        );

        // 3. ScrollPane для аналитики
        analyticsScrollPane = new ScrollPane(scrollContent);
        analyticsScrollPane.setFitToWidth(true);
        analyticsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        analyticsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // 4. Закрепленная прямоугольная диаграмма (Волна)
        waveProgressBar = new WaveProgressBar();
        waveProgressBar.setPrefHeight(40);
        waveProgressBar.setMaxHeight(40);
        waveProgressBar.setMaxWidth(890);

        fixedWaveContainer = new StackPane(waveProgressBar);
        fixedWaveContainer.setAlignment(Pos.TOP_CENTER);
        fixedWaveContainer.setPadding(new Insets(60, 20, 0, 20));
        fixedWaveContainer.setPickOnBounds(false);
        fixedWaveContainer.setMouseTransparent(true);
        StackPane.setAlignment(fixedWaveContainer, Pos.TOP_CENTER);

        waveProgressBar.setOpacity(0.0);

        // --- СКРОЛЛ И СЖАТИЕ ДИАГРАММЫ ---
        final double SCROLL_SPEED = 0.05;

        root.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (!analyticsScrollPane.isVisible()) return;

            event.consume();

            double deltaY = event.getDeltaY();
            if (deltaY == 0) return;

            double direction = deltaY > 0 ? -1.0 : 1.0;
            double newVvalue = analyticsScrollPane.getVvalue() + (direction * SCROLL_SPEED);
            analyticsScrollPane.setVvalue(Math.min(1.0, Math.max(0.0, newVvalue)));
        });

        analyticsScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            double scroll = newVal.doubleValue();

            if (scroll <= 0.0) {
                pieChartWidget.setPrefHeight(CustomPieChartWidget.DEFAULT_HEIGHT);
                pieChartWidget.setMaxHeight(CustomPieChartWidget.DEFAULT_HEIGHT);
                pieChartWidget.setOpacity(1.0);
                pieChartWidget.setVisible(true);
                waveProgressBar.setOpacity(0.0);
                return;
            }

            double contentHeight = scrollContent.getBoundsInLocal().getHeight();
            double viewportHeight = analyticsScrollPane.getViewportBounds().getHeight();
            double maxScrollPx = contentHeight - viewportHeight;

            double scrollPx = (maxScrollPx > 0) ? scroll * maxScrollPx : scroll * CustomPieChartWidget.DEFAULT_HEIGHT;
            double targetHeight = Math.max(0.0, CustomPieChartWidget.DEFAULT_HEIGHT - scrollPx);

            double factor = 1.0 - (targetHeight / CustomPieChartWidget.DEFAULT_HEIGHT);

            if (factor >= 0.98) factor = 1.0;
            if (factor <= 0.02) factor = 0.0;

            pieChartWidget.setOpacity(1.0 - factor);
            waveProgressBar.setOpacity(factor);

            pieChartWidget.setPrefHeight(targetHeight);
            pieChartWidget.setMaxHeight(targetHeight);

            pieChartWidget.setVisible(targetHeight > 0.5);
        });

        // 5. САЙДБАР
        sidebarView = new SidebarView(
                trackingService,
                selectedPeriod -> {
                    processRowMap.clear();
                    detailsListContainer.getChildren().clear();
                    showAnalyticsScreen();
                    loadData();
                },
                this::showAnalyticsScreen,
                this::showSettingsScreen
        );

        StackPane.setAlignment(sidebarView, Pos.TOP_LEFT);

        // 6. СОЗДАЕМ ЭКРАН НАСТРОЕК С ПЕРЕДАЧЕЙ SettingsRepository
        settingsView = new SettingsView(
                settings,
                Main.getSettingsRepository(),
                this::applyThemeToAllUI,
                sidebarView::show
        );
        settingsView.setVisible(false);

        headerWidget.getMenuIcon().setOnMouseEntered(e -> sidebarView.show());

        if (settingsView.getMenuIcon() != null) {
            settingsView.getMenuIcon().setOnMouseEntered(e -> sidebarView.show());
        }

        root.getChildren().addAll(analyticsScrollPane, settingsView, fixedWaveContainer, sidebarView);

        // Применяем сохраненную тему при запуске
        applyThemeToAllUI(settings.getCurrentTheme());

        loadData();
        startAutoUpdate();
    }

    private void applyThemeToAllUI(ThemePalette theme) {
        if (theme == null) return;

        // 1. Главный фон и скролл
        root.setStyle(String.format("-fx-background-color: %s; -fx-font-family: 'Segoe UI', sans-serif;", theme.getBgColor()));
        scrollContent.setStyle(String.format("-fx-background-color: %s;", theme.getBgColor()));
        analyticsScrollPane.setStyle(String.format(
                "-fx-background-color: %s; -fx-background: %s; -fx-border-color: transparent; -fx-viewport-background-color: %s;",
                theme.getBgColor(), theme.getBgColor(), theme.getBgColor()
        ));

        // 2. Карточки и тексты аналитики
        detailsTitle.setStyle(String.format("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s;", theme.getSubtextColor()));

        String cardStyle = String.format(
                "-fx-background-color: %s; -fx-background-radius: 10; -fx-border-color: %s; -fx-border-radius: 10; -fx-border-width: 1;",
                theme.getCardBgColor(), theme.getPrimaryColor()
        );
        cardTotal.setStyle(cardStyle);
        cardTopApp.setStyle(cardStyle);

        totalTimeValueLabel.setStyle(String.format("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: %s;", theme.getPrimaryColor()));
        topAppValueLabel.setStyle(String.format("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: %s;", theme.getPrimaryColor()));

        // 3. Перекрашиваем Сайдбар
        if (sidebarView != null) {
            sidebarView.applyTheme(theme);
        }

        // 4. Перекрашиваем экран Настроек
        if (settingsView != null) {
            settingsView.applyThemeStyles(theme);
        }

        // 5. Перекрашиваем все строки процессов
        for (ProcessRowWidget widget : processRowMap.values()) {
            widget.applyTheme(theme);
        }
    }

    private void showAnalyticsScreen() {
        settingsView.setVisible(false);
        analyticsScrollPane.setVisible(true);
        fixedWaveContainer.setVisible(true);
        sidebarView.hide();
    }

    private void showSettingsScreen() {
        analyticsScrollPane.setVisible(false);
        fixedWaveContainer.setVisible(false);
        settingsView.setVisible(true);
        sidebarView.hide();
    }

    public Parent getView() {
        return root;
    }

    public void startAutoUpdate() {
        autoUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (analyticsScrollPane.isVisible()) {
                loadData();
            }
        }));
        autoUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        autoUpdateTimeline.play();
    }

    public void stopAutoUpdate() {
        if (autoUpdateTimeline != null) autoUpdateTimeline.stop();
    }

    public void loadData() {
        var statsMap = trackingService.getStats();

        List<AppActivity> activities = statsMap.values().stream()
                .sorted(Comparator.comparingLong(AppActivity::durationSeconds).reversed())
                .toList();

        long totalSeconds = activities.stream().mapToLong(AppActivity::durationSeconds).sum();

        for (AppActivity act : activities) {
            getUniqueAppColor(act.processName());
        }

        updateHeaderStats(totalSeconds, activities);
        pieChartWidget.updateData(activities, appColorMap);
        updateWaveBar(activities, totalSeconds);
        updateProcessList(activities, totalSeconds);
    }

    private void updateHeaderStats(long totalSeconds, List<AppActivity> activities) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            totalTimeValueLabel.setText(String.format("%dч %dмин", hours, minutes));
        } else if (minutes > 0) {
            totalTimeValueLabel.setText(String.format("%dмин %dсек", minutes, seconds));
        } else {
            totalTimeValueLabel.setText(String.format("%dсек", seconds));
        }

        if (!activities.isEmpty()) {
            AppActivity top = activities.get(0);
            long topMins = top.durationSeconds() / 60;
            topAppValueLabel.setText(topMins > 0
                    ? String.format("%s (%dмин)", top.processName(), topMins)
                    : String.format("%s (%dсек)", top.processName(), top.durationSeconds()));
        } else {
            topAppValueLabel.setText("—");
        }
    }

    private void updateWaveBar(List<AppActivity> activities, long totalSeconds) {
        List<WaveProgressBar.SegmentData> waveSegments = new ArrayList<>();
        for (AppActivity act : activities) {
            if (act.durationSeconds() <= 0) continue;
            double pct = totalSeconds > 0 ? (double) act.durationSeconds() / totalSeconds : 0;
            waveSegments.add(new WaveProgressBar.SegmentData(act.processName(), pct, getUniqueAppColor(act.processName())));
        }
        waveProgressBar.setSegments(waveSegments);
    }

    private void updateProcessList(List<AppActivity> activities, long totalSeconds) {
        Set<String> activeProcessNames = new HashSet<>();
        int index = 1;

        for (AppActivity act : activities) {
            if (act.durationSeconds() <= 0) continue;

            double percentage = totalSeconds > 0 ? (double) act.durationSeconds() / totalSeconds : 0;
            String processName = act.processName();
            activeProcessNames.add(processName);

            ProcessRowWidget widget = processRowMap.get(processName);
            if (widget == null) {
                widget = new ProcessRowWidget(index, act, percentage, settings.getCurrentTheme());
                processRowMap.put(processName, widget);
                detailsListContainer.getChildren().add(widget.getNode());
            } else {
                widget.update(index, act, percentage);
            }
            index++;
        }

        processRowMap.keySet().removeIf(procName -> {
            if (!activeProcessNames.contains(procName)) {
                ProcessRowWidget widget = processRowMap.get(procName);
                if (widget != null) {
                    detailsListContainer.getChildren().remove(widget.getNode());
                }
                return true;
            }
            return false;
        });

        List<Node> sortedNodes = new ArrayList<>(detailsListContainer.getChildren());
        sortedNodes.sort(Comparator.comparingInt(node -> {
            String procName = (String) node.getUserData();
            for (int i = 0; i < activities.size(); i++) {
                if (activities.get(i).processName().equals(procName)) return i;
            }
            return Integer.MAX_VALUE;
        }));

        detailsListContainer.getChildren().setAll(sortedNodes);
    }

    private Color getUniqueAppColor(String processName) {
        return appColorMap.computeIfAbsent(processName, name -> {
            int hash = Math.abs(name.hashCode());
            double hue = (hash % 360);
            double saturation = 0.65 + ((hash % 20) / 100.0);
            double brightness = 0.75 + ((hash % 15) / 100.0);
            return Color.hsb(hue, saturation, brightness);
        });
    }
}