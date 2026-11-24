package com.permadeath.mechanics;

import com.permadeath.PermadeathConfig;
import com.permadeath.PermadeathMod;
import com.permadeath.PhaseManager;
import com.permadeath.ServerUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Consumer;

/**
 * Mecanicas sobre mobs:
 *  Dia 10: aranas con 1-3 efectos (el doble mobcap esta en MobCap).
 *  Dia 20: aranas con 3-5 efectos y esqueleto encima (5 clases); phantoms tamano 9 y doble vida.
 *  Dia 25: aranas con 5 efectos; ravagers; gigaslimes, giga magmacubes y ghast demoniacos.
 *  Dia 30: esqueletos con clase, calamares->guardianes, murcielagos->blazes, creepers electricos, pillagers,
 *          pigmans con armadura, golems, endermans, silverfish, nombres especiales, Ender Ghast/Creeper en el End.
 * Las acciones se encolan y se ejecutan al final del tick para no tocar el mundo durante la carga de la entidad.
 */
public final class MobHooks {
    private static final String TAG_SEEN = "pd_seen";
    private static final String TAG_FX = "pd_fx";
    private static final String TAG_FX2 = "pd_fx2";
    private static final String TAG_FX3 = "pd_fx3";
    private static final String TAG_RIDER = "pd_rider";
    private static final String TAG_PHANTOM = "pd_phantom";
    private static final String TAG_RAV = "pd_rav25";
    private static final String TAG_GIGA = "pd_giga";
    private static final String TAG_GHAST = "pd_ghast25";
    private static final String TAG_CLASS = "pd_class";
    private static final String TAG_CONV = "pd_conv";
    private static final String TAG_GOLEM = "pd_golem30";
    private static final String TAG_CHARGED = "pd_charged";
    private static final String TAG_PILL = "pd_pill30";
    private static final String TAG_PIG = "pd_pig30";
    private static final String TAG_ENDERMAN = "pd_ender30";
    private static final String TAG_SF = "pd_sf30";
    private static final String TAG_NAMED = "pd_named";
    private static final String TAG_END = "pd_endroll";

    /** {efecto, amplificador}: Velocidad III, Fuerza IV, Salto V, Brillo, Regeneracion IV, Invisibilidad,
     *  Caida lenta, Resistencia III. */
    private static final String[][] SPIDER_EFFECTS = {
            {"speed", "2"}, {"strength", "3"}, {"jump_boost", "4"}, {"glowing", "0"},
            {"regeneration", "3"}, {"invisibility", "0"}, {"slow_falling", "0"}, {"resistance", "2"}
    };

    private static final ArrayDeque<Runnable> QUEUE = new ArrayDeque<>();
    private static final Random RANDOM = new Random();
    private static boolean suppress = false;

    /** Zonas donde acaba de morir un slime/magmacube grande: sus hijos no se agrandan. */
    private static final class Zone {
        final Level level; final double x, y, z, r; final long expire;
        Zone(Level level, double x, double y, double z, double r, long expire) {
            this.level = level; this.x = x; this.y = y; this.z = z; this.r = r; this.expire = expire;
        }
    }
    private static final List<Zone> ZONES = new ArrayList<>();

    private MobHooks() {}

    public static void enqueue(Runnable r) {
        QUEUE.add(r);
    }

    private static boolean inSplitZone(Entity e) {
        long now = System.currentTimeMillis();
        ZONES.removeIf(z -> z.expire < now);
        for (Zone z : ZONES) {
            if (z.level != e.level()) continue;
            double dx = e.getX() - z.x, dy = e.getY() - z.y, dz = e.getZ() - z.z;
            if (dx * dx + dy * dy + dz * dz <= z.r * z.r) return true;
        }
        return false;
    }

