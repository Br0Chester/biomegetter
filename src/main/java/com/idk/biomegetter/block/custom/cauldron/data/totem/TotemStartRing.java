package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.idk.biomegetter.block.custom.cauldron.data.RitualTemplate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Кольцо-триггер старта варки тотема — data/biomegetter/template_totem_start_ring/*.json.
 * Оборачивает уже существующий RitualTemplate (layers/keys), добавляя только {@code level} —
 * уровень кольца ОБЯЗАН совпадать с уровнем компонентов, заложенных в котёл, иначе варка не
 * начнётся (см. согласованное правило E). Блоки кольца поглощаются сразу при старте варки
 * (переиспользуем BlockConsumptionRule-механизм с result_pool=[air] за кулисами кода, не JSON).
 */
public record TotemStartRing(int level, RitualTemplate template) {
    public static final Codec<TotemStartRing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(TotemStartRing::level),
            RitualTemplate.CODEC.fieldOf("template").forGetter(TotemStartRing::template)
    ).apply(instance, TotemStartRing::new));
}