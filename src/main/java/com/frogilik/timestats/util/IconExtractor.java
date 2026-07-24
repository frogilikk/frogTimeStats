package com.frogilik.timestats.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IconExtractor {

    private static final Map<String, Image> ICON_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> PENDING_SEARCH = new ConcurrentHashMap<>();
    private static final ExecutorService ASYNC_POOL = Executors.newFixedThreadPool(2);
    private static Image DEFAULT_ICON;

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("nux")
            || System.getProperty("os.name").toLowerCase().contains("nix");

    private static final Map<String, String[]> KNOWN_ALIASES = new HashMap<>();

    static {
        // IntelliJ IDEA
        KNOWN_ALIASES.put("jetbrains-idea", new String[]{"idea", "intellij-idea-ultimate", "intellij-idea-community", "idea64"});
        KNOWN_ALIASES.put("idea", new String[]{"idea", "intellij-idea-ultimate", "intellij-idea-community"});
        KNOWN_ALIASES.put("idea64", new String[]{"idea", "intellij-idea-ultimate"});

        // Spotify
        KNOWN_ALIASES.put("spotify", new String[]{"com.spotify.Client", "spotify", "spotify-client"});
        KNOWN_ALIASES.put("com.spotify.client", new String[]{"com.spotify.Client", "spotify"});

        // KDE & System
        KNOWN_ALIASES.put("org.kde.plasmashell", new String[]{"plasma", "kde", "preferences-desktop-wallpaper"});
        KNOWN_ALIASES.put("plasmashell", new String[]{"plasma", "kde"});

        // Твой трекер
        KNOWN_ALIASES.put("frogtimestats", new String[]{"time-tracker", "clock", "preferences-system-time"});

        // Другие популярные
        KNOWN_ALIASES.put("code-url-handler", new String[]{"com.visualstudio.code", "vscode"});
        KNOWN_ALIASES.put("code", new String[]{"com.visualstudio.code", "vscode"});
        KNOWN_ALIASES.put("vscode", new String[]{"com.visualstudio.code", "vscode"});
        KNOWN_ALIASES.put("telegramdesktop", new String[]{"telegram", "org.telegram.desktop"});
        KNOWN_ALIASES.put("org.telegram.desktop", new String[]{"telegram", "org.telegram.desktop"});
        KNOWN_ALIASES.put("steamwebhelper", new String[]{"steam"});
        KNOWN_ALIASES.put("steam", new String[]{"steam"});
        KNOWN_ALIASES.put("discord", new String[]{"discord", "com.discordapp.Discord"});
        KNOWN_ALIASES.put("vesktop", new String[]{"vesktop", "dev.vencord.Vesktop"});
        KNOWN_ALIASES.put("org.kde.konsole", new String[]{"utilities-terminal", "konsole"});
        KNOWN_ALIASES.put("org.kde.dolphin", new String[]{"system-file-manager", "dolphin"});
    }

    public static Image getIconForProcess(String processName, String executablePath) {
        if (processName == null || processName.isBlank()) {
            return getDefaultIcon();
        }

        String key = processName.toLowerCase().trim();

        Image cached = ICON_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        if (!PENDING_SEARCH.containsKey(key)) {
            PENDING_SEARCH.put(key, true);
            ASYNC_POOL.submit(() -> {
                try {
                    Image icon = null;
                    if (IS_WINDOWS && executablePath != null && !executablePath.isBlank()) {
                        icon = extractIconFromFileSystemWindows(executablePath);
                    } else if (IS_LINUX) {
                        icon = extractIconLinux(key, executablePath);
                    }

                    if (icon != null) {
                        ICON_CACHE.put(key, icon);
                    }
                } finally {
                    PENDING_SEARCH.remove(key);
                }
            });
        }

        return getDefaultIcon();
    }

    private static Image extractIconLinux(String processName, String executablePath) {
        String name = processName.toLowerCase().trim();

        // 1. Проверяем алиасы
        if (KNOWN_ALIASES.containsKey(name)) {
            for (String alias : KNOWN_ALIASES.get(name)) {
                Image img = searchIconInSystemFast(alias);
                if (img != null) return img;
            }
        }

        // 2. Прямой поиск
        Image img = searchIconInSystemFast(processName); // пробуем оригинальное имя с регистром
        if (img != null) return img;

        img = searchIconInSystemFast(name);
        if (img != null) return img;

        // 3. Короткое имя без доменов (например, org.kde.kate -> kate)
        if (name.contains(".")) {
            String shortName = name.substring(name.lastIndexOf('.') + 1);
            img = searchIconInSystemFast(shortName);
            if (img != null) return img;

            if (KNOWN_ALIASES.containsKey(shortName)) {
                for (String alias : KNOWN_ALIASES.get(shortName)) {
                    Image imgAlias = searchIconInSystemFast(alias);
                    if (imgAlias != null) return imgAlias;
                }
            }
        }

        // 4. Сканирование .desktop файлов
        String iconFromDesktop = findIconFromDesktop(name, executablePath);
        if (iconFromDesktop != null) {
            img = searchIconInSystemFast(iconFromDesktop);
            if (img != null) return img;
        }

        return null;
    }

    private static String findIconFromDesktop(String appName, String exePath) {
        String[] desktopDirs = {
                "/usr/share/applications/",
                System.getProperty("user.home") + "/.local/share/applications/",
                "/var/lib/flatpak/exports/share/applications/",
                System.getProperty("user.home") + "/.local/share/flatpak/exports/share/applications/"
        };

        String cleanExe = (exePath != null && !exePath.isBlank()) ? new File(exePath).getName().toLowerCase() : appName;

        for (String dirPath : desktopDirs) {
            File dir = new File(dirPath);
            if (!dir.exists() || !dir.isDirectory()) continue;

            File[] files = dir.listFiles((d, name) -> name.endsWith(".desktop"));
            if (files == null) continue;

            for (File file : files) {
                String fname = file.getName().toLowerCase();
                if (fname.contains(appName) || fname.contains(cleanExe)) {
                    String icon = parseDesktopFile(file);
                    if (icon != null) return icon;
                }
            }
        }
        return null;
    }

    private static String parseDesktopFile(File file) {
        try (java.util.Scanner scanner = new java.util.Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.startsWith("Icon=")) {
                    String icon = line.substring(5).trim();
                    if (icon.endsWith(".png") || icon.endsWith(".svg") || icon.endsWith(".xpm")) {
                        icon = icon.substring(0, icon.lastIndexOf('.'));
                    }
                    return icon;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Image searchIconInSystemFast(String iconName) {
        if (iconName == null || iconName.isBlank()) return null;

        if (iconName.startsWith("/")) {
            File f = new File(iconName);
            if (f.exists()) return loadFxImage(f);
        }

        String userHome = System.getProperty("user.home");

        // Проверяем с сохранением чувствительности к регистру (для com.spotify.Client)
        String[] variants = { iconName, iconName.toLowerCase() };

        for (String cleanName : variants) {
            String[] fastPaths = {
                    // Flatpak (Spotify часто лежит тут с регистром!)
                    "/var/lib/flatpak/exports/share/icons/hicolor/48x48/apps/" + cleanName + ".png",
                    "/var/lib/flatpak/exports/share/icons/hicolor/64x64/apps/" + cleanName + ".png",
                    "/var/lib/flatpak/exports/share/icons/hicolor/scalable/apps/" + cleanName + ".svg",
                    userHome + "/.local/share/flatpak/exports/share/icons/hicolor/48x48/apps/" + cleanName + ".png",

                    // Системные иконы
                    "/usr/share/pixmaps/" + cleanName + ".png",
                    "/usr/share/pixmaps/" + cleanName + ".svg",
                    "/usr/share/icons/hicolor/48x48/apps/" + cleanName + ".png",
                    "/usr/share/icons/hicolor/64x64/apps/" + cleanName + ".png",
                    "/usr/share/icons/hicolor/128x128/apps/" + cleanName + ".png",
                    "/usr/share/icons/hicolor/scalable/apps/" + cleanName + ".svg",
                    "/usr/share/icons/breeze/apps/48/" + cleanName + ".svg",
                    "/usr/share/icons/papirus/48x48/apps/" + cleanName + ".png",
                    "/usr/share/icons/papirus/scalable/apps/" + cleanName + ".svg",
                    userHome + "/.local/share/icons/hicolor/48x48/apps/" + cleanName + ".png"
            };

            for (String path : fastPaths) {
                File file = new File(path);
                if (file.exists()) {
                    if (path.endsWith(".svg")) {
                        Image img = convertSvgToPngImage(file);
                        if (img != null) return img;
                    } else {
                        Image img = loadFxImage(file);
                        if (img != null) return img;
                    }
                }
            }
        }

        return null;
    }

    private static Image extractIconFromFileSystemWindows(String exePath) {
        try {
            File file = new File(exePath);
            if (!file.exists()) return null;

            Icon swingIcon = FileSystemView.getFileSystemView().getSystemIcon(file);
            if (swingIcon == null) return null;

            int width = swingIcon.getIconWidth();
            int height = swingIcon.getIconHeight();
            if (width <= 0 || height <= 0) return null;

            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = bufferedImage.createGraphics();
            swingIcon.paintIcon(null, g2, 0, 0);
            g2.dispose();

            WritableImage fxImage = new WritableImage(width, height);
            PixelWriter writer = fxImage.getPixelWriter();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    writer.setArgb(x, y, bufferedImage.getRGB(x, y));
                }
            }
            return fxImage;
        } catch (Exception e) {
            return null;
        }
    }

    private static Image convertSvgToPngImage(File svgFile) {
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{
                    "rsvg-convert", "-w", "32", "-h", "32", svgFile.getAbsolutePath()
            });
            Image img = new Image(proc.getInputStream());
            if (!img.isError() && img.getWidth() > 0) {
                return img;
            }
        } catch (Exception ignored) {}
        return loadFxImage(svgFile);
    }

    private static Image loadFxImage(File file) {
        try {
            Image img = new Image(file.toURI().toString(), 24, 24, true, true);
            if (!img.isError() && img.getWidth() > 0) {
                return img;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Image getDefaultIcon() {
        if (DEFAULT_ICON == null) {
            WritableImage img = new WritableImage(16, 16);
            PixelWriter pw = img.getPixelWriter();
            Color defaultColor = Color.rgb(166, 227, 161);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    pw.setColor(x, y, defaultColor);
                }
            }
            DEFAULT_ICON = img;
        }
        return DEFAULT_ICON;
    }
}