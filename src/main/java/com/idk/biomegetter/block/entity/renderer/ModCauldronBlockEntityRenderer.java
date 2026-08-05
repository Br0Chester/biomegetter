package com.idk.biomegetter.block.entity.renderer;

import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Пустой BER: визуал котла рисуется обычной блочной моделью (blockstate).
 * Нужен только чтобы у типа BE не был null renderer (краш в 26.x).
 */
public class ModCauldronBlockEntityRenderer implements BlockEntityRenderer<ModCauldronBlockEntity, BlockEntityRenderState> {

    public ModCauldronBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }

    @Override
    public void extractRenderState(
            ModCauldronBlockEntity blockEntity,
            BlockEntityRenderState state,
            float tickProgress,
            Vec3 cameraPos,
            @Nullable CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
    }

    @Override
    public void submit(
            BlockEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
    }
}
