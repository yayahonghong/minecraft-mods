package com.ysh.serverhelper.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModConfigManagerTest {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    void testConfigSerializationRoundTrip() {
        ModConfig config = new ModConfig();
        config.getExcludedPlayers().add("Steve");
        config.getAstrbot().setUmo("aiocqhttp:GroupMessage:12345");
        config.getAstrbot().setApiKey("abk_test");
        config.getAstrbot().setBridgeUrl("https://example.com/mcbridge");
        config.getAstrbot().setInternalToken("tok");
        config.getAstrbot().setCommandPrefix("!");
        config.getAstrbot().getAdminQq().add("10001");
        config.getAstrbot().setInsecureTls(true);

        String json = gson.toJson(config);
        ModConfig parsed = gson.fromJson(json, ModConfig.class);
        assertNotNull(parsed);
        assertTrue(parsed.getExcludedPlayers().contains("Steve"));
        assertEquals("aiocqhttp:GroupMessage:12345", parsed.getAstrbot().getUmo());
        assertEquals("abk_test", parsed.getAstrbot().getApiKey());
        assertEquals("https://example.com/mcbridge", parsed.getAstrbot().getBridgeUrl());
        assertEquals("!", parsed.getAstrbot().getCommandPrefix());
        assertTrue(parsed.getAstrbot().isInsecureTls());
        assertEquals("10001", parsed.getAstrbot().getAdminQq().get(0));
        assertEquals(8, parsed.getEvents().size());
    }

    @Test
    void testDefaultEvents() {
        ModConfig config = new ModConfig();
        assertTrue(config.getEvents().get("join").isEnabled());
        assertTrue(config.getEvents().get("join").getMessage().contains("{player}"));
        assertFalse(config.getEvents().get("chat").isEnabled());
    }

    @Test
    void testLegacyQQMigration() {
        String legacy = """
                {
                  "qq": {
                    "enabled": true,
                    "api_url": "http://localhost:3000",
                    "token": "secret",
                    "group_id": 251243068,
                    "command_prefix": "#",
                    "insecure_tls": false,
                    "admin_qq": ["3281952670"]
                  },
                  "events": {},
                  "excluded_players": []
                }
                """;

        String migrated = ModConfigManager.migrateLegacyQQConfig(legacy, gson);
        assertFalse(migrated.contains("\"qq\""));
        assertTrue(migrated.contains("aiocqhttp:GroupMessage:251243068"));

        ModConfig parsed = gson.fromJson(migrated, ModConfig.class);
        assertTrue(parsed.getAstrbot().isEnabled());
        assertEquals("#", parsed.getAstrbot().getCommandPrefix());
        assertEquals("3281952670", parsed.getAstrbot().getAdminQq().get(0));
        assertEquals("aiocqhttp:GroupMessage:251243068", parsed.getAstrbot().getUmo());
        assertTrue(parsed.getAstrbot().getApiKey().isEmpty());
    }

    @Test
    void testNoMigrationWhenAstrbotPresent() {
        String modern = """
                {
                  "qq": { "enabled": false },
                  "astrbot": { "enabled": true, "api_key": "abk_x" }
                }
                """;
        String result = ModConfigManager.migrateLegacyQQConfig(modern, gson);
        assertSame(modern, result);
    }
}
