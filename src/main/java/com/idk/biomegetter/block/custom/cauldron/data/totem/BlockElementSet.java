package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Один файл — один элемент, перечисляющий ВСЕ блоки, ему принадлежащие —
 * data/biomegetter/block_element/<element>.json (например fire.json, water.json). Id файла =
 * название элемента (используется как ключ во всех формулах — ElementMatrixLoader и т.д.).
 * Блок может входить сразу в НЕСКОЛЬКО файлов элементов одновременно (например, лава — и в
 * fire.json, и в earth.json).
 */
public record BlockElementSet(List<Identifier> blocks) {
    public static final Codec<BlockElementSet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.listOf().fieldOf("blocks").forGetter(BlockElementSet::blocks)
    ).apply(instance, BlockElementSet::new));
}