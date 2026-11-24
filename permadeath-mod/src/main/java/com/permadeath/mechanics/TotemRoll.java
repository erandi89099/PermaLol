package com.permadeath.mechanics;

import com.permadeath.PermadeathMod;
import com.permadeath.PhaseManager;
import com.permadeath.ServerUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Dia 30: los totems tienen 99% de activarse y 1% de fallar. Se tira un dado de 1 a 100 cuando el jugador
 * lleva un totem y va a morir: con un 1 el totem no se activa (el jugador muere); con otro numero se activa
 * y el chat muestra el numero sobre 100. Lo llama LivingEntityMixin.
 */
public final class TotemRoll {
    private static final Map<UUID, Integer> LAST = new HashMap<>();
    private static final Random RANDOM = new Random();

    private TotemRoll() {}

    /** Devuelve true si el totem debe fallar (el 1%). */
    public static boolean shouldFail(LivingEntity e) {
        if (!(e instanceof ServerPlayer p) || !PhaseManager.dayAtLeast(30)) return false;
        if (!p.getMainHandItem().is(Items.TOTEM_OF_UNDYING) && !p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return false;
        int roll = 1 + RANDOM.nextInt(100);
        LAST.put(p.getUUID(), roll);
        return roll == 1;
    }

    /** El totem se activo: avisa en el chat con la tirada. */
    public static void activated(LivingEntity e) {
        if (!(e instanceof ServerPlayer p) || PermadeathMod.server == null) return;
        Integer roll = LAST.remove(p.getUUID());
        if (roll == null) return;
        ServerUtil.broadcast(PermadeathMod.server, Component.literal("El tótem de ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(p.getName().getString()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal(" se activó · probabilidad " + roll + "/100").withStyle(ChatFormatting.YELLOW)));
    }
}
