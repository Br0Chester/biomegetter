package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Одна дельта, которую кольцо накладывает на конкретный стат скилла — см. TotemStatRing.
 * {@code multiplier} применяется до учёта элементного модификатора (см.
 * ElementMatrixLoader#computeModifier); итоговое воздействие на стат считается вызывающим
 * кодом как: sign*(multiplier-1) * elementModifier, применённое к базовому значению стата.
 */
public record TotemStatModifier(String stat, float multiplier) {
    public static final Codec<TotemStatModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("stat").forGetter(TotemStatModifier::stat),
            Codec.FLOAT.fieldOf("multiplier").forGetter(TotemStatModifier::multiplier)
    ).apply(instance, TotemStatModifier::new));
}