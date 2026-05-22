package com.ysh.serverhelper.mixin;

import com.ysh.serverhelper.handler.AdvancementHandler;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
    private void onAward(AdvancementHolder advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            PlayerAdvancements self = (PlayerAdvancements) (Object) this;
            if (self.getOrStartProgress(advancement).isDone()) {
                AdvancementHandler.onAdvancementGranted(this.player, advancement);
            }
        }
    }
}