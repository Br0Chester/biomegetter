package com.idk.biomegetter.block.custom.cauldron.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record JuiceType(Identifier stillTexture, int tintColor) {
    public static final Codec<JuiceType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("still_texture").forGetter(JuiceType::stillTexture),
            Codec.INT.fieldOf("tint_color").forGetter(JuiceType::tintColor)
    ).apply(instance, JuiceType::new));
}