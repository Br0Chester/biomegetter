package com.idk.biomegetter.mixin;

import com.idk.biomegetter.fluid.ConfigurableFluid;
import com.idk.biomegetter.fluid.FluidConfig;
import com.idk.biomegetter.fluid.ModFluidConfigLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * WaterFogEnvironment.getBaseColor(...) всегда возвращает биомный EnvironmentAttributes.
 * WATER_FOG_COLOR — эта точка отвечает и за тонировку экрана/дальнего тумана при погружении
 * камеры в ЛЮБУЮ жидкость с типом FogType.WATER (в том числе наши ConfigurableFluid, раз они
 * зарегистрированы в #minecraft:water — см. ModFluidTagsProvider). Подменяем возврат на
 * fluid_config.tintColor(), если камера физически стоит в клетке нашей жидкости.
 */
@Mixin(net.minecraft.client.renderer.fog.environment.WaterFogEnvironment.class)
public abstract class FluidFogColorMixin {

    @Inject(method = "getBaseColor", at = @At("RETURN"), cancellable = true)
    private void biomegetter$applyCustomFogTint(
            ClientLevel level, Camera camera, int renderDistance, float partialTicks,
            CallbackInfoReturnable<Integer> cir
    ) {
        BlockPos cameraPos = BlockPos.containing(camera.position());
        FluidState fluidState = level.getFluidState(cameraPos);
        if (!(fluidState.getType() instanceof ConfigurableFluid configurableFluid)) return;

        FluidConfig config = ModFluidConfigLoader.get(configurableFluid.getConfigId());
        if (config == null) return;

        config.tintColor().ifPresent(cir::setReturnValue);
    }
}