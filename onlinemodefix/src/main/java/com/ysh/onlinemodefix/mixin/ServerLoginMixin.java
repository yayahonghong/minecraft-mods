package com.ysh.onlinemodefix.mixin;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginMixin {

    @Shadow @Final private static Logger LOGGER;

    @Shadow private String requestedUsername;

    @Shadow private void startClientVerification(GameProfile profile) {
        throw new UnsupportedOperationException();
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

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
            LOGGER.info("lookupProfile: checking UUID for {}", name);
            HttpRequest uuidReq = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> uuidResp = HTTP_CLIENT.send(uuidReq, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("lookupProfile: UUID API returned {}", uuidResp.statusCode());
            if (uuidResp.statusCode() == 200) {
                JsonObject uuidJson = JsonParser.parseString(uuidResp.body()).getAsJsonObject();
                String id = uuidJson.get("id").getAsString();
                String uuidStr = id.substring(0, 8) + "-" + id.substring(8, 12) + "-"
                    + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20);
                UUID premiumUuid = UUID.fromString(uuidStr);
                LOGGER.info("lookupProfile: UUID={}, fetching skin profile", premiumUuid);

                HttpRequest profileReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + id))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
                HttpResponse<String> profileResp = HTTP_CLIENT.send(profileReq, HttpResponse.BodyHandlers.ofString());
                LOGGER.info("lookupProfile: profile API returned {}", profileResp.statusCode());
                if (profileResp.statusCode() == 200) {
                    JsonObject profileJson = JsonParser.parseString(profileResp.body()).getAsJsonObject();
                    ArrayListMultimap<String, Property> delegate = ArrayListMultimap.<String, Property>create();
                    var properties = profileJson.getAsJsonArray("properties");
                    if (properties != null) {
                        for (var elem : properties) {
                            JsonObject prop = elem.getAsJsonObject();
                            String propName = prop.get("name").getAsString();
                            String value = prop.get("value").getAsString();
                            String signature = prop.has("signature") ? prop.get("signature").getAsString() : null;
                            delegate.put(propName,
                                signature != null
                                    ? new Property(propName, value, signature)
                                    : new Property(propName, value));
                        }
                    }
                    GameProfile fullProfile = new GameProfile(premiumUuid, profileJson.get("name").getAsString(), new PropertyMap(delegate));
                    LOGGER.info("{} identified as premium player, UUID={}, properties loaded", name, premiumUuid);
                    return fullProfile;
                }
                LOGGER.warn("{} UUID found but profile fetch returned {}, using bare UUID", name, profileResp.statusCode());
                return new GameProfile(premiumUuid, name);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to look up {} on Mojang API: {}", name, e.toString());
            LOGGER.warn("Stack trace:", e);
        }
        LOGGER.info("{} treated as offline player", name);
        return UUIDUtil.createOfflineProfile(name);
    }
}
