package com.permadeath.mechanics;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayDeque;

/**
 * Desde el inicio del mundo no se puede conseguir netherita de forma manual:
 *  - El ancient debris del Nether se convierte en netherrack al cargarse cada chunk.
 *  - Los objetos de netherita "en bruto" (ancient debris, scrap, lingotes, bloques y plantillas de mejora)
 *    se quitan de TODAS las tablas de loot al generarse (cofres de bastiones, etc.).
 * La unica netherita es la armadura que sueltan los mobs del dia 25.
 */
public final class NetheriteLock {
    private record ChunkTask(ServerLevel level, LevelChunk chunk) {}

    private static final ArrayDeque<ChunkTask> TASKS = new ArrayDeque<>();

    private NetheriteLock() {}

    public static void register() {
        // Quita los objetos de netherita en bruto de cualquier loot generado (cofres, etc.)
        LootTableEvents.MODIFY_DROPS.register((table, context, drops) ->
                drops.removeIf(stack -> stack.is(Items.ANCIENT_DEBRIS)
                        || stack.is(Items.NETHERITE_SCRAP)
                        || stack.is(Items.NETHERITE_INGOT)
                        || stack.is(Items.NETHERITE_BLOCK)
                        || stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)));

        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            if (Level.NETHER.equals(level.dimension())) TASKS.add(new ChunkTask(level, chunk));
        });
    }

    /** Cada tick: procesa unos pocos chunks para no provocar lag. */
    public static void tickChunks() {
        int budget = 3;
        ChunkTask t;
        while (budget-- > 0 && (t = TASKS.poll()) != null) {
            scrub(t.level(), t.chunk());
        }
    }

    private static void scrub(ServerLevel level, LevelChunk chunk) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        if (!level.hasChunkAt(new BlockPos(minX, 64, minZ))) return; // se descargo mientras esperaba
        BlockState replacement = Blocks.NETHERRACK.defaultBlockState();
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(s -> s.is(Blocks.ANCIENT_DEBRIS))) continue;
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (sec.getBlockState(x, y, z).is(Blocks.ANCIENT_DEBRIS)) {
                            level.setBlock(new BlockPos(minX + x, baseY + y, minZ + z), replacement, 2);
                        }
                    }
                }
            }
        }
    }
}
