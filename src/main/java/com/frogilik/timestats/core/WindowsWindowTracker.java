package com.frogilik.timestats.core;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class WindowsWindowTracker implements WindowTracker {

    private static boolean debugPrivilegeEnabled = false;

    public WindowsWindowTracker() {
        if (!debugPrivilegeEnabled) {
            tryEnableDebugPrivilege();
        }
    }

    private static void tryEnableDebugPrivilege() {
        // Используем HANDLEByReference для надежного получения токена
        HANDLEByReference hTokenRef = new HANDLEByReference();

        if (!Advapi32.INSTANCE.OpenProcessToken(
                Kernel32.INSTANCE.GetCurrentProcess(),
                WinNT.TOKEN_ADJUST_PRIVILEGES | WinNT.TOKEN_QUERY,
                hTokenRef)) {
            return;
        }

        HANDLE hToken = hTokenRef.getValue();

        try {
            WinNT.LUID luid = new WinNT.LUID();
            if (!Advapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_DEBUG_NAME, luid)) {
                return;
            }

            WinNT.TOKEN_PRIVILEGES tp = new WinNT.TOKEN_PRIVILEGES(1);
            tp.Privileges[0] = new WinNT.LUID_AND_ATTRIBUTES(luid, new DWORD(WinNT.SE_PRIVILEGE_ENABLED));

            if (Advapi32.INSTANCE.AdjustTokenPrivileges(hToken, false, tp, 0, null, null)) {
                if (Kernel32.INSTANCE.GetLastError() == WinError.ERROR_SUCCESS) {
                    debugPrivilegeEnabled = true;
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(hToken);
        }
    }

    @Override
    public String getActiveProcessName() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) return "Idle";

        IntByReference processId = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, processId);

        if (processId.getValue() == 0) {
            return "System Idle Process";
        }

        // Запрашиваем ограниченные права чтения информации о процессе
        HANDLE process = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_LIMITED_INFORMATION,
                false,
                processId.getValue()
        );

        if (process == null) {
            // Фолбэк для старых версий Windows или заблокированных процессов
            process = Kernel32.INSTANCE.OpenProcess(
                    Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ,
                    false,
                    processId.getValue()
            );
        }

        if (process != null) {
            try {
                char[] buffer = new char[1024];

                // 1. Пробуем GetModuleFileNameExW
                int len = Psapi.INSTANCE.GetModuleFileNameExW(process, null, buffer, buffer.length);
                if (len > 0) {
                    return new File(new String(buffer, 0, len)).getName();
                }

                // 2. Пробуем QueryFullProcessImageNameW (Корректная передача IntByReference)
                IntByReference size = new IntByReference(buffer.length);
                if (Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, buffer, size)) {
                    if (size.getValue() > 0) {
                        return new File(new String(buffer, 0, size.getValue())).getName();
                    }
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(process);
            }
        }

        return "Unknown Process";
    }

    @Override
    public String getActiveWindowTitle() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) return "Desktop";

        char[] buffer = new char[2048];
        int length = User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
        if (length > 0) {
            return Native.toString(buffer).trim();
        }
        return "";
    }
}