package com.idk.biomegetter.block.custom;

import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/**
 * Общая база для всех наших котлов с содержимым (вода/молоко/сок/ягоды):
 * отслеживание блока сверху/снизу (по neighborChanged, без сканирования каждый тик),
 * кипение и его последствия (урон, испарение). Конкретное содержимое — в наследниках.
 */
public abstract class AbstractModCauldronBlock extends LayeredCauldronBlock implements EntityBlock {

    protected AbstractModCauldronBlock(
            Properties properties,
            Biome.Precipitation precipitationType,
            CauldronInteraction.Dispatcher interactions
    ) {
        super(precipitationType, interactions, properties);
    }

    /**
     * Наследник решает, "жидкое" ли сейчас содержимое (кипятится/испаряется) — например, пустой котёл не кипит.
     */
    protected abstract boolean isBoilable(BlockState state, ModCauldronBlockEntity blockEntity);

    /**
     * Урон при нахождении в кипящем содержимом — переопределяемо под конкретный тип.
     */
    protected float getBoilingDamage() {
        return 2.0F;
    }

    /**
     * Что происходит, когда испарение "натикало" — обычно уменьшение уровня. Переопределяется наследником.
     */
    protected abstract void onEvaporationTick(Level level, BlockPos pos, BlockState state, ModCauldronBlockEntity blockEntity);

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron) {
            cauldron.setHasBlockAbove(!level.getBlockState(pos.above()).isAir());
            cauldron.setHeatedBelow(isHeatSource(level.getBlockState(pos.below())));
        }
    }

    private static boolean isHeatSource(BlockState below) {
        return below.is(BlockTags.FIRE)
                || below.is(net.minecraft.world.level.block.Blocks.LAVA)
                || below.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                || (below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT));
    }

    @Override
    protected void entityInside(
            BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise
    ) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise); // сохраняем ванильное тушение огня и т.п.

        if (level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron && this.isBoilable(state, cauldron) && cauldron.isHeatedBelow()) {
            if (level instanceof ServerLevel serverLevel) {
                DamageSource source = level.damageSources().hotFloor();
                entity.hurtServer(serverLevel, source, this.getBoilingDamage());
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron && this.isBoilable(state, cauldron) && cauldron.isHeatedBelow()) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.9;
            double z = pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.BUBBLE_POP, x + random.nextDouble() * 0.6 - 0.3, y, z + random.nextDouble() * 0.6 - 0.3, 0.0, 0.05, 0.0);
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.DRIPPING_WATER, x + random.nextDouble() * 0.6 - 0.3, y + 0.2, z + random.nextDouble() * 0.6 - 0.3, 0.0, 0.0, 0.0);
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createServerTicker(type, this::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityTicker<T> createServerTicker(
            BlockEntityType<T> type, CauldronTickCallback callback
    ) {
        return (lvl, pos, st, be) -> callback.tick(lvl, pos, st, (ModCauldronBlockEntity) be);
    }

    private void serverTick(Level level, BlockPos pos, BlockState state, ModCauldronBlockEntity cauldron) {
        if (!this.isBoilable(state, cauldron) || !cauldron.isHeatedBelow() || cauldron.hasBlockAbove()) {
            return; // не кипит либо накрыто сверху — испарение не идёт, лишней работы не делаем
        }
        if (cauldron.tickEvaporation()) {
            this.onEvaporationTick(level, pos, state, cauldron);
        }
    }

    @FunctionalInterface
    private interface CauldronTickCallback {
        void tick(Level level, BlockPos pos, BlockState state, ModCauldronBlockEntity blockEntity);
    }
}