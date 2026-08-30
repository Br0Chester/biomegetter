package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Классификация ингредиента тотема: {@code level} — уровень (все компоненты, заложенные в
 * котёл при старте варки тотема, обязаны быть ОДНОГО уровня — иначе варка не начнётся),
 * {@code element} — используется только рисунками (см. TotemStatRing) для матчинга силы/
 * слабости; НЕ путать с {@code group} у SolidComponentType/LiquidComponentType — материал
 * тотема (organic/metal) переиспользует именно {@code group}, а не это поле.
 */
public record TotemComponentLevel(int level, java.util.Optional<String> element) {
    public static final Codec<TotemComponentLevel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(TotemComponentLevel::level),
            Codec.STRING.optionalFieldOf("element").forGetter(TotemComponentLevel::element)
    ).apply(instance, TotemComponentLevel::new));
}