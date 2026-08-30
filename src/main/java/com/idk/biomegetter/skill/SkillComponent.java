package com.idk.biomegetter.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Один независимый "кусок" поведения скилла — скилл целиком (TotemSkill) состоит из СПИСКА
 * таких компонентов (см. обсуждение: "скилл — это множество компонентов, каждый делает что-то
 * своё"). Компонент реагирует ТОЛЬКО на свой trigger; в этом подэтапе реализован единственный
 * практический путь: on_cast + delivery(self) [+опционально areaShape] -> effect на каждую
 * найденную цель.
 */
public record SkillComponent(
        String trigger,
        Optional<SkillDelivery> delivery,
        Optional<AreaShape> areaShape,
        TotemSkillEffect effect
) {
    public static final Codec<SkillComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("trigger", SkillTrigger.ON_CAST).forGetter(SkillComponent::trigger),
            SkillDelivery.CODEC.optionalFieldOf("delivery").forGetter(SkillComponent::delivery),
            AreaShape.CODEC.optionalFieldOf("area_shape").forGetter(SkillComponent::areaShape),
            TotemSkillEffect.CODEC.fieldOf("effect").forGetter(SkillComponent::effect)
    ).apply(instance, SkillComponent::new));

    /**
     * Вызывается SkillComponentRunner при наступлении события trigger.
     */
    public void run(SkillCastContext context) {
        List<SkillTarget> baseTargets = delivery.map(d -> d.resolveTargets(context))
                .orElse(List.of(new SkillTarget.EntityTarget(context.caster())));

        for (SkillTarget target : baseTargets) {
            if (areaShape.isPresent()) {
                for (SkillTarget affected : areaShape.get().findAffected(context, target)) {
                    effect.apply(context, affected);
                }
            } else {
                effect.apply(context, target);
            }
        }
    }
}