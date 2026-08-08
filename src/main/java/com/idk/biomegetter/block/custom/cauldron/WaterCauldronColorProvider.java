package com.idk.biomegetter.block.custom.cauldron;

import com.idk.biomegetter.block.custom.ModCauldronBlock;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class WaterCauldronColorProvider extends BlockColors {

    @Override
    public int getColoringProperties(BlockState state, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos, int tintIndex) {
        CauldronContentType type = CauldronContentTypes.get(state.getValue(ModCauldronBlock.CONTENT));

        if (type.tintColor() == -1) {
            return world != null && pos != null ? BiomeColors.getAverageWaterColor(world, pos) : 0x3F76E4;
        }

        return type.tintColor();
    }
}