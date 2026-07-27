package com.frogilik.timestats.model;

public enum ThemePalette {
    MINIONS(
            "Миньоны 🍌",
            "#1e1e2e", // Фон приложения
            "#2a2a3c", // Фон карточек
            "#fee12b", // Главный акцент (Миньон Yellow)
            "#4b8be8", // Вторичный акцент (Джинсовый Blue)
            "#ffffff", // Текст заголовков
            "#cdd6f4"  // Обычный текст
    ),
    HELLO_KITTY(
            "Hello Kitty 🎀",
            "#241d24", // Тёмно-сливовый фон
            "#322833", // Карточки
            "#ff79c6", // Главный акцент (Ярко-розовый)
            "#ffb86c", // Вторичный акцент (Персиковый)
            "#f8f8f2", // Текст заголовков (Бело-кремовый)
            "#e0d0e0"  // Обычный текст (Нежно-сиреневый/розовый)
    ),
    FROG(
            "Лягушка 🐸",
            "#182019", // Тёмно-зеленый лес
            "#212c23", // Карточки
            "#a6e3a1", // Неоново-зеленое яблоко
            "#94e2d5", // Мятный
            "#eafaf1", // Светло-зеленый текст
            "#a6adc8"  // Серый текст
    );

    private final String title;
    private final String bgColor;
    private final String cardBgColor;
    private final String primaryColor;
    private final String secondaryColor;
    private final String textColor;
    private final String subtextColor;

    ThemePalette(String title, String bgColor, String cardBgColor,
                 String primaryColor, String secondaryColor,
                 String textColor, String subtextColor) {
        this.title = title;
        this.bgColor = bgColor;
        this.cardBgColor = cardBgColor;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.textColor = textColor;
        this.subtextColor = subtextColor;
    }

    public String getTitle() { return title; }
    public String getBgColor() { return bgColor; }
    public String getCardBgColor() { return cardBgColor; }
    public String getPrimaryColor() { return primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }
    public String getTextColor() { return textColor; }
    public String getSubtextColor() { return subtextColor; }
}