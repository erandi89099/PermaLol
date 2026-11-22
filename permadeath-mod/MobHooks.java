package com.permadeath.mechanics;

import com.permadeath.PermadeathMod;
import com.permadeath.PhaseManager;
import com.permadeath.ServerUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.*;

/**
 * Dia 10 (fase 1):
 *  - Doble de mobs: cada mob hostil nuevo genera un duplicado.
 *  - Todas las aranas reciben entre 1 y 3 efectos de pocion al azar (infinitos).
 * Las acciones se encolan y se ejecutan al final del tick para no tocar el mundo durante la carga de la entidad.
 */
public final class MobHooks {
    private static final String TAG_SEEN = "pd_seen";
    private static final String TAG_FX = "pd_fx";

    private static final Set<String> NO_DOUBLE = Set.of(
            "minecraft:wither", "minecraft:warden", "minecraft:elder_guardian", "minecraft:ender_dragon");

    /** {efecto, amplificador}: Velocidad III, Fuerza IV, Salto V, Brillo, Regeneracion IV, Invisibilidad,
     *  Caida lenta, Resistencia III. */
    private static final String[][] SPIDER_EFFECTS = {
            {"speed", "2"}, {"strength", "3"}, {"jump_boost", "4"}, {"glowing", "0"},
            {"regeneration", "3"}, {"invisibility", "0"}, {"slow_falling", "0"}, {"resistance", "2"}
    };

    private static final ArrayDeque<Runnable> QUEUE = new ArrayDeque<>();
    private static final Random RANDOM = new Random();
    private static boolean suppress = false;

    private MobHooks() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (PermadeathMod.state == null) return;
            if (entity.getType().getCategory() != MobCategory.MONSTER) return;

            String id = String.valueOf(EntityType.getKey(entity.getType()));
            int phase = PhaseManager.currentPhase();
            UUID uuid = entity.getUUID();

            // Doble de mobs (solo mobs "nuevos": los ya vistos llevan la etiqueta guardada)
            if (entity.addTag(TAG_SEEN) && phase >= 1 && !suppress && !NO_DOUBLE.contains(id)) {
                QUEUE.add(() -> cloneMob(uuid, id));
            }

            // Efectos de pocion a todas las aranas
            if (phase >= 1 && (id.equals("minecraft:spider") || id.equals("minecraft:cave_spider"))
                    && entity.addTag(TAG_FX)) {
                QUEUE.add(() -> applySpiderEffects(uuid));
            }
        });
    }

    /** Llamar cada tick de servidor. */
    public static void tick() {
        int budget = 40;
        Runnable r;
        while (budget-- > 0 && (r = QUEUE.poll()) != null) {
            r.run();
        }
    }

    private static void cloneMob(UUID uuid, String id) {
        suppress = true; // el clon no genera otro clon
        try {
            ServerUtil.run(PermadeathMod.server, "execute as " + uuid + " at @s run summon " + id + " ~ ~ ~");
        } finally {
            suppress = false;
        }
    }

    private static void applySpiderEffects(UUID uuid) {
        List<String[]> pool = new ArrayList<>(Arrays.asList(SPIDER_EFFECTS));
        Collections.shuffle(pool, RANDOM);
        int n = 1 + RANDOM.nextInt(3); // 1 a 3 efectos
        for (int i = 0; i < n; i++) {
            String[] fx = pool.get(i);
            ServerUtil.run(PermadeathMod.server,
                    "effect give " + uuid + " minecraft:" + fx[0] + " infinite " + fx[1] + " true");
        }
    }
}
