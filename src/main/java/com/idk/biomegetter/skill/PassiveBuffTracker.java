package com.idk.biomegetter.skill;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

/**
 * Отслеживает, какие infinite mob-эффекты сейчас наложены НАШИМИ пассивками (по каждой
 * сущности), чтобы: (а) не переналагать эффект заново каждый тик без надобности, (б) СНЯТЬ
 * эффект в момент, когда источник (предмет с этой пассивкой) покинул руки — реализует
 * согласованный принцип "тотем выпал из руки -> убрали именно наш бафф, даже если чужой такой
 * же тут же встанет на его место снова". Хранится как простая in-memory карта (не персистится
 * между рестартами сервера — при перезаходе игрока эффект просто переналожится/снимется на
 * следующем тике по факту наличия/отсутствия тотема в руке, это не критично).
 */
public final class PassiveBuffTracker {

    private static final Map<UUID, Set<Holder<MobEffect>>> PENDING = new HashMap<>();  // накапливается в течение тика через markActive
    private static final Map<UUID, Set<Holder<MobEffect>>> ACTIVE = new HashMap<>();   // подтверждённое состояние с прошлого reconcile

    private PassiveBuffTracker() {
    }

    /**
     * Вызывается КАЖДЫЙ раз, когда источник (тотем в руке) применяет свою пассивку в этом тике.
     */
    public static void markActive(LivingEntity entity, Holder<MobEffect> effect) {
        PENDING.computeIfAbsent(entity.getUUID(), k -> new HashSet<>()).add(effect);
    }

    /**
     * Забирает и очищает накопленный за тик набор для этой сущности (вызывается координатором).
     */
    public static Set<Holder<MobEffect>> consumeThisTick(LivingEntity entity) {
        Set<Holder<MobEffect>> result = PENDING.remove(entity.getUUID());
        return result == null ? Set.of() : result;
    }

    /**
     * Снимает эффекты, которые были активны на прошлом тике, но не подтвердились в этом
     * (источник пропал из руки) — вызывается координатором один раз за тик игрока.
     */
    public static void reconcile(LivingEntity entity, Set<Holder<MobEffect>> confirmedThisTick) {
        UUID id = entity.getUUID();
        Set<Holder<MobEffect>> previous = ACTIVE.get(id);
        if (previous != null) {
            for (Holder<MobEffect> effect : previous) {
                if (!confirmedThisTick.contains(effect)) {
                    entity.removeEffect(effect);
                }
            }
        }
        if (confirmedThisTick.isEmpty()) {
            ACTIVE.remove(id);
        } else {
            ACTIVE.put(id, new HashSet<>(confirmedThisTick));
        }
    }
}