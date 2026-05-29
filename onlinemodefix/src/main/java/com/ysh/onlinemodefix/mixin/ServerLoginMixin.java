package com.ysh.onlinemodefix.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginMixin {

    @Shadow @Final private static Logger LOGGER;

    @Shadow private String requestedUsername;

    @Shadow private void startClientVerification(GameProfile profile) {
        throw new UnsupportedOperationException();
    }

    @Redirect(
        method = "handleHello",
        at = @At(
            value = "NEW",
            target = "(Ljava/lang/String;[B[BZ)Lnet/minecraft/network/protocol/login/ClientboundHelloPacket;"
        )
    )
    private ClientboundHelloPacket createHelloPacket(String serverId, byte[] publicKey, byte[] challenge, boolean needsAuth) {
        return new ClientboundHelloPacket(serverId, publicKey, challenge, false);
    }

    @Redirect(
        method = "handleKey",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Thread;start()V"
        )
    )
    private void onAuthThreadStart(Thread thread) {
        String name = this.requestedUsername;
        new Thread(() -> {
            GameProfile profile = lookupProfile(name);
            startClientVerification(profile);
        }, "onlinemodefix-auth-" + name).start();
    }

    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void onDisconnect(Component reason, CallbackInfo ci) {
        var contents = reason.getContents();
        if (contents instanceof TranslatableContents tc) {
            String key = tc.getKey();
            if ("multiplayer.disconnect.unverified_username".equals(key)
                || "multiplayer.disconnect.authservers_down".equals(key)) {
                GameProfile offlineProfile = UUIDUtil.createOfflineProfile(requestedUsername);
                LOGGER.info("{} logged in as offline player (online auth failed, key={})", requestedUsername, key);
                this.startClientVerification(offlineProfile);
                ci.cancel();
            }
        }
    }

    private static GameProfile lookupProfile(String name) {
        try {
            Path cachePath = Path.of("usercache.json");
            if (Files.exists(cachePath)) {
                String content = Files.readString(cachePath);
                JsonArray array = JsonParser.parseString(content).getAsJsonArray();
                for (var elem : array) {
                    JsonObject entry = elem.getAsJsonObject();
                    if (entry.get("name").getAsString().equalsIgnoreCase(name)) {
                        UUID uuid = UUID.fromString(entry.get("uuid").getAsString());
                        LOGGER.info("{} found in usercache, UUID={}", name, uuid);
                        return new GameProfile(uuid, name);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read usercache for {}: {}", name, e.toString());
        }
        LOGGER.info("{} not in usercache, treated as offline player", name);
        return UUIDUtil.createOfflineProfile(name);
    }
}
