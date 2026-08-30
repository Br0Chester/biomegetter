package com.idk.biomegetter.skill;

import java.util.List;

/**
 * Точка входа для запуска скилла (список компонентов) по конкретному событию. Сейчас
 * поддерживает только ON_CAST (все остальные триггеры — заглушки под следующие подэтапы:
 * projectile/raycast должны будут сами вызывать runTrigger(..., ON_HIT/ON_EXPIRE, ...) из
 * своей логики тика/попадания).
 */
public final class SkillComponentRunner {

    private SkillComponentRunner() {
    }

    public static void cast(List<SkillComponent> components, SkillCastContext context) {
        runTrigger(components, SkillTrigger.ON_CAST, context);
    }

    public static void runTrigger(List<SkillComponent> components, String trigger, SkillCastContext context) {
        for (SkillComponent component : components) {
            if (component.trigger().equals(trigger)) {
                component.run(context);
            }
        }
    }
}