package com.idk.biomegetter.entity.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;

public class AllyZombieRenderer extends ZombieRenderer {

    // Полупрозрачный голубоватый тон: ARGB.color(alpha, r, g, b)
    private static final int GHOST_TINT = ARGB.color(140, 140, 165, 255);

    public AllyZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected int getModelTint(ZombieRenderState state) {
        return GHOST_TINT;
    }

    @Override
    protected RenderType getRenderType(ZombieRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        // Форсируем полупрозрачный рендер-тип, чтобы альфа из getModelTint() реально учитывалась
        return RenderTypes.entityTranslucentCullItemTarget(this.getTextureLocation(state));
    }
}