    public static void register() {
        // Los slimes y magmacubes se dividen al morir: se anota donde para que los hijos no se agranden
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            String id = String.valueOf(EntityType.getKey(entity.getType()));
            if ((id.equals("minecraft:slime") || id.equals("minecraft:magma_cube")) && entity.getBbWidth() >= 1.0f) {
                ZONES.add(new Zone(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                        entity.getBbWidth() + 3.0, System.currentTimeMillis() + 6000L));
            }
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (PermadeathMod.state == null) return;

            String id = String.valueOf(EntityType.getKey(entity.getType()));
            int phase = PhaseManager.currentPhase();
            boolean day25 = PhaseManager.day25Active();
            boolean day30 = PhaseManager.dayAtLeast(30);
            UUID uuid = entity.getUUID();

            // ---- Dia 30: transformaciones de mobs no hostiles ----
            if (day30) {
                if ((id.equals("minecraft:squid") || id.equals("minecraft:glow_squid")) && entity.addTag(TAG_CONV)) {
                    QUEUE.add(() -> replaceWith(entity, "minecraft:guardian", "Calamar Guardián", "", "", "~",
                            nid -> effect(nid, "speed", 1)));
                    return;
                }
                if (id.equals("minecraft:bat") && entity.addTag(TAG_CONV)) {
                    QUEUE.add(() -> replaceWith(entity, "minecraft:blaze", "Murciélago Blaze", "", "", "~",
                            nid -> effect(nid, "resistance", 1)));
                    return;
                }
                if (id.equals("minecraft:iron_golem") && entity.addTag(TAG_GOLEM)) {
                    QUEUE.add(() -> effect(uuid, "speed", 3)); // Velocidad IV
                }
            }

            if (entity.getType().getCategory() != MobCategory.MONSTER) return;

            // ---- Dia 30: endermans del End -> Ender Ghast / Ender Creeper ----
            if (day30 && !suppress && id.equals("minecraft:enderman") && Level.END.equals(level.dimension())
                    && entity.addTag(TAG_END)) {
                PermadeathConfig cfg = PermadeathMod.config;
                int roll = RANDOM.nextInt(100);
                if (roll < cfg.enderGhastPercent) {
                    entity.addTag(TAG_SEEN);
                    QUEUE.add(() -> replaceWith(entity, "minecraft:ghast", EnderMobs.GHAST_NAME,
                            ",\"" + TAG_GHAST + "\"", "", "~4", nid -> buffGhast(nid, null)));
                    return;
                } else if (roll < cfg.enderGhastPercent + cfg.enderCreeperPercent) {
                    entity.addTag(TAG_SEEN);
                    QUEUE.add(() -> replaceWith(entity, "minecraft:creeper", EnderMobs.CREEPER_NAME,
                            ",\"" + TAG_CHARGED + "\"", ",powered:1b", "~", nid -> effect(nid, "invisibility", 0)));
                    return;
                }
            }

            boolean slimeLike = id.equals("minecraft:slime") || id.equals("minecraft:magma_cube");
            boolean splitChild = slimeLike && inSplitZone(entity);

            boolean spider = id.equals("minecraft:spider") || id.equals("minecraft:cave_spider");
            if (spider) {
                if (day25) {
                    if (entity.addTag(TAG_FX3)) QUEUE.add(() -> applySpiderEffects(uuid, 5, 5, true));
                } else if (phase >= 2) {
                    if (entity.addTag(TAG_FX2)) QUEUE.add(() -> applySpiderEffects(uuid, 3, 5, true));
                } else if (phase >= 1) {
                    if (entity.addTag(TAG_FX)) QUEUE.add(() -> applySpiderEffects(uuid, 1, 3, false));
                }
                if (phase >= 2 && id.equals("minecraft:spider") && entity.addTag(TAG_RIDER)) {
                    QUEUE.add(() -> spawnRider(entity));
                }
            }

            if (phase >= 2 && id.equals("minecraft:phantom") && entity.addTag(TAG_PHANTOM)) {
                QUEUE.add(() -> buffPhantom(uuid));
            }

            if (day25) {
                if (id.equals("minecraft:ravager") && entity.addTag(TAG_RAV)) {
                    QUEUE.add(() -> buffRavager(uuid));
                }
                if (slimeLike && entity.addTag(TAG_GIGA) && !splitChild) {
                    QUEUE.add(() -> buffGiga(uuid, id.equals("minecraft:slime")));
                }
                if (id.equals("minecraft:ghast") && entity.addTag(TAG_GHAST)) {
                    QUEUE.add(() -> buffGhast(uuid, "Ghast Demoníaco"));
                }
            }

            if (day30) {
                // Nombres especiales para los gigas y ghasts que ya existian
                if (slimeLike && entity.getBbWidth() >= 7.0f && entity.addTag(TAG_NAMED)) {
                    String n = id.equals("minecraft:slime") ? "Gigaslime" : "Giga Magmacube";
                    QUEUE.add(() -> nameMob(uuid, n));
                }
                if (id.equals("minecraft:ghast") && entity instanceof LivingEntity le && le.getMaxHealth() >= 30.0f
                        && !(entity.hasCustomName()) && entity.addTag(TAG_NAMED)) {
                    QUEUE.add(() -> nameMob(uuid, "Ghast Demoníaco"));
                }

                if (id.equals("minecraft:skeleton") && entity.addTag(TAG_CLASS)) {
                    QUEUE.add(() -> assignClass(entity));
                }
                if (id.equals("minecraft:creeper") && entity.addTag(TAG_CHARGED)) {
                    QUEUE.add(() -> ServerUtil.run(PermadeathMod.server, "data merge entity " + uuid + " {powered:1b}"));
                }
                if (id.equals("minecraft:pillager") && entity.addTag(TAG_PILL)) {
                    QUEUE.add(() -> buffPillager(uuid));
                }
                if (id.equals("minecraft:zombified_piglin") && entity.addTag(TAG_PIG)) {
                    QUEUE.add(() -> dressPigman(uuid));
                }
                if (id.equals("minecraft:enderman") && entity.addTag(TAG_ENDERMAN)) {
                    QUEUE.add(() -> effect(uuid, "strength", 1)); // Fuerza II
                }
                if (id.equals("minecraft:silverfish") && entity.addTag(TAG_SF)) {
                    QUEUE.add(() -> applySpiderEffects(uuid, 5, 5, false));
                }
                if (id.equals("minecraft:shulker") && entity.addTag(TAG_NAMED)) {
                    QUEUE.add(() -> nameMob(uuid, "Shulker Explosivo"));
                }
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

    // ---------------- Utilidades ----------------

    private static void effect(UUID uuid, String effect, int amplifier) {
        ServerUtil.run(PermadeathMod.server, "effect give " + uuid + " minecraft:" + effect + " infinite " + amplifier + " true");
    }

    private static void nameMob(UUID uuid, String name) {
        ServerUtil.run(PermadeathMod.server, "data merge entity " + uuid + " {CustomName:\"" + name + "\",CustomNameVisible:1b}");
    }

    /** Sustituye un mob por otro (mismo sitio), con nombre y etiquetas. */
    private static void replaceWith(Entity old, String newId, String name, String extraTags, String extraNbt,
                                    String yPos, Consumer<UUID> afterSpawn) {
        if (old.isRemoved()) return;
        UUID nid = UUID.randomUUID();
        String nbt = "{UUID:" + SkeletonClasses.uuidInts(nid) + ",Tags:[\"" + TAG_SEEN + "\"" + extraTags + "]"
                + ",CustomName:\"" + name + "\",CustomNameVisible:1b" + extraNbt + "}";
        boolean ok;
        suppress = true;
        try {
            ok = ServerUtil.runChecked(PermadeathMod.server,
                    "execute as " + old.getUUID() + " at @s run summon " + newId + " ~ " + yPos + " ~ " + nbt);
        } finally {
            suppress = false;
        }
        if (ok) {
            if (afterSpawn != null) afterSpawn.accept(nid);
            old.discard();
        }
    }

    private static void applySpiderEffects(UUID uuid, int min, int max, boolean clearFirst) {
        if (clearFirst) ServerUtil.run(PermadeathMod.server, "effect clear " + uuid);
        List<String[]> pool = new ArrayList<>(Arrays.asList(SPIDER_EFFECTS));
        Collections.shuffle(pool, RANDOM);
        int n = min + RANDOM.nextInt(max - min + 1);
        for (int i = 0; i < n; i++) {
            String[] fx = pool.get(i);
            ServerUtil.run(PermadeathMod.server,
                    "effect give " + uuid + " minecraft:" + fx[0] + " infinite " + fx[1] + " true");
        }
    }

    private static void buffPhantom(UUID uuid) {
        ServerUtil.run(PermadeathMod.server, "attribute " + uuid + " minecraft:max_health base set 40");
        // Se incluyen las dos grafias de la clave por compatibilidad entre versiones
        ServerUtil.run(PermadeathMod.server, "data merge entity " + uuid + " {Size:9,size:9,Health:40f}");
    }

    // ---------------- Dia 25 ----------------

    private static void buffRavager(UUID uuid) {
        effect(uuid, "strength", 1); // Fuerza II
        effect(uuid, "speed", 0);    // Velocidad I
    }

    /** Gigaslime: tamano 15 y doble de vida. Giga magmacube: tamano 16. (La clave Size guarda tamano-1.) */
    private static void buffGiga(UUID uuid, boolean slime) {
        int size = slime ? 15 : 16;
        ServerUtil.run(PermadeathMod.server, "data merge entity " + uuid + " {Size:" + (size - 1) + "}");
        int hp = slime ? size * size * 2 : size * size;
        if (slime) ServerUtil.run(PermadeathMod.server, "attribute " + uuid + " minecraft:max_health base set " + hp);
        ServerUtil.run(PermadeathMod.server, "data merge entity " + uuid + " {Health:" + hp + "f}");
        if (PhaseManager.dayAtLeast(30)) nameMob(uuid, slime ? "Gigaslime" : "Giga Magmacube");
    }

    /** Ghast demoniaco: 40-60 de vida y bolas de fuego con explosion de potencia 3, 4 o 5. name == null: no se renombra. */
    static void buffGhast(UUID uuid, String name) {
        int hp = 40 + RANDOM.nextInt(21);
        int power = 3 + RANDOM.nextInt(3);
        ServerUtil.run(PermadeathMod.server, "attribute " + uuid + " minecraft:max_health base set " + hp);
        ServerUtil.run(PermadeathMod.server, "data merge entity " + uuid + " {Health:" + hp + "f,ExplosionPower:" + power
                + "b,explosion_power:" + power + "b}");
        if (name != null && PhaseManager.dayAtLeast(30)) nameMob(uuid, name);
    }

    // ---------------- Dia 30 ----------------

    private static void buffPillager(UUID uuid) {
        effect(uuid, "invisibility", 0);
        ServerUtil.run(PermadeathMod.server, "data merge entity " + uuid
                + " {equipment:{mainhand:{id:\"minecraft:crossbow\",count:1,components:"
                + "{\"minecraft:enchantments\":{\"minecraft:quick_charge\":10}}}}}");
    }

    private static void dressPigman(UUID uuid) {
        ServerUtil.run(PermadeathMod.server, "data merge entity " + uuid
                + " {equipment:{head:{id:\"minecraft:diamond_helmet\",count:1},chest:{id:\"minecraft:diamond_chestplate\",count:1},"
                + "legs:{id:\"minecraft:diamond_leggings\",count:1},feet:{id:\"minecraft:diamond_boots\",count:1}},"
                + "drop_chances:{head:0f,chest:0f,legs:0f,feet:0f}}");
    }

    /** Da una clase a un esqueleto. Tactico y Pesadilla son esqueletos Wither: sustituyen al esqueleto normal. */
    private static void assignClass(Entity old) {
        if (old.isRemoved()) return;
        SkeletonClasses.Spec[] set = SkeletonClasses.set(true);
        SkeletonClasses.Spec spec = set[RANDOM.nextInt(set.length)];
        if (spec.entity().equals("minecraft:skeleton")) {
            SkeletonClasses.applyExisting(PermadeathMod.server, old.getUUID(), spec);
            return;
        }
        UUID nid = UUID.randomUUID();
        boolean ok;
        suppress = true;
        try {
            ok = ServerUtil.runChecked(PermadeathMod.server, "execute as " + old.getUUID() + " at @s run summon "
                    + spec.entity() + " ~ ~ ~ " + SkeletonClasses.summonNbt(spec, nid));
        } finally {
            suppress = false;
        }
        if (ok) {
            if (spec.speed()) SkeletonClasses.applySpeed(PermadeathMod.server, nid);
            old.discard();
        } else {
            PermadeathMod.LOGGER.warn("No se pudo crear la clase {}; se usa el Guerrero", spec.name());
            SkeletonClasses.applyExisting(PermadeathMod.server, old.getUUID(), set[0]);
        }
    }

    // ---------------- Esqueleto sobre la arana ----------------

    private static void spawnRider(Entity spider) {
        if (spider.isRemoved() || spider.isVehicle()) return;
        UUID spiderId = spider.getUUID();
        UUID skelId = UUID.randomUUID();
        SkeletonClasses.Spec[] set = SkeletonClasses.set(PhaseManager.dayAtLeast(30));
        SkeletonClasses.Spec spec = set[RANDOM.nextInt(set.length)];

        suppress = true; // el esqueleto jinete no se duplica
        try {
            String summon = "execute as " + spiderId + " at @s run summon " + spec.entity() + " ~ ~ ~ ";
            boolean ok = ServerUtil.runChecked(PermadeathMod.server, summon + SkeletonClasses.summonNbt(spec, skelId));
            if (!ok) {
                PermadeathMod.LOGGER.warn("No se pudo crear el esqueleto de clase {} ({}) con NBT completo; se usa uno basico", spec.name(), spec.entity());
                summon = "execute as " + spiderId + " at @s run summon minecraft:skeleton ~ ~ ~ ";
                ok = ServerUtil.runChecked(PermadeathMod.server, summon + "{UUID:" + SkeletonClasses.uuidInts(skelId)
                        + ",Tags:[\"pd_seen\"],equipment:{mainhand:{id:\"minecraft:bow\",count:1}}}");
            }
            if (ok) {
                if (spec.speed()) SkeletonClasses.applySpeed(PermadeathMod.server, skelId);
                ServerUtil.run(PermadeathMod.server, "ride " + skelId + " mount " + spiderId);
            }
        } finally {
            suppress = false;
        }
    }
}
