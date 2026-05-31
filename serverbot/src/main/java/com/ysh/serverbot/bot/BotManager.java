package com.ysh.serverbot.bot;

import com.mojang.authlib.GameProfile;
import com.ysh.serverbot.network.BotNetworkHandler;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BotManager {
    private static final int MAX_BOTS = 5;
    private static final BotManager INSTANCE = new BotManager();

    private final Map<String, ServerBotPlayer> bots = new ConcurrentHashMap<>();

    private BotManager() {}

    public static BotManager getInstance() {
        return INSTANCE;
    }

    public ServerBotPlayer spawnBot(ServerPlayer owner) {
        if (bots.size() >= MAX_BOTS) {
            owner.sendSystemMessage(Component.literal("§c已达到最大假人数限制 (" + MAX_BOTS + " 个)"));
            return null;
        }

        String name = allocateName();
        if (name == null) {
            owner.sendSystemMessage(Component.literal("§c无法分配假人名称"));
            return null;
        }

        ServerLevel level = owner.level();
        MinecraftServer server = level.getServer();
        if (server == null) return null;

        GameProfile profile = new GameProfile(UUID.randomUUID(), name);

        ServerBotPlayer bot = new ServerBotPlayer(server, level, profile, ClientInformation.createDefault());

        Connection connection = new Connection(PacketFlow.CLIENTBOUND) {
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
            public void send(Packet<?> packet, ChannelFutureListener listener, boolean b) {
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
        };

        server.getPlayerList().placeNewPlayer(connection, bot, CommonListenerCookie.createInitial(profile, false));

        bot.connection = new BotNetworkHandler(server, bot, connection);

        bots.put(name, bot);
        server.getPlayerList().broadcastSystemMessage(
            Component.literal("§7[ServerBot] 假人 §a" + name + " §7已上线"),
            false
        );

        return bot;
    }

    public boolean removeBot(String name) {
        ServerBotPlayer bot = bots.remove(name);
        if (bot == null) return false;

        MinecraftServer server = bot.level().getServer();
        if (server != null) {
            bot.connection.disconnect(Component.literal("Bot removed"));
            server.getPlayerList().remove(bot);
            server.getPlayerList().broadcastSystemMessage(
                Component.literal("§7[ServerBot] 假人 §e" + name + " §7已移除"),
                false
            );
        }
        return true;
    }

    public List<String> listBots() {
        return List.copyOf(bots.keySet());
    }

    public int getOnlineCount() {
        return bots.size();
    }

    public int getMaxBots() {
        return MAX_BOTS;
    }

    public void shutdown(MinecraftServer server) {
        for (String name : new HashSet<>(bots.keySet())) {
            removeBot(name);
        }
    }

    private String allocateName() {
        for (int i = 1; i <= MAX_BOTS; i++) {
            String name = String.format("Bot%02d", i);
            if (!bots.containsKey(name)) {
                return name;
            }
        }
        return null;
    }
}
