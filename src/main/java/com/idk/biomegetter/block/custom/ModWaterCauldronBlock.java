package com.idk.biomegetter.block.custom;

import com.idk.biomegetter.block.ModBlockEntities;
import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * Наш аналог водяного котла: тот же принцип LEVEL (1-3), что и в ванили,
 * но с BlockEntity для кипения/испарения/памяти об окружении.
 */
public class ModWaterCauldronBlock extends AbstractModCauldronBlock {

    public ModWaterCauldronBlock(Properties properties) {
        // Biome.Precipitation.RAIN + CauldronInteraction.WATER — переиспользуем ровно те же
        // ванильные правила заполнения дождём и ванильные реакции на вёдра/предметы,
        // что и у обычного водяного котла. Своё поведение (кипение/испарение) добавляется поверх.
        super(properties, Biome.Precipitation.RAIN, CauldronInteractions.WATER);
    }

    @Override
    protected boolean isBoilable(BlockState state) {
        return true;
    }

    @Override
    protected void onEvaporationTick(Level level, BlockPos pos, BlockState state, ModCauldronBlockEntity blockEntity) {
        int currentLevel = state.getValue(BlockStateProperties.LEVEL_CAULDRON);
        if (currentLevel <= 1) {
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.CAULDRON.defaultBlockState());
        } else {
            level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LEVEL_CAULDRON, currentLevel - 1));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModCauldronBlockEntity(ModBlockEntities.CAULDRON, pos, state);
    }
}