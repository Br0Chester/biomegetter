package com.idk.biomegetter.fluid;

import com.idk.biomegetter.block.custom.cauldron.data.IngredientEffectDef;
import com.idk.biomegetter.util.HexColorCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Поведенческая конфигурация физической жидкости — data/biomegetter/fluid_config/*.json.
 * Java-регистрация (ModFluids) содержит только структурную часть (id, class, block, bucket);
 * ВСЕ числа читаются отсюда заново на каждое обращение (аналогично SoupProcessConfig), т.к.
 * Fluid/Block/Item обязаны быть зарегистрированы ДО загрузки любого датапака.
 * <p>
 * gravity_mode: "normal" (Фаза 1, единственный реализованный — поведение как у воды),
 * "zero"/"anti" зарезервированы под Фазу 2 (см. ConfigurableFluid — пока падают в normal
 * с предупреждением в лог).
 */

/**
 * ...
 *
 * @param viscosity     множитель гашения скорости при плавании (см. LivingEntity.getWaterSlowDown,
 *                      перехватывается FluidSwimResistanceMixin) — 1.0 = как в воздухе (не гасится),
 *                      0.8 = как обычная вода (ванильный дефолт), меньше = тяжелее плавать.
 * @param damagePerTick урон, наносимый КАЖДЫЙ ТИК сущности, стоящей в клетке этой жидкости
 *                      (см. LivingEntitySwimEffectMixin) — 0 = не наносит урона.
 *                      ...
 */
public record FluidConfig(
        Identifier textureStill,
        Identifier textureFlowing,
        Optional<Integer> tintColor,
        float viscosity,
        String gravityMode,
        int maxUpDistance,
        int spreadDropoff,
        boolean interactsWithSoulSand,
        boolean interactsWithMagma,
        List<IngredientEffectDef.EffectEntry> swimEffects,
        float damagePerTick
) {
    public static final Codec<FluidConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("texture_still").forGetter(FluidConfig::textureStill),
            Identifier.CODEC.fieldOf("texture_flowing").forGetter(FluidConfig::textureFlowing),
            HexColorCodec.CODEC.optionalFieldOf("tint_color").forGetter(FluidConfig::tintColor),
            Codec.FLOAT.optionalFieldOf("viscosity", 1.0f).forGetter(FluidConfig::viscosity),
            Codec.STRING.optionalFieldOf("gravity_mode", "normal").forGetter(FluidConfig::gravityMode),
            Codec.INT.optionalFieldOf("max_up_distance", 10).forGetter(FluidConfig::maxUpDistance), // только для gravity_mode "anti"
            Codec.INT.optionalFieldOf("spread_dropoff", 1).forGetter(FluidConfig::spreadDropoff),
            Codec.BOOL.optionalFieldOf("interacts_with_soul_sand", false).forGetter(FluidConfig::interactsWithSoulSand),
            Codec.BOOL.optionalFieldOf("interacts_with_magma", false).forGetter(FluidConfig::interactsWithMagma),
            IngredientEffectDef.EffectEntry.CODEC.listOf().optionalFieldOf("swim_effects", List.of()).forGetter(FluidConfig::swimEffects),
            Codec.FLOAT.optionalFieldOf("damage_per_tick", 0.0f).forGetter(FluidConfig::damagePerTick)
    ).apply(instance, FluidConfig::new));
}