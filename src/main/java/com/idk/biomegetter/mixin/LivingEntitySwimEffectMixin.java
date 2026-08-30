package com.idk.biomegetter.mixin;

import com.idk.biomegetter.block.custom.cauldron.data.IngredientEffectDef;
import com.idk.biomegetter.fluid.ConfigurableFluid;
import com.idk.biomegetter.fluid.FluidConfig;
import com.idk.biomegetter.fluid.ModFluidConfigLoader;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Раз в тик проверяет, стоит ли сущность в клетке физической жидкости мода (по глазам, как
 * ваниль определяет "в воде"), и если да — применяет swim_effects из fluid_config. Не трогает
 * ванильные жидкости — срабатывает только если FluidState.getType() — наш ConfigurableFluid.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySwimEffectMixin {

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void biomegetter$applySwimEffects(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        FluidState fluidState = self.level().getFluidState(self.blockPosition());
        if (!(fluidState.getType() instanceof ConfigurableFluid configurableFluid)) return;

        FluidConfig config = ModFluidConfigLoader.get(configurableFluid.getConfigId());
        if (config == null) return;

        for (IngredientEffectDef.EffectEntry entry : config.swimEffects()) {
            self.addEffect(new MobEffectInstance(entry.effect(), entry.durationTicks(), entry.amplifier()));
        }
        // Флаг damage_per_tick — простой урон КАЖДЫЙ ТИК, пока сущность стоит в клетке жидкости.
        if (config.damagePerTick() > 0.0f && self.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            self.hurtServer(serverLevel, self.damageSources().generic(), config.damagePerTick());
        }
    }
}