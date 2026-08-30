package com.idk.biomegetter.skill;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Тонкая обёртка над TemporaryBlockSavedData — персистентное (переживает рестарт сервера)
 * хранилище отложенных откатов блоков (transform_block и подобные эффекты).
 */
public final class TemporaryBlockTracker {

    private TemporaryBlockTracker() {
    }

    public static void schedule(ServerLevel level, BlockPos pos, BlockState originalState, int durationTicks) {
        var data = TemporaryBlockSavedData.get(level);
//        BiomeGetter.LOGGER.info("Scheduled block revert: pos={}, data instance hash={}", pos, System.identityHashCode(data));
        data.add(new TemporaryBlockSavedData.Entry(pos, originalState, durationTicks));
    }

    public static void register() {

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                TemporaryBlockSavedData data = TemporaryBlockSavedData.get(level);
//                BiomeGetter.LOGGER.info("TemporaryBlockTracker \\ Tick check: data instance hash={}, entries={}", System.identityHashCode(data), data.entries().size());
                if (data.entries().isEmpty()) continue;

                List<TemporaryBlockSavedData.Entry> remaining = new ArrayList<>();
                boolean changed = false;
                for (TemporaryBlockSavedData.Entry entry : data.entries()) {
                    if (entry.ticksLeft() <= 1) {
                        level.setBlockAndUpdate(entry.pos(), entry.originalState());
                        changed = true;
                    } else {
                        remaining.add(new TemporaryBlockSavedData.Entry(entry.pos(), entry.originalState(), entry.ticksLeft() - 1));
                    }
                }
                if (changed || remaining.size() != data.entries().size()) {
                    data.replaceAll(remaining);
                }
            }
        });
    }
}