package com.idk.biomegetter.entity.client.renderer;

import com.idk.biomegetter.entity.custom.projectile.UnicornBoltEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Временно без собственной модели — снаряд виден только по частицам,
 * которые оставляет за собой в полёте. Полноценный визуал добавим позже.
 * Важно: рендерер всё равно обязателен, иначе краш (мы это уже проходили).
 */
public class UnicornBoltRenderer extends EntityRenderer<UnicornBoltEntity, EntityRenderState> {

    public UnicornBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}