package com.idk.biomegetter.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Определяет, КАКИЕ цели найдены для дальнейшей обработки (AreaShape, если задан, либо сразу
 * effect). ОТКРЫТЫЙ реестр — тот же принцип, что и TotemSkillEffect. Сейчас реализован только
 * SELF; block_under/projectile/raycast — следующие подэтапы (см. обсуждение).
 */
public interface SkillDelivery {

    Map<String, MapCodec<? extends SkillDelivery>> REGISTRY = new HashMap<>();

    Codec<SkillDelivery> CODEC = Codec.STRING.dispatch("type", SkillDelivery::typeId, type -> {
        MapCodec<? extends SkillDelivery> codec = REGISTRY.get(type);
        if (codec == null) {
            throw new IllegalArgumentException("Unknown skill delivery type: " + type
                    + " (registered types: " + REGISTRY.keySet() + ")");
        }
        return codec;
    });

    static void register(String type, MapCodec<? extends SkillDelivery> codec) {
        REGISTRY.put(type, codec);
    }

    String typeId();

    /**
     * @return начальные точки — SELF даёт EntityTarget(caster), BLOCK_UNDER даёт
     * BlockTarget(клетка под ногами caster'а). Для будущих projectile/raycast это
     * будет асинхронно (после полёта снаряда) — тогда данный метод не будет
     * использоваться напрямую, а delivery получит собственную ветку обработки в
     * SkillComponentRunner (следующий подэтап).
     */
    List<SkillTarget> resolveTargets(SkillCastContext context);

    record Self() implements SkillDelivery {
        public static final MapCodec<Self> CODEC = com.mojang.serialization.MapCodec.unit(Self::new);

        @Override
        public String typeId() {
            return "self";
        }

        @Override
        public List<SkillTarget> resolveTargets(SkillCastContext context) {
            return List.of(new SkillTarget.EntityTarget(context.caster()));
        }
    }

    /**
     * "Просто способ передать координату" (см. обсуждение) — клетка под ногами применяющего.
     * Дальнейшая логика (форма зоны, что происходит с блоками) — целиком через AreaShape/effect,
     * этот delivery не содержит никакой собственной механики кроме указания точки.
     */
    record BlockUnder() implements SkillDelivery {
        public static final MapCodec<BlockUnder> CODEC = com.mojang.serialization.MapCodec.unit(BlockUnder::new);

        @Override
        public String typeId() {
            return "block_under";
        }

        @Override
        public List<SkillTarget> resolveTargets(SkillCastContext context) {
            return List.of(new SkillTarget.BlockTarget(context.caster().blockPosition().below()));
        }
    }

    static void registerAll() {
        register("self", Self.CODEC);
        register("block_under", BlockUnder.CODEC);
    }
}