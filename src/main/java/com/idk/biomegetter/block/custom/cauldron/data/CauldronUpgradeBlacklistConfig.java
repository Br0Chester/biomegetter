package com.idk.biomegetter.block.custom.cauldron.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Единственный файл-конфиг блэклиста — data/biomegetter/unupgraded_cauldron_blacklist/config.json
 * (ровно один файл, не папка суммируемых записей). Перечисляет группы (group) и/или теги (tags)
 * жидкостей, которые НЕЛЬЗЯ налить/хранить в неулучшенном котле (см. ModCauldronBlockBasic).
 */
public record CauldronUpgradeBlacklistConfig(List<String> groups, List<String> tags) {
    public static final Codec<CauldronUpgradeBlacklistConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("groups", List.of()).forGetter(CauldronUpgradeBlacklistConfig::groups),
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(CauldronUpgradeBlacklistConfig::tags)
    ).apply(instance, CauldronUpgradeBlacklistConfig::new));
}