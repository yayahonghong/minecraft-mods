package com.ysh.smartplace.mixin;

import com.ysh.smartplace.DirectionHelper;
import com.ysh.smartplace.SmartPlaceState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerPlayer player;

    @ModifyVariable(
        method = "useItemOn",
        at = @At("HEAD"),
        argsOnly = true,
        index = 4
    )
    private BlockHitResult modifyDirection(BlockHitResult hitResult) {
        if (!SmartPlaceState.isEnabled(this.player)) {
            return hitResult;
        }
        return hitResult.withDirection(DirectionHelper.fromLookAngle(this.player.getLookAngle()));
    }
}
