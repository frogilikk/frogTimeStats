package com.frogilik.timestats.util;

import java.io.InputStream;
import java.util.Properties;

public class AppVersion {

    private static String version = "Unknown";

    static {
        try (InputStream input = AppVersion.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                version = prop.getProperty("app.version", "Unknown");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getVersion() {
        return version;
    }
}