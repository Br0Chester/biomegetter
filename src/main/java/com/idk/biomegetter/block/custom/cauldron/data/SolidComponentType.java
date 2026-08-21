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
 * Универсальное описание одного вида "трети" твёрдого содержимого котла (ягода, снег,
 * самородки и любые будущие предметы) — полностью датапак-driven: новый предмет = новый
 * JSON-файл в data/biomegetter/solid_component/*.json, без изменений в Java.
 * <p>
 * Правило выбора цвета для рендера (см. использование в ModCauldronBlockEntityRenderer):
 * 1) если {@code tintColor} указан явно — используется он;
 * 2) иначе, если указан {@code producesJuice} — берётся цвет соответствующего JuiceType;
 * 3) иначе — белый (без изменений).
 * <p>
 * {@code producesJuice.isPresent()} — единственный признак того, что компонент "давится"
 * падением/прыжком (ведёт себя как ягода); при отсутствии — просто лежит (как снег/самородки).
 * <p>
 * Особая логика поведения сверх этого (предметы, трансформирующиеся сами по себе со временем
 * и т.п.) в эту схему пока не входит — такие случаи потребуют отдельного кода, ссылающегося
 * на конкретный Identifier типа
 * для таяния снега).
 */
public record SolidComponentType(
        Item inputItem,
        int inputCount,
        String group,
        Identifier texture,
        Optional<Integer> tintColor,
        boolean returnsEmptyBucket,
        Item outputItem,
        int outputCount,
        Optional<Identifier> producesJuice,
        Optional<Item> collectBucketItem,
        SoundEvent placeSound,
        int lightLevel,
        Optional<String> uniqueStarter,
        List<String> tags
) {
    public static final Codec<SolidComponentType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("input_item").forGetter(SolidComponentType::inputItem),
            Codec.INT.fieldOf("input_count").forGetter(SolidComponentType::inputCount),
            Codec.STRING.optionalFieldOf("group", "neutral").forGetter(SolidComponentType::group),
            Identifier.CODEC.fieldOf("texture").forGetter(SolidComponentType::texture),
            HexColorCodec.CODEC.optionalFieldOf("tint_color").forGetter(SolidComponentType::tintColor),
            Codec.BOOL.optionalFieldOf("returns_empty_bucket", false).forGetter(SolidComponentType::returnsEmptyBucket),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("output_item").forGetter(SolidComponentType::outputItem),
            Codec.INT.fieldOf("output_count").forGetter(SolidComponentType::outputCount),
            Identifier.CODEC.optionalFieldOf("produces_juice").forGetter(SolidComponentType::producesJuice),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("collect_bucket_item").forGetter(SolidComponentType::collectBucketItem),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("place_sound").forGetter(SolidComponentType::placeSound),
            Codec.intRange(0, 15).optionalFieldOf("light_level", 0).forGetter(SolidComponentType::lightLevel),
            // unique_starter — метка домена ("soup" и т.п.), отмечающая предмет как "ключ", запускающий соответствующую творческую подсистему при полном однородном бульоне.
            Codec.STRING.optionalFieldOf("unique_starter").forGetter(SolidComponentType::uniqueStarter),
            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(SolidComponentType::tags)
    ).apply(instance, SolidComponentType::new));

    /**
     * @return true, если этот тип отжимается падением/прыжком (даёт сок) — "ягода"-подобный
     */
    public boolean isPressable() {
        return this.producesJuice.isPresent();
    }
}