package com.frogilik.timestats.util;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SingleInstanceManager {

    // Выбираем уникальный порт (главное, чтобы не пересекался с другими программами)
    private static final int PORT = 47852;
    private static ServerSocket serverSocket;

    /**
     * Проверяет, запущена ли уже копия приложения.
     * Если запущена — отправляет сигнал "SHOW" и возвращает true.
     * Если не запущена — открывает сокет и возвращает false.
     */
    public static boolean isAlreadyRunning(Runnable onShowRequested) {
        try {
            // Пытаемся занять порт на локальном интерфейсе (127.0.0.1)
            serverSocket = new ServerSocket(PORT, 10, InetAddress.getByName("127.0.0.1"));

            // Если успешно подняли сервер — запускаем фоновый поток прослушивания команд
            Thread listenerThread = new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                        String message = in.readLine();
                        if ("SHOW".equals(message)) {
                            // Передаем управление в JavaFX поток для разворачивания окна
                            Platform.runLater(onShowRequested);
                        }
                    } catch (Exception ignored) {
                        // Игнорируем ошибки закрытия сокета при выходе
                    }
                }
            });
            listenerThread.setDaemon(true); // Поток умрёт вместе с приложением
            listenerThread.start();

            return false; // Приложение запускается впервые

        } catch (Exception e) {
            // Если занять порт не удалось — приложение УЖЕ запущено
            notifyFirstInstance();
            return true;
        }
    }

    /**
     * Отправляет сигнал уже запущенному экземпляру
     */
    private static void notifyFirstInstance() {
        try (Socket socket = new Socket("127.0.0.1", PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
            out.println("SHOW");
        } catch (Exception ignored) {
            // Ошибка связи с первым экземпляром
        }
    }
}