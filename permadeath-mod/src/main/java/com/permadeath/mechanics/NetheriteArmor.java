package com.permadeath.mechanics;

import com.permadeath.ServerUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cada pieza de armadura de netherita equipada da 1 corazon extra (2 de vida maxima). */
public final class NetheriteArmor {
    private static final String MODIFIER_ID = "permadeath:netherite_hearts";
    private static final Map<UUID, Integer> LAST = new HashMap<>();

    private NetheriteArmor() {}

    private static int countPieces(ServerPlayer p) {
        int n = 0;
        if (p.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET)) n++;
        if (p.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE)) n++;
        if (p.getItemBySlot(EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS)) n++;
        if (p.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS)) n++;
        return n;
    }

    /** Llamar una vez por segundo. */
    public static void tick(MinecraftServer server) {
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            int n = countPieces(p);
            Integer last = LAST.get(p.getUUID());
            if (last != null && last == n) continue;
            LAST.put(p.getUUID(), n);
            UUID id = p.getUUID();
            ServerUtil.run(server, "attribute " + id + " minecraft:max_health modifier remove " + MODIFIER_ID);
            if (n > 0) {
                ServerUtil.run(server, "attribute " + id + " minecraft:max_health modifier add "
                        + MODIFIER_ID + " " + (n * 2) + " add_value");
            }
        }
    }
}
