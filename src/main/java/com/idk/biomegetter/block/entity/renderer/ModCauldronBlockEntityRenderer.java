package com.idk.biomegetter.block.entity.renderer;

import com.idk.biomegetter.block.custom.cauldron.data.*;
import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ModCauldronBlockEntityRenderer implements BlockEntityRenderer<ModCauldronBlockEntity, ModCauldronRenderState> {

    //  Инициализируем текстуры
//    private static final Identifier POWDER_SNOW_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "block/powder_snow");
    private static final Identifier FALLBACK_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "block/water_still");
    // новая константа рядом с существующими POWDER_SNOW_TEXTURE/FALLBACK_TEXTURE
//    private static final Identifier LAVA_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "block/lava_still");
//    private static final Identifier IRON_NUGGETS_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "block/lava_still");

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

        state.lightCoords = level != null
                ? LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos))
                : 0;

        // Тотем: рендерится ПЕРЕД любой другой проверкой (isCooking и т.д.), безусловно на
        // весь период варки тотема (и cook, и await-фазы) — иначе во время cook-фазы (isCooking()
        // == true) выполнение уходило бы в старую ветку супа/рецепта раньше, чем дойдёт сюда.
        if (blockEntity.isTotemBrewing()) {
            state.liquidHeight = 15f / 16f;
            state.liquidTexture = FALLBACK_TEXTURE; // ЗАГЛУШКА (water_still) — замените на свою
            state.liquidTint = 0xFFFFFFFF;
            state.solidHeight = 0f;
            state.solidTexture = null;
            state.solidTint = 0xFFFFFFFF;
            return;
        }

        // ---- Верхний слой жидкости ----
        if (blockEntity.isCooking()) {
            ModCauldronBlockEntity.BrewingState brewing = blockEntity.getBrewing();
            if (brewing != null && brewing.soup()) {
                SoupProcessConfig config = CauldronSoupProcessLoader.get();
                state.liquidHeight = 15f / 16f;
                state.liquidTexture = config.cookTexture();
                state.liquidTint = config.cookTint().orElse(0xFFFFFFFF);
                state.solidHeight = 0f;
                state.solidTexture = null;
                state.solidTint = 0xFFFFFFFF;
                return;
            }
            CauldronRecipe recipe = brewing != null ? CauldronRecipeLoader.get(brewing.recipeId()) : null;
            if (recipe != null) {
                state.liquidHeight = 15f / 16f;
                state.liquidTexture = recipe.intermediateTexture();
                state.liquidTint = recipe.intermediateTint().orElse(0xFFFFFFFF);
                state.solidHeight = 0f;
                state.solidTexture = null;
                state.solidTint = 0xFFFFFFFF;
                return; // остальной блок (обычный рендер по реальным стекам) пропускаем
            }
        }

        int liquidCount = blockEntity.getLiquidCount();
        ModCauldronBlockEntity.LiquidLayer topLiquid = blockEntity.getTopLiquidLayer();

        if (liquidCount == 0 || topLiquid == null) {
            state.liquidHeight = 0f;
            state.liquidTexture = null;
            state.liquidTint = 0xFFFFFFFF;
        } else {
            state.liquidHeight = (6f + liquidCount * 3f) / 16f;


            LiquidVisual visual;
            if (topLiquid instanceof ModCauldronBlockEntity.LiquidLayer.Soup soup) {
                visual = new LiquidVisual(soup.data().texture(), soup.data().tint());
            } else if (topLiquid instanceof ModCauldronBlockEntity.LiquidLayer.Liquid liquid) {
                LiquidComponentType type = CauldronLiquidComponentLoader.get(liquid.typeId());
                if (type == null) {
                    visual = new LiquidVisual(null, 0xFFFFFFFF);
                } else {
                    int tint;
                    if (type.useBiomeTint() && level instanceof BlockAndTintGetter tintGetter) {
                        int c = BiomeColors.getAverageWaterColor(tintGetter, pos);
                        tint = 0xFF000000 | (c & 0xFFFFFF);
                    } else {
                        tint = type.tintColor().orElse(0xFFFFFFFF);
                    }
                    visual = new LiquidVisual(type.texture(), tint);
                }
            } else {
                visual = new LiquidVisual(null, 0xFFFFFFFF);
            }

            state.liquidTexture = visual.texture();
            state.liquidTint = visual.tint();
        }

        // ---- Верхний элемент твёрдого стека ----
        int solidCount = blockEntity.getSolidCount();
        ModCauldronBlockEntity.SolidEntry topSolid = blockEntity.getTopSolidEntry();

        if (solidCount == 0 || topSolid == null) {
            state.solidHeight = 0f;
            state.solidTexture = null;
            state.solidTint = 0xFFFFFFFF;
        } else {
            state.solidHeight = (6f + solidCount * 3f) / 16f;
            SolidComponentType type = CauldronSolidComponentLoader.get(topSolid.typeId());

            if (type == null) {
                state.solidTexture = FALLBACK_TEXTURE;
                state.solidTint = 0xFFFFFFFF;
            } else {
                state.solidTexture = type.texture();
                if (type.tintColor().isPresent()) {
                    state.solidTint = type.tintColor().get();
                } else if (type.producesJuice().isPresent()) {
                    LiquidComponentType juice = CauldronLiquidComponentLoader.get(type.producesJuice().get());
                    state.solidTint = (juice != null && juice.tintColor().isPresent()) ? juice.tintColor().get() : 0xFFFFFFFF;
                } else {
                    state.solidTint = 0xFFFFFFFF;
                }
            }
        }

        // ---- "Пена"-заглушка на стадии ожидания ингредиентов ----
        if (blockEntity.isAwaitingIngredient() && blockEntity.getLiquidCount() < 3) {
            ModCauldronBlockEntity.BrewingState brewing = blockEntity.getBrewing();
            CauldronRecipe recipe = brewing != null ? CauldronRecipeLoader.get(brewing.recipeId()) : null;
            int foamColor = recipe != null ? recipe.foamColor().orElse(0xA0808080) : 0xA0808080;

            state.overlayHeight = 15f / 16f;
            state.overlayTexture = FALLBACK_TEXTURE; // временная текстура-заглушка под пену
            state.overlayTint = foamColor;
        } else {
            state.overlayHeight = 0f;
            state.overlayTexture = null;
            state.overlayTint = 0xFFFFFFFF;
        }
    }

    private record LiquidVisual(@Nullable Identifier texture, int tint) {
    }

    @Override
    public void submit(
            ModCauldronRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        RenderType renderType = RenderTypes.translucentMovingBlock();

        if (state.liquidTexture != null && state.liquidHeight > 0f) {
            submitQuad(poseStack, submitNodeCollector, renderType,
                    state.liquidHeight, state.liquidTint, state.liquidTexture, state.lightCoords);
        }
        if (state.solidTexture != null && state.solidHeight > 0f) {
            submitQuad(poseStack, submitNodeCollector, renderType,
                    state.solidHeight, state.solidTint, state.solidTexture, state.lightCoords);
        }
        if (state.overlayTexture != null && state.overlayHeight > 0f) {
            submitQuad(poseStack, submitNodeCollector, renderType,
                    state.overlayHeight, state.overlayTint, state.overlayTexture, state.lightCoords);
        }
    }

    private static void submitQuad(
            PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
            float height, int argb, Identifier texture, int lightCoords
    ) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS)
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