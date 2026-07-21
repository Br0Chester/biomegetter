package com.idk.biomegetter.datagen;

import com.idk.biomegetter.block.ModBlocks;
import com.idk.biomegetter.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecepiesProvider extends FabricRecipeProvider {
    public ModRecepiesProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            @Override
            public void buildRecipes() {
                // Add your recipes here
                List<ItemLike> BIOME_SMELTABLE = List.of(ModItems.BIOMES_STICK, ModBlocks.BIOME_ORE,
                        ModBlocks.DEEPSLATE_BIOME_ORE);
                oreSmelting(BIOME_SMELTABLE, RecipeCategory.MISC, CookingBookCategory.BLOCKS, ModBlocks.BIOME_BLOCK, 0.25f, 200, "biomes_smelting");
                oreBlasting(BIOME_SMELTABLE, RecipeCategory.MISC, CookingBookCategory.BLOCKS, ModBlocks.BIOME_BLOCK, 0.25f, 100, "biomes_smelting");

                nineBlockStorageRecipes(RecipeCategory.MISC, ModItems.BIOMES_STICK_2, RecipeCategory.BUILDING_BLOCKS, ModBlocks.BIOME_BLOCK);

                //  Генерация рецепта с формой
                shaped(RecipeCategory.MISC, ModBlocks.RAW_BIOME_BLOCK)
                        .pattern("RR")
                        .pattern("RR")
                        .define('R', ModItems.BIOMES_STICK_2)
                        .unlockedBy(getHasName(ModItems.BIOMES_STICK_2), has(ModItems.BIOMES_STICK_2))
                        .group("biomes_crafting")
                        .save(output);

                //  Если для крафта нужно просто положить 1 предмет
                shapeless(RecipeCategory.MISC, ModItems.BIOMES_STICK_2, 4)
                        .requires(ModBlocks.RAW_BIOME_BLOCK)
                        .unlockedBy(getHasName(ModBlocks.RAW_BIOME_BLOCK), has(ModBlocks.RAW_BIOME_BLOCK))
                        .group("biomes_crafting")
//                        .save(output);
                        //  Если выход у рецепта повторяется дважды, нужно дописывать id
                        .save(output, "way_to_get_wtf");
            }
        };
    }

    @Override
    public String getName() {
        return "Biomes mod recepies";
    }
}
