package com.idk.biomegetter.block.custom;

import com.idk.biomegetter.block.ModBlockEntities;
import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;


/**
 * Наш аналог водяного котла: тот же принцип LEVEL (1-3), что и в ванили,
 * но с BlockEntity для кипения/испарения/памяти об окружении.
 */
public class ModWaterCauldronBlock extends AbstractModCauldronBlock {

    public ModWaterCauldronBlock(Properties properties) {
        super(properties, Biome.Precipitation.RAIN, new CauldronInteraction.Dispatcher());
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack itemStack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        Item item = itemStack.getItem();
        ModCauldronBlockEntity.Content content = cauldron.getContent();

        if (item == Items.WATER_BUCKET && (content == ModCauldronBlockEntity.Content.EMPTY || content == ModCauldronBlockEntity.Content.WATER)) {
            return fillWith(cauldron, ModCauldronBlockEntity.Content.WATER, state, level, pos, player, hand, itemStack);
        }
        if (item == Items.LAVA_BUCKET && content == ModCauldronBlockEntity.Content.EMPTY) {
            return fillWith(cauldron, ModCauldronBlockEntity.Content.LAVA, state, level, pos, player, hand, itemStack);
        }
        if (item == Items.MILK_BUCKET && content == ModCauldronBlockEntity.Content.EMPTY) {
            return fillWith(cauldron, ModCauldronBlockEntity.Content.MILK, state, level, pos, player, hand, itemStack);
        }
        if (item == Items.BUCKET && content != ModCauldronBlockEntity.Content.EMPTY) {
            ItemStack filled = switch (content) {
                case WATER -> new ItemStack(Items.WATER_BUCKET);
                case LAVA -> new ItemStack(Items.LAVA_BUCKET);
                case MILK -> new ItemStack(Items.MILK_BUCKET);
                default -> ItemStack.EMPTY; // сок ведром не забирается — сделаем отдельно
            };
            if (!filled.isEmpty()) {
                if (!level.isClientSide()) {
                    cauldron.setContent(ModCauldronBlockEntity.Content.EMPTY);
                    level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LEVEL_CAULDRON, 1).setValue(EMPTY, true));
                    swapItem(player, hand, itemStack, filled);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (item == Items.GLASS_BOTTLE && content == ModCauldronBlockEntity.Content.WATER) {
            int currentLevel = state.getValue(BlockStateProperties.LEVEL_CAULDRON);
            if (!level.isClientSide()) {
                if (currentLevel <= 1) {
                    cauldron.setContent(ModCauldronBlockEntity.Content.EMPTY);
                    level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LEVEL_CAULDRON, 1).setValue(EMPTY, true));
                } else {
                    level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LEVEL_CAULDRON, currentLevel - 1));
                }
                swapItem(player, hand, itemStack, new ItemStack(Items.POTION));
            }
            return InteractionResult.SUCCESS;
        }

//        ВРОДЕ БЫ ПОФИКСИЛО ТУПИЗМ КОТЛА
//        return InteractionResult.TRY_WITH_EMPTY_HAND;
        return CauldronInteractions.WATER.get(itemStack).interact(state, level, pos, player, hand, itemStack);
    }

    private static InteractionResult fillWith(
            ModCauldronBlockEntity cauldron, ModCauldronBlockEntity.Content newContent,
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack itemInHand
    ) {
        if (!level.isClientSide()) {
            cauldron.setContent(newContent);
            level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LEVEL_CAULDRON, 3).setValue(EMPTY, false));
            swapItem(player, hand, itemInHand, new ItemStack(Items.BUCKET));
        }
        return InteractionResult.SUCCESS;
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

    @Override
    protected boolean isBoilable(BlockState state, ModCauldronBlockEntity blockEntity) {
        return blockEntity.getContent() != ModCauldronBlockEntity.Content.EMPTY;
    }

    @Override
    protected void onEvaporationTick(Level level, BlockPos pos, BlockState state, ModCauldronBlockEntity blockEntity) {
        int currentLevel = state.getValue(BlockStateProperties.LEVEL_CAULDRON);
        if (currentLevel <= 1) {
            blockEntity.setContent(ModCauldronBlockEntity.Content.EMPTY);
            level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LEVEL_CAULDRON, 1).setValue(EMPTY, true));
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