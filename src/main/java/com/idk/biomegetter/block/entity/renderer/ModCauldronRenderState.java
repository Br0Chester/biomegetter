package com.idk.biomegetter.block.entity.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Рендерим только ВЕРХНИЙ слой жидкостного стека и верхний элемент твёрдого стека —
 * по договорённости, полноценный рендер всех разнородных слоёв отложен на будущее.
 */
public class ModCauldronRenderState extends BlockEntityRenderState {
    public float liquidHeight;
    public int liquidTint = -1;
    @Nullable
    public Identifier liquidTexture;

    public float solidHeight;
    public int solidTint = -1;
    @Nullable
    public Identifier solidTexture;

    public int lightCoords;

    public float overlayHeight;
    public int overlayTint = -1;
    @Nullable
    public Identifier overlayTexture;
}