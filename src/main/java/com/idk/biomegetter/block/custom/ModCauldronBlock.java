package com.idk.biomegetter.block.custom;

import com.idk.biomegetter.block.ModBlockEntities;
import com.idk.biomegetter.block.custom.cauldron.CauldronContentType;
import com.idk.biomegetter.block.custom.cauldron.CauldronContentTypes;
import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import com.idk.biomegetter.block.entity.ModCauldronBlockEntity.Content;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Единственный класс нашего котла. Тип содержимого — значение блокстейта CONTENT,
 * а не отдельный Java-класс. Поведение каждого типа жидкости описано таблицей
 * в CauldronContentTypes — добавление новой жидкости не требует нового класса.
 * Полностью независим от ванильных CauldronInteraction/LayeredCauldronBlock —
 * никогда не подменяется на ванильный блок.
 */
public class ModCauldronBlock extends AbstractCauldronBlock implements EntityBlock {

    public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);

    private static final VoxelShape[] FILLED_SHAPES = Util.make(() -> Block.boxes(
            2, level -> Shapes.or(AbstractCauldronBlock.SHAPE, Block.column(12.0, 4.0, 6.0 + (level + 1) * 3.0))
    ));

    private static final MapCodec<ModCauldronBlock> CODEC = simpleCodec(ModCauldronBlock::new);

    public ModCauldronBlock(Properties properties) {
        super(properties, new CauldronInteraction.Dispatcher()); // не используется — весь интеракт свой, ниже
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.LEVEL_CAULDRON, 1)
                .setValue(CONTENT, Content.EMPTY));
    }

    @Override
    public MapCodec<ModCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.LEVEL_CAULDRON, CONTENT);
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(CONTENT) != Content.EMPTY && state.getValue(BlockStateProperties.LEVEL_CAULDRON) == 3;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        if (state.getValue(CONTENT) == Content.EMPTY) {
            return 0.0;
        }
        return (6.0 + state.getValue(BlockStateProperties.LEVEL_CAULDRON) * 3.0) / 16.0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AbstractCauldronBlock.SHAPE;
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        if (state.getValue(CONTENT) == Content.EMPTY) {
            return Shapes.empty();
        }
        return FILLED_SHAPES[state.getValue(BlockStateProperties.LEVEL_CAULDRON) - 1];
    }

    // ---- Взаимодействия: полностью свои, без единого обращения к ванильным CauldronInteraction ----

    @Override
    protected InteractionResult useItemOn(
            ItemStack itemStack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        Item item = itemStack.getItem();
        Content content = state.getValue(CONTENT);

        // 1. Пустое ведро — забрать содержимое
        if (item == Items.BUCKET && content != Content.EMPTY) {
            CauldronContentType type = CauldronContentTypes.get(content);
            // Общее правило для ЛЮБОЙ жидкости: ведро = ровно 3/3, забрать его можно только из полного котла
            if (type.fillBucket() != null && state.getValue(BlockStateProperties.LEVEL_CAULDRON) == 3) {
                if (!level.isClientSide()) {
                    level.setBlockAndUpdate(pos, state.setValue(CONTENT, Content.EMPTY).setValue(BlockStateProperties.LEVEL_CAULDRON, 1));
                    level.playSound(null, pos, type.emptySound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    swapItem(player, hand, itemStack, new ItemStack(type.fillBucket()));
                }
                return InteractionResult.SUCCESS;
            }
        }

        // 2. Наполнение подходящим ведром
        for (CauldronContentType type : CauldronContentTypes.all()) {
            if (type.fillBucket() == item && (content == Content.EMPTY || content == type.id())) {
                if (!level.isClientSide()) {
                    level.setBlockAndUpdate(pos, state.setValue(CONTENT, type.id()).setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
                    level.playSound(null, pos, type.fillSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    swapItem(player, hand, itemStack, new ItemStack(Items.BUCKET));
                }
                return InteractionResult.SUCCESS;
            }
        }

        // 3. Бутылка — только вода, уменьшает уровень (своя логика, не lowerFillLevel!)
        if (item == Items.GLASS_BOTTLE && content == Content.WATER) {
            if (!level.isClientSide()) {
                lowerOrEmpty(level, pos, state);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                swapItem(player, hand, itemStack, PotionContents.createItemStack(Items.POTION, Potions.WATER));
            }
            return InteractionResult.SUCCESS;
        }

        // 4. Снятие красителя с кожаных вещей — только вода
        if (content == Content.WATER && itemStack.get(DataComponents.DYED_COLOR) != null) {
            if (!level.isClientSide()) {
                itemStack.remove(DataComponents.DYED_COLOR);
                lowerOrEmpty(level, pos, state);
                level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /**
     * Уменьшает уровень на 1, либо переводит в EMPTY, если был последний уровень. Наша замена lowerFillLevel.
     */
    private static void lowerOrEmpty(Level level, BlockPos pos, BlockState state) {
        int currentLevel = state.getValue(BlockStateProperties.LEVEL_CAULDRON);
        if (currentLevel <= 1) {
            level.setBlockAndUpdate(pos, state.setValue(CONTENT, Content.EMPTY).setValue(BlockStateProperties.LEVEL_CAULDRON, 1));
        } else {
            level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LEVEL_CAULDRON, currentLevel - 1));
        }
    }

    private static void swapItem(Player player, InteractionHand hand, ItemStack original, ItemStack result) {
        if (!player.getAbilities().instabuild) {
            original.shrink(1);
            if (original.isEmpty()) {
                player.setItemInHand(hand, result);
            } else if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }
    }

    // ---- Кипение/урон/партиклы — читаются из таблицы, не по switch ----

    @Override
    protected void entityInside(
            BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise
    ) {
        Content content = state.getValue(CONTENT);
        if (content == Content.EMPTY) {
            return;
        }
        CauldronContentType type = CauldronContentTypes.get(content);
        if (!type.damagesEntities() || !(level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron)) {
            return;
        }
        boolean shouldDamage = !type.requiresHeatToDamage() || cauldron.isHeatedBelow();
        if (shouldDamage && level instanceof ServerLevel serverLevel) {
            entity.hurtServer(serverLevel, level.damageSources().hotFloor(), type.damageAmount());
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Content content = state.getValue(CONTENT);
        if (content == Content.EMPTY) {
            return;
        }
        CauldronContentType type = CauldronContentTypes.get(content);
        if (level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron
                && (!type.requiresHeatToDamage() || cauldron.isHeatedBelow())) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.9;
            double z = pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.BUBBLE_POP, x + random.nextDouble() * 0.6 - 0.3, y, z + random.nextDouble() * 0.6 - 0.3, 0.0, 0.05, 0.0);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron) {
            BlockState above = level.getBlockState(pos.above());
            cauldron.setHasBlockAbove(!above.isAir());
            cauldron.setPowderSnowAbove(above.is(Blocks.POWDER_SNOW));
            cauldron.setHeatedBelow(isHeatSource(level.getBlockState(pos.below())));
        }
    }

    private static boolean isHeatSource(BlockState below) {
        return below.is(BlockTags.FIRE)
                || below.is(Blocks.LAVA)
                || below.is(Blocks.MAGMA_BLOCK)
                || (below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof ModCauldronBlockEntity cauldron) {
                serverTick(lvl, pos, st, cauldron);
            }
        };
    }

    private static void serverTick(Level level, BlockPos pos, BlockState state, ModCauldronBlockEntity cauldron) {
        // Таяние рыхлого снега сверху — независимо от того, что внутри котла
        if (cauldron.isHeatedBelow() && cauldron.isPowderSnowAbove() && cauldron.tickMelt()) {
            level.setBlockAndUpdate(pos.above(), Blocks.WATER.defaultBlockState());
        }

        Content content = state.getValue(CONTENT);
        if (content == Content.EMPTY) {
            return;
        }
        CauldronContentType type = CauldronContentTypes.get(content);
        // Таяние содержимого котла (например, рыхлого снега) при нагреве — не зависит от того, накрыт ли котёл сверху
        if (type.meltsIntoWhenHeated() != null && cauldron.isHeatedBelow() && cauldron.tickEvaporation()) {
            level.setBlockAndUpdate(pos, state.setValue(CONTENT, type.meltsIntoWhenHeated()));
            return;
        }

        if (!type.evaporates() || !cauldron.isHeatedBelow() || cauldron.hasBlockAbove()) {
            return;
        }
        if (cauldron.tickEvaporation()) {
            lowerOrEmpty(level, pos, state);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModCauldronBlockEntity(ModBlockEntities.CAULDRON, pos, state);
    }
}