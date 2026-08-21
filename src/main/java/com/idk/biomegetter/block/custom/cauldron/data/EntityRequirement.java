package com.idk.biomegetter.block.custom.cauldron.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

public record EntityRequirement(EntityType<?> entityType) {
    public static final Codec<EntityRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity_type").forGetter(EntityRequirement::entityType)
    ).apply(instance, EntityRequirement::new));
}