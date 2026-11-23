package com.permadeath;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

public final class ServerUtil {
    private ServerUtil() {}

    /** Ejecuta un comando como la consola del servidor, sin spam de feedback. */
    public static void run(MinecraftServer server, String command) {
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }

    /** Igual que run(), pero devuelve si el comando tuvo exito (y registra el motivo si falla). */
    public static boolean runChecked(MinecraftServer server, String command) {
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        try {
            return server.getCommands().getDispatcher().execute(command, source) > 0;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            PermadeathMod.LOGGER.warn("Comando fallido: {} -> {}", command, e.getMessage());
            return false;
        }
    }

    public static void broadcast(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    /** Titulo + subtitulo en pantalla para todos los jugadores conectados. */
    public static void sendTitle(MinecraftServer server, Component title, Component subtitle,
                                 int fadeIn, int stay, int fadeOut) {
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
            p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            p.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    public static String formatMs(long ms) {
        long totalMin = Math.max(0, ms) / 60_000L;
        return (totalMin / 60) + " h " + (totalMin % 60) + " min";
    }
}
