package com.frogilik.timestats.core;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class WindowsWindowTracker implements WindowTracker {

    @Override
    public String getActiveProcessName() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) return "Unknown";

        IntByReference processId = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, processId);

        // В Windows для чтения информации о процессе нужны соответствующие флаги доступа
        HANDLE process = Kernel32.INSTANCE.OpenProcess(
                Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ,
                false,
                processId.getValue()
        );

        if (process != null) {
            try {
                char[] buffer = new char[1024];

                // Используем GetModuleFileNameExW
                int len = Psapi.INSTANCE.GetModuleFileNameExW(process, null, buffer, buffer.length);

                if (len > 0) {
                    String fullPath = new String(buffer, 0, len);
                    // Извлекаем только имя файла (например, "chrome.exe") из полного пути
                    return new File(fullPath).getName();
                }
            } finally {
                // Обязательно закрываем хэндл процесса
                Kernel32.INSTANCE.CloseHandle(process);
            }
        }

        return "Unknown";
    }

    @Override
    public String getActiveWindowTitle() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) return "";

        char[] buffer = new char[1024];
        User32.INSTANCE.GetWindowText(hwnd, buffer, 1024);
        return Native.toString(buffer).trim();
    }
}