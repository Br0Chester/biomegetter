package com.idk.biomegetter.block.custom.cauldron.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Правило "поглощения"/трансформации блоков вокруг котла. Срабатывает либо при завершении
 * КОНКРЕТНОЙ стадии рецепта ({@code stageIndex} задан), либо при завершении ВСЕЙ варки
 * ({@code stageIndex} отсутствует). Может быть несколько правил на один рецепт — каждое
 * проверяется независимо.
 * <p>
 * {@code area} — ссылка на ritual-шаблон (та же папка template_rituals, тот же формат
 * layers/pattern/keys), задающий ОБЛАСТЬ поглощения — реальные позиции определяются заново
 * матчингом шаблона против мира (это автоматически решает вопрос зеркалирования/поворотов —
 * переиспользуем уже готовую логику RitualTemplate#match). Если {@code area} не задан —
 * используются позиции, реально совпавшие при последней успешной проверке ritual-условия
 * ЭТОГО рецепта (см. ModCauldronBlockEntity#cachedRitualPositions).
 * <p>
 * {@code resultPool} — равновероятный пул результатов (конкретные id блоков; для уничтожения
 * без замены используйте "minecraft:air" как единственный/один из элементов пула).
 */
public record BlockConsumptionRule(
        Optional<Integer> stageIndex,
        float chance,
        int maxBlocks,
        List<Identifier> area,
        List<Identifier> resultPool
) {
    /**
     * "area" в JSON принимает либо один id, либо массив id — тот же комбинированный формат,
     * что и "ritual" в CauldronRecipe (см. CauldronRecipe.RITUAL_CODEC): несколько фигур
     * накладываются друг на друга (последняя в списке побеждает при совпадении позиций), вся
     * объединённая область матчится как одно целое. Пустой список = area не задана явно —
     * используются позиции последней успешно сматченной ritual-структуры этого рецепта
     * (см. ModCauldronBlockEntity#resolveConsumptionArea).
     */
    public static final Codec<List<Identifier>> AREA_CODEC = CauldronRecipe.RITUAL_CODEC;

    public static final Codec<BlockConsumptionRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("stage_index").forGetter(BlockConsumptionRule::stageIndex),
            Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(BlockConsumptionRule::chance),
            Codec.INT.optionalFieldOf("max_blocks", 8).forGetter(BlockConsumptionRule::maxBlocks),
            AREA_CODEC.optionalFieldOf("area", List.of()).forGetter(BlockConsumptionRule::area),
            Identifier.CODEC.listOf().fieldOf("result_pool").forGetter(BlockConsumptionRule::resultPool)
    ).apply(instance, BlockConsumptionRule::new));
}