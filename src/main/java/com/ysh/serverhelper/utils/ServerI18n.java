package com.ysh.serverhelper.utils;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ServerI18n extends Language {
    private static final Map<String, String> GLOBAL_TRANSLATIONS = new HashMap<>();

    private final Map<String, String> translations;
    private final Language fallback;

    public static void init() {
        Map<String, String> map = new HashMap<>();
        try (InputStream is = ServerI18n.class.getResourceAsStream("/zh_cn.json")) {
            if (is != null) {
                JsonObject json = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
                json.entrySet().forEach(e -> map.put(e.getKey(), e.getValue().getAsString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        GLOBAL_TRANSLATIONS.putAll(map);
        Language.inject(new ServerI18n(Language.getInstance(), map));
    }

    public static String get(String key) {
        return GLOBAL_TRANSLATIONS.get(key);
    }

    public ServerI18n(Language fallback, Map<String, String> translations) {
        this.fallback = fallback;
        this.translations = ImmutableMap.copyOf(translations);
    }

    @Override
    public String getOrDefault(String key) {
        return translations.getOrDefault(key, fallback.getOrDefault(key));
    }

    @Override
    public String getOrDefault(String key, String fallbackStr) {
        if (translations.containsKey(key)) return translations.get(key);
        return fallback.getOrDefault(key, fallbackStr);
    }

    @Override
    public boolean has(String key) {
        return translations.containsKey(key) || fallback.has(key);
    }

    @Override
    public boolean isDefaultRightToLeft() {
        return false;
    }

    @Override
    public FormattedCharSequence getVisualOrder(FormattedText text) {
        return fallback.getVisualOrder(text);
    }
}