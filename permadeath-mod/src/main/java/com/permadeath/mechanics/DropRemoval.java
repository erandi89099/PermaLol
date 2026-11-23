package com.permadeath.mechanics;

import com.permadeath.PermadeathMod;
import com.permadeath.PhaseManager;
import com.permadeath.ServerUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Dia 20:
 *  - Los mobs de la lista no sueltan items (se eliminan los items recien soltados; la experiencia se mantiene).
 *  - 1 de cada 100 Ravagers suelta un Totem de Inmortalidad.
 */
public final class DropRemoval {
    private static final Set<String> NO_DROPS = Set.of(
            "minecraft:iron_golem", "minecraft:zombified_piglin", "minecraft:ghast", "minecraft:guardian",
            "minecraft:elder_guardian", "minecraft:magma_cube", "minecraft:enderman", "minecraft:witch",
            "minecraft:wither_skeleton", "minecraft:evoker", "minecraft:phantom", "minecraft:slime",
            "minecraft:drowned", "minecraft:blaze");

    private static final Random RANDOM = new Random();

    private DropRemoval() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (PermadeathMod.state == null || PhaseManager.currentPhase() < 2) return;
            String id = String.valueOf(EntityType.getKey(entity.getType()));

            if (NO_DROPS.contains(id)) {
                Level level = entity.level();
                AABB box = entity.getBoundingBox().inflate(3.0);
                MobHooksAccess.enqueue(() -> {
                    for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
                        if (item.tickCount <= 3) item.discard(); // solo items recien soltados por la muerte
                    }
                });
            }

            if (id.equals("minecraft:ravager") && RANDOM.nextInt(100) == 0) {
                UUID uuid = entity.getUUID();
                MobHooksAccess.enqueue(() -> ServerUtil.run(PermadeathMod.server,
                        "execute as " + uuid + " at @s run summon minecraft:item ~ ~ ~ "
                                + "{Item:{id:\"minecraft:totem_of_undying\",count:1}}"));
            }
        });
    }

    /** Pequeno puente para no exponer la cola completa. */
    private static final class MobHooksAccess {
        static void enqueue(Runnable r) { MobHooks.enqueue(r); }
    }
}
