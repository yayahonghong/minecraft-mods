# NoCreeperGrief Mod Design

## Objective

A lightweight Fabric mod that prevents creeper explosions from destroying terrain while keeping entity damage intact and replacing the explosion visual with firework particles.

## Requirements

- Creepers do NOT destroy blocks (no terrain grief)
- Creepers still damage players and entities (vanilla damage calculation)
- Explosion sound is unchanged
- Visual effect: firework rocket particles (`ParticleTypes.FIREWORK`) instead of normal explosion smoke/debris
- Creeper still dies and drops loot normally
- No configuration file; behavior is fixed
- Server-side only (no client-side changes needed)

## Implementation

### Module Setup

New submodule `nocreepergrief` following the existing multi-module pattern:

```
nocreepergrief/
├── build.gradle                    # fabric-loom plugin, same template as serverhelper
├── src/main/
│   ├── java/com/ysh/nocreepergrief/
│   │   ├── NoCreeperGriefMod.java  # ModInitializer entrypoint
│   │   └── mixin/
│   │       └── CreeperExplosionMixin.java
│   └── resources/
│       ├── fabric.mod.json
│       └── nocreepergrief.mixins.json
└── config/.gitkeep
```

Entrypoint package: `com.ysh.nocreepergrief`
Mod ID: `nocreepergrief`

### Mixin Strategy

Target: `CreeperEntity.explodeCreeper()` at HEAD, cancellable.

The mixin will:
1. Cancel the original `createExplosion()` call (prevents block destruction and default particles)
2. Create a `DestructionType.KEEP` explosion for entity damage + original explosion sound
3. Spawn `ParticleTypes.FIREWORK` particles at the explosion center

Pseudo-code:

```java
@Mixin(CreeperEntity.class)
public class CreeperExplosionMixin {
    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void onExplodeCreeper(CallbackInfo ci) {
        CreeperEntity self = (CreeperEntity)(Object)this;
        ServerWorld world = (ServerWorld)self.getWorld();
        float radius = self.isCharged() ? 6.0f : 3.0f;

        // Mark dead and discard (original behavior)
        self.dead = true;

        // Create explosion with KEEP type — damages entities, plays sound, no block damage
        world.createExplosion(self, self.getX(), self.getY(), self.getZ(), radius,
            Explosion.DestructionType.KEEP);

        // Spawn firework particles in lieu of normal explosion visual
        world.spawnParticles(ParticleTypes.FIREWORK,
            self.getX(), self.getY(), self.getZ(),
            50,   // count
            2.0, 2.0, 2.0,  // spread
            0.1); // speed

        self.discard();
        ci.cancel();
    }
}
```

### Dependencies

- `fabric-loader` (>=0.18.5)
- `minecraft` (~26.1)
- `java` (>=25)
- `fabric-api` (*) — minimally needed for mixin infrastructure, but required by Loom convention

### Files to modify

- `settings.gradle` — add `include 'nocreepergrief'`

### New files to create

- `nocreepergrief/build.gradle`
- `nocreepergrief/src/main/java/com/ysh/nocreepergrief/NoCreeperGriefMod.java`
- `nocreepergrief/src/main/java/com/ysh/nocreepergrief/mixin/CreeperExplosionMixin.java`
- `nocreepergrief/src/main/resources/fabric.mod.json`
- `nocreepergrief/src/main/resources/nocreepergrief.mixins.json`
