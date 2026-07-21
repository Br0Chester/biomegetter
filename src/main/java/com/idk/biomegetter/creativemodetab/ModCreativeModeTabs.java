package com.idk.biomegetter.creativemodetab;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.block.ModBlocks;
import com.idk.biomegetter.item.ModItems;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTabs {

    //  biomegetter_items, видимо, нужен просто для инииализации ID в потоке
    public static final CreativeModeTab BIOMEGETTER_ITEM_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "biomegetter_items"),
            FabricCreativeModeTab.builder().icon(() -> new ItemStack(ModItems.BIOMES_STICK))
                    .title(Component.translatable("creativemodetab.biomegetter.biomes_stick"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BIOMES_STICK);
                        output.accept(ModItems.BIOMES_STICK_2);
                        output.accept(ModItems.WAND);
                    })
                    .build());

    public static final CreativeModeTab BIOMEGETTER_BLOCK_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "biomegetter_blocks"),
            FabricCreativeModeTab.builder().icon(() -> new ItemStack(ModItems.BIOMES_STICK_2))
                    .title(Component.translatable("creativemodetab.biomegetter.biomes_blocks"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.BIOME_BLOCK);
                        output.accept(ModBlocks.RAW_BIOME_BLOCK);
                        output.accept(ModBlocks.BIOME_ORE);
                        output.accept(ModBlocks.DEEPSLATE_BIOME_ORE);
                    })
                    .build());

    public static void registerModCreativeModeTabs() {
        BiomeGetter.LOGGER.info("Registering Mod Creative Mode Tabs for " + BiomeGetter.MOD_ID);

    }
}
