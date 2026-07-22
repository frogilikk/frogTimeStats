package com.frogilik.timestats.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class UiFactory {

    public static VBox createMetricCard(String title, Label valueLabel) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: #313244; -fx-background-radius: 8;");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #a6adc8;");

        valueLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #89b4fa;");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    public static Button createSidebarButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        if (active) {
            btn.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 12;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a6adc8; -fx-font-size: 13px; -fx-background-radius: 6; -fx-padding: 8 12;");
        }
        return btn;
    }
}