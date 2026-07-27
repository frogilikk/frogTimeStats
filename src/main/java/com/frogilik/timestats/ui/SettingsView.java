package com.frogilik.timestats.ui;

import com.frogilik.timestats.model.AppSettings;
import com.frogilik.timestats.model.ThemePalette;
import com.frogilik.timestats.repository.SettingsRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public class SettingsView extends ScrollPane {

    private final AppSettings settings;
    private final SettingsRepository settingsRepository;
    private final Consumer<ThemePalette> onThemeChanged;
    private final Label menuIcon;

    private final VBox contentBox;
    private VBox appearanceCard;
    private VBox behaviorCard;
    private ComboBox<ThemePalette> themeComboBox;

    public SettingsView(AppSettings settings, SettingsRepository settingsRepository, Consumer<ThemePalette> onThemeChanged, Runnable onMenuHover) {
        this.settings = (settings != null) ? settings : new AppSettings();
        this.settingsRepository = settingsRepository;
        this.onThemeChanged = onThemeChanged;

        setFitToWidth(true);
        setFitToHeight(true);

        contentBox = new VBox(20);
        contentBox.setPadding(new Insets(20, 30, 25, 30));
        contentBox.setMaxWidth(Double.MAX_VALUE);
        contentBox.setMaxHeight(Double.MAX_VALUE);

        // --- ШАПКА НАСТРОЕК ---
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        menuIcon = new Label("☰");
        menuIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: #a6adc8; -fx-cursor: hand;");

        if (onMenuHover != null) {
            menuIcon.setOnMouseEntered(e -> onMenuHover.run());
            menuIcon.setOnMouseClicked(e -> onMenuHover.run());
        }

        Label titleLabel = new Label("Настройки");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #cdd6f4;");

        headerBox.getChildren().addAll(menuIcon, titleLabel);

        appearanceCard = createCard("🎨 Внешний вид и Оформление", createAppearanceSettings());
        behaviorCard = createCard("🚀 Поведение и Система", createBehaviorSettings());

        contentBox.getChildren().addAll(headerBox, appearanceCard, behaviorCard);
        setContent(contentBox);

        // Применяем текущую тему
        applyThemeStyles(this.settings.getCurrentTheme());
    }

    public SettingsView() {
        this(new AppSettings(), null, null, null);
    }

    public Label getMenuIcon() {
        return menuIcon;
    }

    private VBox createCard(String titleText, VBox content) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(18));
        card.setMaxWidth(700);

        Label cardTitle = new Label(titleText);
        cardTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        card.getChildren().addAll(cardTitle, content);
        return card;
    }

    private VBox createAppearanceSettings() {
        VBox layout = new VBox(12);

        HBox themeRow = new HBox(15);
        themeRow.setAlignment(Pos.CENTER_LEFT);

        Label themeLabel = new Label("Текущая тема:");
        themeLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll(ThemePalette.values());
        themeComboBox.setValue(settings.getCurrentTheme());

        themeComboBox.setCellFactory(param -> new ListCell<ThemePalette>() {
            @Override
            protected void updateItem(ThemePalette item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getTitle());
                    ThemePalette current = settings.getCurrentTheme();

                    String defaultStyle = String.format(
                            "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-padding: 8 12; -fx-cursor: hand;",
                            current.getCardBgColor(), current.getTextColor()
                    );

                    String hoverStyle = String.format(
                            "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-padding: 8 12; -fx-cursor: hand;",
                            item.getBgColor(), item.getPrimaryColor()
                    );

                    setStyle(defaultStyle);

                    setOnMouseEntered(e -> setStyle(hoverStyle));
                    setOnMouseExited(e -> setStyle(defaultStyle));
                }
            }
        });

        themeComboBox.setButtonCell(new ListCell<ThemePalette>() {
            @Override
            protected void updateItem(ThemePalette item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Активна: " + item.getTitle());
                    ThemePalette current = settings.getCurrentTheme();
                    setStyle(String.format(
                            "-fx-text-fill: %s; -fx-font-weight: bold;",
                            current.getPrimaryColor()
                    ));
                }
            }
        });

        themeComboBox.setOnAction(e -> {
            ThemePalette selected = themeComboBox.getValue();
            if (selected != null) {
                settings.setCurrentTheme(selected);

                if (settingsRepository != null) {
                    settingsRepository.save(settings);
                }

                applyThemeStyles(selected);

                if (onThemeChanged != null) {
                    onThemeChanged.accept(selected);
                }
            }
        });

        themeRow.getChildren().addAll(themeLabel, themeComboBox);
        layout.getChildren().add(themeRow);

        return layout;
    }

    private VBox createBehaviorSettings() {
        VBox layout = new VBox(10);

        CheckBox cbTray = new CheckBox("Сворачивать в трей при закрытии");
        cbTray.setSelected(settings.isMinimizeToTray());
        cbTray.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 13px; -fx-cursor: hand;");
        cbTray.setOnAction(e -> {
            settings.setMinimizeToTray(cbTray.isSelected());
            if (settingsRepository != null) settingsRepository.save(settings);
        });

        CheckBox cbAutostart = new CheckBox("Запускать вместе с системой");
        cbAutostart.setSelected(settings.isStartWithSystem());
        cbAutostart.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 13px; -fx-cursor: hand;");
        cbAutostart.setOnAction(e -> {
            settings.setStartWithSystem(cbAutostart.isSelected());
            if (settingsRepository != null) settingsRepository.save(settings);
        });

        layout.getChildren().addAll(cbTray, cbAutostart);
        return layout;
    }

    public void applyThemeStyles(ThemePalette theme) {
        if (theme == null) return;

        // 1. Принудительное переопределение CSS базовых переменных ScrollPane и его Viewport
        String scrollStyle = String.format(
                "-fx-background-color: %s; -fx-background: %s; -fx-viewport-background-color: %s; -fx-border-color: transparent;",
                theme.getBgColor(), theme.getBgColor(), theme.getBgColor()
        );
        this.setStyle(scrollStyle);

        // Гарантируем перекрашивание внутреннего узла .viewport через lookup
        Node viewportNode = this.lookup(".viewport");
        if (viewportNode != null) {
            viewportNode.setStyle(String.format("-fx-background-color: %s;", theme.getBgColor()));
        }

        // 2. Заливка главного VBox
        if (contentBox != null) {
            contentBox.setStyle(String.format("-fx-background-color: %s;", theme.getBgColor()));
        }

        // 3. Покраска карточек
        String cardStyle = String.format(
                "-fx-background-color: %s; -fx-background-radius: 10; -fx-border-color: %s; -fx-border-radius: 10; -fx-border-width: 1;",
                theme.getCardBgColor(), theme.getPrimaryColor()
        );

        if (appearanceCard != null) appearanceCard.setStyle(cardStyle);
        if (behaviorCard != null) behaviorCard.setStyle(cardStyle);

        // 4. Покраска ComboBox
        if (themeComboBox != null) {
            themeComboBox.setStyle(String.format(
                    "-fx-background-color: %s; " +
                            "-fx-mark-color: %s; " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: %s; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-control-inner-background: %s;",
                    theme.getBgColor(),
                    theme.getPrimaryColor(),
                    theme.getPrimaryColor(),
                    theme.getCardBgColor()
            ));
        }
    }
}