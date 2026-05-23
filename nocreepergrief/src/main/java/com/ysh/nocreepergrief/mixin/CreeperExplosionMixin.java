package com.ysh.nocreepergrief.mixin;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(Creeper.class)
public class CreeperExplosionMixin {
    @Redirect(
        method = "explodeCreeper",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
        )
    )
    private void onExplode(ServerLevel world, Entity source, double x, double y, double z, float power, Level.ExplosionInteraction interaction) {
        world.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0f, 1.0f);

        float radius = power * 2.0f;
        Vec3 center = new Vec3(x, y, z);
        List<Entity> targets = world.getEntities(source, AABB.ofSize(center, radius * 2, radius * 2, radius * 2));
        for (Entity target : targets) {
            if (target instanceof LivingEntity living) {
                double distSq = living.distanceToSqr(center);
                if (distSq < radius * radius) {
                    float damage = (float)((1.0 - Math.sqrt(distSq) / radius) * 20.0f);
                    if (damage > 0) {
                        living.hurt(world.damageSources().explosion(source, source), damage);
                    }
                }
            }
        }

        List<FireworkExplosion> explosions = new ArrayList<>();
        IntArrayList colors1 = new IntArrayList(new int[]{0xFF0000, 0xFFD700, 0xFFFFFF});
        IntArrayList fade1 = new IntArrayList(new int[]{0xFF8C00});
        explosions.add(new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, colors1, fade1, true, true));

        IntArrayList colors2 = new IntArrayList(new int[]{0x00BFFF, 0x8A2BE2, 0x00FF7F});
        IntArrayList fade2 = new IntArrayList(new int[]{0xFFFFFF});
        explosions.add(new FireworkExplosion(FireworkExplosion.Shape.BURST, colors2, fade2, false, true));

        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        stack.set(DataComponents.FIREWORKS, new Fireworks(1, explosions));

        FireworkRocketEntity rocket = new FireworkRocketEntity(world, stack, x, y + 0.5, z, false);
        rocket.setOwner(source);
        ((FireworkRocketEntityAccessor) rocket).setLifetime(1);
        ((FireworkRocketEntityAccessor) rocket).setLife(0);
        world.addFreshEntity(rocket);
    }
}
