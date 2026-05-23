package com.ysh.nocreepergrief.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin {
    @Inject(method = "dealExplosionDamage", at = @At("HEAD"), cancellable = true)
    private void onDealExplosionDamage(ServerLevel level, CallbackInfo ci) {
        if (((FireworkRocketEntity)(Object)this).getOwner() instanceof Creeper) {
            ci.cancel();
        }
    }
}
