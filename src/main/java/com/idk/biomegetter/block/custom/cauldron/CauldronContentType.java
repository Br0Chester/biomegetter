package com.idk.biomegetter.block.custom.cauldron;

import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

public record CauldronContentType(
        ModCauldronBlockEntity.Content id,
        Item fillBucket,               // null, если ведром не наполняется (например, сок — только ягодами)
        ResourceLocation level1Model,
        ResourceLocation level2Model,
        ResourceLocation level3Model,
        int tintColor,                  // -1 = биомный цвет воды, иначе фиксированный ARGB
        boolean evaporates,
        boolean damagesEntities,
        boolean requiresHeatToDamage,   // true = как вода (дамажит только при кипении), false = как лава (всегда)
        float damageAmount,
        SoundEvent fillSound,
        SoundEvent emptySound
) {
    public ResourceLocation modelForLevel(int level) {
        return switch (level) {
            case 1 -> this.level1Model;
            case 2 -> this.level2Model;
            default -> this.level3Model;
        };
    }
}