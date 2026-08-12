package com.idk.biomegetter.block.entity.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class ModCauldronRenderState extends BlockEntityRenderState {
    public float baseHeight;      // 0..1, содержимое из старой системы (вода/лава/молоко/снег)
    public int baseTint = -1;
    @Nullable
    public Identifier baseTexture;

    public float juiceHeight;     // 0..1
    public int juiceTint = -1;
    @Nullable
    public Identifier juiceTexture;

    public float solidHeight;     // 0..1
    public int solidTint = -1;
    @Nullable
    public Identifier solidTexture;

    public int lightCoords;
}