package com.ysh.serverhelper.config;

import com.ysh.serverhelper.ServerHelperMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModConfigManager {
    private static final Path CONFIG_PATH = Paths.get("config", "serverhelper.json");
    private final Gson gson;
    private ModConfig config;

    public ModConfigManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.config = new ModConfig();
    }

    public void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String content = Files.readString(CONFIG_PATH);
                config = gson.fromJson(content, ModConfig.class);
                if (config == null) config = new ModConfig();
                ServerHelperMod.LOGGER.info("Configuration loaded from {}", CONFIG_PATH);
            } catch (IOException e) {
                ServerHelperMod.LOGGER.warn("Failed to load config, using defaults", e);
                config = new ModConfig();
            }
        } else {
            ServerHelperMod.LOGGER.info("No config at {}, creating default", CONFIG_PATH);
            save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, gson.toJson(config));
            ServerHelperMod.LOGGER.info("Configuration saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            ServerHelperMod.LOGGER.warn("Failed to save config", e);
        }
    }

    public ModConfig getConfig() { return config; }
    public void reload() { load(); }
}
