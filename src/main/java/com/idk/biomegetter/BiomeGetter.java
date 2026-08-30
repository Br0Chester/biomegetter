package com.idk.biomegetter;

import com.idk.biomegetter.block.ModBlockEntities;
import com.idk.biomegetter.block.ModBlocks;
import com.idk.biomegetter.block.custom.cauldron.data.*;
import com.idk.biomegetter.creativemodetab.ModCreativeModeTabs;
import com.idk.biomegetter.datagen.ModBlockTagsProvider;
import com.idk.biomegetter.entity.ModEntities;
import com.idk.biomegetter.fluid.ModFluidConfigLoader;
import com.idk.biomegetter.fluid.ModFluids;
import com.idk.biomegetter.item.ModItems;
import com.idk.biomegetter.skill.BuiltinSkillEffects;
import com.idk.biomegetter.skill.PlayerPassiveBuffCoordinator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
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

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemPassiveSkillLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemActiveSkillLoader());
        BuiltinSkillEffects.registerAll();
        PlayerPassiveBuffCoordinator.register();
        com.idk.biomegetter.skill.TemporaryBlockTracker.register();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new com.idk.biomegetter.block.custom.cauldron.data.totem.TotemProcessLoader());

//        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronJuiceTypeLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronLiquidReactionLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronSolidReactionLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronLiquidComponentLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronSolidComponentLoader());

        ModFluids.registerModFluids();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new ModFluidConfigLoader());

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronRecipeLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronSoupSpiceLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronIngredientEffectLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronSoupProcessLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronSoupCategoryLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronSoupIngredientLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronUpgradeBlacklistLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CauldronRitualTemplateLoader());

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new com.idk.biomegetter.block.custom.cauldron.data.totem.ElementMatrixLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemSolidLevelLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemLiquidLevelLoader());

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemStartRingLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemStatRingLoader());
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new com.idk.biomegetter.block.custom.cauldron.data.totem.BlockElementLoader());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
