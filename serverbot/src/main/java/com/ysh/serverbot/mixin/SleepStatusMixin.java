package com.ysh.serverbot.mixin;

import com.ysh.serverbot.bot.ServerBotPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SleepStatus.class)
public class SleepStatusMixin {
    @Redirect(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z")
    )
    private boolean skipBotsForSleep(ServerPlayer player) {
        return player.isSpectator() || player instanceof ServerBotPlayer;
    }
}
