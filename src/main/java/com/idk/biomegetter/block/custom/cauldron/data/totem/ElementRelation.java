package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Одна направленная связь между элементами — data/biomegetter/element_matrix/*.json.
 * {@code source} "смотрит" на {@code target}: relation = +1 (source силён к target),
 * -1 (source слаб к target), 0 или отсутствие записи = нейтрально.
 * <p>
 * Связь НЕ симметрична автоматически — если "земля слаба к воде" (-1), это не означает
 * "вода сильна к земле" (+1) технически, но по замыслу авторы датапака должны заводить
 * обе записи зеркально. См. {@link ElementMatrixLoader#validateSymmetry()} — предупреждение
 * в лог при несимметричных парах, без падения загрузки.
 */
public record ElementRelation(String source, String target, int relation) {
    public static final Codec<ElementRelation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("source").forGetter(ElementRelation::source),
            Codec.STRING.fieldOf("target").forGetter(ElementRelation::target),
            Codec.intRange(-1, 1).fieldOf("relation").forGetter(ElementRelation::relation)
    ).apply(instance, ElementRelation::new));
}