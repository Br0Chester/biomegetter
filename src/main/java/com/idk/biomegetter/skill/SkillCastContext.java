package com.idk.biomegetter.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Разделяемое состояние ОДНОГО применения скилла — все компоненты этого скилла (список в
 * TotemSkill.components) получают ОДИН И ТОТ ЖЕ экземпляр контекста, что позволяет им
 * координироваться друг с другом (например, компонент А проверяет spawnedThisCast, чтобы
 * узнать, кого уже призвал компонент Б в рамках этого же применения — см. пример с "волнами,
 * встречающими друг друга" из обсуждения). Не привязан к тотему — годится для любого предмета,
 * использующего эту систему скиллов.
 */
public final class SkillCastContext {
    private final ServerLevel level;
    private final Player caster;
    private final Map<String, Float> stats;
    private final int durabilityRemaining;
    private final List<Entity> spawnedThisCast = new ArrayList<>();

    public SkillCastContext(ServerLevel level, Player caster, Map<String, Float> stats, int durabilityRemaining) {
        this.level = level;
        this.caster = caster;
        this.stats = stats;
        this.durabilityRemaining = durabilityRemaining;
    }

    public ServerLevel level() {
        return level;
    }

    public Player caster() {
        return caster;
    }

    public Map<String, Float> stats() {
        return stats;
    }

    public float stat(String key, float fallback) {
        return stats.getOrDefault(key, fallback);
    }

    public int durabilityRemaining() {
        return durabilityRemaining;
    }

    public List<Entity> spawnedThisCast() {
        return spawnedThisCast;
    }
}