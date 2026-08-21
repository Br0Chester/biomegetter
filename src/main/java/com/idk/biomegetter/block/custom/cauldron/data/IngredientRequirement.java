package com.idk.biomegetter.block.custom.cauldron.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * Требование к слоту стека — либо конкретный компонент ({@code type}), либо ЛЮБОЙ компонент,
 * несущий метку {@code tag} (см. {@code SolidComponentType.tags()} /
 * {@code LiquidComponentType.tags()} — НЕ путать с {@code group()}, который отвечает только за
 * физическую совместимость стека и матчинга рецептов не касается). Указывается ровно ОДНО из
 * двух полей. Существующие JSON с "type" продолжают работать без изменений.
 */
public record IngredientRequirement(Optional<Identifier> type, Optional<String> tag, int count) {
    public static final Codec<IngredientRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("type").forGetter(IngredientRequirement::type),
            Codec.STRING.optionalFieldOf("tag").forGetter(IngredientRequirement::tag),
            Codec.INT.fieldOf("count").forGetter(IngredientRequirement::count)
    ).apply(instance, IngredientRequirement::new));
}