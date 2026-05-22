package com.ysh.serverhelper.notifier;

public class JoinNotification {
    private final String playerName;
    private final String uuid;
    private final String ip;
    private final String clientBrand;
    private final long joinTime;

    public JoinNotification(String playerName, String uuid, String ip, String clientBrand, long joinTime) {
        this.playerName = playerName; this.uuid = uuid; this.ip = ip;
        this.clientBrand = clientBrand; this.joinTime = joinTime;
    }

    public String getPlayerName() { return playerName; }
    public String getUuid() { return uuid; }
    public String getIp() { return ip; }
    public String getClientBrand() { return clientBrand; }
    public long getJoinTime() { return joinTime; }
}
