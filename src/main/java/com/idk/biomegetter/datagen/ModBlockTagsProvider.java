package com.idk.biomegetter.datagen;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.block.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends FabricTagsProvider.BlockTagsProvider {

    public ModBlockTagsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookupFuture) {
        super(output, registryLookupFuture);
    }

    public static void registryModBlockTagsProvider() {
        BiomeGetter.LOGGER.info("Registered Mod Blocks Tags for " + BiomeGetter.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        valueLookupBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.BIOME_BLOCK)
                .add(ModBlocks.BIOME_ORE)
                .add(ModBlocks.RAW_BIOME_BLOCK)
                .add(ModBlocks.DEEPSLATE_BIOME_ORE);

        valueLookupBuilder(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.BIOME_ORE)
                .add(ModBlocks.DEEPSLATE_BIOME_ORE);
    }
}
