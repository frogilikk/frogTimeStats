package com.frogilik.timestats.ui;

import com.frogilik.timestats.model.ThemePalette;
import com.frogilik.timestats.service.TrackingService;
import com.frogilik.timestats.service.TrackingService.TimePeriod;
import com.frogilik.timestats.util.AppVersion;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SidebarView extends VBox {

    private final TranslateTransition transition;
    private final double sidebarWidth = 200;

    private final Label logoLabel;
    private final Button periodButton;
    private final VBox periodsSubMenu;
    private final List<Button> subMenuButtons = new ArrayList<>();
    private final Button btnAnalytics;
    private final Button btnSettings;
    private final Label versionLabel;

    private boolean isMenuExpanded = false;
    private Timeline menuAnimation;

    private Button activeNavButton = null;
    private ThemePalette currentTheme;

    public SidebarView(TrackingService trackingService,
                       Consumer<TimePeriod> onPeriodChanged,
                       Runnable onMainViewClicked,
                       Runnable onSettingsClicked) {
        super(10);
        setPrefWidth(sidebarWidth);
        setMaxWidth(sidebarWidth);
        setPadding(new Insets(20, 10, 20, 10));

        // Смещение влево за пределы экрана
        setTranslateX(-sidebarWidth);

        // Настройка анимации выезда
        transition = new TranslateTransition(Duration.millis(200), this);

        logoLabel = new Label("frogTimeStats");

        // 1. Создаём кнопки навигации
        btnAnalytics = UiFactory.createSidebarButton("📊  Аналитика", false);
        btnSettings = UiFactory.createSidebarButton("⚙️  Настройки", false);

        setupNavButtonEffects(btnAnalytics, () -> {
            if (onMainViewClicked != null) onMainViewClicked.run();
        });

        setupNavButtonEffects(btnSettings, () -> {
            if (onSettingsClicked != null) onSettingsClicked.run();
        });

        // 2. Главная выпадающая кнопка выбора периода
        periodButton = new Button("📅  " + trackingService.getCurrentPeriod().getTitle() + "  ▾");
        periodButton.setMaxWidth(Double.MAX_VALUE);
        periodButton.setAlignment(Pos.CENTER_LEFT);

        // 3. Контейнер для подпунктов периодов
        periodsSubMenu = new VBox(2);
        periodsSubMenu.setPadding(new Insets(0, 0, 0, 10));

        periodsSubMenu.setPrefHeight(0);
        periodsSubMenu.setMinHeight(0);
        periodsSubMenu.setMaxHeight(0);
        periodsSubMenu.setVisible(false);
        periodsSubMenu.setManaged(false);

        for (TimePeriod period : TimePeriod.values()) {
            Button subItem = new Button("• " + period.getTitle());
            subItem.setMaxWidth(Double.MAX_VALUE);
            subItem.setAlignment(Pos.CENTER_LEFT);
            subItem.setPrefHeight(28);

            subItem.setOnAction(e -> {
                trackingService.setTimePeriod(period);
                periodButton.setText("📅  " + period.getTitle() + "  ▾");

                if (onPeriodChanged != null) {
                    onPeriodChanged.accept(period);
                }

                if (onMainViewClicked != null) {
                    onMainViewClicked.run();
                    setActiveNavButton(btnAnalytics);
                }

                toggleSubMenu(false);
                hide();
            });

            subMenuButtons.add(subItem);
            periodsSubMenu.getChildren().add(subItem);
        }

        // Выпадающее меню переключается по клику
        periodButton.setOnAction(e -> toggleSubMenu(!isMenuExpanded));

        // Устанавливаем активную кнопку
        setActiveNavButton(btnAnalytics);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        versionLabel = new Label("v" + AppVersion.getVersion());

        getChildren().addAll(logoLabel, periodButton, periodsSubMenu, btnAnalytics, btnSettings, spacer, versionLabel);

        setOnMouseExited(e -> {
            toggleSubMenu(false);
            hide();
        });
    }

    /**
     * Динамическое применение темы ко всей боковой панели и её элементам
     */
    public void applyTheme(ThemePalette theme) {
        if (theme == null) return;
        this.currentTheme = theme;

        // Фон панели и border справа
        setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 0 1 0 0;",
                theme.getCardBgColor(), theme.getPrimaryColor()
        ));

        // Логотип
        logoLabel.setStyle(String.format(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-padding: 0 0 15 10;",
                theme.getPrimaryColor()
        ));

        // Выпадающая кнопка периода
        periodButton.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;",
                theme.getBgColor(), theme.getTextColor()
        ));

        // Кнопки подменю периодов
        for (Button subItem : subMenuButtons) {
            updateSubItemStyle(subItem, false);
            subItem.setOnMouseEntered(e -> updateSubItemStyle(subItem, true));
            subItem.setOnMouseExited(e -> updateSubItemStyle(subItem, false));
        }

        // Обновление стиля активной и неактивных кнопок навигации
        updateNavButtonStyles();

        // Футер версии
        versionLabel.setStyle(String.format(
                "-fx-font-size: 11px; -fx-text-fill: %s; -fx-padding: 5 0 0 10;",
                theme.getSubtextColor()
        ));
    }

    private void updateSubItemStyle(Button subItem, boolean isHover) {
        if (currentTheme == null) return;
        if (isHover) {
            subItem.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 4;",
                    currentTheme.getBgColor(), currentTheme.getPrimaryColor()
            ));
        } else {
            subItem.setStyle(String.format(
                    "-fx-background-color: transparent; -fx-text-fill: %s; -fx-font-size: 12px; -fx-cursor: hand;",
                    currentTheme.getSubtextColor()
            ));
        }
    }

    private void setupNavButtonEffects(Button button, Runnable action) {
        button.setMaxWidth(Double.MAX_VALUE);

        button.setOnMouseEntered(e -> {
            if (button != activeNavButton && currentTheme != null) {
                button.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; -fx-alignment: CENTER_LEFT; -fx-padding: 8 12; -fx-background-radius: 6; -fx-cursor: hand;",
                        currentTheme.getBgColor(), currentTheme.getTextColor()
                ));
            }
        });

        button.setOnMouseExited(e -> {
            if (button != activeNavButton && currentTheme != null) {
                setNormalNavButtonStyle(button);
            }
        });

        button.setOnAction(e -> {
            setActiveNavButton(button);
            if (action != null) action.run();
            hide();
        });
    }

    private void setActiveNavButton(Button button) {
        activeNavButton = button;
        updateNavButtonStyles();
    }

    private void updateNavButtonStyles() {
        if (currentTheme == null) return;

        setNormalNavButtonStyle(btnAnalytics);
        setNormalNavButtonStyle(btnSettings);

        if (activeNavButton != null) {
            // Активная кнопка выделяется основным цветом акцента и более темной подложкой
            activeNavButton.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 13px; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT; -fx-padding: 8 12; -fx-background-radius: 6; -fx-cursor: hand;",
                    currentTheme.getBgColor(), currentTheme.getPrimaryColor()
            ));
        }
    }

    private void setNormalNavButtonStyle(Button button) {
        if (currentTheme == null) return;
        button.setStyle(String.format(
                "-fx-background-color: transparent; -fx-text-fill: %s; -fx-font-size: 13px; -fx-alignment: CENTER_LEFT; -fx-padding: 8 12; -fx-background-radius: 6; -fx-cursor: hand;",
                currentTheme.getTextColor()
        ));
    }

    private void toggleSubMenu(boolean expand) {
        if (isMenuExpanded == expand) return;
        isMenuExpanded = expand;

        if (menuAnimation != null) {
            menuAnimation.stop();
        }

        double targetHeight = expand ? (periodsSubMenu.getChildren().size() * 30.0) : 0.0;

        if (expand) {
            periodsSubMenu.setVisible(true);
            periodsSubMenu.setManaged(true);
        }

        menuAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(periodsSubMenu.prefHeightProperty(), periodsSubMenu.getPrefHeight()),
                        new KeyValue(periodsSubMenu.maxHeightProperty(), periodsSubMenu.getMaxHeight())
                ),
                new KeyFrame(Duration.millis(180),
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