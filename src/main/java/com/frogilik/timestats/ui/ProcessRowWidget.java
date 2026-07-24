package com.frogilik.timestats.ui;

import com.frogilik.timestats.model.AppActivity;
import com.frogilik.timestats.util.IconExtractor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class ProcessRowWidget {

    private final HBox rowNode;
    private final ImageView iconView; // <--- Добавили ImageView
    private final Label nameLabel;
    private final ProgressBar progressBar;
    private final Label timeLabel;

    public ProcessRowWidget(int index, AppActivity activity, double percentage) {
        rowNode = new HBox(12);
        rowNode.setAlignment(Pos.CENTER_LEFT);
        rowNode.setPadding(new Insets(8, 12, 8, 12));
        rowNode.setStyle("-fx-background-color: #181825; -fx-background-radius: 6;");
        rowNode.setUserData(activity.processName());

        // Настройка иконки
        iconView = new ImageView();
        iconView.setFitWidth(20);
        iconView.setFitHeight(20);
        iconView.setPreserveRatio(true);

        nameLabel = new Label();
        nameLabel.setPrefWidth(160);
        nameLabel.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold;");

        progressBar = new ProgressBar(percentage);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #89b4fa;");
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        timeLabel = new Label();
        timeLabel.setPrefWidth(140);
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        timeLabel.setStyle("-fx-text-fill: #a6adc8;");

        // Помещаем iconView перед nameLabel!
        rowNode.getChildren().addAll(iconView, nameLabel, progressBar, timeLabel);
        update(index, activity, percentage);
    }

    public void update(int index, AppActivity activity, double percentage) {
        nameLabel.setText(String.format("%d. %s", index, activity.processName()));
        progressBar.setProgress(percentage);
        timeLabel.setText(formatTime(activity.durationSeconds(), percentage));

        // Извлекаем и обновляем иконку при каждом вызове update
        // (Предполагается, что в AppActivity есть метод exePath() или getExePath())
        Image icon = IconExtractor.getIconForProcess(activity.processName(), activity.exePath());
        iconView.setImage(icon);
    }

    public HBox getNode() {
        return rowNode;
    }

    private String formatTime(long totalSeconds, double percentage) {
        long hrs = totalSeconds / 3600;
        long mins = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;

        if (hrs > 0) {
            return String.format("%dч %dмин (%.0f%%)", hrs, mins, percentage * 100);
        } else if (mins > 0) {
            return String.format("%dмин %dсек (%.0f%%)", mins, secs, percentage * 100);
        } else {
            return String.format("%dсек (%.0f%%)", secs, percentage * 100);
        }
    }
}