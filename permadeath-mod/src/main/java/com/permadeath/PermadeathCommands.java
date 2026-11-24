package com.permadeath;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PermadeathCommands {
    private PermadeathCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("permadeath")
                .then(Commands.literal("estado")
                    .executes(ctx -> estado(ctx.getSource())))
                .then(Commands.literal("mensaje")
                    .then(Commands.argument("texto", StringArgumentType.greedyString())
                        .executes(ctx -> mensaje(ctx.getSource(), StringArgumentType.getString(ctx, "texto")))))
                .then(Commands.literal("revivir")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("jugador", StringArgumentType.word())
                        .executes(ctx -> revivir(ctx.getSource(), StringArgumentType.getString(ctx, "jugador")))))
                .then(Commands.literal("setdia")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("dia", IntegerArgumentType.integer(1, 1000))
                        .executes(ctx -> setDia(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "dia")))))
                .then(Commands.literal("tormenta")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("sumar")
                        .then(Commands.argument("horas", IntegerArgumentType.integer(1, 10000))
                            .executes(ctx -> tormentaSumar(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "horas")))))
                    .then(Commands.literal("limpiar")
                        .executes(ctx -> tormentaLimpiar(ctx.getSource()))))
            ));
    }

    private static int estado(CommandSourceStack src) {
        SeasonState st = PermadeathMod.state;
        PermadeathConfig cfg = PermadeathMod.config;
        long now = System.currentTimeMillis();
        int day = st.currentDay(now);
        String msg;
        if (day > cfg.seasonDays) {
            msg = "La serie ha terminado.";
        } else {
            int phase = PhaseManager.phaseForDay(day, cfg);
            int next = (phase + 1) * cfg.phaseLengthDays;
            msg = "Día " + day + "/" + cfg.seasonDays + " | Fase " + phase
                    + (next <= cfg.seasonDays ? " | Próximo aumento: día " + next : " | Última fase")
                    + " | Tormenta restante: " + ServerUtil.formatMs(StormManager.remainingMs(st, now));
        }
        final String out = msg;
        src.sendSuccess(() -> Component.literal(out), false);
        return 1;
    }

    private static int mensaje(CommandSourceStack src, String texto) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        String clean = texto.replaceAll("[\\p{Cntrl}§]", "").trim();
        if (clean.length() > 120) clean = clean.substring(0, 120);
        if (clean.isEmpty()) {
            src.sendFailure(Component.literal("El mensaje no puede estar vacío."));
            return 0;
        }
        String name = player.getName().getString().toLowerCase();
        PermadeathMod.state.deathMessages.put(name, clean);
        PermadeathMod.state.prompted.add(name);
        PermadeathMod.state.save();
        final String shown = clean;
        src.sendSuccess(() -> Component.literal("Tu mensaje de muerte quedó guardado: " + shown), false);
        return 1;
    }

    private static int revivir(CommandSourceStack src, String name) {
        PermadeathMod.state.pendingBans.remove(name);
        PermadeathMod.state.save();
        ServerUtil.run(PermadeathMod.server, "pardon " + name);
        src.sendSuccess(() -> Component.literal("Jugador " + name + " perdonado."), true);
        return 1;
    }

    private static int setDia(CommandSourceStack src, int dia) {
        SeasonState st = PermadeathMod.state;
        st.startMs = System.currentTimeMillis() - (long) (dia - 1) * SeasonState.DAY_MS;
        st.lastPhaseAnnounced = PhaseManager.phaseForDay(dia, PermadeathMod.config) - 1;
        st.seasonEndAnnounced = false;
        st.lastResetAnnounced = Math.max(0, dia / PermadeathMod.config.stormResetDays - 1);
        st.save();
        PhaseManager.refresh(st, PermadeathMod.config);
        src.sendSuccess(() -> Component.literal("Día ajustado a " + dia + "."), true);
        return 1;
    }

    private static int tormentaSumar(CommandSourceStack src, int horas) {
        StormManager.addHours(PermadeathMod.state, horas, System.currentTimeMillis());
        PermadeathMod.state.save();
        src.sendSuccess(() -> Component.literal("+" + horas + " h de tormenta."), true);
        return 1;
    }

    private static int tormentaLimpiar(CommandSourceStack src) {
        PermadeathMod.state.stormEndMs = 0;
        PermadeathMod.state.save();
        src.sendSuccess(() -> Component.literal("Tormenta acumulada borrada."), true);
        return 1;
    }
}
