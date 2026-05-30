package com.ysh.serverhelper.qqcmd;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MenuSession {
    private static final long TIMEOUT_SECONDS = 60;
    private static final Map<Long, MenuSession> SESSIONS = new ConcurrentHashMap<>();

    private final long userId;
    private final long groupId;
    private final List<MenuItem> items;
    private final Instant createdAt;

    public record MenuItem(String label, String action) {}

    public MenuSession(long userId, long groupId, List<MenuItem> items) {
        this.userId = userId;
        this.groupId = groupId;
        this.items = items;
        this.createdAt = Instant.now();
    }

    public static boolean hasActive(long userId) {
        var s = SESSIONS.get(userId);
        if (s == null) return false;
        if (s.isExpired()) { SESSIONS.remove(userId); return false; }
        return true;
    }

    public static MenuSession get(long userId) {
        var s = SESSIONS.get(userId);
        if (s != null && s.isExpired()) { SESSIONS.remove(userId); return null; }
        return s;
    }

    public static void set(MenuSession s) { SESSIONS.put(s.userId, s); }
    public static void remove(long userId) { SESSIONS.remove(userId); }

    private boolean isExpired() {
        return createdAt.plusSeconds(TIMEOUT_SECONDS).isBefore(Instant.now());
    }

    public long getUserId() { return userId; }
    public long getGroupId() { return groupId; }
    public List<MenuItem> getItems() { return items; }
}
