package com.frogilik.timestats.core;

public class MockWindowTracker implements WindowTracker {
    @Override
    public String getActiveProcessName() {
        return "idea64.exe"; // Для имитации работы
    }

    @Override
    public String getActiveWindowTitle() {
        return "frogTimeStats – Main.java";
    }
}