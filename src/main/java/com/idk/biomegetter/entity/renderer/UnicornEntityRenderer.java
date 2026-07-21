package com.idk.biomegetter.entity.renderer;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.entity.client.ModEntityModelLayers;
import com.idk.biomegetter.entity.client.UnicornEntityModel;
import com.idk.biomegetter.entity.custom.UnicornEntity;
import com.idk.biomegetter.entity.state.UnicornEntityRendererState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class UnicornEntityRenderer extends MobRenderer<UnicornEntity, UnicornEntityRendererState, UnicornEntityModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "textures/entity/mini_golem.png");

    public UnicornEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new UnicornEntityModel(context.bakeLayer(ModEntityModelLayers.UNICORN)), 0.375f); // 0.375 shadow radius
    }

    @Override
    public UnicornEntityRendererState createRenderState() {
        return new UnicornEntityRendererState();
    }

    @Override
    public Identifier getTextureLocation(UnicornEntityRendererState state) {
        return TEXTURE;
    }
}