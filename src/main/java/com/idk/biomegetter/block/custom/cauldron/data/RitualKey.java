package com.idk.biomegetter.block.custom.cauldron.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Один символ ритуального паттерна — набор допустимых блоков (blocks) И/ИЛИ тегов (tags),
 * матчится по ИЛИ (совпадение хотя бы с одним условием достаточно).
 */
public record RitualKey(List<Identifier> blocks, List<String> tags, boolean anyBlock, boolean anyFluid,
                        boolean anyElementBlock) {
    public static final Codec<RitualKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(RitualKey::blocks),
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(RitualKey::tags),
            Codec.BOOL.optionalFieldOf("any_block", false).forGetter(RitualKey::anyBlock), // матчит ЛЮБОЙ блок, включая воздух — дублирует "_", оставлено для обратной совместимости
            Codec.BOOL.optionalFieldOf("any_fluid", false).forGetter(RitualKey::anyFluid),  // матчит клетку, где физически есть жидкость (любая)
            Codec.BOOL.optionalFieldOf("any_element_block", false).forGetter(RitualKey::anyElementBlock) // матчит любой блок, классифицированный хотя бы в одном block_element файле
    ).apply(instance, RitualKey::new));

    public boolean matches(BlockState state) {
        if (anyBlock) return true;
        if (anyFluid) return !state.getFluidState().isEmpty();
        if (anyElementBlock)
            return !com.idk.biomegetter.block.custom.cauldron.data.totem.BlockElementLoader.elementsOf(state).isEmpty();

        Identifier actualId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blocks.contains(actualId)) return true;
        for (String tag : tags) {
            TagKey<net.minecraft.world.level.block.Block> tagKey =
                    TagKey.create(Registries.BLOCK, Identifier.parse(tag));
            if (state.is(tagKey)) return true;
        }
        return false;
    }
}