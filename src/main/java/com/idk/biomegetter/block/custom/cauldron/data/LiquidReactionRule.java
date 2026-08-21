package com.idk.biomegetter.block.custom.cauldron.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Правило реакции между двумя ГРУППАМИ жидкости (например, "molten" встречает "aqueous").
 * Симметрично по умолчанию — достаточно одного файла на пару групп, работает в обе стороны
 * (см. CauldronLiquidReactionLoader#find). Сам факт вытеснения верхних слоёв — механика
 * движка, не настраивается; effects описывают только ПОСЛЕДСТВИЯ (что выпадает, какой звук).
 */
public record LiquidReactionRule(String groupA, String groupB, List<ReactionEffect> effects) {
    public static final Codec<LiquidReactionRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("group_a").forGetter(LiquidReactionRule::groupA),
            Codec.STRING.fieldOf("group_b").forGetter(LiquidReactionRule::groupB),
            ReactionEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(LiquidReactionRule::effects)
    ).apply(instance, LiquidReactionRule::new));
}