package com.frogilik.timestats.core;

public interface WindowTracker {
    String getActiveProcessName();
    String getActiveWindowTitle();
    String getActiveProcessPath(); // <--- Новый метод
}