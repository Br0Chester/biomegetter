package com.idk.biomegetter.block.custom.cauldron.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Правило реакции жидкости (по группе) с твёрдым содержимым (по группе). Не симметрично по смыслу (жидкость атакует твёрдое), поэтому направление фиксировано.
 */
public record SolidReactionRule(String liquidGroup, String solidGroup, List<ReactionEffect> effects) {
    public static final Codec<SolidReactionRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("liquid_group").forGetter(SolidReactionRule::liquidGroup),
            Codec.STRING.fieldOf("solid_group").forGetter(SolidReactionRule::solidGroup),
            ReactionEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(SolidReactionRule::effects)
    ).apply(instance, SolidReactionRule::new));
}