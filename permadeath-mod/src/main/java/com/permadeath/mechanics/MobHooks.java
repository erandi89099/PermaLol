package com.permadeath.mechanics;

import com.permadeath.PermadeathMod;
import com.permadeath.PhaseManager;
import com.permadeath.ServerUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.*;

/**
 * Mecanicas sobre mobs hostiles:
 *  Dia 10: doble de mobs; aranas con 1-3 efectos.
 *  Dia 20: aranas con 3-5 efectos y esqueleto encima (5 clases); phantoms tamano 9 y doble vida.
 * Las acciones se encolan y se ejecutan al final del tick para no tocar el mundo durante la carga de la entidad.
 */
public final class MobHooks {
    private static final String TAG_SEEN = "pd_seen";
    private static final String TAG_FX = "pd_fx";
    private static final String TAG_FX2 = "pd_fx2";
    private static final String TAG_RIDER = "pd_rider";
    private static final String TAG_PHANTOM = "pd_phantom";

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

    public static void enqueue(Runnable r) {
        QUEUE.add(r);
    }

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

            boolean spider = id.equals("minecraft:spider") || id.equals("minecraft:cave_spider");
            if (spider) {
                if (phase >= 2) {
                    // Dia 20: de 3 a 5 efectos (se limpian los anteriores)
                    if (entity.addTag(TAG_FX2)) QUEUE.add(() -> applySpiderEffects(uuid, 3, 5, true));
                } else if (phase >= 1) {
                    if (entity.addTag(TAG_FX)) QUEUE.add(() -> applySpiderEffects(uuid, 1, 3, false));
                }
                // Dia 20: esqueleto encima de cada arana
                if (phase >= 2 && id.equals("minecraft:spider") && entity.addTag(TAG_RIDER)) {
                    QUEUE.add(() -> spawnRider(entity));
                }
            }

            // Dia 20: phantoms de tamano 9 y doble de vida
            if (phase >= 2 && id.equals("minecraft:phantom") && entity.addTag(TAG_PHANTOM)) {
                QUEUE.add(() -> buffPhantom(uuid));
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

    // ---------------- Esqueleto sobre la arana (5 clases) ----------------

    private static void spawnRider(Entity spider) {
        if (spider.isRemoved() || spider.isVehicle()) return;
        UUID spiderId = spider.getUUID();
        UUID skelId = UUID.randomUUID();
        int kind = RANDOM.nextInt(5);

        suppress = true; // el esqueleto jinete no se duplica
        try {
            String summon = "execute as " + spiderId + " at @s run summon minecraft:skeleton ~ ~ ~ ";
            boolean ok = ServerUtil.runChecked(PermadeathMod.server, summon + skeletonNbt(kind, skelId));
            if (!ok) {
                PermadeathMod.LOGGER.warn("No se pudo crear el esqueleto de clase {} con NBT completo; se usa uno basico", kind);
                ok = ServerUtil.runChecked(PermadeathMod.server, summon + "{UUID:" + uuidInts(skelId)
                        + ",Tags:[\"pd_seen\"],equipment:{mainhand:{id:\"minecraft:bow\",count:1}}}");
            }
            if (ok) ServerUtil.run(PermadeathMod.server, "ride " + skelId + " mount " + spiderId);
        } finally {
            suppress = false;
        }
    }

    private static String uuidInts(UUID u) {
        long m = u.getMostSignificantBits(), l = u.getLeastSignificantBits();
        return "[I;" + (int) (m >> 32) + "," + (int) m + "," + (int) (l >> 32) + "," + (int) l + "]";
    }

    private static String piece(String id, String components) {
        return "{id:\"minecraft:" + id + "\",count:1"
                + (components == null ? "" : ",components:{" + components + "}") + "}";
    }

    private static String armorSet(String material, String components) {
        return "head:" + piece(material + "_helmet", components)
                + ",chest:" + piece(material + "_chestplate", components)
                + ",legs:" + piece(material + "_leggings", components)
                + ",feet:" + piece(material + "_boots", components);
    }

    private static String enchant(String name, int level) {
        return "\"minecraft:enchantments\":{\"minecraft:" + name + "\":" + level + "}";
    }

    /**
     * Clases (segun la imagen): 0 diamante / 20 HP; 1 cota de malla + Arco Punch XX / 40 HP;
     * 2 hierro + Hacha de hierro Fire Aspect II / 20 HP; 3 oro + Ballesta Sharpness XX / 40 HP;
     * 4 cuero rojo + Arco Power X / 40 HP.
     */
    private static String skeletonNbt(int kind, UUID id) {
        String hand, armor;
        boolean tanky = false;
        switch (kind) {
            case 1 -> { hand = piece("bow", enchant("punch", 20)); armor = armorSet("chainmail", null); tanky = true; }
            case 2 -> { hand = piece("iron_axe", enchant("fire_aspect", 2)); armor = armorSet("iron", null); }
            case 3 -> { hand = piece("crossbow", enchant("sharpness", 20)); armor = armorSet("golden", null); tanky = true; }
            case 4 -> { hand = piece("bow", enchant("power", 10)); armor = armorSet("leather", "\"minecraft:dyed_color\":11141120"); tanky = true; }
            default -> { hand = piece("bow", null); armor = armorSet("diamond", null); }
        }
        return "{UUID:" + uuidInts(id) + ",Tags:[\"pd_seen\"]"
                + (tanky ? ",Health:40f,attributes:[{id:\"minecraft:max_health\",base:40}]" : "")
                + ",equipment:{mainhand:" + hand + "," + armor + "}"
                + ",drop_chances:{mainhand:0f,head:0f,chest:0f,legs:0f,feet:0f}}";
    }
}
