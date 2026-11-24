package com.permadeath.mechanics;

import com.permadeath.PermadeathMod;
import com.permadeath.PhaseManager;
import com.permadeath.ServerUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Dia 30: Ender Ghast y Ender Creeper (aparecen en el End en lugar de algunos endermans).
 *  - Ender Ghast: ghast demoniaco que se teletransporta cada cierto tiempo y siempre que recibe dano.
 *  - Ender Creeper: creeper electrico invisible que a veces se teletransporta y puede hacerlo al recibir dano.
 * Se identifican por su nombre personalizado.
 */
public final class EnderMobs {
    public static final String GHAST_NAME = "Ender Ghast";
    public static final String CREEPER_NAME = "Ender Creeper";

    private static final Random RANDOM = new Random();
    /** Momento (ms) del proximo teletransporte aleatorio del Ender Ghast. */
    private static final Map<Entity, Long> NEXT_GHAST = new WeakHashMap<>();
    private static final Map<Entity, Long> LAST_TP = new WeakHashMap<>();

    private EnderMobs() {}

    private static String nameOf(Entity e) {
        return e.hasCustomName() ? e.getCustomName().getString() : null;
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!PhaseManager.dayAtLeast(30)) return true;
            String n = nameOf(entity);
            if (GHAST_NAME.equals(n)) {
                MobHooks.enqueue(() -> teleport(entity, false));
            } else if (CREEPER_NAME.equals(n) && RANDOM.nextInt(100) < 50) {
                MobHooks.enqueue(() -> teleport(entity, true));
            }
            return true;
        });
    }

    /** Una vez por segundo. */
    public static void tick(MinecraftServer server) {
        if (!PhaseManager.dayAtLeast(30)) return;
        long now = System.currentTimeMillis();
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            Level level = p.level();
            for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64.0))) {
                String n = nameOf(mob);
                if (GHAST_NAME.equals(n)) {
                    Long next = NEXT_GHAST.get(mob);
                    if (next == null) {
                        NEXT_GHAST.put(mob, now + 6000L + RANDOM.nextInt(6000));
                    } else if (now >= next) {
                        NEXT_GHAST.put(mob, now + 6000L + RANDOM.nextInt(6000)); // cada 6-12 s
                        teleport(mob, false);
                    }
                } else if (CREEPER_NAME.equals(n) && RANDOM.nextInt(100) < 6) { // ~6% por segundo
                    teleport(mob, true);
                }
            }
        }
    }

    /** Teletransporta a un punto libre cercano. needsGround: debe haber suelo bajo los pies (creeper). */
    private static void teleport(Entity e, boolean needsGround) {
        if (e.isRemoved() || PermadeathMod.server == null) return;
        long now = System.currentTimeMillis();
        Long last = LAST_TP.get(e);
        if (last != null && now - last < 1000L) return; // evita teletransportes en cadena
        Level level = e.level();
        for (int i = 0; i < 10; i++) {
            double dx = (RANDOM.nextDouble() * 2 - 1) * 12.0;
            double dz = (RANDOM.nextDouble() * 2 - 1) * 12.0;
            double dy = needsGround ? RANDOM.nextInt(5) - 2 : RANDOM.nextInt(9) - 4;
            AABB box = e.getBoundingBox().move(dx, dy, dz);
            if (!level.noCollision(e, box)) continue;
            if (needsGround && level.noCollision(e, box.move(0, -1, 0))) continue;
            LAST_TP.put(e, now);
            ServerUtil.run(PermadeathMod.server, String.format(Locale.ROOT, "tp %s %.2f %.2f %.2f",
                    e.getUUID(), e.getX() + dx, e.getY() + dy, e.getZ() + dz));
            return;
        }
    }
}
