package com.idk.biomegetter;

import com.idk.biomegetter.block.ModBlockEntities;
import com.idk.biomegetter.block.entity.renderer.ModCauldronBlockEntityRenderer;
import com.idk.biomegetter.entity.ModEntities;
import com.idk.biomegetter.entity.client.ModEntityModelLayers;
import com.idk.biomegetter.entity.client.renderer.AllySkeletonRenderer;
import com.idk.biomegetter.entity.client.renderer.AllyZombieRenderer;
import com.idk.biomegetter.entity.client.renderer.UnicornBoltRenderer;
import com.idk.biomegetter.entity.renderer.UnicornEntityRenderer;
import com.idk.biomegetter.fluid.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;

public class BiomeGetterCustomEntityClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModEntityModelLayers.registerModelLayers();
        EntityRenderers.register(ModEntities.UNICORN, UnicornEntityRenderer::new);

        // Временно используем ванильные рендереры без изменений —
        // это чинит краш (renderer == null). Тинт/прозрачность добавим
        // отдельным шагом, когда будем делать кастомный рендерер.
        EntityRenderers.register(ModEntities.ALLY_ZOMBIE, AllyZombieRenderer::new);
        EntityRenderers.register(ModEntities.ALLY_SKELETON, AllySkeletonRenderer::new);
        EntityRenderers.register(ModEntities.ALLY_WITHER_SKELETON, WitherSkeletonRenderer::new);
        EntityRenderers.register(ModEntities.UNICORN_BOLT, UnicornBoltRenderer::new);

        BlockEntityRenderers.register(ModBlockEntities.CAULDRON, ModCauldronBlockEntityRenderer::new);

        registerFluidRender(ModFluids.ACID);
        registerFluidRender(ModFluids.GEYSER_GAS);
    }

    private static void registerFluidRender(com.idk.biomegetter.fluid.ModFluids.FluidEntry entry) {
        com.idk.biomegetter.fluid.FluidConfig cfg = com.idk.biomegetter.fluid.ModFluidConfigLoader.get(entry.id());
        net.minecraft.resources.Identifier stillTexture = cfg != null ? cfg.textureStill()
                : net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "block/water_still");
        net.minecraft.resources.Identifier flowingTexture = cfg != null ? cfg.textureFlowing()
                : net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "block/water_flow");
        int tint = cfg != null ? cfg.tintColor().orElse(0xFFFFFFFF) : 0xFFFFFFFF;

        net.minecraft.client.resources.model.sprite.Material stillMaterial =
                new net.minecraft.client.resources.model.sprite.Material(stillTexture);
        net.minecraft.client.resources.model.sprite.Material flowingMaterial =
                new net.minecraft.client.resources.model.sprite.Material(flowingTexture);

        net.minecraft.client.color.block.BlockTintSource tintSource = state -> {
            com.idk.biomegetter.fluid.FluidConfig liveCfg = com.idk.biomegetter.fluid.ModFluidConfigLoader.get(entry.id());
            return liveCfg != null ? liveCfg.tintColor().orElse(0xFFFFFFFF) : 0xFFFFFFFF;
        };

        net.minecraft.client.renderer.block.FluidModel.Unbaked model =
                new net.minecraft.client.renderer.block.FluidModel.Unbaked(stillMaterial, flowingMaterial, null, tintSource);

        net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(
                entry.still(), entry.flowing(), model
        );
    }


}