package com.idk.biomegetter;

import com.idk.biomegetter.entity.ModEntities;
import com.idk.biomegetter.entity.client.ModEntityModelLayers;
import com.idk.biomegetter.entity.renderer.UnicornEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class BiomeGetterCustomEntityClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModEntityModelLayers.registerModelLayers();
        EntityRenderers.register(ModEntities.UNICORN, UnicornEntityRenderer::new);
    }
}