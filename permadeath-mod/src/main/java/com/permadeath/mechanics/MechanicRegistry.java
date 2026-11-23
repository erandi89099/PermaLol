package com.permadeath.mechanics;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public final class MechanicRegistry {
    private static final List<Mechanic> MECHANICS = new ArrayList<>();

    private MechanicRegistry() {}

    public static void register(Mechanic m) {
        MECHANICS.add(m);
    }

    /** Aqui se registraran las mecanicas de las fases 2..11 conforme las definamos. */
    public static void registerAll() {
        register(new SleepMinPlayers()); // Dia 10: minimo 4 jugadores para pasar la noche
        // Dia 10: doble de mobs y efectos en aranas -> ver MobHooks (se registra en PermadeathMod)
        // Dia 20: ver MobHooks, DropRemoval y AggroMobs (se registran en PermadeathMod)
        // Ejemplo (fase 3 = dia 30):
        // register(new MiMecanica());
    }

    public static void tick(MinecraftServer server, int phase) {
        for (Mechanic m : MECHANICS) {
            if (phase >= m.fromPhase()) m.onTick(server, phase);
        }
    }

    public static void onPhaseStart(MinecraftServer server, int phase) {
        for (Mechanic m : MECHANICS) {
            if (m.fromPhase() == phase) m.onPhaseStart(server, phase);
        }
    }
}
