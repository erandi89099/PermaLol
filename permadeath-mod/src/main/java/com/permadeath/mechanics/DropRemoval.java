package com.permadeath.mechanics;

import com.permadeath.PermadeathMod;
import com.permadeath.PhaseManager;
import com.permadeath.ServerUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Dia 20: los mobs de la lista no sueltan items (la experiencia se mantiene) y los Ravagers sueltan un Totem (1%).
 * Dia 25-29: los Ravagers sueltan Totem con 20%. Gigaslimes, giga magmacubes, ghast demoniacos y aranas de cueva
 *         sueltan piezas de armadura de netherite (configurable en permadeath.properties); a partir del dia 30 ya no.
 * Dia 30: los Shulkers sueltan Shulker Shell con 20% y una dinamita encendida.
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
            UUID uuid = entity.getUUID();

            // 1) Limpiar los drops normales (primero, para no borrar los que se anaden despues)
            boolean shulker = id.equals("minecraft:shulker") && PhaseManager.dayAtLeast(30);
            if (NO_DROPS.contains(id) || shulker) {
                Level level = entity.level();
                AABB box = entity.getBoundingBox().inflate(3.0);
                MobHooks.enqueue(() -> {
                    for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
                        if (item.tickCount <= 3) item.discard(); // solo items recien soltados por la muerte
                    }
                });
            }

            // Dia 30: los shulkers sueltan Shulker Shell con 20% (en vez del drop normal)
            if (shulker && RANDOM.nextInt(100) < 20) dropItem(uuid, "shulker_shell");
            // Shulker Explosivo: al morir deja caer una dinamita encendida
            if (shulker) {
                MobHooks.enqueue(() -> ServerUtil.run(PermadeathMod.server,
                        "execute as " + uuid + " at @s run summon minecraft:tnt ~ ~ ~ {fuse:80s}"));
            }

            // 2) Totem de los Ravagers
            if (id.equals("minecraft:ravager")) {
                int chance = PhaseManager.day25Active() ? 20 : 1;
                if (RANDOM.nextInt(100) < chance) dropItem(uuid, "totem_of_undying");
            }

            // 3) Armadura de netherite: solo se puede conseguir del dia 25 al 29
            if (PhaseManager.day25Active() && !PhaseManager.dayAtLeast(30)) {
                switch (id) {
                    case "minecraft:slime" -> { if (entity.getBbWidth() >= 7.0f) netherite("gigaslime", uuid); }
                    case "minecraft:magma_cube" -> { if (entity.getBbWidth() >= 7.0f) netherite("gigamagmacube", uuid); }
                    case "minecraft:ghast" -> { if (entity.getMaxHealth() >= 30.0f) netherite("ghast", uuid); }
                    case "minecraft:cave_spider" -> netherite("cavespider", uuid);
                    default -> { }
                }
            }
        });
    }

    private static void netherite(String key, UUID uuid) {
        String[] d = PermadeathMod.config.netheriteDrops.get(key);
        if (d == null) return;
        int chance;
        try {
            chance = Integer.parseInt(d[1]);
        } catch (NumberFormatException e) {
            chance = 0;
        }
        if (RANDOM.nextInt(100) < chance) dropItem(uuid, d[0]);
    }

    private static void dropItem(UUID uuid, String item) {
        MobHooks.enqueue(() -> ServerUtil.run(PermadeathMod.server,
                "execute as " + uuid + " at @s run summon minecraft:item ~ ~ ~ "
                        + "{Item:{id:\"minecraft:" + item + "\",count:1}}"));
    }
}
