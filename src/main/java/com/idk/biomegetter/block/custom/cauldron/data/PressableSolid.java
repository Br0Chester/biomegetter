package com.idk.biomegetter.block.custom.cauldron.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

public record PressableSolid(
        Item item,
        Identifier producesJuice,
        SoundEvent pressSound,
        Item residueItem,
        Identifier solidTexture
) {
    public static final Codec<PressableSolid> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(PressableSolid::item),
            Identifier.CODEC.fieldOf("produces_juice").forGetter(PressableSolid::producesJuice),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("press_sound").forGetter(PressableSolid::pressSound),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("residue_item").forGetter(PressableSolid::residueItem),
            Identifier.CODEC.fieldOf("solid_texture").forGetter(PressableSolid::solidTexture)
    ).apply(instance, PressableSolid::new));
}