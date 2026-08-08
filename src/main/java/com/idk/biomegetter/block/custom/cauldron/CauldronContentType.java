package com.idk.biomegetter.block.custom.cauldron;

import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import com.idk.biomegetter.block.entity.ModCauldronBlockEntity.Content;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public record CauldronContentType(
        ModCauldronBlockEntity.Content id,
        Item fillBucket,               // null, если ведром не наполняется (например, сок — только ягодами)
        Identifier level1Model,
        Identifier level2Model,
        Identifier level3Model,
        int tintColor,                  // -1 = биомный цвет воды, иначе фиксированный ARGB
        boolean evaporates,
        boolean damagesEntities,
        boolean requiresHeatToDamage,   // true = как вода (дамажит только при кипении), false = как лава (всегда)
        float damageAmount,
        SoundEvent fillSound,
        SoundEvent emptySound,
        @Nullable Content meltsIntoWhenHeated // null = не тает
) {
    public Identifier modelForLevel(int level) {
        return switch (level) {
            case 1 -> this.level1Model;
            case 2 -> this.level2Model;
            default -> this.level3Model;
        };
    }
}