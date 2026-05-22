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
        config.getQq().setGroupId(12345L);
        config.getQq().setCommandPrefix("!");
        config.getQq().getAdminQq().add(10001L);

        String json = gson.toJson(config);
        ModConfig parsed = gson.fromJson(json, ModConfig.class);
        assertNotNull(parsed);
        assertTrue(parsed.getExcludedPlayers().contains("Steve"));
        assertEquals(12345L, parsed.getQq().getGroupId());
        assertEquals("!", parsed.getQq().getCommandPrefix());
        assertEquals(1, parsed.getQq().getAdminQq().size());
        assertEquals(8, parsed.getEvents().size());
    }

    @Test
    void testDefaultEvents() {
        ModConfig config = new ModConfig();
        assertTrue(config.getEvents().get("join").isEnabled());
        assertTrue(config.getEvents().get("join").getMessage().contains("{player}"));
        assertFalse(config.getEvents().get("chat").isEnabled());
    }
}
