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
import net.minecraft.client.renderer.texture.OverlayTexture;
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

//        BiomeGetter.LOGGER.info("state={} content={} level={}",
//                blockState,
//                blockState.getValue(ModCauldronBlock.CONTENT),
//                blockState.hasProperty(BlockStateProperties.LEVEL_CAULDRON)
//                        ? blockState.getValue(BlockStateProperties.LEVEL_CAULDRON)
//                        : -1
//        );
//        BiomeGetter.LOGGER.info("BE juiceId={} juiceLvl={} solidLvl={}",
//                blockEntity.getJuiceType(),
//                blockEntity.getJuiceLevel(),
//                blockEntity.getSolidLevel()
//        );

        state.lightCoords = level != null
                ? LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos))
                : 0;

        // Базовое содержимое (вода/лава/молоко/снег) — как и раньше, через CauldronContentTypes
//        Content content = blockState.getValue(ModCauldronBlock.CONTENT);
//        if (content == Content.EMPTY) {
//            state.baseHeight = 0f;
//            state.baseTint = -1;
//        } else {
//            int lvl = blockState.getValue(BlockStateProperties.LEVEL_CAULDRON);
//            state.baseHeight = (6f + lvl * 3f) / 16f;
//            CauldronContentType type = CauldronContentTypes.get(content);
//            state.baseTexture = type.contentTexture(); // временно берём как есть; текстуру дальше достаём через sprite-lookup
//            state.baseTint = type.useBiomeWaterTint() && level instanceof BlockAndTintGetter tintGetter
//                    ? BiomeColors.getAverageWaterColor(tintGetter, pos)
//                    : type.tintColor();
//        }
        Content content = blockState.getValue(ModCauldronBlock.CONTENT);
        int lvl = blockState.getValue(BlockStateProperties.LEVEL_CAULDRON);

        if (content == Content.EMPTY) {
            state.baseHeight = 0f;
            state.baseTexture = null;
            state.baseTint = 0xFFFFFFFF;
        } else {
            CauldronContentType type = CauldronContentTypes.get(content);
            state.baseHeight = (6f + lvl * 3f) / 16f;
            state.baseTexture = type.contentTexture();

            if (type.useBiomeWaterTint() && level instanceof BlockAndTintGetter tintGetter) {
                int c = BiomeColors.getAverageWaterColor(tintGetter, pos);
                state.baseTint = 0xFF000000 | (c & 0xFFFFFF);
            } else {
                state.baseTint = type.tintColor(); // для лавы/молока 0xFFFFFFFF — это ОКМАЛЬНО
            }
        }

        // Сок (динамический тип из датапака)
        Identifier juiceId = blockEntity.getJuiceType();
        int juiceLvl = blockEntity.getJuiceLevel();

        if (juiceId == null || juiceLvl <= 0) {
            state.juiceHeight = 0f;
            state.juiceTexture = null;
            state.juiceTint = 0xFFFFFFFF;
        } else {
            JuiceType juice = CauldronJuiceTypeLoader.get(juiceId);
            if (juice != null) {
                state.juiceHeight = (6f + juiceLvl * 3f) / 16f;
                state.juiceTexture = juice.stillTexture(); // обязательно
                state.juiceTint = juice.tintColor();       // обязательно, без -1
            } else {
                state.juiceHeight = 0f;
                state.juiceTexture = null;
                state.juiceTint = 0xFFFFFFFF;
            }
        }

        // Осадок (ягоды)
        int solidLvl = blockEntity.getSolidLevel();

        if (solidLvl <= 0) {
            state.solidHeight = 0f;
            state.solidTexture = null;
            state.solidTint = 0xFFFFFFFF;
        } else {
            state.solidHeight = (6f + solidLvl * 3f) / 16f;

            // текстура осадка из BE (solid_texture из JSON)
            state.solidTexture = blockEntity.getSolidType();

            // цвет можно взять от связанного juice
            Identifier juiceIdForSolid = blockEntity.getJuiceType();
            JuiceType juice = juiceIdForSolid != null
                    ? CauldronJuiceTypeLoader.get(juiceIdForSolid) : null;
            state.solidTint = juice != null ? juice.tintColor() : 0xFFFFFFFF;

            // fallback, если solidType ещё null
            if (state.solidTexture == null) {
                state.solidTexture = Identifier.fromNamespaceAndPath("minecraft", "block/water_still");
            }
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

        if (state.baseTexture != null && state.baseHeight > 0f) {
            submitQuad(poseStack, submitNodeCollector, renderType,
                    state.baseHeight, state.baseTint, state.baseTexture, state.lightCoords);
        }
        if (state.juiceTexture != null && state.juiceHeight > 0f) {
            submitQuad(poseStack, submitNodeCollector, renderType,
                    state.juiceHeight, state.juiceTint, state.juiceTexture, state.lightCoords);
        }
        if (state.solidTexture != null && state.solidHeight > 0f) {
            submitQuad(poseStack, submitNodeCollector, renderType,
                    state.solidHeight, state.solidTint, state.solidTexture, state.lightCoords);
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
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        // порядок вершин: для верхней грани смотрящей вверх
        buffer.addVertex(pose.pose(), x0, y, z0).setColor(r, g, b, a).setUv(u0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0f, 1f, 0f);
        buffer.addVertex(pose.pose(), x0, y, z1).setColor(r, g, b, a).setUv(u0, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0f, 1f, 0f);
        buffer.addVertex(pose.pose(), x1, y, z1).setColor(r, g, b, a).setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0f, 1f, 0f);
        buffer.addVertex(pose.pose(), x1, y, z0).setColor(r, g, b, a).setUv(u1, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(pose, 0f, 1f, 0f);
    }
}