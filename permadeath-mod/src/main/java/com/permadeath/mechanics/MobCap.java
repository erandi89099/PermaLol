package com.permadeath.mechanics;

import com.permadeath.PermadeathMod;
import com.permadeath.PhaseManager;
import net.minecraft.world.entity.MobCategory;

import java.lang.reflect.Field;

/**
 * Dia 10: "Doble de mobs" = el doble de mobcap de los mobs hostiles (no se duplican mobs).
 * Se multiplica por 2 el limite de la categoria MONSTER (70 -> 140 por defecto).
 */
public final class MobCap {
    private static Field maxField;
    private static int original = -1;

    static {
        try {
            maxField = MobCategory.class.getDeclaredField("max");
            maxField.setAccessible(true);
            original = maxField.getInt(MobCategory.MONSTER);
        } catch (ReflectiveOperationException | RuntimeException e) {
            PermadeathMod.LOGGER.error("MobCap: no se pudo acceder al limite de mobs hostiles", e);
            maxField = null;
        }
    }

    private MobCap() {}

    /** Valor actual del mobcap hostil (-1 si no se pudo leer). */
    public static int current() {
        if (maxField == null) return -1;
        try {
            return maxField.getInt(MobCategory.MONSTER);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return -1;
        }
    }

    /** Una vez por segundo: mantiene el mobcap segun la fase. */
    public static void tick() {
        if (maxField == null) return;
        int desired = PhaseManager.currentPhase() >= 1 ? original * 2 : original;
        try {
            if (maxField.getInt(MobCategory.MONSTER) != desired) {
                maxField.setInt(MobCategory.MONSTER, desired);
                PermadeathMod.LOGGER.info("MobCap hostil ajustado a {}", desired);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            PermadeathMod.LOGGER.error("MobCap: no se pudo cambiar el limite", e);
            maxField = null;
        }
    }
}
