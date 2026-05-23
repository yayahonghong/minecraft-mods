package com.ysh.smartplace;

import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SmartPlaceState {
    private static final Map<UUID, Boolean> enabledPlayers = new ConcurrentHashMap<>();

    public static boolean isEnabled(ServerPlayer player) {
        return enabledPlayers.getOrDefault(player.getUUID(), false);
    }

    public static boolean toggle(ServerPlayer player) {
        return enabledPlayers.compute(player.getUUID(), (k, v) -> v == null || !v);
    }

    private SmartPlaceState() {}
}
