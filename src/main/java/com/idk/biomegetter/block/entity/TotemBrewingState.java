package com.idk.biomegetter.block.entity;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Состояние варки тотема — полностью изолировано от обычного brewing/soup (аналогично тому,
 * как соп-варка не завязана на CauldronRecipe). Живёт параллельно с обычными brewing-полями
 * ModCauldronBlockEntity, они взаимоисключающие (не может одновременно вариться и суп/рецепт,
 * и тотем).
 */
public record TotemBrewingState(
        String material,           // "organic" / "metal" (= SolidComponentType.group() стартовых компонентов)
        int level,
        int stage,                   // 0 = сборка пассивки, 1 = сборка активки
        boolean cooking, // true = идёт автоматическая варка этапа (клики игнорируются), false = ждёт перемешивания
        int ticksRemaining, // таймер текущей фазы (cook или await-таймаут)// 0 = сборка пассивки, 1 = сборка активки
        int ritualBudgetRemaining, // сквозной бюджет уровней ритуальных колец на всю варку
        List<Identifier> collectedTags, // накопленные component-id, брошенные на ТЕКУЩЕМ этапе
        Optional<Identifier> forcedSkill, // показанный кликом тотем — id его скилла для текущего этапа
        Optional<Identifier> chosenPassiveSkill,
        Map<String, Float> passiveStats,
        int emptyStageCount
) {
}