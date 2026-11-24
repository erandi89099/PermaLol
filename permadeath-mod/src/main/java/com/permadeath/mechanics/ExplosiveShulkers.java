package com.permadeath.mechanics;

import com.permadeath.PhaseManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Dia 30: Shulkers Explosivos - sus proyectiles explotan donde impactan. */
public final class ExplosiveShulkers {
    private static final float POWER = 2.5f;

    private ExplosiveShulkers() {}

    public static void register() {
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (!PhaseManager.dayAtLeast(30)) return;
            if (!"minecraft:shulker_bullet".equals(String.valueOf(EntityType.getKey(entity.getType())))) return;
            Entity.RemovalReason reason = entity.getRemovalReason();
            if (reason == null || !reason.shouldDestroy()) return; // descarga de chunk: sin explosion
            double x = entity.getX(), y = entity.getY(), z = entity.getZ();
            MobHooks.enqueue(() -> level.explode(null, x, y, z, POWER, Level.ExplosionInteraction.MOB));
        });
    }
}
