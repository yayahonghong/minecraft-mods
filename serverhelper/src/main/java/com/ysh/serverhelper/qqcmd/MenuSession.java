package com.ysh.serverhelper.qqcmd;

import java.time.Instant;
import java.util.List;

public class MenuSession {
    private static final long TIMEOUT_SECONDS = 60;

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

    public boolean isExpired() {
        return createdAt.plusSeconds(TIMEOUT_SECONDS).isBefore(Instant.now());
    }

    public long getUserId() { return userId; }
    public long getGroupId() { return groupId; }
    public List<MenuItem> getItems() { return items; }
}
