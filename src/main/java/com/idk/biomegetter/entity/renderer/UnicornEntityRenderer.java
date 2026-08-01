package com.idk.biomegetter.entity.renderer;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.entity.client.BabyUnicornEntityModel;
import com.idk.biomegetter.entity.client.ModEntityModelLayers;
import com.idk.biomegetter.entity.client.UnicornEntityModel;
import com.idk.biomegetter.entity.custom.UnicornEntity;
import com.idk.biomegetter.entity.state.UnicornEntityRendererState;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public class UnicornEntityRenderer extends AgeableMobRenderer<UnicornEntity, UnicornEntityRendererState, UnicornEntityModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "textures/entity/unicorn.png");

    public UnicornEntityRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new UnicornEntityModel(context.bakeLayer(ModEntityModelLayers.UNICORN)),
                new BabyUnicornEntityModel(context.bakeLayer(ModEntityModelLayers.UNICORN_BABY)),
                0.375f
        );
    }

    @Override
    public UnicornEntityRendererState createRenderState() {
        return new UnicornEntityRendererState();
    }

    @Override
    public Identifier getTextureLocation(UnicornEntityRendererState state) {
        return TEXTURE;
    }

    @Override
    public void extractRenderState(UnicornEntity entity, UnicornEntityRendererState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.idleAnimationState.copyFrom(entity.idleAnimationState);
        state.walkAnimationState.copyFrom(entity.walkAnimationState);
    }
}