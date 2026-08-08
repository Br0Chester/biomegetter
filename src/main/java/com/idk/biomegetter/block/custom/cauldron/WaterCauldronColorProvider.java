package com.idk.biomegetter.block.custom.cauldron;

import com.idk.biomegetter.block.custom.ModCauldronBlock;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Set;

/**
 * Один тинт-провайдер на все типы содержимого нашего котла:
 * читает Content из блокстейта и берёт нужный цвет из CauldronContentTypes.
 * Для воды (tintColor == -1) — берёт настоящий биомный цвет, как у ванильной воды.
 */
public class WaterCauldronColorProvider implements BlockTintSource {

    @Override
    public int color(BlockState state) {
        CauldronContentType type = CauldronContentTypes.get(state.getValue(ModCauldronBlock.CONTENT));
        return type.tintColor() == -1 ? 0x3F76E4 : type.tintColor(); // без доступа к миру биомный цвет недоступен — берём стандартный синий
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        CauldronContentType type = CauldronContentTypes.get(state.getValue(ModCauldronBlock.CONTENT));
        if (type.tintColor() == -1) {
            return BiomeColors.getAverageWaterColor(level, pos);
        }
        return type.tintColor();
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Set.of(ModCauldronBlock.CONTENT);
    }
}