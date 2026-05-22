package com.ysh.serverhelper.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private QQConfig qq = new QQConfig();
    private List<String> excluded_players = new ArrayList<>();
    private Map<String, EventConfig> events = defaultEvents();

    public QQConfig getQq() { return qq; }
    public void setQq(QQConfig qq) { this.qq = qq; }
    public List<String> getExcludedPlayers() { return excluded_players; }
    public void setExcludedPlayers(List<String> excluded_players) { this.excluded_players = excluded_players; }
    public Map<String, EventConfig> getEvents() { return events; }
    public void setEvents(Map<String, EventConfig> events) { this.events = events; }

    public static class QQConfig {
        private boolean enabled = false;
        private String api_url = "http://localhost:3000";
        private String token = "";
        private long group_id = 0;
        private int callback_port = 8080;
        private String command_prefix = "#";
        private List<Long> admin_qq = new ArrayList<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getApiUrl() { return api_url; }
        public void setApiUrl(String api_url) { this.api_url = api_url; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public long getGroupId() { return group_id; }
        public void setGroupId(long group_id) { this.group_id = group_id; }
        public int getCallbackPort() { return callback_port; }
        public void setCallbackPort(int callback_port) { this.callback_port = callback_port; }
        public String getCommandPrefix() { return command_prefix; }
        public void setCommandPrefix(String command_prefix) { this.command_prefix = command_prefix; }
        public List<Long> getAdminQq() { return admin_qq; }
        public void setAdminQq(List<Long> admin_qq) { this.admin_qq = admin_qq; }
    }

    public static class EventConfig {
        private boolean enabled = true;
        private String message = "";

        public EventConfig() {}
        public EventConfig(boolean enabled, String message) {
            this.enabled = enabled;
            this.message = message;
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    private static Map<String, EventConfig> defaultEvents() {
        Map<String, EventConfig> map = new LinkedHashMap<>();
        map.put("join", new EventConfig(true, "🟢 {player} 加入了游戏"));
        map.put("quit", new EventConfig(true, "🔴 {player} 退出了游戏"));
        map.put("death", new EventConfig(true, "💀 {player} {death_message}"));
        map.put("advancement", new EventConfig(true, "🏆 {player} 获得了成就 {advancement}"));
        map.put("chat", new EventConfig(false, "💬 {player}: {message}"));
        map.put("server_start", new EventConfig(true, "✅ 服务器已启动"));
        map.put("server_stop", new EventConfig(true, "🛑 服务器即将关闭"));
        map.put("op_change", new EventConfig(true, "👑 {player} 的 OP 状态已变更为 {status}"));
        return map;
    }
}
