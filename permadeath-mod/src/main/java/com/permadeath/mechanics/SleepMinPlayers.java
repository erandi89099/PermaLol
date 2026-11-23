package com.permadeath.mechanics;

import com.permadeath.ServerUtil;
import net.minecraft.server.MinecraftServer;

/**
 * Dia 10: minimo 4 jugadores durmiendo para pasar la noche. Dia 20 en adelante: no se puede saltar la noche.
 * Se ajusta la gamerule players_sleeping_percentage segun los jugadores conectados.
 */
public final class SleepMinPlayers implements Mechanic {
    private static final int MIN_SLEEPING = 4;
    private int lastPct = -1;

    @Override public String id() { return "sleep_min_players"; }
    @Override public int fromPhase() { return 1; }

    @Override
    public void onTick(MinecraftServer server, int phase) {
        if (server.getTickCount() % 20 != 0) return;
        int online = server.getPlayerList().getPlayerCount();
        int pct;
        if (phase >= 2) {
            pct = 200; // Dia 20: ciclo constante, imposible saltar la noche
        } else {
            // <4 conectados: 101% = imposible saltar la noche. Si no, porcentaje que equivale a 4 jugadores.
            pct = online < MIN_SLEEPING ? 101 : Math.max(1, (MIN_SLEEPING * 100) / online);
        }
        if (pct != lastPct) {
            lastPct = pct;
            ServerUtil.run(server, "gamerule players_sleeping_percentage " + pct);
        }
    }
}
