package com.ysh.nocreepergrief.mixin;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketEntityAccessor {
    @Accessor("lifetime")
    void setLifetime(int lifetime);

    @Accessor("life")
    void setLife(int life);
}
