package com.idk.biomegetter.block;

import com.idk.biomegetter.BiomeGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class ModBlocks {

    //  По аналогии с Items происходит регистрация
    //  Однако здесь мы развёрнуто прописываем properties
    public static final Block BIOME_BLOCK = registerBlock("biome_block",
            properties -> new Block(properties.strength(4f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS)));

    public static final Block RAW_BIOME_BLOCK = registerBlock("raw_biome_block",
            properties -> new Block(properties.strength(3f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.GRASS)));

    public static final Block BIOME_ORE = registerBlock("biome_ore",
            properties -> new DropExperienceBlock(UniformInt.of(2, 5),
                    properties.strength(3f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.GILDED_BLACKSTONE)));

    public static final Block DEEPSLATE_BIOME_ORE = registerBlock("deepslate_biome_ore",
            properties -> new DropExperienceBlock(UniformInt.of(4, 5),
                    properties.strength(4f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.BAMBOO)));

    private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> function) {
        Block toRegister = function.apply(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name))));
        registerBlockItem(name, toRegister);
        return Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name), toRegister);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name),
                new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix()
                        .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name)))));
    }
    
    public static final Block WATER_CAULDRON = registerBlock("water_cauldron",
            properties -> new com.idk.biomegetter.block.custom.ModWaterCauldronBlock(properties
                    .strength(2f)
                    .sound(SoundType.LANTERN)
                    .noOcclusion()));

    public static void registerModBlocks() {
        BiomeGetter.LOGGER.info("Registered Mod Blocks for " + BiomeGetter.MOD_ID);
    }
}
