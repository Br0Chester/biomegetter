package com.idk.biomegetter.block.custom.cauldron.data.totem;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Разрешает выбор СКОЛЬКИХ УГОДНО статовых колец за один вызов (один вызов = один этап варки
 * тотема), при условии, что СУММА их уровней (с учётом уже потраченного на предыдущих этапах
 * бюджета — {@code remainingBudget} передаётся вызывающим кодом и убывает между этапами) не
 * превышает лимит. Приоритет — по убыванию площади (число ячеек паттерна), как приближение к
 * "физической заметности"/"кольцо сверху" (согласованный Вариант A): крупная фигура визуально
 * перекрывает часть более мелкой, значит она "главнее" и проверяется первой. Клетки, занятые
 * уже принятым кольцом, при проверке следующих (более мелких) кандидатов считаются "подходят
 * всегда" — не мешают соседним меньшим кольцам занимать оставшееся место. Среди кандидатов
 * ОДИНАКОВОГО размера — случайный порядок перебора.
 */
public final class TotemStatRingResolver {

    public record ResolvedRing(Identifier id, TotemStatRing ring, String element, List<BlockPos> positions) {
    }

    private TotemStatRingResolver() {
    }

    /**
     * @return список применённых на этом этапе колец (может быть пустым, если ничего не
     * подошло) — вызывающий код обязан вычесть суммарный {@code ring.level()} всех
     * элементов результата из своего сквозного (между этапами) остатка бюджета.
     */
    public static List<ResolvedRing> resolve(Level level, BlockPos cauldronPos, int remainingBudget, RandomSource randomSource) {
        List<ResolvedRing> resolved = new ArrayList<>();
        if (remainingBudget <= 0) return resolved;

        Set<BlockPos> claimed = new HashSet<>();
        int budgetLeft = remainingBudget;

        Map<Integer, List<Map.Entry<Identifier, TotemStatRing>>> bySize = new TreeMap<>(Comparator.reverseOrder());
        for (Map.Entry<Identifier, TotemStatRing> entry : CauldronTotemStatRingLoader.all().entrySet()) {
            int size = entry.getValue().template().toCanonicalCells().map(List::size).orElse(0);
            if (size == 0) continue; // невалидный шаблон
            bySize.computeIfAbsent(size, k -> new ArrayList<>()).add(entry);
        }

        for (List<Map.Entry<Identifier, TotemStatRing>> sameSizeGroup : bySize.values()) {
            if (budgetLeft <= 0) break;

            List<Map.Entry<Identifier, TotemStatRing>> shuffled = new ArrayList<>(sameSizeGroup);
            for (int i = shuffled.size(); i > 1; i--) {
                Collections.swap(shuffled, i - 1, randomSource.nextInt(i));
            }

            for (Map.Entry<Identifier, TotemStatRing> candidate : shuffled) {
                if (budgetLeft <= 0) break;
                TotemStatRing ring = candidate.getValue();
                if (ring.level() > budgetLeft) continue; // превышает остаток — пропускаем, пробуем следующего кандидата

                Optional<List<BlockPos>> matchOpt = ring.template().matchIgnoringClaimed(level, cauldronPos, claimed);
                if (matchOpt.isEmpty()) continue; // физически не построено (с учётом уже занятых клеток)

                List<BlockPos> positions = matchOpt.get();
                Optional<String> elementOpt = ring.resolveElement(level, positions);
                if (elementOpt.isEmpty()) continue; // смешанный/неклассифицированный элемент — невалидно

                resolved.add(new ResolvedRing(candidate.getKey(), ring, elementOpt.get(), positions));
                claimed.addAll(positions);
                budgetLeft -= ring.level();
            }
        }

        return resolved;
    }
}