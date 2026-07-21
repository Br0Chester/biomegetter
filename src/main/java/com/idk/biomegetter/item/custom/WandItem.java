package com.idk.biomegetter.item.custom;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

public class WandItem extends Item {
    public static final Map<Block, Block> CHISEL_MAP =
            Map.of(
                    Blocks.STONE, Blocks.STONE_BRICKS,
                    Blocks.CLAY, Blocks.GRAY_TERRACOTTA

            );

    public WandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        //  RCB
        //  Change Block from A to B

        Level level = context.getLevel();
        Block clickedBlock = level.getBlockState(context.getClickedPos()).getBlock();

        Player player = context.getPlayer();
        Holder<Biome> biomeHolder = level.getBiome(context.getClickedPos());
        if (player != null && !level.isClientSide()) {
            // Получаем название биома
            String biomeName = biomeHolder.getRegisteredName();

            // Красивое сообщение
            player.sendSystemMessage(
                    Component.literal("§aБиом: §f" + biomeName)
            );
        }

        if (CHISEL_MAP.containsKey(clickedBlock) && !level.isClientSide()) {
            //Server side
            level.setBlockAndUpdate(context.getClickedPos(), CHISEL_MAP.get(clickedBlock).defaultBlockState());
            context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getHand());
        }

        return InteractionResult.SUCCESS;
    }
}
