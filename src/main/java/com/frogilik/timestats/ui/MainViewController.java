package com.frogilik.timestats.ui;

import com.frogilik.timestats.model.AppActivity;
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
    private Timeline autoUpdateTimeline;

    // Компоненты
    private final HeaderWidget headerWidget;
    private final CustomPieChartWidget pieChartWidget;
    private final WaveProgressBar waveProgressBar;
    private final VBox detailsListContainer;
    private final SidebarView sidebarView;
    private final Label totalTimeValueLabel;
    private final Label topAppValueLabel;

    // Кэш процессов и цветов
    private final Map<String, ProcessRowWidget> processRowMap = new HashMap<>();
    private final Map<String, Color> appColorMap = new HashMap<>();

    public MainViewController(TrackingService trackingService) {
        this.trackingService = trackingService;

        root = new StackPane();
        root.setPrefSize(950, 650);
        root.setStyle("-fx-background-color: #1e1e2e; -fx-font-family: 'Segoe UI', sans-serif;");

        // 1. Создаем виджеты
        headerWidget = new HeaderWidget();
        pieChartWidget = new CustomPieChartWidget();

        totalTimeValueLabel = new Label("0ч 0мин");
        VBox cardTotal = UiFactory.createMetricCard("Всего активен:", totalTimeValueLabel);

        topAppValueLabel = new Label("—");
        VBox cardTopApp = UiFactory.createMetricCard("Топ приложение:", topAppValueLabel);
        HBox cardsBox = new HBox(15, cardTotal, cardTopApp);

        Label detailsTitle = new Label("Детализация:");
        detailsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #a6adc8;");
        detailsListContainer = new VBox(10);

        // 2. Основное содержимое
        VBox scrollContent = new VBox(15);
        scrollContent.setPadding(new Insets(20));
        scrollContent.setStyle("-fx-background-color: #1e1e2e;");
        scrollContent.getChildren().addAll(
                headerWidget,
                pieChartWidget,
                cardsBox,
                detailsTitle,
                detailsListContainer
        );

        // 3. ScrollPane
        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
                "-fx-background-color: #1e1e2e; " +
                        "-fx-background: #1e1e2e; " +
                        "-fx-border-color: #1e1e2e; " +
                        "-fx-viewport-background-color: #1e1e2e;"
        );

        // 4. Закрепленная прямоугольная диаграмма
        waveProgressBar = new WaveProgressBar();
        waveProgressBar.setPrefHeight(40);
        waveProgressBar.setMaxHeight(40);
        waveProgressBar.setMaxWidth(890);

        StackPane fixedWaveContainer = new StackPane(waveProgressBar);
        fixedWaveContainer.setAlignment(Pos.TOP_CENTER);
        fixedWaveContainer.setPadding(new Insets(60, 20, 0, 20));
        fixedWaveContainer.setPickOnBounds(false);
        fixedWaveContainer.setMouseTransparent(true);
        StackPane.setAlignment(fixedWaveContainer, Pos.TOP_CENTER);

        waveProgressBar.setOpacity(0.0);

        // --- 1. ОПТИМАЛЬНАЯ СКОРОСТЬ ПРОКРУТКИ СПИСКА ---
        final double SCROLL_SPEED = 0.05;

        root.addEventFilter(ScrollEvent.SCROLL, event -> {
            event.consume();

            double deltaY = event.getDeltaY();
            if (deltaY == 0) return;

            double direction = deltaY > 0 ? -1.0 : 1.0;
            double newVvalue = scrollPane.getVvalue() + (direction * SCROLL_SPEED);
            scrollPane.setVvalue(Math.min(1.0, Math.max(0.0, newVvalue)));
        });

        // --- 2. СЖАТИЕ ДИАГРАММЫ ПРИ СКРОЛЛЕ ---
        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
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
            double viewportHeight = scrollPane.getViewportBounds().getHeight();
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

        // --- 5. САЙДБАР С ВЫБОРОМ ПЕРИОДА ВРЕМЕНИ ---
        sidebarView = new SidebarView(trackingService, selectedPeriod -> {
            // При смене периода очищаем старые виджеты строк для корректной перерисовоки
            processRowMap.clear();
            detailsListContainer.getChildren().clear();

            // Загружаем статистику за выбранный период
            loadData();
        });

        StackPane.setAlignment(sidebarView, Pos.TOP_LEFT);
        headerWidget.getMenuIcon().setOnMouseEntered(e -> sidebarView.show());

        root.getChildren().addAll(scrollPane, fixedWaveContainer, sidebarView);

        loadData();
        startAutoUpdate();
    }

    public Parent getView() {
        return root;
    }

    public void startAutoUpdate() {
        autoUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> loadData()));
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
                widget = new ProcessRowWidget(index, act, percentage);
                processRowMap.put(processName, widget);
                detailsListContainer.getChildren().add(widget.getNode());
            } else {
                widget.update(index, act, percentage);
            }
            index++;
        }

        // Удаляем приложения, у которых нет активности в выбранном периоде
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