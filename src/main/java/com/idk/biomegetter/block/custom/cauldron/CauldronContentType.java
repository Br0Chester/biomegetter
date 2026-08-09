package com.idk.biomegetter.block.custom.cauldron;

import com.idk.biomegetter.block.entity.ModCauldronBlockEntity.Content;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public record CauldronContentType(
        Content id,
        Item fillBucket,
        Identifier level1Model,
        Identifier level2Model,
        Identifier level3Model,
        boolean useBiomeWaterTint,   // true только у настоящей воды
        int tintColor,
// ARGB С АЛЬФОЙ (0xFFxxxxxx для непрозрачного) — используется только если useBiomeWaterTint == false
        boolean evaporates,
        boolean damagesEntities,
        boolean requiresHeatToDamage,
        float damageAmount,
        SoundEvent fillSound,
        SoundEvent emptySound,
        @Nullable Content meltsIntoWhenHeated
) {
    public Identifier modelForLevel(int level) {
        return switch (level) {
            case 1 -> this.level1Model;
            case 2 -> this.level2Model;
            default -> this.level3Model;
        };
    }
}