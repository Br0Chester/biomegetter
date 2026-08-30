package com.idk.biomegetter.mixin;

import com.idk.biomegetter.fluid.ConfigurableFluid;
import com.idk.biomegetter.fluid.FluidConfig;
import com.idk.biomegetter.fluid.ModFluidConfigLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Перехватывает LivingEntity.getWaterSlowDown() — единственную точку, которой ваниль определяет
 * "насколько тяжело плавать" (используется в travelInWater как множитель гашения скорости).
 * Если сущность физически стоит в клетке нашей ConfigurableFluid — подменяем ванильный дефолт
 * (0.8F) на fluid_config.viscosity(). Работает только для fluid'ов, помеченных
 * #minecraft:water (иначе ваниль вообще не пойдёт по ветке travelInWater/getWaterSlowDown) —
 * см. ModFluidTagsProvider.
 */
@Mixin(LivingEntity.class)
public abstract class FluidSwimResistanceMixin {

    @Inject(method = "getWaterSlowDown", at = @At("RETURN"), cancellable = true)
    private void biomegetter$applyCustomViscosity(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        FluidState fluidState = self.level().getFluidState(self.blockPosition());
        if (!(fluidState.getType() instanceof ConfigurableFluid configurableFluid)) return;

        FluidConfig config = ModFluidConfigLoader.get(configurableFluid.getConfigId());
        if (config == null) return;

        cir.setReturnValue(config.viscosity());
    }
}