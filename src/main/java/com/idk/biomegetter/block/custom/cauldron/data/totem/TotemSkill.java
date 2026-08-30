package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Общая схема одного скилла (пассивного ИЛИ активного — оба используют одну и ту же
 * структуру, различаются только загрузчиком/папкой на диске: totem_passive_skill /
 * totem_active_skill).
 * <p>
 * {@code stats} — произвольный словарь базовых числовых характеристик (см. согласованное
 * решение G) — например {"power": 5.0, "debuff_duration_ticks": 100.0}. Кольца
 * (TotemStatRing.modifiers) домножают эти значения по ключу; отсутствующие у скилла ключи
 * из кольца просто игнорируются (см. computedStats).
 * <p>
 * {@code durabilityCost} / {@code durabilityCostChance} — расход прочности при срабатывании
 * (для пассивки — шанс срабатывания расхода на тик/событие; для активки по согласованному
 * пункту тратится при каждом применении — тогда {@code durabilityCostChance} можно
 * оставить дефолтным 1.0).
 */
public record TotemSkill(
        String nameKey,
        List<String> tags,
        Map<String, Float> stats,
        int durabilityCost,
        float durabilityCostChance,
        List<com.idk.biomegetter.skill.SkillComponent> components
) {
    public static final Codec<TotemSkill> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(TotemSkill::nameKey),
            Codec.STRING.listOf().fieldOf("tags").forGetter(TotemSkill::tags),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("stats", Map.of()).forGetter(TotemSkill::stats),
            Codec.INT.optionalFieldOf("durability_cost", 1).forGetter(TotemSkill::durabilityCost),
            Codec.FLOAT.optionalFieldOf("durability_cost_chance", 1.0f).forGetter(TotemSkill::durabilityCostChance),
            com.idk.biomegetter.skill.SkillComponent.CODEC.listOf().fieldOf("components").forGetter(TotemSkill::components)
    ).apply(instance, TotemSkill::new));

    /**
     * @param appliedRings все кольца, применённые TotemStatRingResolver#resolve на ЭТОМ этапе
     *                     (может быть несколько, суммарно не превышая бюджет уровня — см.
     *                     согласованное правило). Каждое кольцо обрабатывается независимо и
     *                     последовательно домножает уже накопленный результат предыдущих.
     */
    public Map<String, Float> computedStats(java.util.List<TotemStatRingResolver.ResolvedRing> appliedRings, java.util.Set<String> cauldronElements) {
        Map<String, Float> result = new java.util.HashMap<>(stats);
        for (TotemStatRingResolver.ResolvedRing resolved : appliedRings) {
            int modifier = ElementMatrixLoader.computeModifier(cauldronElements, resolved.element());
            if (modifier == 0) continue; // аннулировано согласно финальной формуле

            for (TotemStatModifier statMod : resolved.ring().modifiers()) {
                Float base = result.get(statMod.stat());
                if (base == null) continue; // у этого скилла нет такого стата — тихий пропуск

                float delta = statMod.multiplier() - 1.0f;
                float scaled = delta * Math.abs(modifier);
                float finalDelta = modifier > 0 ? scaled : -scaled;
                result.put(statMod.stat(), base * (1.0f + finalDelta));
            }
        }
        return result;
    }
}