package com.ysh.serverhelper.qqcmd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MenuSessionManager {
    private static final Map<Long, MenuSession> SESSIONS = new ConcurrentHashMap<>();

    public static boolean hasActive(long userId) {
        var session = SESSIONS.get(userId);
        if (session == null) return false;
        if (session.isExpired()) {
            SESSIONS.remove(userId);
            return false;
        }
        return true;
    }

    public static MenuSession get(long userId) {
        var session = SESSIONS.get(userId);
        if (session != null && session.isExpired()) {
            SESSIONS.remove(userId);
            return null;
        }
        return session;
    }

    public static void set(MenuSession session) {
        SESSIONS.put(session.getUserId(), session);
    }

    public static void remove(long userId) {
        SESSIONS.remove(userId);
    }
}
