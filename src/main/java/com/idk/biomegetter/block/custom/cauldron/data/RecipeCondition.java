package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * Условия, которые должны непрерывно соблюдаться, пока идёт варка (иначе таймер стадии замирает).
 */
public record RecipeCondition(
        boolean requiresBlockAbove,
        boolean requiresHeated,
        Optional<Integer> minLight,
        Optional<Integer> maxLight
) {
    public static final Codec<RecipeCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("requires_block_above", false).forGetter(RecipeCondition::requiresBlockAbove),
            Codec.BOOL.optionalFieldOf("requires_heated", false).forGetter(RecipeCondition::requiresHeated),
            Codec.INT.optionalFieldOf("min_light").forGetter(RecipeCondition::minLight),
            Codec.INT.optionalFieldOf("max_light").forGetter(RecipeCondition::maxLight)
    ).apply(instance, RecipeCondition::new));

    public boolean isSatisfied(ModCauldronBlockEntity cauldron) {
        if (requiresBlockAbove && !cauldron.hasBlockAbove()) return false;
        if (requiresHeated && !cauldron.isHeatedBelow()) return false;
        if (minLight.isPresent() && cauldron.getLightLevel() < minLight.get()) return false;
        if (maxLight.isPresent() && cauldron.getLightLevel() > maxLight.get()) return false;
        return true;
    }
}