package com.permadeath.mechanics;

import com.permadeath.ServerUtil;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/** Clases de esqueleto. Dia 20: 5 clases sobre aranas. Dia 30: todos los esqueletos tienen clase, nombre y flechas de Dano II. */
public final class SkeletonClasses {
    private SkeletonClasses() {}

    private static final String SKELETON = "minecraft:skeleton";
    private static final String WITHER = "minecraft:wither_skeleton";

    /** entity: tipo de mob (esqueleto normal o Wither). name == null: sin nombre (clases del dia 20). */
    public record Spec(String entity, String name, int hp, boolean speed, boolean arrows, String hand, String armor) {}

    private static final String LEATHER_RED = "\"minecraft:dyed_color\":11141120";

    /** Clases del dia 20 (sobre aranas). La cota de malla y la de cuero rojo son esqueletos Wither. */
    private static final Spec[] DAY20 = {
            new Spec(SKELETON, null, 20, false, false, piece("bow", null), armorSet("diamond", null)),
            new Spec(WITHER, null, 40, false, false, piece("bow", ench("punch", 20)), armorSet("chainmail", null)),
            new Spec(SKELETON, null, 20, false, false, piece("iron_axe", ench("fire_aspect", 2)), armorSet("iron", null)),
            new Spec(SKELETON, null, 40, false, false, piece("crossbow", ench("sharpness", 20)), armorSet("golden", null)),
            new Spec(WITHER, null, 40, false, false, piece("bow", ench("power", 10)), armorSet("leather", LEATHER_RED))
    };

    /** Guerrero, Infernal, Asesino (esqueletos) y Tactico, Pesadilla (esqueletos Wither). 20 corazones = 40 de vida. */
    private static final Spec[] DAY30 = {
            new Spec(SKELETON, "Esqueleto Guerrero", 40, false, true, piece("bow", null), armorSet("diamond", ench("protection", 4))),
            new Spec(SKELETON, "Esqueleto Infernal", 40, false, true, piece("diamond_axe", ench("fire_aspect", 10)), armorSet("iron", null)),
            new Spec(SKELETON, "Esqueleto Asesino", 40, true, true, piece("crossbow", ench("sharpness", 25)), armorSet("golden", null)),
            new Spec(WITHER, "Esqueleto Táctico", 40, false, true, piece("bow", ench("punch", 30, "power", 25)), armorSet("chainmail", null)),
            new Spec(WITHER, "Esqueleto Pesadilla", 40, false, true, piece("bow", ench("power", 50)), armorSet("leather", LEATHER_RED))
    };

    private static final String ARROWS = "{id:\"minecraft:tipped_arrow\",count:1,components:"
            + "{\"minecraft:potion_contents\":{potion:\"minecraft:strong_harming\"}}}";

    public static Spec[] set(boolean day30) {
        return day30 ? DAY30 : DAY20;
    }

    public static String uuidInts(UUID u) {
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

    /** ench("punch", 30, "power", 25) -> componente de encantamientos. */
    private static String ench(Object... nameLevel) {
        StringBuilder sb = new StringBuilder("\"minecraft:enchantments\":{");
        for (int i = 0; i < nameLevel.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append("\"minecraft:").append(nameLevel[i]).append("\":").append(nameLevel[i + 1]);
        }
        return sb.append('}').toString();
    }

    private static String equipment(Spec s) {
        return "equipment:{mainhand:" + s.hand() + (s.arrows() ? ",offhand:" + ARROWS : "") + "," + s.armor() + "}";
    }

    private static final String NO_DROPS = "drop_chances:{mainhand:0f,offhand:0f,head:0f,chest:0f,legs:0f,feet:0f}";

    private static String nameNbt(Spec s) {
        return s.name() == null ? "" : ",CustomName:\"" + s.name() + "\",CustomNameVisible:1b";
    }

    /** NBT completo para invocar un esqueleto (jinete de arana). */
    public static String summonNbt(Spec s, UUID id) {
        return "{UUID:" + uuidInts(id) + ",Tags:[\"pd_seen\"" + (s.name() != null ? ",\"pd_class\"" : "") + "]"
                + nameNbt(s)
                + (s.hp() != 20 ? ",Health:" + s.hp() + "f,attributes:[{id:\"minecraft:max_health\",base:" + s.hp() + "}]" : "")
                + "," + equipment(s) + "," + NO_DROPS + "}";
    }

    /** Convierte un esqueleto ya existente en uno de esta clase. */
    public static void applyExisting(MinecraftServer server, UUID uuid, Spec s) {
        ServerUtil.run(server, "attribute " + uuid + " minecraft:max_health base set " + s.hp());
        ServerUtil.run(server, "data merge entity " + uuid + " {Health:" + s.hp() + "f" + nameNbt(s) + ","
                + equipment(s) + "," + NO_DROPS + "}");
        if (s.speed()) applySpeed(server, uuid);
    }

    public static void applySpeed(MinecraftServer server, UUID uuid) {
        ServerUtil.run(server, "effect give " + uuid + " minecraft:speed infinite 1 true"); // Speed II
    }
}
