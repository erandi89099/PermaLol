package com.permadeath;

import com.permadeath.mechanics.AggroMobs;
import com.permadeath.mechanics.DropRemoval;
import com.permadeath.mechanics.MechanicRegistry;
import com.permadeath.mechanics.MobHooks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermadeathMod implements ModInitializer {
    public static final String MOD_ID = "permadeath";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static MinecraftServer server;
    public static SeasonState state;
    public static PermadeathConfig config;

    private static int tickCounter = 0;

    @Override
    public void onInitialize() {
        config = PermadeathConfig.load();
        MechanicRegistry.registerAll();
        MobHooks.register();
        DropRemoval.register();
        FirstJoin.register();
        PermadeathCommands.register();

        // El estado se carga al principio para que la fase ya este calculada cuando carguen los mobs
        ServerLifecycleEvents.SERVER_STARTING.register(s -> {
            server = s;
            state = SeasonState.load(s.getWorldPath(LevelResource.ROOT));
            PhaseManager.refresh(state, config);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            if (state != null) state.save();
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (state != null && server != null && entity instanceof ServerPlayer player) {
                DeathHandler.onPlayerDeath(server, state, config, player);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (state == null) return;
            MobHooks.tick();
            MechanicRegistry.tick(s, PhaseManager.currentPhase());
            if (++tickCounter % 20 != 0) return; // una vez por segundo
            PhaseManager.tick(s, state, config);
            StormManager.tick(s, state);
            DeathHandler.tickBans(s, state);
            FirstJoin.tick(s, state);
            AggroMobs.tick(s);
        });

        LOGGER.info("Permadeath cargado.");
    }
}
