package com.idk.biomegetter.skill;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/**
 * Обобщённая "цель" — либо сущность, либо позиция блока. AreaShape/TotemSkillEffect работают
 * через этот интерфейс, а не жёстко через LivingEntity — конкретный эффект сам решает, какой
 * подтип ему нужен (через instanceof), несовместимые комбинации просто ничего не делают (это
 * ответственность автора датапака — не смешивать эффект-для-сущностей с block_under delivery).
 */
public sealed interface SkillTarget {
    record EntityTarget(LivingEntity entity) implements SkillTarget {
    }

    record BlockTarget(BlockPos pos) implements SkillTarget {
    }
}