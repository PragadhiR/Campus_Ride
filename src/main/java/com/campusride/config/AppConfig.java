package com.campusride.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages application configuration and database credentials.
 * Loads from environment variables first, then local .env file, with sensible local defaults.
 */
public class AppConfig {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 3306;
    private static final String DEFAULT_DB_NAME = "campusride";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static String dbHost = DEFAULT_HOST;
    private static int dbPort = DEFAULT_PORT;
    private static String dbName = DEFAULT_DB_NAME;
    private static String dbUser = DEFAULT_USER;
    private static String dbPassword = DEFAULT_PASSWORD;

    static {
        loadConfig();
    }

    public static synchronized void loadConfig() {
        Map<String, String> envMap = new HashMap<>();

        // 1. Try reading from .env file if it exists
        Path envPath = Paths.get(".env");
        if (Files.exists(envPath)) {
            try (BufferedReader reader = Files.newBufferedReader(envPath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int equalsIdx = line.indexOf('=');
                    if (equalsIdx > 0) {
                        String key = line.substring(0, equalsIdx).trim();
                        String value = line.substring(equalsIdx + 1).trim();
                        // Strip quotes if present
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        envMap.put(key, value);
                    }
                }
            } catch (IOException e) {
                System.err.println("Notice: Could not read .env file: " + e.getMessage());
            }
        }

        // 2. Resolve values with priority: System.getenv > .env > default
        dbHost = getEnvOrProperty("DB_HOST", envMap, DEFAULT_HOST);
        String portStr = getEnvOrProperty("DB_PORT", envMap, String.valueOf(DEFAULT_PORT));
        try {
            dbPort = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            dbPort = DEFAULT_PORT;
        }
        dbName = getEnvOrProperty("DB_NAME", envMap, DEFAULT_DB_NAME);
        dbUser = getEnvOrProperty("DB_USER", envMap, DEFAULT_USER);
        dbPassword = getEnvOrProperty("DB_PASSWORD", envMap, DEFAULT_PASSWORD);
    }

    private static String getEnvOrProperty(String key, Map<String, String> fileMap, String defaultValue) {
        String sysEnv = System.getenv(key);
        if (sysEnv != null && !sysEnv.trim().isEmpty()) {
            return sysEnv.trim();
        }
        String fileVal = fileMap.get(key);
        if (fileVal != null) {
            return fileVal;
        }
        return defaultValue;
    }

    /**
     * Persists updated database credentials to the local .env file.
     */
    public static synchronized void saveConfig(String host, int port, String name, String user, String password) throws IOException {
        dbHost = host;
        dbPort = port;
        dbName = name;
        dbUser = user;
        dbPassword = password;

        StringBuilder sb = new StringBuilder();
        sb.append("# CampusRide Database Credentials\n");
        sb.append("DB_HOST=").append(host).append("\n");
        sb.append("DB_PORT=").append(port).append("\n");
        sb.append("DB_NAME=").append(name).append("\n");
        sb.append("DB_USER=").append(user).append("\n");
        sb.append("DB_PASSWORD=").append(password).append("\n");

        Files.writeString(Paths.get(".env"), sb.toString());
    }

    public static String getDbHost() { return dbHost; }
    public static int getDbPort() { return dbPort; }
    public static String getDbName() { return dbName; }
    public static String getDbUser() { return dbUser; }
    public static String getDbPassword() { return dbPassword; }

    public static String getJdbcUrl() {
        return String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
            dbHost, dbPort, dbName
        );
    }
}
