package com.idk.biomegetter.entity.custom.ally;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.Level;

public class AllySkeletonEntity extends Skeleton {

    public AllySkeletonEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return AllyMobs.isValidTarget(target) && super.canAttack(target);
    }

    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
        // намеренно ничего не делаем — союзная нежить не оставляет лут
    }
}
