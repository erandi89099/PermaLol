package com.permadeath;

import com.permadeath.mechanics.MechanicRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/**
 * Fases: fase 0 = dias 1-9 (vanilla + tormenta). Desde el dia 10 y cada 10 dias se aplica
 * un aumento de dificultad: dia 10 = fase 1, dia 20 = fase 2, ... dia 110 = fase 11.
 * Ademas, cada 25 dias (25, 50, 75, 100) se reinicia el Death Train; el dia 25 trae cambios "sorpresa".
 */
public final class PhaseManager {
    private static volatile int currentPhase = 0;
    private static volatile int currentDay = 1;

    private PhaseManager() {}

    public static int currentPhase() {
        return currentPhase;
    }

    public static int currentDay() {
        return currentDay;
    }

    /** True desde el dia 25 hasta el final de la serie (cambios sorpresa y Death Train reforzado). */
    public static boolean day25Active() {
        PermadeathConfig cfg = PermadeathMod.config;
        return cfg != null && currentDay >= cfg.stormResetDays && currentDay <= cfg.seasonDays;
    }

    /** True desde el dia indicado hasta el final de la serie. */
    public static boolean dayAtLeast(int day) {
        PermadeathConfig cfg = PermadeathMod.config;
        return cfg != null && currentDay >= day && currentDay <= cfg.seasonDays;
    }

    public static int phaseForDay(int day, PermadeathConfig cfg) {
        return Math.max(0, day / cfg.phaseLengthDays);
    }

    /** Calcula la fase actual sin anunciar nada (se usa al arrancar el servidor). */
    public static void refresh(SeasonState state, PermadeathConfig cfg) {
        int day = state.currentDay(System.currentTimeMillis());
        currentDay = day;
        if (day <= cfg.seasonDays) currentPhase = phaseForDay(day, cfg);
    }

    /** Se llama una vez por segundo. */
    public static void tick(MinecraftServer server, SeasonState state, PermadeathConfig cfg) {
        long now = System.currentTimeMillis();
        int day = state.currentDay(now);
        currentDay = day;

        if (day > cfg.seasonDays) {
            if (!state.seasonEndAnnounced) {
                state.seasonEndAnnounced = true;
                state.save();
                ServerUtil.broadcast(server, Component.literal(
                        "La serie Permadeath ha terminado (" + cfg.seasonDays + " dias).")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
            return;
        }

        int phase = phaseForDay(day, cfg);
        currentPhase = phase;
        if (phase > state.lastPhaseAnnounced) {
            state.lastPhaseAnnounced = phase;
            state.save();

            if (phase == 0) {
                ServerUtil.broadcast(server, Component.literal("=== PERMADEATH === (" + cfg.seasonDays + " dias)")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            } else {
                int labelDay = phase * cfg.phaseLengthDays;
                announce(server, "AUMENTO DE DIFICULTAD", labelDay, cfg);
            }
            sendLines(server, cfg.announcements.get(phase));
            MechanicRegistry.onPhaseStart(server, phase);
        }

        // Reset del Death Train cada 25 dias (25, 50, 75, 100)
        int resetIdx = day / cfg.stormResetDays;
        if (resetIdx > state.lastResetAnnounced) {
            state.lastResetAnnounced = resetIdx;
            state.save();
            int resetDay = resetIdx * cfg.stormResetDays;
            announce(server, resetIdx == 1 ? "AUMENTO DE DIFICULTAD SORPRESA" : "RESET DEL DEATH TRAIN", resetDay, cfg);
            List<String> msgs = cfg.dayAnnouncements.get(resetDay);
            if (msgs == null) {
                msgs = List.of("RESET DEL DEATH TRAIN: las horas de tormenta por muerte se reinician como si fuese el día 1");
            }
            sendLines(server, msgs);
        }
    }

    private static void announce(MinecraftServer server, String title, int labelDay, PermadeathConfig cfg) {
        ServerUtil.sendTitle(server,
                Component.literal(title).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                Component.literal("Día " + labelDay + "/" + cfg.seasonDays).withStyle(ChatFormatting.WHITE),
                20, 100, 40);
        ServerUtil.broadcast(server, Component.literal(
                "=== " + title + " · DÍA " + labelDay + "/" + cfg.seasonDays + " ===")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
    }

    private static void sendLines(MinecraftServer server, List<String> msgs) {
        if (msgs == null) return;
        for (String m : msgs) {
            ServerUtil.broadcast(server, Component.literal("☠ " + m).withStyle(ChatFormatting.YELLOW));
        }
    }
}
