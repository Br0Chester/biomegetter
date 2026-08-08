package com.idk.biomegetter;

import com.idk.biomegetter.block.ModBlockEntities;
import com.idk.biomegetter.block.ModBlocks;
import com.idk.biomegetter.block.custom.cauldron.WaterCauldronColorProvider;
import com.idk.biomegetter.block.entity.renderer.ModCauldronBlockEntityRenderer;
import com.idk.biomegetter.entity.ModEntities;
import com.idk.biomegetter.entity.client.ModEntityModelLayers;
import com.idk.biomegetter.entity.client.renderer.AllySkeletonRenderer;
import com.idk.biomegetter.entity.client.renderer.AllyZombieRenderer;
import com.idk.biomegetter.entity.client.renderer.UnicornBoltRenderer;
import com.idk.biomegetter.entity.renderer.UnicornEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;

import java.util.List;

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

        BlockColorRegistry.register(
                List.of(new WaterCauldronColorProvider()),
                ModBlocks.WATER_CAULDRON
        );
    }

}