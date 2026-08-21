package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.block.custom.cauldron.data.spec_spices.BuffTransform;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

import java.util.List;
import java.util.Optional;

public record IngredientEffectDef(
        List<EffectEntry> effects,
        int sustenance,
        Optional<String> category,
        List<EffectModifier> modifiers,
        List<BuffTransform> buffTransforms
) {
    public record EffectEntry(Holder<MobEffect> effect, int amplifier, int durationTicks, int maxAmplifier) {
        public static final Codec<EffectEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(EffectEntry::effect),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(EffectEntry::amplifier),
                Codec.INT.fieldOf("duration_ticks").forGetter(EffectEntry::durationTicks),
                Codec.INT.optionalFieldOf("max_amplifier", 2).forGetter(EffectEntry::maxAmplifier)
        ).apply(instance, EffectEntry::new));
    }

    /**
     * Модификатор, который специя накладывает на УЖЕ накопленные эффекты других ингредиентов
     * (финальный пересчёт, см. {@code ModCauldronBlockEntity#aggregateComposite}). Цель — либо
     * категория ({@code targetCategory}), либо конкретный ингредиент ({@code targetIngredient}).
     * Оба поля опциональны независимо — если указаны сразу оба, модификатор сработает при
     * совпадении ЛЮБОГО из условий (по любому из вкладов эффекта). На насыщение не влияет.
     */
    public record EffectModifier(
            Optional<String> targetCategory,
            Optional<Identifier> targetIngredient,
            Optional<Float> durationMultiplier,
            Optional<Integer> amplifierBonus
    ) {
        public static final Codec<EffectModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("target_category").forGetter(EffectModifier::targetCategory),
                Identifier.CODEC.optionalFieldOf("target_ingredient").forGetter(EffectModifier::targetIngredient),
                Codec.FLOAT.optionalFieldOf("duration_multiplier").forGetter(EffectModifier::durationMultiplier),
                Codec.INT.optionalFieldOf("amplifier_bonus").forGetter(EffectModifier::amplifierBonus)
        ).apply(instance, EffectModifier::new));
    }

    public static final Codec<IngredientEffectDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EffectEntry.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(IngredientEffectDef::effects),
            Codec.INT.optionalFieldOf("sustenance", 0).forGetter(IngredientEffectDef::sustenance),
            Codec.STRING.optionalFieldOf("category").forGetter(IngredientEffectDef::category),
            EffectModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(IngredientEffectDef::modifiers),
            BuffTransform.CODEC.listOf().optionalFieldOf("buff_transforms", List.of()).forGetter(IngredientEffectDef::buffTransforms)
    ).apply(instance, IngredientEffectDef::new));
}