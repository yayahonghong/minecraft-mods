package com.ysh.serverhelper.notifier;

/**
 * 通知接口
 */
public interface Notifier {
    /**
     * 获取通知类型名称
     * @return 类型名称
     */
    String getName();

    /**
     * 是否启用
     * @return true 启用  false 关闭
     */
    boolean isEnabled();

    /**
     * 发送消息
     * @param message 消息内容
     */
    void send(String message);
}
