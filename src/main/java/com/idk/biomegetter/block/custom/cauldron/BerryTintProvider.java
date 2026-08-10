package com.idk.biomegetter.block.custom.cauldron;

import com.idk.biomegetter.block.custom.ModCauldronBlock;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Set;

/**
 * Временный плейсхолдер-тинт для ягод/сока из ягод — пока нет реальных текстур.
 */
public class BerryTintProvider implements BlockTintSource {

    @Override
    public int color(BlockState state) {
        return 0xFFB0202A; // тот же красный плейсхолдер, что и у сока
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(ModCauldronBlock.BERRY_LEVEL);
    }
}