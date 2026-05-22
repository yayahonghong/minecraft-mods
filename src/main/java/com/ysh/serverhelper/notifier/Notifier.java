package com.ysh.serverhelper.notifier;

public interface Notifier {
    String getName();
    boolean isEnabled();
    void send(JoinNotification notification, String message);
}
