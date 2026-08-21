package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.util.HexColorCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Правило выбора итогового названия composite-результата (суп/зелье/etc). Срабатывает, если
 * ВСЕ категории из requiresCategories присутствуют среди категорий положенных ингредиентов.
 * Из всех подошедших правил выбирается с наименьшим priority (меньше число — выше приоритет).
 * Пустой requiresCategories всегда подходит — удобно для "запасного" правила (веганский суп).
 * Тип НЕ запрещает (нет "исключающих" категорий) — просто выбирается наиболее подходящий на
 * момент завершения варки, когда все ингредиенты уже добавлены.
 */
public record SoupCategoryRule(
        List<String> requiresCategories,
        int priority,
        String displayName,
        Optional<Identifier> icon,       // текстура жидкости готового супа (см. ModCauldronBlockEntityRenderer)
        Optional<Integer> tintColor      // тинт жидкости готового супа
) {
    public static final Codec<SoupCategoryRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("requires_categories", List.of()).forGetter(SoupCategoryRule::requiresCategories),
            Codec.INT.fieldOf("priority").forGetter(SoupCategoryRule::priority),
            Codec.STRING.fieldOf("display_name").forGetter(SoupCategoryRule::displayName),
            Identifier.CODEC.optionalFieldOf("icon").forGetter(SoupCategoryRule::icon),
            HexColorCodec.CODEC.optionalFieldOf("tint_color").forGetter(SoupCategoryRule::tintColor)
    ).apply(instance, SoupCategoryRule::new));
}