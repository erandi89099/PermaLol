package com.permadeath.mechanics;

import com.permadeath.PhaseManager;
import com.permadeath.SeasonState;
import com.permadeath.ServerUtil;
import net.minecraft.server.MinecraftServer;

/**
 * Dia 25: durante el Death Train (tormenta perpetua activa) todos los mobs tienen Fuerza I, Velocidad I y Resistencia I.
 * Se refresca cada segundo en los mobs cercanos a los jugadores (efectos cortos, desaparecen al terminar la tormenta).
 */
public final class DeathTrain {
    private DeathTrain() {}

    /** Llamar una vez por segundo. */
    public static void tick(MinecraftServer server, SeasonState state) {
        if (!PhaseManager.day25Active()) return;
        if (System.currentTimeMillis() >= state.stormEndMs) return;
        String sel = "@e[type=!minecraft:player,distance=..64]";
        ServerUtil.run(server, "execute as @a at @s run effect give " + sel + " minecraft:strength 5 0 true");
        ServerUtil.run(server, "execute as @a at @s run effect give " + sel + " minecraft:speed 5 0 true");
        ServerUtil.run(server, "execute as @a at @s run effect give " + sel + " minecraft:resistance 5 0 true");
    }
}
