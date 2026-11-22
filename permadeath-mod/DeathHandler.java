package com.permadeath;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class DeathHandler {
    private DeathHandler() {}

    public static void onPlayerDeath(MinecraftServer server, SeasonState state, PermadeathConfig cfg, ServerPlayer dead) {
        long now = System.currentTimeMillis();
        int day = state.currentDay(now);
        if (day > cfg.seasonDays) return; // la serie ya termino

        String name = dead.getName().getString();

        // 1) Tormenta: tantas horas reales como el numero del dia, acumulable
        StormManager.addHours(state, day, now);
        long remaining = StormManager.remainingMs(state, now);

        // 2) Pantalla: "¡Permadeath!" + "<jugador> ha muerto" para todos, con sonido
        ServerUtil.sendTitle(server,
                Component.literal("¡Permadeath!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                Component.literal(name + " ha muerto").withStyle(ChatFormatting.WHITE),
                10, 80, 30);
        // Sonido global: el ultimo parametro (volumen minimo = 1) lo hace audible sin importar la distancia
        ServerUtil.run(server, "execute as @a at @s run playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 1 1 1");

        // 3) Chat: mensaje oficial
        Component head = Component.literal("Este es el comienzo del sufrimiento eterno de ")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal(name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal(". ¡HA SIDO PERMABANEADO!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        ServerUtil.broadcast(server, head);

        // 4) Chat: mensaje de muerte personalizado del jugador (si lo configuro)
        String custom = state.deathMessages.get(name.toLowerCase());
        if (custom != null && !custom.isBlank()) {
            ServerUtil.broadcast(server, Component.literal(name + ": ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(custom).withStyle(ChatFormatting.WHITE)));
        }

        // 5) Info de tormenta
        ServerUtil.broadcast(server, Component.literal("Tormenta perpetua: +" + day + " h (total restante: "
                + ServerUtil.formatMs(remaining) + ")").withStyle(ChatFormatting.GRAY));

        // 6) Baneo programado
        if (!cfg.exempt.contains(name.toLowerCase())) {
            state.pendingBans.put(name, now + cfg.banDelaySeconds * 1000L);
        }
        state.save();
    }

    /** Se llama una vez por segundo: banea a quien ya cumplio su espera. */
    public static void tickBans(MinecraftServer server, SeasonState state) {
        if (state.pendingBans.isEmpty()) return;
        long now = System.currentTimeMillis();
        List<String> due = new ArrayList<>();
        state.pendingBans.forEach((name, t) -> {
            if (now >= t) due.add(name);
        });
        for (String name : due) {
            state.pendingBans.remove(name);
            ServerUtil.run(server, "ban " + name + " PERMADEATH: has sido permabaneado");
        }
        if (!due.isEmpty()) state.save();
    }
}
