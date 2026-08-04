package com.idk.biomegetter;

import com.idk.biomegetter.block.ModBlockEntities;
import com.idk.biomegetter.block.ModBlocks;
import com.idk.biomegetter.creativemodetab.ModCreativeModeTabs;
import com.idk.biomegetter.datagen.ModBlockTagsProvider;
import com.idk.biomegetter.entity.ModEntities;
import com.idk.biomegetter.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BiomeGetter implements ModInitializer {
    public static final String MOD_ID = "biomegetter";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerModBlockEntities();
        ModBlockTagsProvider.registryModBlockTagsProvider();
        ModCreativeModeTabs.registerModCreativeModeTabs();
        ModEntities.registerModEntityTypes();
        ModEntities.registerAttributes();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
