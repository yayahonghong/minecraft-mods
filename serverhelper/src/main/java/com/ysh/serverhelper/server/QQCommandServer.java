package com.ysh.serverhelper.server;

import com.ysh.serverhelper.ServerHelperMod;
import com.ysh.serverhelper.config.ModConfig;
import com.ysh.serverhelper.qqcmd.QQCommandHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class QQCommandServer {
    private HttpServer server;

    public void start(ModConfig.QQConfig config, MinecraftServer mcServer) {
        QQCommandHandler.init(mcServer);
        try {
            int port = config.getCallbackPort();
            if (port <= 0) {
                ServerHelperMod.LOGGER.info("QQ command server disabled (port <= 0)");
                return;
            }
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/qq/callback", exchange -> {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                byte[] body = exchange.getRequestBody().readAllBytes();
                
                String token = config.getToken();
                if (token != null && !token.trim().isEmpty()) {
                    String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                    if (authHeader == null) authHeader = exchange.getRequestHeaders().getFirst("authorization");
                    
                    String xSignature = exchange.getRequestHeaders().getFirst("X-Signature");
                    if (xSignature == null) xSignature = exchange.getRequestHeaders().getFirst("x-signature");

                    boolean valid = false;
                    
                    // NapCat might use HMAC SHA1 signature (X-Signature: sha1=...)
                    if (xSignature != null && xSignature.startsWith("sha1=")) {
                        try {
                            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
                            mac.init(new javax.crypto.spec.SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
                            byte[] hash = mac.doFinal(body);
                            
                            StringBuilder hexString = new StringBuilder();
                            for (byte b : hash) {
                                String hex = Integer.toHexString(0xff & b);
                                if (hex.length() == 1) hexString.append('0');
                                hexString.append(hex);
                            }
                            
                            String expectedSignature = "sha1=" + hexString.toString();
                            if (xSignature.equals(expectedSignature)) {
                                valid = true;
                            }
                        } catch (Exception e) {
                            ServerHelperMod.LOGGER.error("Failed to compute HMAC SHA1 signature", e);
                        }
                    } else if (authHeader != null) {
                        if (authHeader.equals("Bearer " + token) || authHeader.equals("Token " + token) || authHeader.equals(token)) {
                            valid = true;
                        }
                    }
                    
                    if (!valid) {
                        ServerHelperMod.LOGGER.warn("QQ callback rejected: Invalid or missing token/signature");
                        exchange.sendResponseHeaders(403, -1);
                        return;
                    }
                }
                String json = new String(body, StandardCharsets.UTF_8);
                ServerHelperMod.LOGGER.debug("QQ callback: {}", json);

                QQCommandHandler.handle(json, config);

                byte[] resp = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.getResponseBody().close();
            });
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();
            ServerHelperMod.LOGGER.info("QQ command server listening on port {}", port);
        } catch (IOException e) {
            ServerHelperMod.LOGGER.warn("Failed to start QQ command server", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            ServerHelperMod.LOGGER.info("QQ command server stopped");
        }
    }
}
