package com.frogilik.timestats.ui;

import com.frogilik.timestats.model.ThemePalette;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ThemeManager {

    public static void applyTheme(StackPane root, ScrollPane analyticsScrollPane, VBox scrollContent, ThemePalette theme) {
        if (root == null || theme == null) return;

        // 1. Фон всего приложения
        root.setStyle(String.format(
                "-fx-background-color: %s; -fx-font-family: 'Segoe UI', sans-serif;",
                theme.getBgColor()
        ));

        // 2. Фон области прокрутки аналитики
        if (analyticsScrollPane != null) {
            analyticsScrollPane.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background: %s; -fx-border-color: %s; -fx-viewport-background-color: %s;",
                    theme.getBgColor(), theme.getBgColor(), theme.getBgColor(), theme.getBgColor()
            ));

            // Дополнительный сброс фона для внутреннего узла .viewport
            Node viewportNode = analyticsScrollPane.lookup(".viewport");
            if (viewportNode != null) {
                viewportNode.setStyle(String.format("-fx-background-color: %s;", theme.getBgColor()));
            }
        }

        // 3. Контейнер содержимого
        if (scrollContent != null) {
            scrollContent.setStyle(String.format("-fx-background-color: %s;", theme.getBgColor()));

            // Обновляем стиль карточек в аналитике (метрики)
            for (Node node : scrollContent.getChildren()) {
                if (node instanceof VBox card && card != scrollContent) {
                    // Карточки метрик
                    if (card.getStyle().contains("-fx-background-color")) {
                        card.setStyle(String.format(
                                "-fx-background-color: %s; -fx-background-radius: 10; -fx-border-color: %s; -fx-border-radius: 10; -fx-border-width: 1;",
                                theme.getCardBgColor(), theme.getPrimaryColor()
                        ));
                    }
                }
            }
        }
    }
}