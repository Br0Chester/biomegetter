package com.idk.biomegetter.block.entity.renderer;

import com.idk.biomegetter.block.custom.ModCauldronBlock;
import com.idk.biomegetter.block.custom.cauldron.CauldronContentType;
import com.idk.biomegetter.block.custom.cauldron.CauldronContentTypes;
import com.idk.biomegetter.block.custom.cauldron.data.CauldronJuiceTypeLoader;
import com.idk.biomegetter.block.custom.cauldron.data.JuiceType;
import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import com.idk.biomegetter.block.entity.ModCauldronBlockEntity.Content;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ModCauldronBlockEntityRenderer implements BlockEntityRenderer<ModCauldronBlockEntity, ModCauldronRenderState> {

    public ModCauldronBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public ModCauldronRenderState createRenderState() {
        return new ModCauldronRenderState();
    }

    @Override
    public void extractRenderState(
            ModCauldronBlockEntity blockEntity,
            ModCauldronRenderState state,
            float tickProgress,
            Vec3 cameraPos,
            @Nullable CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);

        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        BlockState blockState = blockEntity.getBlockState();

        state.lightCoords = level != null
                ? LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos))
                : 0;

        // Базовое содержимое (вода/лава/молоко/снег) — как и раньше, через CauldronContentTypes
        Content content = blockState.getValue(ModCauldronBlock.CONTENT);
        if (content == Content.EMPTY) {
            state.baseHeight = 0f;
            state.baseTint = -1;
        } else {
            int lvl = blockState.getValue(BlockStateProperties.LEVEL_CAULDRON);
            state.baseHeight = (6f + lvl * 3f) / 16f;
            CauldronContentType type = CauldronContentTypes.get(content);
            state.baseTexture = type.contentTexture(); // временно берём как есть; текстуру дальше достаём через sprite-lookup
            state.baseTint = type.useBiomeWaterTint() && level instanceof BlockAndTintGetter tintGetter
                    ? BiomeColors.getAverageWaterColor(tintGetter, pos)
                    : type.tintColor();
        }

        // Сок (динамический тип из датапака)
        Identifier juiceId = blockEntity.getJuiceType();
        if (juiceId == null || blockEntity.getJuiceLevel() == 0) {
            state.juiceHeight = 0f;
            state.juiceTint = -1;
        } else {
            JuiceType juice = CauldronJuiceTypeLoader.get(juiceId);
            if (juice != null) {
                state.juiceHeight = (6f + blockEntity.getJuiceLevel() * 3f) / 16f;
                state.juiceTexture = juice.stillTexture();
                state.juiceTint = juice.tintColor();
            }
        }

        // Осадок (ягоды)
        if (blockEntity.getSolidLevel() == 0) {
            state.solidHeight = 0f;
            state.solidTint = -1;
        } else {
            state.solidHeight = (6f + blockEntity.getSolidLevel() * 3f) / 16f;
            state.solidTexture = Identifier.fromNamespaceAndPath("minecraft", "block/water_still"); // временная заглушка
            state.solidTint = 0xFF7A1F1F;
        }
    }

    @Override
    public void submit(
            ModCauldronRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        RenderType renderType = RenderTypes.translucentMovingBlock();

        if (state.solidTint != -1 && state.solidTexture != null) {
            submitQuad(poseStack, submitNodeCollector, renderType, state.solidHeight, state.solidTint, state.solidTexture, state.lightCoords);
        }
        if (state.baseTint != -1 && state.baseTexture != null) {
            submitQuad(poseStack, submitNodeCollector, renderType, state.baseHeight, state.baseTint, state.baseTexture, state.lightCoords);
        }
        if (state.juiceTint != -1 && state.juiceTexture != null) {
            submitQuad(poseStack, submitNodeCollector, renderType, state.juiceHeight, state.juiceTint, state.juiceTexture, state.lightCoords);
        }
    }

    private static void submitQuad(
            PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
            float height, int argb, Identifier texture, int lightCoords
    ) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)  // minecraft:blocks
                .getSprite(texture);

        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                drawTopQuad(pose, buffer, height, r, g, b, a, sprite, lightCoords));
    }

    private static void drawTopQuad(
            PoseStack.Pose pose, VertexConsumer buffer, float y,
            int r, int g, int b, int a, TextureAtlasSprite sprite, int lightCoords
    ) {
        float x0 = 2f / 16f, x1 = 14f / 16f, z0 = 2f / 16f, z1 = 14f / 16f;

        buffer.addVertex(x0, y, z0).setColor(r, g, b, a).setUv(sprite.getU0(), sprite.getV0()).setUv2(lightCoords, 0);
        buffer.addVertex(x0, y, z1).setColor(r, g, b, a).setUv(sprite.getU0(), sprite.getV1()).setUv2(lightCoords, 0);
        buffer.addVertex(x1, y, z1).setColor(r, g, b, a).setUv(sprite.getU1(), sprite.getV1()).setUv2(lightCoords, 0);
        buffer.addVertex(x1, y, z0).setColor(r, g, b, a).setUv(sprite.getU1(), sprite.getV0()).setUv2(lightCoords, 0);
    }
}