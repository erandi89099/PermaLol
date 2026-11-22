package com.permadeath;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Primera vez que un jugador entra al mundo: ventana (dialogo nativo de Minecraft) para escribir su
 * mensaje de muerte personalizado. Funciona con clientes vanilla. Respaldo: /permadeath mensaje <texto>.
 */
public final class FirstJoin {
    private static final Map<String, Long> PENDING = new HashMap<>();

    private static final String DIALOG = "{type:\"minecraft:notice\","
            + "title:\"PERMADEATH\","
            + "body:{type:\"minecraft:plain_message\",contents:\"Escribe tu mensaje de muerte personalizado. "
            + "Se mostrará en el chat cuando mueras y seas permabaneado.\"},"
            + "can_close_with_escape:true,"
            + "inputs:[{type:\"minecraft:text\",key:\"msg\",label:\"Tu mensaje de muerte\",width:320,max_length:120,initial:\"\"}],"
            + "action:{label:\"Guardar\",action:{type:\"minecraft:dynamic/run_command\",template:\"permadeath mensaje $(msg)\"}}}";

    private FirstJoin() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (PermadeathMod.state == null) return;
            ServerPlayer player = handler.getPlayer();
            String name = player.getName().getString();
            if (!PermadeathMod.state.prompted.contains(name.toLowerCase())) {
                PENDING.put(name, System.currentTimeMillis() + 3000L);
            }
        });
    }

    /** Una vez por segundo. */
    public static void tick(MinecraftServer server, SeasonState state) {
        if (PENDING.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (now < e.getValue()) continue;
            it.remove();
            String name = e.getKey();
            ServerPlayer player = server.getPlayerList().getPlayerByName(name);
            if (player == null) continue; // se desconecto: se le preguntara la proxima vez
            state.prompted.add(name.toLowerCase());
            state.save();
            player.sendSystemMessage(Component.literal(
                    "Escribe tu mensaje de muerte con /permadeath mensaje <texto> (también puedes cambiarlo cuando quieras).")
                    .withStyle(ChatFormatting.GRAY));
            ServerUtil.run(server, "dialog show " + name + " " + DIALOG);
        }
    }
}
