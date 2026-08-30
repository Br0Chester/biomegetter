package com.idk.biomegetter.block.custom.cauldron.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Один горизонтальный "срез" ритуальной структуры. {@code yOffset} — высота относительно
 * котла (0 = уровень самого котла, необязательно должен присутствовать среди слоёв).
 * {@code cauldronRow}/{@code cauldronCol} — координаты точки-привязки котла, ОБЯЗАНЫ быть
 * заданы РОВНО У ОДНОГО слоя во всём {@link RitualTemplate} — эти же координаты (не
 * пересчитываемые заново) используются как система отсчёта для ВСЕХ слоёв одновременно.
 */
public record RitualLayer(int yOffset, List<String> pattern, Optional<Integer> cauldronRow,
                          Optional<Integer> cauldronCol) {
    public static final Codec<RitualLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("y_offset").forGetter(RitualLayer::yOffset),
            Codec.STRING.listOf().fieldOf("pattern").forGetter(RitualLayer::pattern),
            Codec.INT.optionalFieldOf("cauldron_row").forGetter(RitualLayer::cauldronRow),
            Codec.INT.optionalFieldOf("cauldron_col").forGetter(RitualLayer::cauldronCol)
    ).apply(instance, RitualLayer::new));
}