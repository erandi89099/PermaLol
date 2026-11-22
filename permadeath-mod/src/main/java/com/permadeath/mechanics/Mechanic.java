package com.permadeath.mechanics;

import net.minecraft.server.MinecraftServer;

/**
 * Mecanica de juego que se activa a partir de cierta fase.
 * Para anadir una: crea una clase que implemente esta interfaz y registrala en MechanicRegistry.
 */
public interface Mechanic {
    String id();

    /** Fase a partir de la cual esta activa (fase 1 = dias 1-10, fase 2 = dias 11-20, ...). */
    int fromPhase();

    /** Se llama cada tick de servidor mientras la mecanica esta activa. */
    default void onTick(MinecraftServer server, int phase) {}

    /** Se llama una vez cuando empieza la fase en la que se activa. */
    default void onPhaseStart(MinecraftServer server, int phase) {}
}
