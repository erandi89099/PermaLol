package com.permadeath.mechanics;

import com.permadeath.PermadeathMod;
import com.permadeath.PhaseManager;
import com.permadeath.ServerUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Dia 20: todos los mobs pacificos de la lista atacan a los jugadores y los pigmans (zombified piglin)
 * estan enfadados de antemano.
 * Se anade un objetivo de IA a los mobs cercanos a cada jugador (se comprueba cada segundo).
 */
public final class AggroMobs {
    private static final Set<String> PASSIVE = Set.of(
            "minecraft:cod", "minecraft:salmon", "minecraft:pufferfish", "minecraft:tropical_fish",
            "minecraft:horse", "minecraft:bat", "minecraft:bee", "minecraft:cat", "minecraft:chicken",
            "minecraft:cow", "minecraft:dolphin", "minecraft:fox", "minecraft:llama", "minecraft:trader_llama",
            "minecraft:ocelot", "minecraft:panda", "minecraft:parrot", "minecraft:pig", "minecraft:rabbit",
            "minecraft:sheep", "minecraft:snow_golem", "minecraft:squid", "minecraft:glow_squid",
            "minecraft:turtle", "minecraft:villager", "minecraft:wolf");
    private static final String PIGMAN = "minecraft:zombified_piglin";
    /** Mobs que no se mueven por navegacion normal: se les da un empujon directo hacia el jugador. */
    private static final Set<String> NUDGE = Set.of("minecraft:bat", "minecraft:squid", "minecraft:glow_squid");

    private static final float DAMAGE = 3.0f; // 1,5 corazones por golpe

    /** Mobs ya procesados (las metas de IA no se guardan en disco, por eso no se usa una etiqueta). */
    private static final Set<Mob> DONE = Collections.newSetFromMap(new WeakHashMap<>());

    private static Field goalField;
    private static Field targetField;

    static {
        try {
            goalField = Mob.class.getDeclaredField("goalSelector");
            goalField.setAccessible(true);
            targetField = Mob.class.getDeclaredField("targetSelector");
            targetField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            PermadeathMod.LOGGER.error("AggroMobs: no se pudo acceder a los selectores de IA", e);
            goalField = null;
        }
    }

    private AggroMobs() {}

    /** Llamar una vez por segundo. */
    public static void tick(MinecraftServer server) {
        if (goalField == null || PhaseManager.currentPhase() < 2) return;
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (p.isSpectator()) continue;
            Level level = p.level();
            for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48.0))) {
                String id = String.valueOf(EntityType.getKey(mob.getType()));
                boolean pigman = id.equals(PIGMAN);
                if (!pigman && !PASSIVE.contains(id)) continue;
                if (DONE.add(mob)) apply(mob, pigman, NUDGE.contains(id));
            }
        }
    }

    private static void apply(Mob mob, boolean pigman, boolean nudge) {
        try {
            if (pigman) {
                GoalSelector targets = (GoalSelector) targetField.get(mob);
                targets.addGoal(0, new NearestAttackableTargetGoal<>(mob, Player.class, false));
            } else {
                GoalSelector goals = (GoalSelector) goalField.get(mob);
                goals.addGoal(0, new AggroGoal(mob, nudge));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            PermadeathMod.LOGGER.warn("AggroMobs: no se pudo aplicar a {}", mob.getType(), e);
        }
    }

    /** Persigue al jugador mas cercano y le hace dano al llegar (con el comando damage, asi no hace falta atributo de ataque). */
    private static final class AggroGoal extends Goal {
        private final Mob mob;
        private final boolean nudge;
        private Player target;
        private int cooldown = 0;

        AggroGoal(Mob mob, boolean nudge) {
            this.mob = mob;
            this.nudge = nudge;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private boolean valid(Player p) {
            return p != null && p.isAlive() && !p.isCreative() && !p.isSpectator();
        }

        @Override
        public boolean canUse() {
            target = mob.level().getNearestPlayer(mob, 20.0);
            return valid(target);
        }

        @Override
        public boolean canContinueToUse() {
            return valid(target) && mob.distanceToSqr(target) < 24.0 * 24.0;
        }

        @Override
        public void tick() {
            if (!valid(target)) return;
            mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
            mob.getNavigation().moveTo(target, 1.3);
            if (nudge) {
                Vec3 d = target.position().add(0, 0.5, 0).subtract(mob.position());
                if (d.lengthSqr() > 0.01) {
                    mob.setDeltaMovement(mob.getDeltaMovement().add(d.normalize().scale(0.08)));
                }
            }
            if (cooldown > 0) cooldown--;
            if (cooldown == 0 && mob.distanceToSqr(target) <= 6.0) {
                cooldown = 20;
                ServerUtil.run(PermadeathMod.server, "damage " + target.getUUID() + " " + DAMAGE
                        + " minecraft:mob_attack by " + mob.getUUID());
            }
        }
    }
}
