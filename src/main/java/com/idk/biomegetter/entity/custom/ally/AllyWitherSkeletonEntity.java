package com.idk.biomegetter.entity.custom.ally;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.level.Level;

public class AllyWitherSkeletonEntity extends WitherSkeleton implements SummonedAlly {

    private final AllyLifetime lifetime = new AllyLifetime(this);

    public AllyWitherSkeletonEntity(EntityType<? extends WitherSkeleton> type, Level level) {
        super(type, level);
    }

    @Override
    public void setLifetimeTicks(int ticks) {
        this.lifetime.set(ticks);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.lifetime.tick();
        }
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