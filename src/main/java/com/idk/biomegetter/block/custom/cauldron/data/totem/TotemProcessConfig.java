package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * data/biomegetter/totem_process/*.json — ожидается ровно один файл, общие тайминги варки тотема.
 */
public record TotemProcessConfig(int cookDurationTicks, int awaitTimeoutTicks, boolean magicEffect) {
    public static final Codec<TotemProcessConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("cook_duration_ticks", 200).forGetter(TotemProcessConfig::cookDurationTicks),
            Codec.INT.optionalFieldOf("await_timeout_ticks", 600).forGetter(TotemProcessConfig::awaitTimeoutTicks),
            Codec.BOOL.optionalFieldOf("magic_effect", true).forGetter(TotemProcessConfig::magicEffect)
    ).apply(instance, TotemProcessConfig::new));
}