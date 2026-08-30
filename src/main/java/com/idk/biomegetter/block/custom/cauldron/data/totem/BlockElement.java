package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Классификация физического блока по элементу — data/biomegetter/block_element/*.json.
 * Используется, чтобы определить, из каких элементов реально сложено ритуальное кольцо
 * (TotemStatRing больше не хранит element сам по себе — он вычисляется по факту того, какими
 * блоками кольцо было физически построено в мире).
 */
public record BlockElement(String element) {
    public static final Codec<BlockElement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("element").forGetter(BlockElement::element)
    ).apply(instance, BlockElement::new));
}