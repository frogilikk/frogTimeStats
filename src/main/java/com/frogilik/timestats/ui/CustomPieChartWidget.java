package com.frogilik.timestats.ui;

import com.frogilik.timestats.model.AppActivity;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.*;

public class CustomPieChartWidget extends VBox {

    public static final double DEFAULT_HEIGHT = 300.0;

    private final PieChart pieChart;
    private final FlowPane legendPane;
    private final Map<String, PieChart.Data> chartDataMap = new HashMap<>();

    public CustomPieChartWidget() {
        super(10);
        setAlignment(Pos.CENTER);
        setPrefHeight(DEFAULT_HEIGHT);
        setMaxHeight(DEFAULT_HEIGHT);
        setMinHeight(0);

        pieChart = new PieChart();
        pieChart.setAnimated(false);
        pieChart.setLegendVisible(false);
        pieChart.setLabelsVisible(false);
        pieChart.setPrefHeight(220);

        legendPane = new FlowPane();
        legendPane.setHgap(15);
        legendPane.setVgap(10);
        legendPane.setAlignment(Pos.CENTER);
        legendPane.setPadding(new Insets(5, 0, 0, 0));

        getChildren().addAll(pieChart, legendPane);
    }

    public void updateData(List<AppActivity> activities, Map<String, Color> colorMap) {
        legendPane.getChildren().clear();
        Set<String> activeProcesses = new HashSet<>();

        for (AppActivity act : activities) {
            if (act.durationSeconds() <= 0) continue;

            String procName = act.processName();
            activeProcesses.add(procName);
            Color color = colorMap.get(procName);

            String hexColor = String.format("#%02x%02x%02x",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));

            PieChart.Data data = chartDataMap.get(procName);
            if (data != null) {
                data.setPieValue(act.durationSeconds());
            } else {
                data = new PieChart.Data(procName, act.durationSeconds());
                chartDataMap.put(procName, data);
                pieChart.getData().add(data);
            }

            final PieChart.Data targetData = data;
            Runnable applyStyle = () -> {
                if (targetData.getNode() != null) {
                    targetData.getNode().setStyle("-fx-pie-color: " + hexColor + ";");
                }
            };

            if (data.getNode() != null) {
                applyStyle.run();
            } else {
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) applyStyle.run();
                });
            }

            legendPane.getChildren().add(createLegendItem(procName, color));
        }

        chartDataMap.keySet().removeIf(procName -> {
            if (!activeProcesses.contains(procName)) {
                pieChart.getData().removeIf(d -> d.getName().equals(procName));
                return true;
            }
            return false;
        });
    }

    private HBox createLegendItem(String name, Color color) {
        Circle circle = new Circle(4, color);
        Label label = new Label(name);
        label.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 12px;");

        HBox item = new HBox(6, circle, label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }
}