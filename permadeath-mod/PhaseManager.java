package com.permadeath;

import com.permadeath.mechanics.MechanicRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/**
 * Fases: fase 0 = dias 1-9 (vanilla + tormenta). Desde el dia 10 y cada 10 dias se aplica
 * un aumento de dificultad: dia 10 = fase 1, dia 20 = fase 2, ... dia 110 = fase 11.
 */
public final class PhaseManager {
    private static volatile int currentPhase = 0;

    private PhaseManager() {}

    public static int currentPhase() {
        return currentPhase;
    }

    public static int phaseForDay(int day, PermadeathConfig cfg) {
        return Math.max(0, day / cfg.phaseLengthDays);
    }

    /** Calcula la fase actual sin anunciar nada (se usa al arrancar el servidor). */
    public static void refresh(SeasonState state, PermadeathConfig cfg) {
        int day = state.currentDay(System.currentTimeMillis());
        if (day <= cfg.seasonDays) currentPhase = phaseForDay(day, cfg);
    }

    /** Se llama una vez por segundo. */
    public static void tick(MinecraftServer server, SeasonState state, PermadeathConfig cfg) {
        long now = System.currentTimeMillis();
        int day = state.currentDay(now);

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
                ServerUtil.sendTitle(server,
                        Component.literal("AUMENTO DE DIFICULTAD").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                        Component.literal("Día " + labelDay + "/" + cfg.seasonDays).withStyle(ChatFormatting.WHITE),
                        20, 100, 40);
                ServerUtil.broadcast(server, Component.literal(
                        "=== AUMENTO DE DIFICULTAD · DÍA " + labelDay + "/" + cfg.seasonDays + " ===")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
            List<String> msgs = cfg.announcements.get(phase);
            if (msgs != null) {
                for (String m : msgs) {
                    ServerUtil.broadcast(server, Component.literal("☠ " + m).withStyle(ChatFormatting.YELLOW));
                }
            }
            MechanicRegistry.onPhaseStart(server, phase);
        }
    }
}
