package com.ysh.serverhelper.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private AstrBotConfig astrbot = new AstrBotConfig();
    private List<String> excluded_players = new ArrayList<>();
    private Map<String, EventConfig> events = defaultEvents();

    public AstrBotConfig getAstrbot() { return astrbot; }
    public void setAstrbot(AstrBotConfig astrbot) { this.astrbot = astrbot; }
    public List<String> getExcludedPlayers() { return excluded_players; }
    public void setExcludedPlayers(List<String> excluded_players) { this.excluded_players = excluded_players; }
    public Map<String, EventConfig> getEvents() { return events; }
    public void setEvents(Map<String, EventConfig> events) { this.events = events; }

    public static class AstrBotConfig {
        private boolean enabled = false;
        private String base_url = "http://localhost:6185";
        private String api_key = "";
        private String umo = "";
        private String bridge_url = "";
        private String internal_token = "";
        private int poll_interval_ms = 1500;
        private String command_prefix = "#";
        private List<String> admin_qq = new ArrayList<>();
        private boolean insecure_tls = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBaseUrl() { return base_url; }
        public void setBaseUrl(String base_url) { this.base_url = base_url; }
        public String getApiKey() { return api_key; }
        public void setApiKey(String api_key) { this.api_key = api_key; }
        public String getUmo() { return umo; }
        public void setUmo(String umo) { this.umo = umo; }
        public String getBridgeUrl() { return bridge_url; }
        public void setBridgeUrl(String bridge_url) { this.bridge_url = bridge_url; }
        public String getInternalToken() { return internal_token; }
        public void setInternalToken(String internal_token) { this.internal_token = internal_token; }
        public int getPollIntervalMs() { return poll_interval_ms; }
        public void setPollIntervalMs(int poll_interval_ms) { this.poll_interval_ms = poll_interval_ms; }
        public String getCommandPrefix() { return command_prefix; }
        public void setCommandPrefix(String command_prefix) { this.command_prefix = command_prefix; }
        public List<String> getAdminQq() { return admin_qq; }
        public void setAdminQq(List<String> admin_qq) { this.admin_qq = admin_qq; }
        public boolean isInsecureTls() { return insecure_tls; }
        public void setInsecureTls(boolean insecure_tls) { this.insecure_tls = insecure_tls; }
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
