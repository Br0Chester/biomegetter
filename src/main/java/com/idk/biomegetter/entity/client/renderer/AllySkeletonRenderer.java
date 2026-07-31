package com.idk.biomegetter.entity.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;

public class AllySkeletonRenderer extends SkeletonRenderer {

    private static final int GHOST_TINT = ARGB.color(140, 140, 165, 255);

    public AllySkeletonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected int getModelTint(SkeletonRenderState state) {
        return GHOST_TINT;
    }

    @Override
    protected RenderType getRenderType(SkeletonRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        return RenderTypes.entityTranslucentCullItemTarget(this.getTextureLocation(state));
    }
}