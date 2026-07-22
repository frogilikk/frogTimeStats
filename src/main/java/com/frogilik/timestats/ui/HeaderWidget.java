package com.frogilik.timestats.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class HeaderWidget extends HBox {

    private final Label menuIcon;

    public HeaderWidget() {
        super(15);
        setAlignment(Pos.CENTER_LEFT);

        menuIcon = new Label("☰");
        menuIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: #89b4fa; -fx-cursor: hand; -fx-padding: 0 5 0 0;");

        String todayFormatted = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru")))
                .toUpperCase();
        Label dateHeaderLabel = new Label("СЕГОДНЯ: " + todayFormatted);
        dateHeaderLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #cdd6f4;");

        getChildren().addAll(menuIcon, dateHeaderLabel);
    }

    public Label getMenuIcon() {
        return menuIcon;
    }
}