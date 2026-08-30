package com.idk.biomegetter.mixin;

import com.idk.biomegetter.fluid.ConfigurableFluid;
import com.idk.biomegetter.fluid.FluidConfig;
import com.idk.biomegetter.fluid.ModFluidConfigLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * travelInWater() содержит строку "isSprinting() ? 0.9F : getWaterSlowDown()" — при спринте
 * (Ctrl+W, "плавательная" анимация) ваниль ПОЛНОСТЬЮ пропускает getWaterSlowDown() и
 * захардкоженно берёт 0.9F, из-за чего FluidSwimResistanceMixin (перехватывающий именно
 * getWaterSlowDown) не срабатывает. Здесь перехватываем сам вызов isSprinting() СТРОГО внутри
 * travelInWater (не глобально — не трогает саму анимацию/состояние спринта нигде больше) и
 * подменяем на false, когда сущность физически в нашей жидкости, чтобы тернарник всегда
 * уходил в ветку getWaterSlowDown() с уже применённой вязкостью.
 */
@Mixin(LivingEntity.class)
public abstract class FluidSprintSlowdownMixin {

    @Redirect(method = "travelInWater", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;isSprinting()Z"
    ))
    private boolean biomegetter$forceCustomViscosityBranch(LivingEntity self) {
        boolean sprinting = self.isSprinting();
        if (!sprinting) return false;

        FluidState fluidState = self.level().getFluidState(self.blockPosition());
        if (fluidState.getType() instanceof ConfigurableFluid configurableFluid) {
            FluidConfig config = ModFluidConfigLoader.get(configurableFluid.getConfigId());
            if (config != null) {
                return false; // принудительно уходим в ветку getWaterSlowDown() с нашей вязкостью
            }
        }
        return sprinting;
    }
}