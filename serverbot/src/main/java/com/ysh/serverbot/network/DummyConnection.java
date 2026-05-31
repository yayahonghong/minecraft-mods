package com.ysh.serverbot.network;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

/**
 * 虚拟网络连接，用于假人（Bot）实体。
 * 假人不需要实际的物理网络连接，因此需要重写此类中的所有网络收发操作（如 send, disconnect 等）为空方法。
 * 这样可以欺骗 Minecraft 服务端，使其认为假人是一个正常的在线玩家，避免抛出空指针或网络断开异常。
 */
public class DummyConnection extends Connection {

    public DummyConnection(PacketFlow packetFlow) {
        super(packetFlow);
    }

    @Override
    public boolean isMemoryConnection() {
        return true;
    }

    @Override
    public void send(Packet<?> packet) {
    }

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener) {
    }

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocol, T listener) {
    }

    @Override
    public void setupOutboundProtocol(ProtocolInfo<?> protocol) {
    }

    @Override
    public void disconnect(Component message) {
    }

    @Override
    public void disconnect(DisconnectionDetails details) {
    }
}
