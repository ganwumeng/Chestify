package com.ganwumeng.chestify;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ChestifyConfig {
    public static final int DEFAULT_SEARCH_RADIUS = 40;
    public static final int MIN_SEARCH_RADIUS = 4;
    public static final int MAX_SEARCH_RADIUS = 128;

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("chestify.properties");
    private static int searchRadius = DEFAULT_SEARCH_RADIUS;
    private static boolean canEditServerConfig;

    private ChestifyConfig() {
    }

    public static int searchRadius() {
        return searchRadius;
    }

    public static void setSearchRadius(int radius) {
        searchRadius = clampRadius(radius);
    }

    public static boolean canEditServerConfig() {
        return canEditServerConfig;
    }

    public static void setCanEditServerConfig(boolean canEdit) {
        canEditServerConfig = canEdit;
    }

    public static int clampRadius(int radius) {
        return Math.max(MIN_SEARCH_RADIUS, Math.min(MAX_SEARCH_RADIUS, radius));
    }

    public static void load() {
        Properties properties = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                properties.load(reader);
            } catch (IOException ignored) {
                return;
            }
        }

        String value = properties.getProperty("searchRadius");
        if (value != null) {
            try {
                setSearchRadius(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                setSearchRadius(DEFAULT_SEARCH_RADIUS);
            }
        }
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("searchRadius", Integer.toString(searchRadius));
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                properties.store(writer, "Chestify configuration");
            }
        } catch (IOException ignored) {
        }
    }
}
