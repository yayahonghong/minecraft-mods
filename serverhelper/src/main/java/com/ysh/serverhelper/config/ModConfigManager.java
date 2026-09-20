package com.ysh.serverhelper.config;

import com.ysh.serverhelper.ServerHelperMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
                String migrated = migrateLegacyQQConfig(content, gson);
                if (!migrated.equals(content)) {
                    ServerHelperMod.LOGGER.warn("检测到旧版 NapCat 配置(qq 段)，已自动迁移为 astrbot 段；" +
                            "请手动补充 base_url、api_key 等 AstrBot 参数后重新加载");
                    Files.writeString(CONFIG_PATH, migrated);
                    ServerHelperMod.LOGGER.info("Migrated configuration saved to {}", CONFIG_PATH);
                }
                config = gson.fromJson(migrated, ModConfig.class);
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

    /**
     * 检测旧版 NapCat 的 "qq" 配置段并自动迁移为 "astrbot" 段（纯函数，便于测试）。
     * 可搬运：enabled / command_prefix / admin_qq / insecure_tls / group_id(转 umo)。
     * base_url / api_key / bridge_url / internal_token 无法推断，需手动填写。
     */
    static String migrateLegacyQQConfig(String content, Gson gson) {
        try {
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (!root.has("qq") || root.has("astrbot")) {
                return content;
            }

            JsonObject legacyQQ = root.getAsJsonObject("qq");
            JsonObject astrbot = new JsonObject();
            if (legacyQQ.has("enabled")) astrbot.add("enabled", legacyQQ.get("enabled"));
            if (legacyQQ.has("command_prefix")) astrbot.add("command_prefix", legacyQQ.get("command_prefix"));
            if (legacyQQ.has("admin_qq")) astrbot.add("admin_qq", legacyQQ.get("admin_qq"));
            if (legacyQQ.has("insecure_tls")) astrbot.add("insecure_tls", legacyQQ.get("insecure_tls"));
            if (legacyQQ.has("group_id")) {
                astrbot.addProperty("umo", "aiocqhttp:GroupMessage:" + legacyQQ.get("group_id").getAsString());
            }

            root.remove("qq");
            root.add("astrbot", astrbot);
            return gson.toJson(root);
        } catch (Exception e) {
            return content;
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
