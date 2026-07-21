package com.idk.biomegetter.datagen;

import com.idk.biomegetter.block.ModBlocks;
import com.idk.biomegetter.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootSubProvider {
    public ModLootTableProvider(FabricPackOutput packOutput, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(packOutput, registriesFuture);
    }

    @Override
    public void generate() {
        dropSelf(ModBlocks.BIOME_BLOCK);
        dropSelf(ModBlocks.RAW_BIOME_BLOCK);

        add(ModBlocks.BIOME_ORE, createOreDrop(ModBlocks.BIOME_ORE, ModItems.BIOMES_STICK));
        add(ModBlocks.DEEPSLATE_BIOME_ORE, createMultipleOreDrops(ModBlocks.DEEPSLATE_BIOME_ORE, ModItems.BIOMES_STICK_2, 1, 8));
    }


    //  Взято из недр майнкрафт кода
    public LootTable.Builder createMultipleOreDrops(final Block block, Item item, float minDrops, float maxDrop) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(block, this.applyExplosionDecay(
                block, LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrop)))
                        .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE))))
        );
    }
}
