package com.idk.biomegetter.datagen;

import com.idk.biomegetter.block.ModBlocks;
import com.idk.biomegetter.item.ModItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

public class ModModelProvider extends FabricModelProvider {

    public ModModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockModelGenerators) {
        blockModelGenerators.createTrivialCube(ModBlocks.BIOME_BLOCK);
        blockModelGenerators.createTrivialCube(ModBlocks.RAW_BIOME_BLOCK);
        blockModelGenerators.createTrivialCube(ModBlocks.BIOME_ORE);
        blockModelGenerators.createTrivialCube(ModBlocks.DEEPSLATE_BIOME_ORE);
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerators) {
        itemModelGenerators.generateFlatItem(ModItems.BIOMES_STICK, ModelTemplates.FLAT_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.BIOMES_STICK_2, ModelTemplates.FLAT_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.WAND, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.UNICORN_SPAWN_EGG, ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModelGenerators.generateFlatItem(ModItems.MEAL, ModelTemplates.FLAT_ITEM);
    }
}
