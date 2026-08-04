package com.idk.biomegetter.block;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static final BlockEntityType<ModCauldronBlockEntity> CAULDRON = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "cauldron"),
            FabricBlockEntityTypeBuilder.create((pos, state) -> new ModCauldronBlockEntity(ModBlockEntities.CAULDRON, pos, state), ModBlocks.WATER_CAULDRON).build()
    );

    public static void registerModBlockEntities() {
        BiomeGetter.LOGGER.info("Registered Mod Block Entities for " + BiomeGetter.MOD_ID);
    }
}