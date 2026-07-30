package com.idk.biomegetter;

import com.idk.biomegetter.entity.ModEntities;
import com.idk.biomegetter.entity.client.ModEntityModelLayers;
import com.idk.biomegetter.entity.renderer.UnicornEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;

public class BiomeGetterCustomEntityClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModEntityModelLayers.registerModelLayers();
        EntityRenderers.register(ModEntities.UNICORN, UnicornEntityRenderer::new);

        // Временно используем ванильные рендереры без изменений —
        // это чинит краш (renderer == null). Тинт/прозрачность добавим
        // отдельным шагом, когда будем делать кастомный рендерер.
        EntityRenderers.register(ModEntities.ALLY_ZOMBIE, ZombieRenderer::new);
        EntityRenderers.register(ModEntities.ALLY_SKELETON, SkeletonRenderer::new);
        EntityRenderers.register(ModEntities.ALLY_WITHER_SKELETON, WitherSkeletonRenderer::new);
    }
}