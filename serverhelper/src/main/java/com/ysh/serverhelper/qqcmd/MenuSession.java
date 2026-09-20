package com.ysh.serverhelper.qqcmd;

import java.time.Instant;
import java.util.List;

public class MenuSession {
    private static final long TIMEOUT_SECONDS = 60;

    private final String userId;
    private final String groupId;
    private final List<MenuItem> items;
    private final Instant createdAt;

    public record MenuItem(String label, String action) {}

    public MenuSession(String userId, String groupId, List<MenuItem> items) {
        this.userId = userId;
        this.groupId = groupId;
        this.items = items;
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return createdAt.plusSeconds(TIMEOUT_SECONDS).isBefore(Instant.now());
    }

    public String getUserId() { return userId; }
    public String getGroupId() { return groupId; }
    public List<MenuItem> getItems() { return items; }
}
