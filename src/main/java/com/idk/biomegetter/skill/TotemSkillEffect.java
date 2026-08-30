package com.idk.biomegetter.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * ОТКРЫТЫЙ интерфейс эффекта — НЕ sealed, сторонние моды регистрируют свои реализации через
 * register(...) из своего ModInitializer, без Mixin. Теперь apply() получает КОНКРЕТНУЮ цель
 * (найденную SkillDelivery/AreaShape), а не всегда самого применяющего игрока — это позволяет
 * одному эффекту работать и как self-баф на кастера, и как урон по врагу в area, и как хил по
 * союзнику в радиусе — в зависимости от того, что нашёл delivery/area, а не от самого эффекта.
 */
public interface TotemSkillEffect {

    Map<String, MapCodec<? extends TotemSkillEffect>> REGISTRY = new HashMap<>();

    Codec<TotemSkillEffect> CODEC = Codec.STRING.dispatch("type", TotemSkillEffect::typeId, type -> {
        MapCodec<? extends TotemSkillEffect> codec = REGISTRY.get(type);
        if (codec == null) {
            throw new IllegalArgumentException("Unknown skill effect type: " + type
                    + " (registered types: " + REGISTRY.keySet() + ")");
        }
        return codec;
    });

    static void register(String type, MapCodec<? extends TotemSkillEffect> codec) {
        REGISTRY.put(type, codec);
    }

    String typeId();

    /**
     * @param target конкретная найденная цель — EntityTarget ИЛИ BlockTarget (см. SkillTarget).
     *               Эффект сам проверяет instanceof и совместимость; несовместимый тип цели —
     *               просто no-op (ответственность автора датапака за осмысленные комбинации
     *               delivery/area_shape/effect).
     */
    void apply(SkillCastContext context, SkillTarget target);
}