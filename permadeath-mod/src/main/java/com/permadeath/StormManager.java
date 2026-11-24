package com.permadeath;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Tormenta perpetua acumulable (solo Overworld, horas reales). */
public final class StormManager {
    private static boolean wasActive = false;

    private StormManager() {}

    /**
     * Horas de tormenta por muerte: el numero del dia, pero cada 25 dias se reinicia "como si fuese el dia 1"
     * (dia 24 -> 24 h, dia 25 -> 1 h, dia 26 -> 2 h ... dia 49 -> 25 h, dia 50 -> 1 h).
     */
    public static int hoursForDay(int day, int resetDays) {
        int resets = day / resetDays;
        return day - resets * resetDays + (resets > 0 ? 1 : 0);
    }

    /** Suma horas a la tormenta: si sigue activa se acumulan sobre lo restante. */
    public static void addHours(SeasonState state, long hours, long nowMs) {
        state.stormEndMs = Math.max(state.stormEndMs, nowMs) + hours * SeasonState.HOUR_MS;
    }

    public static long remainingMs(SeasonState state, long nowMs) {
        return Math.max(0, state.stormEndMs - nowMs);
    }

    /** Se llama una vez por segundo. */
    public static void tick(MinecraftServer server, SeasonState state) {
        long now = System.currentTimeMillis();
        ServerLevel overworld = server.overworld();
        if (now < state.stormEndMs) {
            wasActive = true;
            // Si el clima se despeja (dormir, comandos, etc.) se vuelve a forzar la tormenta
            if (!overworld.isThundering()) {
                ServerUtil.run(server, "weather thunder 72000");
            }
        } else if (wasActive) {
            wasActive = false;
            ServerUtil.run(server, "weather clear");
            ServerUtil.broadcast(server,
                    Component.literal("La tormenta perpetua ha terminado.").withStyle(ChatFormatting.AQUA));
        }
    }
}
