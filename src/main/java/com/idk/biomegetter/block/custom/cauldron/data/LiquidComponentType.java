package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.util.HexColorCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Optional;

/**
 * Универсальное описание одной трети ЖИДКОГО содержимого котла (вода/молоко/сок/что угодно
 * ещё) — полностью датапак-driven: новая жидкость = новый JSON в
 * data/biomegetter/liquid_component/*.json, без изменений в Java.
 * ЛАВА сюда не входит — она остаётся отдельным поведением (см. ModCauldronBlockEntity.LiquidLayer.Lava).
 */
public record LiquidComponentType(
        Identifier texture,
        Optional<Integer> tintColor,
        boolean useBiomeTint,
        Optional<Item> pourBucketItem,
        Optional<Item> collectBottleItem,
        Item collectBucketItem,
        SoundEvent fillSound,
        SoundEvent emptySound,
        String group,
        int lightLevel,
        List<String> tags
) {
    public static final Codec<LiquidComponentType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("texture").forGetter(LiquidComponentType::texture),
            HexColorCodec.CODEC.optionalFieldOf("tint_color").forGetter(LiquidComponentType::tintColor),
            Codec.BOOL.optionalFieldOf("use_biome_tint", false).forGetter(LiquidComponentType::useBiomeTint),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("pour_bucket_item").forGetter(LiquidComponentType::pourBucketItem),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("collect_bottle_item").forGetter(LiquidComponentType::collectBottleItem),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("collect_bucket_item").forGetter(LiquidComponentType::collectBucketItem),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("fill_sound").forGetter(LiquidComponentType::fillSound),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("empty_sound").forGetter(LiquidComponentType::emptySound),
            Codec.STRING.optionalFieldOf("group", "neutral").forGetter(LiquidComponentType::group),
            Codec.intRange(0, 15).optionalFieldOf("light_level", 0).forGetter(LiquidComponentType::lightLevel),
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(LiquidComponentType::tags)
    ).apply(instance, LiquidComponentType::new));
}